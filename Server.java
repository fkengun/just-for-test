package p2pfilesharing;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.vfs2.FileChangeEvent;
import org.apache.commons.vfs2.FileListener;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.impl.DefaultFileMonitor;

public class Server implements Runnable {

    public static int serverPort;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private static Map<String, Object> fileMap;
    public static ArrayList<LinkedBlockingQueue<Message>> msgQueues;
    public static String myId;
    private static ArrayList<String> allNeighbors = new ArrayList<String>();
    private static Set<String> timeSeq;
    private static Map<String, String> forwardedMsg = new ConcurrentHashMap<String, String>();
    public static Map<String, P2PFile> fileList;
    private Socket versionNOAskerSocket;

    public Server(int port, ArrayList<LinkedBlockingQueue<Message>> queues,
            ArrayList<String> allPeers,
            Map<String, Object> files,
            Set<String> time,
            Map<String, P2PFile> fList) {
        serverPort = port;
        msgQueues = queues;
        myId = allPeers.get(0);
        allNeighbors.addAll(allPeers);
        allNeighbors.remove(0);
        fileMap = files;
        timeSeq = time;
        fileList = fList;
        System.out.println("I'm " + myId + ", my neighbors are " + allNeighbors);
    }

    public Server(Socket socket) {
        this.clientSocket = socket;
    }

