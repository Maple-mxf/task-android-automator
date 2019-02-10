package one.rewind.android.automator.test.task;

import com.google.common.collect.Lists;
import one.rewind.android.automator.deivce.AndroidDeviceManager;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AndroidException;
import one.rewind.android.automator.exception.TaskException;
import one.rewind.android.automator.task.Task;
import one.rewind.android.automator.task.TaskFactory;
import one.rewind.android.automator.task.TaskHolder;
import org.junit.Test;

import java.util.List;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class SingleTaskTest {


    @Test
    public void getSelfSubscribeMediaTest() throws Exception {

        AndroidDeviceManager deviceManager = AndroidDeviceManager.getInstance();

        deviceManager.detectDevices();

        List<String> media = Lists.newArrayList();
        TaskHolder holder = new TaskHolder(
                "1",
                "one.rewind.android.automator.adapter.wechat.WeChatAdapter",
                "one.rewind.android.automator.adapter.wechat.task.GetSelfSubscribeMediaTask",
                media
        );

        Task task = TaskFactory.getInstance().generateTask(holder);

        deviceManager.submit(task);

        System.in.read();
    }

    @Test
    public void getEssaysTest() throws Exception {
        AndroidDeviceManager deviceManager = AndroidDeviceManager.getInstance();

        deviceManager.detectDevices();

        List<String> media = Lists.newArrayList();

        TaskHolder holder = new TaskHolder(
                "1",
                "one.rewind.android.automator.adapter.wechat.WeChatAdapter",
                "one.rewind.android.automator.adapter.wechat.task.GetSelfSubscribeMediaTask",
                media
        );

        Task task = TaskFactory.getInstance().generateTask(holder);

        deviceManager.submit(task);

        System.in.read();
    }

    @Test
    public void subscribeMedia() throws Exception {
        AndroidDeviceManager deviceManager = AndroidDeviceManager.getInstance();

        deviceManager.detectDevices();


        //
        System.in.read();
    }

    @Test
    public void submitSubscribeTask() throws AccountException.AccountNotLoad, TaskException.IllegalParameters, AndroidException.NoAvailableDevice, InterruptedException {

        List<String> media = Lists.newArrayList();
        media.add("阿里巴巴");


        TaskHolder holder = new TaskHolder(
                "1",
                "one.rewind.android.automator.adapter.wechat.WeChatAdapter",
                "one.rewind.android.automator.adapter.wechat.task.SubscribeMediaTask",
                media
        );

        Task task = TaskFactory.getInstance().generateTask(holder);

        // 任务完成之后执行Idle回调函数继续在deviceTaskMap中取任务
        AndroidDeviceManager.getInstance().submit(task);
    }
}
