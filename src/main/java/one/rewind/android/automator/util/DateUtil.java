package one.rewind.android.automator.util;

import java.util.Calendar;
import java.util.Date;

/**
 * @author MaXueFeng
 * @since 1.0
 */
public class DateUtil {

    public static Date buildDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date time = calendar.getTime();

        if (time.before(new Date())) {
            return addDay(time);
        }
        return time;
    }

    private static Date addDay(Date date) {
        Calendar startDT = Calendar.getInstance();
        startDT.setTime(date);
        startDT.add(Calendar.DAY_OF_MONTH, 1);
        return startDT.getTime();
    }
}
