/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package p2pfilesharing;

import com.google.gson.internal.LinkedTreeMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import org.apache.commons.io.FileUtils;

/**
 * This is class is to download file from another peer
 *
 * @author fk
 */
public class Receiver extends Thread {

    private String senderIp;
    private String senderPort;
    private String fileName;
    private String localDir;

    public Receiver(String ip, String port, String fileName, String localDir) {
        this.senderIp = ip;
        this.senderPort = port;
        this.fileName = fileName;
        this.localDir = localDir;
        System.out.println("Receiver at: " + localDir);
    }

    @Override
    public void run() {
        try {
            int port = Integer.parseInt(senderPort);
            Socket socket = new Socket(senderIp, port + 5000);
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();
            // send file name out first
            System.out.println("Ask for " + fileName);
            Message msg = new Message();
            msg.setCmd(Command.OBTAIN);
            msg.setBody(fileName);
            Transfer.sendMsg(msg, os);
            // save content to file
            //FileUtils.copyInputStreamToFile(is, new File(localDir + "/" + fileName));
            PrintWriter pw = new PrintWriter(localDir + "/" + fileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String str = br.readLine();
            pw.write(str);
            pw.flush();
            pw.close();
            
            // update fileList
            msg = Transfer.recvMsg(is);
            is = socket.getInputStream();
            if (msg != null) {
                int validState = ((Double) ((LinkedTreeMap) msg.getBody()).get("validState")).intValue();
                int ttrState = ((Double) ((LinkedTreeMap) msg.getBody()).get("ttrState")).intValue();
                int versionNum = ((Double) ((LinkedTreeMap) msg.getBody()).get("versionNum")).intValue();
                String ownerId = (String) ((LinkedTreeMap) msg.getBody()).get("ownerId");
                P2PFile file = new P2PFile(fileName, validState, ttrState, versionNum, ownerId);
                Server.fileList.put(fileName, file);
                System.out.println("fileList: " + Server.fileList);
            } else {
                System.out.println("Did not receive file info");
            }
            is.close();
            os.close();
            socket.close();
            System.out.println("Done");
        } catch (UnknownHostException ex) {
            System.err.println("Cannot find the sender: " + senderIp);
            //Logger.getLogger(Receiver.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            System.err.println("Cannot connect the sender: " + senderIp + ":" + senderPort);
            //Logger.getLogger(Receiver.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
