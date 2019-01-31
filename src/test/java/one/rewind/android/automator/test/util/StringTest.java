package one.rewind.android.automator.test.util;

import com.google.common.collect.Maps;
import one.rewind.android.automator.AndroidDeviceManager;
import one.rewind.android.automator.util.Tab;
import org.junit.Test;

import java.util.Map;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class StringTest {

    @Test
    public void testChaineseLength() {
        String tmp = "2018年12月21日";


        System.out.println(tmp.substring(0, 4));

        System.out.println(tmp.substring(5, 7));

//		System.out.println(tmp.substring());
    }

    @Test
    public void testRealMedia() {
        final String media = Tab.realMedia("阿里巴巴Android-Automator-Topic-20190104152236676__udid_ZGHDGSHS");

        System.out.println(media);
    }

    @Test
    public void getDeviceUdid() {
        for (String udid : AndroidDeviceManager.getAvailableDeviceUdids()) {
            System.out.println(udid);
        }

    }

    @Test
    public void testInt2String() {
        int tmp = 2;

        Map<String, Object> map = Maps.newHashMap();

        map.put("account_id", tmp);

        System.out.println(String.valueOf(map.get("account_id")));
    }
}
