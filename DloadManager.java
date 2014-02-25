/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package p2pfilesharing;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is to listen for obtain request from peers and start sender thread
 * to send file to another peer
 *
 * @author fk
 */
public class DloadManager extends Thread {
    private int port;
    private boolean isRunning;
    private ServerSocket serverSocket;
    private String localDir;

    public DloadManager(int port, String localDir) {
        try {
            this.port = port;
            this.serverSocket = new ServerSocket(port);
            this.localDir = localDir;
        } catch (IOException ex) {
            System.err.println("Download manager cannot listen on port: " + port);
            //Logger.getLogger(DloadManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        try {
            isRunning = true;
            while (isRunning) {
                try {
                    Socket socket = serverSocket.accept();
                    Sender sender = new Sender(socket, localDir);
                    sender.start();
                } catch (SocketException socketException) {
                    System.err.println("Download manager fails to accept");
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(DloadManager.class.getName()).log(Level.SEVERE, null, ex);
            this.isRunning = false;
        }
    }

    public void close() {
        try {
            isRunning = false;
            serverSocket.close();
        } catch (IOException ex) {
            Logger.getLogger(DloadManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
