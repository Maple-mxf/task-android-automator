package one.rewind.android.automator.test.call;

import org.junit.Test;

import java.io.IOException;
import java.util.Calendar;
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
        Timer timer = new Timer(false);

        TimerTask task = new TimerTask() {

            @Override
            public void run() {

            }
        };
        timer.schedule(task, buildDate(), 1000 * 60 * 60 * 24);
    }


    public static Date buildDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date time = calendar.getTime();

        if (time.before(new Date())) {
            return addDay(time, 1);
        }
        return time;
    }

    private static Date addDay(Date date, int days) {
        Calendar startDT = Calendar.getInstance();
        startDT.setTime(date);
        startDT.add(Calendar.DAY_OF_MONTH, days);
        return startDT.getTime();
    }

    @Test
    public void testInNextDayExecuteTask() throws IOException {
        Timer timer = new Timer(false);

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                System.out.println("hello world");
            }
        };
        timer.schedule(timerTask, new Date(), 2000);
    }
}
