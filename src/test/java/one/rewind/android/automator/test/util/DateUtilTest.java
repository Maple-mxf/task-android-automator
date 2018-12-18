package one.rewind.android.automator.test.util;

import one.rewind.android.automator.util.DateUtil;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author maxuefeng[m17793873123@163.com]
 */
public class DateUtilTest {

    @Test
    public void testNextDay() {
        Date date = buildDate();
        Date var = new Date();
        long t1 = date.getTime();
        long t2 = var.getTime();
        long tmp = Math.abs(t1 - t2);
        System.out.println(tmp);
    }

    Date buildDate() {
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

    Date addDay(Date date, int days) {
        Calendar startDT = Calendar.getInstance();
        startDT.setTime(date);
        startDT.add(Calendar.DAY_OF_MONTH, days);
        return startDT.getTime();
    }

    @Test
    public void testGetHour() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = DateUtil.addHour(new Date());
        String format = df.format(date);
        System.out.println(format);
    }
}
