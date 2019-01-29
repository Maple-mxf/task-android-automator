package one.rewind.android.automator.test.task;

import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.AndroidDeviceManager;
import one.rewind.android.automator.adapter.wechat.task.GetSelfSubscribeMediaTask;
import one.rewind.android.automator.adapter.wechat.task.SubscribeMediaTask;
import one.rewind.android.automator.exception.AndroidException;
import one.rewind.android.automator.task.TaskHolder;
import one.rewind.android.automator.util.ImageUtil;
import org.junit.Test;

import java.io.IOException;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class TaskTestOfSingleDevice {


    /**
     * 生成历任务
     */
    @Test
    public void historyTest() throws IOException, InterruptedException {
       /* List<OCRParser.TouchableTextArea> touchableTextAreas = TesseractOCRParser.getInstance().getTextBlockArea("D:\\java-workplace\\task-android-automator\\tmp\\task.jpg", false);

        System.out.println(JSON.toJson(touchableTextAreas));*/

        ImageUtil.grayImage("D:\\java-workplace\\task-android-automator\\tmp\\微信图片_20190122111641.jpg", "D:\\java-workplace\\task-android-automator\\tmp\\微信图片_20190122111641.jpg", "png");
    }


    @Test
    public void getSelfSubscribeMediaTest() throws Exception {

        AndroidDeviceManager deviceManager = AndroidDeviceManager.getInstance();

        deviceManager.initialize();

        // 生成TaskHolder
        TaskHolder holder = new TaskHolder("1", "ZX1G423DMM", "one.rewind.android.automator.adapter.wechat.WeChatAdapter");

        // 生成Task
        GetSelfSubscribeMediaTask task = new GetSelfSubscribeMediaTask(holder, "");

        deviceManager.submit(task);

        Thread.sleep(10000000);
    }

    @Test
    public void getEssaysTest() throws Exception {

      /*  AndroidDeviceManager deviceManager = AndroidDeviceManager.getInstance();

        deviceManager.initialize();

        // 生成TaskHolder
        TaskHolder holder = new TaskHolder("1", "ZX1G426B3V", "one.rewind.android.automator.adapter.wechat.WeChatAdapter", 1);

        // 生成Task
        GetMediaEssaysTask task = new GetMediaEssaysTask(holder, "");

        deviceManager.submit(task);

        Thread.sleep(100000);*/
    }

    @Test
    public void subscribeMedia() throws Exception {

        AndroidDeviceManager deviceManager = AndroidDeviceManager.getInstance();

        deviceManager.initialize();

        // 生成TaskHolder
        TaskHolder holder = new TaskHolder("1", "ZX1G426B3V", "one.rewind.android.automator.adapter.wechat.WeChatAdapter", "SubscribeMediaTask");

        // 生成Task
        SubscribeMediaTask task = new SubscribeMediaTask(holder, "");

        deviceManager.submit(task);

        Thread.sleep(100000);
    }

    @Test
    public void testDeviceStart() throws AndroidException.IllegalStatusException {
        AndroidDevice device = new AndroidDevice("ZX1G423DMM");
        device.start();


    }
}
