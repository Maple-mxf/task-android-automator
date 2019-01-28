package one.rewind.android.automator.test.task;

import one.rewind.android.automator.AndroidDeviceManager;
import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.adapter.wechat.task.GetSelfSubscribeMediaTask;
import one.rewind.android.automator.task.TaskHolder;
import one.rewind.android.automator.util.ImageUtil;
import org.junit.Test;

import java.io.IOException;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class TaskTest {

    @Test
    public void testGetAccount() {
        Account account = Account.getAccount("ZX1G426B3V", "one.rewind.android.automator.adapter.wechat.WeChatAdapter");
        System.out.println(account.id);
    }


    /**
     * 生成历任务
     */
    @Test
    public void historyTest() throws IOException {
       /* List<OCRParser.TouchableTextArea> touchableTextAreas = TesseractOCRParser.getInstance().getTextBlockArea("D:\\java-workplace\\task-android-automator\\tmp\\task.jpg", false);

        System.out.println(JSON.toJson(touchableTextAreas));*/

        ImageUtil.grayImage("D:\\java-workplace\\task-android-automator\\tmp\\微信图片_20190122111641.jpg", "D:\\java-workplace\\task-android-automator\\tmp\\微信图片_20190122111641.jpg", "png");
    }


    @Test
    public void getSelfSubscribeMediaTest() throws Exception {

        AndroidDeviceManager deviceManager = AndroidDeviceManager.getInstance();

        deviceManager.initialize();

        // 生成TaskHolder
        TaskHolder holder = new TaskHolder("1", "ZX1G426B3V", "one.rewind.android.automator.adapter.wechat.WeChatAdapter");

        // 生成Task
        GetSelfSubscribeMediaTask task = new GetSelfSubscribeMediaTask(holder, "");

        deviceManager.submit(task);

        Thread.sleep(100000);
    }

}
