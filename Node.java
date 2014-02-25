/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package p2pfilesharing;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.uncommons.maths.random.ExponentialGenerator;

/**
 *
 * @author nliu8
 */
public class Node {

    public static int TOPOLOGY = 1; // 0 for star and 1 for 2D-mesh
    public static int MODE = 0; // 0 for PUSH, 1 for lazy-PULL
    public static int NUM_OF_FILE = 10; // how many files are generated in one node
    public static int WAIT_TIME_BEFORE_CONNECT = 5000; // wait 5 seconds before starting connection
    public static boolean TEST_MODE = false;
    public static String port_S;
    public static int port_I;
    public static int node_ID_NO;
    public static String myId;
    public static String localDir;
    public static int numNeighbor;
    public static final ArrayList<LinkedBlockingQueue<Message>> msgQueues = new ArrayList<LinkedBlockingQueue<Message>>();
    public static final ArrayList<String> allPeers = new ArrayList<String>();
    public static final Map<String, Object> fileMap = new ConcurrentHashMap<String, Object>();
    public static final Set<String> timeSeq = new HashSet<String>();
    public static final Map<String, P2PFile> fileList = new ConcurrentHashMap<String, P2PFile>();

    /**
     * obtain function, will be called when user typed obtain in the command
     * line
     *
     * @param fileName
     * @param ip
     * @param port
     */
    public static void obtain(String fileName, String ip, String port) {
        System.out.println("Downloading from " + ip + ":" + port + " ...");
        // create a Receiver to get file from another peer
        Receiver recvr = new Receiver(ip, port, fileName, localDir);
        recvr.start();
    }

