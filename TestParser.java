/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package p2pfilesharing;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author nliu8
 */
public class TestParser {
    public static void main(String[] args) throws FileNotFoundException, IOException{
        //Parser parser = new Parser();
        
        ArrayList<String> myNeighbour = new ArrayList<String>();
        
        myNeighbour = Parser.configure(1,0);
        System.out.println(Parser.getNumItems(myNeighbour));
        
        System.out.println(Parser.get_node_id());
        
    }
}
