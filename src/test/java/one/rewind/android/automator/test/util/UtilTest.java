package one.rewind.android.automator.test.util;

import org.junit.Test;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author maxuefeng[m17793873123@163.com]
 */
public class UtilTest {




    @Test
    public void testSubscribeNumber() throws SQLException, ClassNotFoundException {
//        int numToday = DBUtil.obtainSubscribeNumToday("ZX1G423DMM");
//        System.out.println(numToday);
		//java校验指定日期格式yyyy年MM月dd日的正则表达式
    }

    @Test
    public void testAccuracySubscribe()   {
//        DeviceUtil.accuracySubscribe("故事与道理的磨合");
    }

    @Test
    public void testString() {
        String b = "";
        String a = null;
        b = b + a;
        System.out.println(b);
    }

    @Test
    public void testThreadLocal() {
        ThreadLocal<Long> times = new ThreadLocal<>();
        times.set(1L);
        times.set(2L);
        times.set(5L);
        Long aLong = times.get();
        System.out.println(aLong);
        System.out.println(aLong);
    }

    @Test
    public void testLocalDateTime() {
        Date date = new Date();

        System.out.println(date.getTime());
    }

    @Test
    public void testQueue() {
        Queue<String> queue = new ConcurrentLinkedQueue<>();
        queue.add("one");
        queue.add("two");
        queue.add("three");
        queue.add("four");
        int length = queue.size();
        for (int i = 0; i < length; i++) {
            System.out.println(queue.poll());
        }
        System.out.println(queue.size());
    }


    @Test
    public void testList() {
        List<String> tmp = Arrays.asList(new String[]{"1"});
        List<String> var = new ArrayList<>(tmp);
        tmp.remove("1");
    }

    @Test
    public void testRandom() {
        Random random = new Random();
        int s = random.nextInt(50000) % (20000 + 1) + 20000;
        System.out.println(s);
    }

    @Test
    public void testSwitch() {
        int a = 1;
        switch (a) {
            case 1:
                System.out.println("sasdasdsad");
            case 2:
                System.out.println("ggggggg");
            default:
                System.out.println("hello world");
        }
    }

    @Test
    public void testString1() {
        String tmp = "2018年10月15日";

    }

}
