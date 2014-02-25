/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package p2pfilesharing;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ningliu
 */
class VersionNOChecker extends Thread {

    private int port;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private String filename;
    private static Map<String, P2PFile> fileList = new ConcurrentHashMap<String, P2PFile>();

    public VersionNOChecker(int port, Map<String, P2PFile> fList) {
        this.port = port;
        //this.serverSocket = new ServerSocket(port);
        this.fileList = fList;
    }

    private VersionNOChecker(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            // get file name from incoming msg and ack back with
            System.out.println("********************************");
            System.out.println("Version NO connection successful");

            // get file name and send back version NO

            BufferedReader in = null;
            InputStream is = clientSocket.getInputStream();
            in = new BufferedReader(new InputStreamReader(is));

            String inputLine;
            inputLine = in.readLine();
            Gson gson = new Gson();

            Message msg;
            msg = new Message(884700);
            // receive msg and based on file name retrive version NO
            msg = gson.fromJson(inputLine, Message.class);
            System.out.println(msg.getFileName());

            int VersionNO;
            VersionNO = fileList.get(msg.getFileName()).getVersionNum();
              
            // send back version no
            msg.setVersionNO(VersionNO);
            System.out.println("Msg received, Start sending message");
            Transfer.sendMsg(msg, this.clientSocket.getOutputStream());

        } catch (IOException ex) {
            Logger.getLogger(VersionNOChecker.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void start() {
        boolean listening = true;
        try {
            serverSocket = new ServerSocket(port);
            while (listening) {
                System.out.println("Checker thread is listening **** **** **** on " + port);
                new Thread(new VersionNOChecker(clientSocket = serverSocket.accept())).start();
                System.out.println("VersionNO checker thread accepts a connection");
            }
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Server cannot listen on port: " + port);
            System.exit(1);
        }
    }
}
