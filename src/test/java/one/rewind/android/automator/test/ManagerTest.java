package one.rewind.android.automator.test;

import com.google.common.collect.Sets;
import one.rewind.android.automator.manager.DefaultDeviceManager;
import one.rewind.android.automator.util.DBUtil;
import org.junit.Test;

import java.util.Set;

public class ManagerTest {

    /**
     * 无任务启动
     *
     * @throws Exception
     */
    @Test
    public void startManagerNotTaskQueue() throws Exception {

        Class.forName("one.rewind.android.automator.manager.DefaultDeviceManager");

        DefaultDeviceManager manager = DefaultDeviceManager.getInstance();

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
        Class.forName("one.rewind.android.automator.manager.DefaultDeviceManager");
        Set<String> set = Sets.newHashSet();
        //初始化原始任务队列
        DBUtil.obtainFullData(set, 20, 80);
        DefaultDeviceManager.originalAccounts.addAll(set);
        DefaultDeviceManager manager = DefaultDeviceManager.getInstance();
        manager.startManager();
    }
}
