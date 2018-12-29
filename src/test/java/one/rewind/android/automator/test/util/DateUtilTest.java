package one.rewind.android.automator.test.util;

import one.rewind.android.automator.util.DateUtil;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.junit.Test;

import java.text.ParseException;
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

	@Test
	public void testFormatByChinese() throws ParseException {
		String format = DateFormatUtils.format(new Date(), "yyyy年MM月dd日");
		System.out.println(format);
		SimpleDateFormat df = new SimpleDateFormat("yyyy年MM月dd日");
		Date parse = df.parse("2018年12月21日");

		System.out.println(parse);

	}

	@Test
	public void testDateCompare() throws ParseException {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		Date d1 = df.parse("2019-12-01 00:00:00");
		System.out.println(d1.before(new Date()));
		// d1 > new Date()
		System.out.println(d1.compareTo(new Date()));
	}


}
