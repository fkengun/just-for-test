/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package p2pfilesharing;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author fk
 */
public class QueueTest {

    private static LinkedBlockingQueue<Message> queue = new LinkedBlockingQueue<Message>();

    public static void main(String[] args) {
        Thread consumerThr = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Message msg = new Message(Command.OK, 1, "hello");
                        queue.put(msg);
                        System.out.println("msg inserted");
                        Thread.sleep(3000);
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(QueueTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        consumerThr.start();

        Thread producerThr = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Message msg = queue.take();
                        System.out.println("Message received: " + msg.cmd + " " + msg.body);
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(QueueTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        producerThr.start();
    }
}
