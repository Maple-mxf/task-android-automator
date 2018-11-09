package one.rewind.android.automator.test.call;

import org.junit.Test;

import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class TimerTest {


    @Test
    public void testTimerExecuteTask() throws IOException {
        Timer timer = new Timer(false);

        TimerTask task = new Task();

        timer.schedule(task, new Date());

        System.in.read();
    }

    class Task extends TimerTask {
        @Override
        public void run() {
            int i = 0;

            boolean flag = true;

            while (flag) {
                i++;
                System.out.println("i : " + i);
                if (i == 10000) {
                    flag = false;
                }
            }
        }
    }

    @Test
    public void testTimerLoopExecuteTask() throws IOException {
        Timer timer = new Timer(false);

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                System.out.println("定时执行!");
            }
        };

        timer.schedule(task, 0, 1000 * 2);

        System.in.read();
    }


    @Test
    public void testTimerTimingExecuteTask() {

    }
}
