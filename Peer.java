package p2pfilesharing;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
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

/**
 *
 * @author fk
 */
public class Peer implements Runnable {

    private String peerId;
    private Socket peerSocket;
    private int neighborServerPort;
    private int localServerPort;
    private String localDir;
    private static DloadManager manager = null;
    private LinkedBlockingQueue<Message> msgQueue;
    private boolean isRunning;
    private ArrayList<String> fileList;

    public Peer(int neighborServerPort, int localServerPort, LinkedBlockingQueue<Message> queue) {
        this.neighborServerPort = neighborServerPort;
        this.localServerPort = localServerPort;
        msgQueue = queue;
        this.peerSocket = new Socket();
        this.fileList = new ArrayList<String>();
        this.localDir = Integer.toString(localServerPort);
        // create directory for this peer
        File dir = new File(localDir);
        dir.mkdir();
    }

    /**
     * registry function, will be called when peer starts up or monitored folder
     * has changed
     *
     * @param peerId
     * @param fileNames
     */
    public void registry(String peerId, ArrayList<String> fileNames) {
        this.fileList = getFileNames(this.localDir);
    }

    public String getRandItem(Set<String> set) {
        int size = set.size();
        int item = new Random().nextInt(size);
        int i = 0;
        for (String obj : set) {
            if (i == item) {
                return obj;
            }
            i = i + 1;
        }

        return null;
    }

    /**
     * get a list of names of all the files in a directory
     *
     * @return
     */
    public ArrayList<String> getFileNames(String dir) {
        ArrayList fileNames = new ArrayList<String>();
        File directory = new File(dir);
        Iterator<File> iter = FileUtils.iterateFiles(directory, null, false);
        while (iter.hasNext()) {
            fileNames.add(iter.next().getName());
        }
        return fileNames;
    }

    /**
     * Auto update function on delete event
     *
     * @param fileName
     */
    public void delete(String fileName) {
        try {
            System.out.println("delete triggered");
            HashMap<String, Object> fileInfo = new HashMap<String, Object>();
            Set<String> id = new HashSet<String>();
            id.add(peerId);

            fileInfo.put(fileName, id);

            System.out.println(fileInfo);

            Message msg = new Message(Command.DELETE, 1, fileInfo);
            Transfer.sendMsg(msg, this.peerSocket.getOutputStream());
        } catch (IOException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Initiate the peer, connect with neighbors
     */
    public void init() {
        // connect with a neighbor to build the network
        try {
            this.peerSocket.connect(new InetSocketAddress("localhost", this.neighborServerPort), 10000);
            this.peerId = this.peerSocket.getLocalAddress() + "/" + this.localServerPort;
            // create directory for this peer
            File dir = new File(localDir);
            dir.mkdir();
            fileList = getFileNames(localDir);
        } catch (UnknownHostException ex) {
            System.err.println("Cannot access the server");
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            //Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("Cannot connect peer");
        }

        try {
            // registry when starts up
            registry(this.peerId, getFileNames(localDir));

            // setup file monitor
            FileSystemManager fsManager = VFS.getManager();
            FileObject listenDir = fsManager.resolveFile(new File(localDir).getAbsolutePath());
            DefaultFileMonitor defFileMonitor = new DefaultFileMonitor(new FileListener() {
                @Override
                public void fileCreated(FileChangeEvent fce) throws Exception {
                    //System.out.println(fce.getFile() + " created");
                    registry(peerId, getFileNames(localDir));
                }

                @Override
                public void fileDeleted(FileChangeEvent fce) throws Exception {
                    //System.out.println(fce.getFile() + " deleted");
                    registry(peerId, getFileNames(localDir));
                }

                @Override
                public void fileChanged(FileChangeEvent fce) throws Exception {
                    //System.out.println(fce.getFile() + " changed");
                    registry(peerId, getFileNames(localDir));
                }
            });
            defFileMonitor.setRecursive(false);
            defFileMonitor.addFile(listenDir);
            defFileMonitor.start();
            isRunning = true;
        } catch (FileSystemException ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void startDloadManager() {
        if (this.manager == null) {
            // create a peer socket and start download manager thread
            int listenPort = this.localServerPort + 5000;
            DloadManager manager = new DloadManager(listenPort, Integer.toString(this.localServerPort));
            manager.start();
            System.out.println(peerId + " is listening on " + listenPort + " for download request");
        }
    }

    @Override
    public String toString() {
        return this.peerId;
    }

    /**
     * Take message from queue and send
     */
    @Override
    public void run() {
        while (isRunning) {
            try {
                Message msg = msgQueue.take();
                //System.out.println(this.peerId + ": a msg send to " + this.neighborServerPort + ": " + msg);
                Transfer.sendMsg(msg, this.peerSocket.getOutputStream());
            } catch (IOException ex) {
                Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Only print at non test mode
     *
     * @param str
     */
    private void print(String str) {
        if (Node.TEST_MODE == false) {
            System.out.println(str);
        }
    }
}
