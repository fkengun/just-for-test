/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package p2pfilesharing;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import org.uncommons.maths.random.ExponentialGenerator;

/**
 *
 * @author nliu8
 */
public class AutoInvalidater {
    
    Timer timer;
    ExponentialGenerator exp;
    
    public AutoInvalidater(ExponentialGenerator exp){
        this.exp = exp;
        
        timer = new Timer();
        timer.schedule(new AutoInvalidater.AutoTask(), 1000);
    }
    
    class AutoTask extends TimerTask {

        public void run() {

            timer.cancel(); //Terminate the timer thread
            timer = new Timer();
            double interval = exp.nextValue();

            //System.out.println("Automatic invalidate triggered! *** *** *** ");
            // increment version number
            // String fileName = fullFileName.substring(fullFileName.lastIndexOf("/") + 1);
            // compute reandom file name to invalidate
            Random rand = new Random();
            String fileName = Server.serverPort + "_" + Math.abs(rand.nextInt()) % 10;
            P2PFile file = Server.fileList.get(fileName);
            file.incrementVersionNum();
            Server.fileList.put(fileName, file);

            long startTime = System.nanoTime();
            rand = new Random(startTime);
            Message msg = new Message(Command.INVALID, Math.abs(rand.nextInt()), Server.myId, null, fileName);
            msg.pushID(Server.myId);
            // insert msg to queue in every peer
            for (int i = 0; i < Server.msgQueues.size(); i++) {
                Server.msgQueues.get(i).add(msg);
            }

            timer.schedule(new AutoTask(), (long) (interval* 1000));
        }
    }
    
}
