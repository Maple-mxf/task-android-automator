package one.rewind.android.automator.test.util;

import com.google.common.collect.Sets;
import one.rewind.android.automator.exception.InvokingBaiduAPIException;
import one.rewind.android.automator.model.BaiduTokens;
import one.rewind.android.automator.model.DBTab;
import one.rewind.android.automator.util.AndroidUtil;
import one.rewind.android.automator.util.BaiduAPIUtil;
import one.rewind.android.automator.util.DBUtil;
import one.rewind.android.automator.util.ShellUtil;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Create By 2018/10/18
 */
public class UtilTest {


    /**
     * List of devicesInfo attached
     * 9YJ7N17429007528	device
     * ZX1G323GNB	device
     */
    @Test
    public void testShellExecute() {
        String[] devicesInfoByShell = AndroidUtil.obtainDevices();
    }


    @Test
    public void testSubscribeNumber() throws SQLException, ClassNotFoundException {
//        int numToday = DBUtil.obtainSubscribeNumToday("ZX1G423DMM");
//        System.out.println(numToday);
    }

    @Test
    public void testBaiduAPI() throws InvokingBaiduAPIException {
        JSONObject jsonObject = BaiduAPIUtil.imageOCR("D:\\workspace\\plus\\wechat-android-automator\\screen\\0a5e2970-7b6a-4be6-a156-0f4d3e8e265e.png");
        System.out.println(jsonObject);
    }

    @Test
    public void testObtainToken() throws Exception {
        BaiduTokens token = BaiduAPIUtil.obtainToken();
        System.out.println(token.app_k);
        System.out.println(token.app_s);
    }

    @Test
    public void testMultiRunnableObtainToken() throws IOException {
        Thread r1 = new Thread(() -> {
            BaiduTokens baiduTokens = null;
            try {
                baiduTokens = BaiduAPIUtil.obtainToken();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println(baiduTokens.id);
        });
        r1.start();

        Thread r2 = new Thread(() -> {
            BaiduTokens baiduTokens = null;
            try {
                baiduTokens = BaiduAPIUtil.obtainToken();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println(baiduTokens.id);
        });
        r2.start();

        System.in.read();
    }

    @Test
    public void testAccuracySubscribe() throws InvokingBaiduAPIException {
//        AndroidUtil.accuracySubscribe("故事与道理的磨合");
    }

    @Test
    public void testMath() throws SQLException {
        long countOf = DBTab.essayDao.
                queryBuilder().
                where().
                eq("media_nick", "云南省驻越南商务代表处").countOf();

        if (countOf >= 17 || Math.abs(17 - countOf) <= 5) {
            System.out.println("1");
        } else {
            System.out.println("2");
        }
    }

    @Test
    public void testString() {
        String b = "";
        String a = null;
        b = b + a;
        System.out.println(b);
    }

    @Test
    public void testExecuteCommand() throws IOException, InterruptedException {
        ShellUtil.shutdownProcess("ZX1G42BX4R", "com.tencent.mm");
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
    public void testDBQueryMedia() {
        Set<String> set = Sets.newHashSet();

        DBUtil.sendAccounts(set, 10);
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

}
