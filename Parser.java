/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package p2pfilesharing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 *
 * @author nliu8
 */
public class Parser {

    public static ArrayList<String> configure(int id, int topology) {

        BufferedReader br = null;
        ArrayList<String> configInfo = new ArrayList();
        String sCurrentLine = null;

        try {
            // topology == 0 means star, 1 means mesh
            if (topology == 0) {
                br = new BufferedReader(new FileReader("src/configure/config_star/config_" + id + ".txt"));
            } else {
                br = new BufferedReader(new FileReader("src/configure/config_mesh/config_" + id + ".txt"));
            }

            while ((sCurrentLine = br.readLine()) != null) {
                configInfo.add(sCurrentLine);
                //System.out.println(sCurrentLine);
            }

            System.out.println(new File(".").getAbsolutePath());

        } catch (IOException e) {
            System.out.println("Read file mistake!");
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ex) {
                System.out.println("Close file mistake!");
            }
        }
        return configInfo;
    }

    public static String get_ip_string(String ID) {
        int start = ID.indexOf("/");
        int end = ID.lastIndexOf("/");
        return ID.substring(start + 1, end);
    }

    public static String get_port_number(String ID) {
        int start = ID.lastIndexOf("/");

        return ID.substring(start + 1);
    }

    public static int getNumItems(ArrayList<String> A) {
        return (A.size() - 1);
    }

    public static int get_node_id() throws FileNotFoundException, IOException {
        BufferedReader br = null;
        br = new BufferedReader(new FileReader("src/configure/id.txt"));
        int counter = 0;
        String NOstring;

        String sCurrentLine = null;
        sCurrentLine = br.readLine();
        sCurrentLine.replaceAll("\n", " ");
        
        //System.out.println(Integer.parseInt(sCurrentLine));

        PrintWriter writer = new PrintWriter("src/configure/id.txt", "UTF-8");
        writer.println(Integer.parseInt(sCurrentLine)+1);
        writer.close();
        
        if (Integer.parseInt(sCurrentLine) >= 10 ){
            writer = new PrintWriter("src/configure/id.txt", "UTF-8");
            writer.println(1);
        }
        writer.close();

        return Integer.parseInt(sCurrentLine);
    }
}
