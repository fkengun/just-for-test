/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package p2pfilesharing;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;

/**
 * This class is to send a file to another peer
 * 
 * @author fk
 */
public class Sender extends Thread {
    private Socket clientSocket;
    private String localDir;
    
    public Sender(Socket sock, String localDir) {
        this.clientSocket = sock;
        this.localDir = localDir;
        System.out.println("Sender at: " + localDir);
    }

    @Override
    public void run() {
        try {
            InputStream is = clientSocket.getInputStream();
            OutputStream os = clientSocket.getOutputStream();
            byte[] bytes = new byte[1024];
            // get file name first
            Gson gson = new Gson();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            Message msg = gson.fromJson(in.readLine(), Message.class);
            String fileName = (String) msg.getBody();
            System.out.println("Sending " + fileName);
            // send file content
            File directory = new File(localDir + "/owned/");
            Iterator<File> iter = FileUtils.iterateFiles(directory, null, false);
            boolean isOwner = false;
            while (iter.hasNext()) {
                if (iter.next().getName().contains(fileName)) 
                    isOwner = true;
            }
            if (isOwner)
                FileUtils.copyFile(new File(localDir + "/owned/" + fileName), os);
            else
                FileUtils.copyFile(new File(localDir + "/" + fileName), os);
            os.flush();
            
            // send file info
            os = clientSocket.getOutputStream();
            msg = new Message();
            msg.setCmd(Command.INFO);
            msg.setBody(Server.fileList.get(fileName));
            Transfer.sendMsg(msg, os);
            os.flush();
            
            is.close();
            os.close();
            clientSocket.close();
            System.out.println("Done");
        } catch (IOException ex) {
            Logger.getLogger(Sender.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
