/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package p2pfilesharing;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 *
 * @author fk
 */
public class Transfer {

    public static Message recvMsg(InputStream is) {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        Gson gson = new Gson();
        return gson.fromJson(br, Message.class);
    }

    public static void sendMsg(Message msg, OutputStream os) {
        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
            Gson gson = new Gson();
            gson.toJson(msg, bw);
            bw.newLine();
            bw.flush();
        } catch (JsonIOException jsonIOException) {
        } catch (IOException iOException) {
        }
    }
}