    /**
     * Initialize server thread, start directory monitoring
     */
    public void init() {
        try {

            final String localDir = Integer.toString(serverPort);
            // registry when starts up
            initFileList(localDir);
            // generate files for test
            generateFiles(localDir);

            // setup file monitor
            FileSystemManager fsManager = VFS.getManager();
            FileObject listenDir = fsManager.resolveFile(new File(localDir).getAbsolutePath());
            DefaultFileMonitor defFileMonitor = new DefaultFileMonitor(new FileListener() {
                @Override
                public void fileCreated(FileChangeEvent fce) throws Exception {
                    //System.out.println(fce.getFile() + " created");
                    updateFileList(fce.getFile().toString(), 0);
                }

                @Override
                public void fileDeleted(FileChangeEvent fce) throws Exception {
                    //System.out.println(fce.getFile() + " deleted");
                    updateFileList(fce.getFile().toString(), 1);
                }

                @Override
                public void fileChanged(FileChangeEvent fce) throws Exception {
                    //System.out.println(fce.getFile() + " changed");
                    //updateFileList(localDir);
                    invalidate(fce.getFile().toString());
                }
            });
            defFileMonitor.setRecursive(true);
            defFileMonitor.addFile(listenDir);
            defFileMonitor.start();
        } catch (FileSystemException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Start server thread listening
     *
     * @throws IOException
     */
    public void start() throws IOException {
        boolean listening = true;
        try {
            serverSocket = new ServerSocket(serverPort);
            while (listening) {
                new Thread(new Server(serverSocket.accept())).start();
                System.out.println("Server thread accepts a connection");
            }
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Server cannot listen on port: " + serverPort);
            System.exit(1);
        }
    }

    /**
     * Get a list of names of all the files in a directory
     *
     * @return
     */
    private void initFileList(String dir) {
//        // initiate fileList with files in downloaded directory
//        File directory = new File(dir);
//        Iterator<File> iter = FileUtils.iterateFiles(directory, null, false);
//        while (iter.hasNext()) {
//            P2PFile file = new P2PFile(iter.next().getName(), FileValidState.VALID, FileTTRState.TTR_EXPIRED, 0, null);
//            fileList.put(file.getFileName(), file);
//        }
        File directory = new File(dir + "/");
        for (File file : directory.listFiles()) {
            file.delete();
        }

        // initiate fileList with files in owned directory
        directory = new File(dir + "/owned/");
        Iterator<File> iter = FileUtils.iterateFiles(directory, null, false);
        while (iter.hasNext()) {
            P2PFile file = new P2PFile(iter.next().getName(), FileValidState.VALID, FileTTRState.TTR_EXPIRED, 0, myId);
            fileList.put(file.getFileName(), file);
        }

        System.out.println("fileList: " + fileList);
    }

    /**
     * Update file list
     *
     * @param fileChanged name of the file that gets changed
     * @param changeEvent 0 for create, 1 for delete, 2 for change
     */
    private void updateFileList(String fileChanged, int changeEvent) {
        // update files in downloaded directory
        String fileName = fileChanged.substring(fileChanged.lastIndexOf("/") + 1);
        if (changeEvent == 0) {
            // new file in owned directory
            if (fileChanged.contains("owned")) {
                // add new file only when it is in "owned" directory
                P2PFile file = new P2PFile(fileName, FileValidState.VALID, FileTTRState.TTR_EXPIRED, 0, myId);
                fileList.put(fileName, file);
            } else {
                // new file in download directory, calculate owner from fileName
                // it should be done when the file is downloaded from other peer, not here
//                String ownerId = "127.0.0.1/" + fileName.substring(0, fileName.lastIndexOf("_"));
//                P2PFile file = new P2PFile(fileName, FileValidState.VALID, FileTTRState.TTR_EXPIRED, 0, ownerId);
//                fileList.put(fileName, file);
            }
        } else if (changeEvent == 1) {
            // file deleted from owned directory
            fileList.remove(fileName);
        }

        System.out.println("fileList: " + fileList);
    }

    /**
     * Called when a file is modified to invalidate downloaded copies on other
     * peers
     *
     * @param fileName file that is modified by the owner
     */
    private void invalidate(String fullFileName) {
        if (Node.MODE == 0) {
            // only trigger invalidate msg when file in "owned" directory is changed
            if (!fullFileName.contains("owned")) {
                return;
            }

            // increment version number
            String fileName = fullFileName.substring(fullFileName.lastIndexOf("/") + 1);
            P2PFile file = fileList.get(fileName);
            file.incrementVersionNum();
            fileList.put(fileName, file);

            long startTime = System.nanoTime();
            Random rand = new Random(startTime);
            Message msg = new Message(Command.INVALID, Math.abs(rand.nextInt()), myId, null, fileName);
            msg.pushID(myId);
            // insert msg to queue in every peer
            for (int i = 0; i < msgQueues.size(); i++) {
                msgQueues.get(i).add(msg);
            }
        } else {
            // only trigger invalidate msg when file in "owned" directory is changed
            if (!fullFileName.contains("owned")) {
                return;
            }

            // increment version number
            String fileName = fullFileName.substring(fullFileName.lastIndexOf("/") + 1);
            P2PFile file = fileList.get(fileName);
            file.incrementVersionNum();
            fileList.put(fileName, file);
        }
    }

//    private void autoInvalidate(String fullFileName) {
//        
//        System.out.println("Automatic invalidate triggered! *** *** *** ");
//
//        // only trigger invalidate msg when file in "owned" directory is changed
//        if (!fullFileName.contains("owned")) {
//            return;
//        }
//
//        // increment version number
//        String fileName = fullFileName.substring(fullFileName.lastIndexOf("/") + 1);
//        P2PFile file = fileList.get(fileName);
//        file.incrementVersionNum();
//        fileList.put(fileName, file);
//
//        long startTime = System.nanoTime();
//        Random rand = new Random(startTime);
//        Message msg = new Message(Command.INVALID, Math.abs(rand.nextInt()), myId, null, fileName);
//        msg.pushID(myId);
//        // insert msg to queue in every peer
//        for (int i = 0; i < msgQueues.size(); i++) {
//            msgQueues.get(i).add(msg);
//        }
//        
//    }
    /**
     * Search for the file in local, ignore file with non valid state
     *
     * @param fileSearched file that searched for
     */
    private boolean searchInLocal(String fileSearchedName) {
        if (Node.MODE == 0) {
            //System.out.println("IN push mode, search ***");
            boolean foundInLocal = false;
            if (fileList.containsKey(fileSearchedName)) {
                P2PFile fileSearched = fileList.get(fileSearchedName);
                if (fileSearched.getValidState() == FileValidState.VALID) {
                    foundInLocal = true;
                } else {
//                    System.out.println(myId + ": " + fileSearchedName + " is invalid, pretend to not have it");
                }
            } else {
//                System.out.println(myId + ": Don't have " + fileSearchedName + " at all");
            }

            return foundInLocal;
        } else {
            //System.out.println("IN pull mode, search ***");
            // mode 1, lazy pull
            boolean foundInLocal = false;
            if (fileList.containsKey(fileSearchedName)) {
                P2PFile fileSearched = fileList.get(fileSearchedName);
                // check stale bit
                if (fileSearched.getValidState() == FileValidState.VALID) {
                    // check TTR bit
                    if (fileSearched.getTtrState() == FileTTRState.TTR_UPTODATE) {
                        foundInLocal = true;
                    } else {
                        // TTR expire, send pull msg to check Version NO
                        // need to parse origin, ownerID

                        try {
                            this.versionNOAskerSocket = new Socket();
                            P2PFile file = fileList.get(fileSearchedName);
                            String ownerId = file.getOwnerId();
                            String ip = Parser.get_ip_string(ownerId);
                            String port = Parser.get_port_number(ownerId);
                            this.versionNOAskerSocket.connect(new InetSocketAddress(ip, Integer.valueOf(port) + 6000), 10000);
                            //System.out.println("asker connects with checker success");

                            Message msg;
                            msg = new Message(884700);
                            msg.setFileName(fileSearchedName);

                            Transfer.sendMsg(msg, this.versionNOAskerSocket.getOutputStream());

                            BufferedReader in = null;
                            InputStream is = versionNOAskerSocket.getInputStream();
                            in = new BufferedReader(new InputStreamReader(is));

                            //System.out.println("Starting receiving msg");

                            String inputLine;
                            inputLine = in.readLine();
                            Gson gson = new Gson();

                            //Transfer.sendMsg(msg, this.clientSocket.getOutputStream());
                            msg = gson.fromJson(inputLine, Message.class);
                            int currentVersionNO;
                            currentVersionNO = msg.getVersionNO();
                            //System.out.println("New version number received: " + msg.getVersionNO());

                            int localVersionNO;
                            localVersionNO = fileList.get(fileSearchedName).getVersionNum();

                            if (currentVersionNO != localVersionNO) {
                                foundInLocal = false;
                                // mark file valid status bit
                                fileList.get(fileSearchedName).setValidState(FileValidState.INVALID);
                            } else {
                                // reset TTR
                                foundInLocal = true;
                                TTRSetter ttrSetter;
                                int timeoutSeconds = 6;
                                ttrSetter = new TTRSetter(timeoutSeconds, fileList, fileSearchedName);
                                //System.out.println("TTR reset to " + timeoutSeconds);
                            }


                        } catch (UnknownHostException ex) {
                            System.err.println("Cannot access the version NO checker");
                        } catch (IOException ex) {
                            //Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
                            System.err.println("Asker cannot connect");
                        }
                    }
                } else {
                    //System.out.println(myId + ": " + fileSearchedName + " is invalid, pretend to not have it");
                }
            } else {
                //System.out.println(myId + ": Don't have " + fileSearchedName + " at all");
            }

            return foundInLocal;
        }
    }

    /**
     * Set the state of a file to INVALID
     *
     * @param fileInvalid
     */
    private void invalidateFile(String fileInvalidName) {
        if (fileList.containsKey(fileInvalidName) && !fileInvalidName.contains(Integer.toString(serverPort))) {
            P2PFile fileInvalid = fileList.get(fileInvalidName);
            fileInvalid.setValidState(FileValidState.INVALID);
            fileList.put(fileInvalidName, fileInvalid);
            //System.out.println(myId + ": " + fileInvalidName + " is invalidated");
        }
    }

    /**
     * Insert msg into msg queues of all peers except exceptQueueId
     *
     * @msg
     * @param exceptQueueId
     */
    private void insertMsgQueues(Message msg, int exceptQueueId) {
        for (int i = 0; i < msgQueues.size(); i++) {
            // do not forward back to prevHop
            if (i != exceptQueueId) {
                msgQueues.get(i).add(msg);
                //System.out.println(myId + ": msg forward to " + allNeighbors.get(i) + ": " + message);
            }
        }
    }

    /**
     * Generate files for test and simulation
     */
    private void generateFiles(String dir) {
        int i = 0;
        while (i < Node.NUM_OF_FILE) {
            String fileName = dir + "/owned/" + serverPort + "_" + i++;
            RandFile.generateRandFile(fileName);
        }
    }

    /**
     * Download files from neighbors automatically for simulation
     */
    private void autoDownloadFiles() {
        for (int i = 0; i < 10; i++) {
            try {
                Random rand = new Random();
                Thread.sleep(rand.nextInt(10) * rand.nextInt(10));
            } catch (InterruptedException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
            int index = i % allNeighbors.size();
            String neighborId = findNeighborFromIndex(index);
            String neighborPort = neighborId.substring(neighborId.lastIndexOf("/") + 1);
            String fileName = neighborPort + "_" + Integer.toString((Integer.valueOf(neighborPort) + i) % 10);
            System.out.println(fileName);
            Receiver recvr = new Receiver(Parser.get_ip_string(neighborId), Parser.get_port_number(neighborId), fileName, Integer.toString(serverPort));
            recvr.start();
        }
    }

    @Override
    public void run() {
        BufferedWriter out = null;
        BufferedReader in = null;
        int index;
        ArrayList<String> route;
        String prevHop;
        String fileSearched;

        try {
            // download some files from neighbors
            autoDownloadFiles();

            OutputStream os = clientSocket.getOutputStream();
            InputStream is = clientSocket.getInputStream();
            out = new BufferedWriter(new OutputStreamWriter(os));
            in = new BufferedReader(new InputStreamReader(is));

            Map<String, Object> receivedMap = new HashMap<String, Object>();

            Message message = new Message(Command.OK, 1, receivedMap);

            String inputLine;
            Command cmd;
            Gson gson = new Gson();

            while ((inputLine = in.readLine()) != null) {
                message = gson.fromJson(inputLine, Message.class);
                cmd = message.getCmd();

                switch (cmd) {
                    case REGISTER:
                        //System.out.println("Register from " + clientSocket.getRemoteSocketAddress());
                        receivedMap = (Map<String, Object>) message.getBody();
                        MapOperator.mergeTwoMaps(receivedMap, fileMap);
                        MapOperator.printMap(fileMap);
                        break;
                    case SEARCH:
                        fileSearched = (String) message.getBody();
                        route = message.getRoute();

                        prevHop = route.get(route.size() - 1);
                        index = findNeighborIndex(prevHop);
//                        System.out.println(myId + ": received a msg: " + message + ", from: " + prevHop);
                        synchronized (forwardedMsg) {
                            if (forwardedMsg.containsKey(Integer.toString(message.getMessageId()))) {
//                            System.out.println(myId + ": msg from " + prevHop + " has been forwarded before, ignore it");
                                break;
                            }
                        }

                        // search in local directory
                        boolean foundInLocal = searchInLocal(fileSearched);
                        if (!foundInLocal) {
//                            System.out.println(myId + ": " + fileSearched + " is not found in local of " + myId);
                        } else {
                            Message hitQuery = new Message();
                            hitQuery.setCmd(Command.RESULT);
                            hitQuery.setMessageId(message.getMessageId());
                            hitQuery.setFileName(fileSearched);
                            hitQuery.setRoute(route);
                            hitQuery.popID();
                            Set<String> peer = new HashSet<String>();
                            peer.add(myId);
                            Map<String, Object> sendMap = new HashMap<String, Object>();
                            sendMap.put(fileSearched, peer);
                            hitQuery.setBody(sendMap);
                            if (index != -1) {
                                msgQueues.get(index).add(hitQuery);
//                                System.out.println(myId + ": hitquery sent back to " + prevHop + ": " + hitQuery);
                            }
                        }

                        // decide if forward msg
                        if (message.decrementTTL() == true) {
                            // ignore dead msg
//                            System.out.println(myId + ": msg is dead and ignored");
                        } else {
                            // record this msg has been forwarded
                            synchronized (forwardedMsg) {
                                forwardedMsg.put(Integer.toString(message.getMessageId()), message.getSource());
                            }
                            // forward alive and non-duplicate msg
                            message.pushID(myId);
                            insertMsgQueues(message, index);
                        }
                        break;
                    case RESULT:
                        if (message.getRoute().isEmpty()) {
//                            System.out.println(myId + ": hitQuery comes back: " + message.getBody());
                            receivedMap = (Map<String, Object>) message.getBody();
                            synchronized (fileMap) {
                                MapOperator.mergeTwoMaps(receivedMap, fileMap);
                            }
                            synchronized (timeSeq) {
                                String peer = ((ArrayList<String>) receivedMap.values().iterator().next()).get(0);
                                timeSeq.add(message.getMessageId() + peer);
                            }
                            //System.out.println("Search results: " + fileMap);
                        } else {
                            String nextHop = message.popID();
                            index = findNeighborIndex(nextHop);
                            message.decrementTTL();
                            if (index != -1) {
                                msgQueues.get(index).add(message);
                            }
                        }
                        break;
                    case INVALID:
                        String fileInvalid = (String) message.getBody();
                        route = message.getRoute();

                        prevHop = route.get(route.size() - 1);
                        index = findNeighborIndex(prevHop);
                        //System.out.println(myId + ": received a msg: " + message + ", from: " + prevHop);
                        synchronized (forwardedMsg) {
                            if (forwardedMsg.containsKey(Integer.toString(message.getMessageId()))) {
                                //System.out.println(myId + ": msg from " + prevHop + " has been forwarded before, ignore it");
                                break;
                            }
                        }

                        invalidateFile(fileInvalid);

                        // decide if forward msg
                        if (message.decrementTTL() == true) {
                            // ignore dead msg
                            //System.out.println(myId + ": msg is dead and ignored");
                        } else {
                            // record this msg has been forwarded
                            synchronized (forwardedMsg) {
                                forwardedMsg.put(Integer.toString(message.getMessageId()), message.getSource());
                            }
                            // forward alive and non-duplicate msg
                            message.pushID(myId);
                            insertMsgQueues(message, index);
                        }
                        break;
                }
            }
        } catch (IOException ex) {
        } finally {
            try {
                out.close();
                in.close();
                clientSocket.close();
            } catch (IOException ex) {
                System.err.println("Close failed!");
            }
        }
    }

    /**
     * Find the index of the corresponding destination
     *
     * @param dest
     * @return -1: not found, otherwise: index
     */
    private int findNeighborIndex(String dest) {
        int index = 0;
        for (String neighbor : allNeighbors) {
            if (neighbor.equals(dest)) {
                return index;
            } else {
                index++;
            }
        }
        return -1;
    }

    /**
     * Find neighbor id from index in allNeighbors array
     *
     * @param index
     * @return
     */
    private String findNeighborFromIndex(int index) {
        return allNeighbors.get(index);
    }

    /**
     * Only print at non test mode
     *
     * @param str
     */
    private void print(String str) {
        //if (Node.TEST_MODE == false)
        System.out.println(str);
    }

    /**
     * Get version number of a file in fileList from file name
     *
     * @param fileSearched
     * @return -1 for file not found, others for version number
     */
    private int getVerNum(String fileSearched) {
        if (!fileList.containsKey(fileSearched)) {
            return -1;
        }
        P2PFile file = fileList.get(fileSearched);
        return file.getVersionNum();
    }

    private String getOwnerId(String fileSearched) {
        if (!fileList.containsKey(fileSearched)) {
            return null;
        }
        P2PFile file = fileList.get(fileSearched);
        return file.getOwnerId();
    }
}
