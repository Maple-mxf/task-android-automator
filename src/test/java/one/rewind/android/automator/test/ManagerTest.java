package one.rewind.android.automator.test;

import one.rewind.android.automator.AndroidDeviceManager;
import org.junit.Test;

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
        AndroidDeviceManager.originalAccounts.add("");

        AndroidDeviceManager manager = AndroidDeviceManager.getInstance();

        manager.startManager();
    }
}
