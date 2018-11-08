package one.rewind.android.automator.test.util;

import one.rewind.android.automator.DBTab;
import one.rewind.android.automator.exception.InvokingBaiduAPIException;
import one.rewind.android.automator.model.BaiduTokens;
import one.rewind.android.automator.util.AndroidUtil;
import one.rewind.android.automator.util.BaiduAPIUtil;
import one.rewind.android.automator.util.DBUtil;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;

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
        int numToday = DBUtil.obtainSubscribeNumToday("ZX1G423DMM");
        System.out.println(numToday);
    }

    @Test
    public void testBaiduAPI() throws InvokingBaiduAPIException {
        JSONObject jsonObject = BaiduAPIUtil.imageOCR("D:\\workspace\\plus\\wechat-android-automator\\screen\\0d69bf67-469f-454f-ae2f-b46f3c14130a.png");
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
        AndroidUtil.accuracySubscribe("故事与道理的磨合");
    }

    @Test
    public void testMath() throws SQLException {
        long countOf = DBTab.subscribeDao.
                queryBuilder().
                where().
                eq("media_name", "云南省驻越南商务代表处").countOf();

        if (countOf >= 17 || Math.abs(17 - countOf) <= 5) {
            System.out.println("1");
        } else {
            System.out.println("2");
        }
    }

}
