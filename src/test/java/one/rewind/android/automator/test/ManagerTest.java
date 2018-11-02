package one.rewind.android.automator.test;

import one.rewind.android.automator.AndroidDeviceManager;
import one.rewind.android.automator.util.DBUtil;
import org.junit.Test;

import java.util.List;

public class ManagerTest {

    /**
     * 无任务启动
     *
     * @throws Exception
     */
    @Test
    public void startManagerNotTaskQueue() throws Exception {

        Class.forName("one.rewind.android.automator.AndroidDeviceManager");

        AndroidDeviceManager manager = AndroidDeviceManager.getInstance();

        Class.forName("one.rewind.android.automator.util.BaiduAPIUtil");

        manager.startManager();
    }


    /**
     * 有任务启动
     *
     * @throws Exception
     */
    @Test
    public void startManagerHasTaskQueue() throws Exception {
        Class.forName("one.rewind.android.automator.AndroidDeviceManager");

        //初始化原始任务队列
        List<String> accounts = DBUtil.sendAccounts(null, 1);
        assert accounts != null;
        AndroidDeviceManager.originalAccounts.addAll(accounts);

        AndroidDeviceManager manager = AndroidDeviceManager.getInstance();
        manager.startManager();
    }
}
