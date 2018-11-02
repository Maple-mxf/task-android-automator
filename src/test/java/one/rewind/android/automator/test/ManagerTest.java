package one.rewind.android.automator.test;

import com.google.common.collect.Sets;
import one.rewind.android.automator.AndroidDeviceManager;
import one.rewind.android.automator.util.DBUtil;
import org.junit.Test;

import java.util.List;
import java.util.Set;

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

        /*Set<String> set = Sets.newHashSet();
        //初始化原始任务队列
        List<String> accounts = DBUtil.obtainFullData(set, 1);
        AndroidDeviceManager.originalAccounts.addAll(set);

        AndroidDeviceManager manager = AndroidDeviceManager.getInstance();
        manager.startManager();*/
    }
}
