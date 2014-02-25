/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package p2pfilesharing;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Kun
 */
public class RandFile {
    public static void generateRandFile(String fileName) {
        try {
            PrintWriter writer = new PrintWriter(fileName, "UTF-8");
            String content = fileName.toUpperCase();
            writer.println(content);
            writer.close();
            writer.flush();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(RandFile.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(RandFile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
