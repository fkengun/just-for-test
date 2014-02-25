/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package p2pfilesharing;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author fk
 */
public class P2pfilesharing {

    public static final ArrayList<LinkedBlockingQueue<Message>> msgQueues = new ArrayList<LinkedBlockingQueue<Message>>();
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        LinkedBlockingQueue<Message> queue1 = new LinkedBlockingQueue<Message>();
        LinkedBlockingQueue<Message> queue2 = new LinkedBlockingQueue<Message>();
        msgQueues.add(queue1);
        msgQueues.add(queue2);
        Message msg = new Message(Command.SEARCH, 0, "a");
        for (int i = 0; i < msgQueues.size(); i++) {
            msgQueues.get(i).add(msg);
        }
        System.out.println(msgQueues);
    }
}