    /**
     * search function, will be called when user typed search in the command
     * line
     *
     * @param fileName
     */
    public static void search(String fileName, int messageId) {
        fileMap.clear();
        Message msg = new Message(Command.SEARCH, messageId, myId, null, fileName);
        msg.pushID(myId);
        // insert msg to queue in every peer
        for (int i = 0; i < numNeighbor; i++) {
            msgQueues.get(i).add(msg);
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 0) {
            System.out.println("args is " + args[0]);
        }

        if (args.length != 0) {
            node_ID_NO = Integer.parseInt(args[0]);
        }

        // num of nodes
        node_ID_NO = Parser.get_node_id();
        // get neighbors and self, 0 for the second para means star and 1 means 2D-mesh
        allPeers.addAll(Parser.configure(node_ID_NO, TOPOLOGY));
        // self ID
        myId = allPeers.get(0);
        // neighbors IDs
        numNeighbor = Parser.getNumItems(allPeers);
        localDir = myId.substring(myId.lastIndexOf('/') + 1);

        port_S = Parser.get_port_number(allPeers.get(0));
        port_I = Integer.parseInt(port_S);

        for (int i = 0; i < numNeighbor; i++) {
            LinkedBlockingQueue<Message> msgQueue = new LinkedBlockingQueue<Message>();
            msgQueues.add(msgQueue);
        }


        // start server: number of server and port NO is based on configure. example 1
        //System.out.println("Index Server is Running...");
        Thread serverThread = new Thread() {
            @Override
            public void run() {
                try {
                    System.out.println("Starting server thread");
                    Server server = new Server(port_I, msgQueues, allPeers, fileMap, timeSeq, fileList);
                    server.init();
                    server.start();
                } catch (IOException ex) {
                    //Logger.getLogger(Node.class.getName()).log(Level.SEVERE, null, ex);
                    System.err.println("Server thread fails and exits");
                }
            }
        };
        serverThread.start();


        // start VersionNO Checker thread
        Thread versionNOCheckerThread = new Thread() {
            @Override
            public void run() {
                System.out.println("***************************");
                System.out.println("Starting Version NO Checker");
                VersionNOChecker checker = new VersionNOChecker(port_I + 6000, fileList);
                checker.start();
            }
        };
        versionNOCheckerThread.start();

        // get connect command from user
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        String fromUser;
        ArrayList<Thread> peerThreads = new ArrayList<Thread>();

        int cnt = 0;

        // wait 5 seconds and then start connection
        try {
            Thread.sleep(WAIT_TIME_BEFORE_CONNECT);
        } catch (InterruptedException ex) {
            Logger.getLogger(Node.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Peer is Running...");
        Thread peerThread;
        for (int i = 0; i < numNeighbor; i++) {
            String neighborId = allPeers.get(i + 1);
            int neighborServerPort = Integer.parseInt(neighborId.substring(neighborId.lastIndexOf('/') + 1));
            String localId = allPeers.get(0);
            int localServerPort = Integer.parseInt(localId.substring(localId.lastIndexOf('/') + 1));
            Peer peer = new Peer(neighborServerPort, localServerPort, msgQueues.get(cnt++));
            peer.init();
            if (i == 0) {
                peer.startDloadManager();
            }
            peerThread = new Thread(peer);
            peerThread.start();
            peerThreads.add(peerThread);
        }
        System.out.println("peer thread started!");
        
        // start auto invalidater thread
        Thread autoInvalidaterThread = new Thread() {
            @Override
            public void run() {
                System.out.println("*******************************");
                System.out.println("Starting autoInvalidater thread");

                Random rng = new Random();
                double lamda = 0.8;
                ExponentialGenerator exp;
                exp = new ExponentialGenerator(lamda, rng);

                AutoInvalidater invalidater = new AutoInvalidater(exp);
            }
        };
        autoInvalidaterThread.start();

        // node process is just like old peer
        Random rand = new Random(System.currentTimeMillis());
        // parse user input
        System.out.print("Input: ");
        try {
            while ((fromUser = stdIn.readLine()) != null) {
                if (fromUser.equalsIgnoreCase("OBTAIN")) {
                    System.out.print("Filename: ");
                    String fileName = stdIn.readLine();
                    System.out.print("Ip: ");
                    String ip = stdIn.readLine();
                    System.out.print("Port: ");
                    String port = stdIn.readLine();
                    System.out.println("Obtaining " + fileName + " from " + ip + ":" + port + " ...");
                    // put obtain command to the producer consumer queue of corresponding thread
                    obtain(fileName, ip, port);
                } else if (fromUser.equalsIgnoreCase("SEARCH")) {
                    System.out.print("Filename: ");
                    String fileName = stdIn.readLine();
                    // put search command to the producer consumer queue 
                    search(fileName, Math.abs(rand.nextInt()));
                } else if (fromUser.equalsIgnoreCase("EXIT")) {
                    stdIn.close();
                    break;
                } else if (fromUser.equalsIgnoreCase("TEST")) {
                    System.out.print("Number of requests: ");
                    int num = Integer.parseInt(stdIn.readLine());
                    // put test command to producer consumer queue
                    test(num);
                } else if (fromUser.equalsIgnoreCase("REFRESH")) {
                    refresh();
                } else {
                    System.out.println("Only SEARCH, OBTAIN, REFRESH and EXIT suppport, please try again");
                    System.out.print("Input: ");
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Called when user type in test to test
     *
     * @param num the number of requests need to be tested
     */
    private static void test(int num) {
        System.out.println("Start performance test, please be patient ...");
        long startTime;
        startTime = System.nanoTime();
        Random rand = new Random(startTime);
        String ports[] = {"4445", "4446"};
        String files[] = {"3", "7", "9"};
        timeSeq.clear();
        int searchCnt = 0;
        for (int i = 0; i < num; i++) {
            if (i % 3 != 0) {
                searchCnt++;
                String fileName = ports[rand.nextInt(2)] + "_" + files[rand.nextInt(3)];
                //System.out.println("Searching for " + fileName + "........");
                search(fileName, Math.abs(rand.nextInt()));
            } else {
                refresh();
            }
            try {
                int sleepTime = 1000;
                Thread.sleep(sleepTime);
            } catch (InterruptedException ex) {
                Logger.getLogger(Node.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        System.out.println("Number of hit query received: " + timeSeq.size());
        System.out.println("Valid msg percentage after " + searchCnt + " times of search: " + (double) timeSeq.size() / searchCnt / 4.0);
//        if (Node.MODE == 0) {
//            // random query
//            for (int i = 0; i < num; i++) {
//                String fileName = Integer.toString(4441 + rand.nextInt() % 10) + "_" + Integer.toString(rand.nextInt() % 10);
//                search(fileName, Math.abs(rand.nextInt()));
//            }
//
//            // wait for result
//            try {
//                int sleepTime = (num * 100 < 3000) ? 3000 : num * 100;
//                Thread.sleep(sleepTime);
//            } catch (InterruptedException ex) {
//                Logger.getLogger(Node.class.getName()).log(Level.SEVERE, null, ex);
//            }
//
//            // random download 
//            for (int i = 0; i < num; i++) {
//                String fileName = Integer.toString(4441 + rand.nextInt() % 10) + "_" + Integer.toString(rand.nextInt() % 10);
//                obtain(fileName, "localhost", Integer.toString(4441 + rand.nextInt() % 10));
//            }
//
//            // random refresh
//            for (int i = 0; i < num; i++) {
//                if (rand.nextInt(2) == 0) {
//                    refresh();
//                }
//            }
//
//            long totalTime = 0;
//            Iterator iter = timeSeq.iterator();
//            while (iter.hasNext()) {
//                long timeStamp = Long.parseLong((String) iter.next());
//                totalTime += timeStamp - startTime;
//            }
//
//            if (!timeSeq.isEmpty()) {
//                PrintWriter writer = null;
//                try {
//                    double averageTime = (double) totalTime / timeSeq.size() / num / 1000000;
//                    String fileName = "src/configure/perf_" + Parser.get_port_number(myId) + ".txt";
//                    writer = new PrintWriter(fileName, "UTF-8");
//                    writer.println(timeSeq.size());
//                    writer.println(averageTime + " ms ");
//                    writer.close();
//                } catch (FileNotFoundException ex) {
//                    Logger.getLogger(Node.class.getName()).log(Level.SEVERE, null, ex);
//                } catch (UnsupportedEncodingException ex) {
//                    Logger.getLogger(Node.class.getName()).log(Level.SEVERE, null, ex);
//                } finally {
//                    writer.close();
//                }
//            } else {
//                System.out.println("file not found");
//            }
//            timeSeq.clear();
//        } else {
//        }
    }

    /**
     * Called when user types in REFRESH, it obtains the latest version from the
     * owner for all invalid files
     */
    private static void refresh() {
        Set<String> fileNames = fileList.keySet();
        for (String fileName : fileNames) {
            P2PFile file = fileList.get(fileName);
            if (file.getValidState() == FileValidState.INVALID) {
                obtain(fileName, Parser.get_ip_string(file.getOwnerId()), Parser.get_port_number(file.getOwnerId()));
            }
        }
    }
}
