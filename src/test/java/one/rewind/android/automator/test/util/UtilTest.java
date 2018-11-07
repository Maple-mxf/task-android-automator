package one.rewind.android.automator.test.util;

import one.rewind.android.automator.exception.InvokingBaiduAPIException;
import one.rewind.android.automator.util.AndroidUtil;
import one.rewind.android.automator.util.BaiduAPIUtil;
import one.rewind.android.automator.util.DBUtil;
import org.json.JSONObject;
import org.junit.Test;

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
        JSONObject jsonObject = BaiduAPIUtil.executeImageRecognitionRequest("D:\\workspace\\plus\\wechat-android-automator\\screen\\0d69bf67-469f-454f-ae2f-b46f3c14130a.png");
        System.out.println(jsonObject);
    }


}
