
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.uncommons.maths.random.ExponentialGenerator;

/**
 * Simple demo that uses java.util.Timer to schedule a task to execute once 5
 * seconds have passed.
 */
public class Reminder {

    Timer timer;
    double gap;
    ExponentialGenerator exp;
    //ExponentialGenerator rand = new ExponentialGenerator(0.000786d, new Random());

    public Reminder(int seconds) {
        timer = new Timer();
        timer.schedule(new RemindTask(), seconds * 1000);

    }

    private Reminder(double gap, ExponentialGenerator exp) {
        this.gap = gap;
        this.exp = exp;
        timer = new Timer();
        timer.schedule(new RemindTask(), (int) (exp.nextValue() * 1000));
    }

    class RemindTask extends TimerTask {

        public void run() {
            System.out.format("Time's up!%n");
            timer.cancel(); //Terminate the timer thread
            double interval = exp.nextValue();
            System.out.println("expo number is " + interval);
            timer = new Timer();
            timer.schedule(new RemindTask(), (long) (interval * 1000));
        }
    }

    public static void main(String args[]) {

        Random rng = new Random();
        double lamda = 0.4;
        ExponentialGenerator exp;
        exp = new ExponentialGenerator(lamda, rng);
        Reminder reminder;
        reminder = new Reminder(exp.nextValue(), exp);
        System.out.format("Task scheduled.%n");
    }
}
