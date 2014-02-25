/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package p2pfilesharing;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author nliu8
 */
public class TTRSetter {

    Timer timer;
    public static Map<String, P2PFile> fileList;
    public static String filename;

    public TTRSetter(int seconds, Map<String, P2PFile> fList, String filename) {
        this.filename = filename;
        TTRSetter.fileList = fList;
        timer = new Timer();
        timer.schedule(new TTRSetterTask(), seconds * 1000);
    }

    class TTRSetterTask extends TimerTask {

        public void run() {
            fileList.get(filename).setTtrState(FileTTRState.TTR_EXPIRED);
            timer.cancel(); //Terminate the timer thread
        }
    }
}
