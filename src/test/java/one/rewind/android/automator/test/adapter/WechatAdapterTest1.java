package one.rewind.android.automator.test.adapter;

import one.rewind.android.automator.deivce.AndroidDevice;
import one.rewind.android.automator.deivce.AndroidDeviceManager;
import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.adapter.Adapter;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import org.junit.Before;
import org.junit.Test;

/**
 * @author maxuefeng[m17793873123@163.com]
 */
public class WechatAdapterTest1 {

    //    String udid = "ZX1G323GNB";
    String udid = AndroidDeviceManager.getAvailableDeviceUdids()[0];
    AndroidDevice device;
    WeChatAdapter adapter;

    @Before
    public void setup() {

        try {
            device = new AndroidDevice(udid);
            Account account = new Account();
            account.username = "一路有寒一世有勋";
            account.src_id = "wxid_uvhpaq2iv3v922";

            adapter = new WeChatAdapter(device, account);
            // 启动
            device.start();
            adapter.start();
        } catch (Exception e) {
            Adapter.logger.error("", e);
        }
    }

    /**
     * 测试安卓自动化操作
     *
     * @throws InterruptedException
     */
    @Test
    public void testAppium() throws InterruptedException {
        WeChatAdapter.UserInfo localUserInfo = adapter.getLocalUserInfo();

        System.out.println(localUserInfo.id);
        System.out.println(localUserInfo.name);

    }
}
