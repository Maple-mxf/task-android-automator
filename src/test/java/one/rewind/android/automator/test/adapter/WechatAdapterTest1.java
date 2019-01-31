package one.rewind.android.automator.test.adapter;

import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.AndroidDeviceManager;
import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.android.automator.exception.AndroidException;
import one.rewind.db.exception.DBInitException;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;

/**
 * @author maxuefeng[m17793873123@163.com]
 */
public class WechatAdapterTest1 {

    //    String udid = "ZX1G323GNB";
    String udid = AndroidDeviceManager.getAvailableDeviceUdids()[0];
    AndroidDevice device;
    WeChatAdapter adapter;

    @Before
    public void setup() throws AndroidException.IllegalStatusException, InterruptedException, AdapterException.OperationException, AccountException.Broken, DBInitException, SQLException {
        device = new AndroidDevice(udid);
        Account account = new Account();
        account.username = "一路有寒一世有勋";
        account.src_id = "wxid_uvhpaq2iv3v922";

        adapter = new WeChatAdapter(device,account);
        // 启动
        device.start();
        adapter.start();
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
