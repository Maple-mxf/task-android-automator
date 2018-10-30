package one.rewind.android.automator.test.util;

import one.rewind.android.automator.exception.InvokingBaiduAPIException;
import one.rewind.android.automator.util.AndroidUtil;
import one.rewind.android.automator.util.BaiduAPIUtil;
import org.json.JSONObject;
import org.junit.Test;

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
    public void testBaiduAPI() throws InvokingBaiduAPIException {
        JSONObject jsonObject = BaiduAPIUtil.executeImageRecognitionRequest(
                "D:\\workspace\\wechat-android-automator\\screen\\0a3f1da4-1edf-4ecc-88fa-2bcf13379a94.png");
        System.out.println(jsonObject);
    }
}
