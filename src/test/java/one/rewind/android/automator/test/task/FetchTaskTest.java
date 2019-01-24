package one.rewind.android.automator.test.task;

import one.rewind.android.automator.util.ImageUtil;
import org.junit.Test;

import java.io.IOException;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class FetchTaskTest {


    /**
     * 生成历任务
     */
    @Test
    public void historyTest() throws IOException, InterruptedException {
       /* List<OCRParser.TouchableTextArea> touchableTextAreas = TesseractOCRParser.getInstance().getTextBlockArea("D:\\java-workplace\\task-android-automator\\tmp\\task.jpg", false);

        System.out.println(JSON.toJson(touchableTextAreas));*/

        ImageUtil.grayImage("D:\\java-workplace\\task-android-automator\\tmp\\微信图片_20190122111641.jpg", "D:\\java-workplace\\task-android-automator\\tmp\\微信图片_20190122111641.jpg", "png");
    }
}
