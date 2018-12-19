package one.rewind.android.automator.test.api;

import one.rewind.io.requester.task.Task;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class APIClient {


    /**
     * 数据导入API任务接口
     * 数据format
     * {"media": [
     * "北京数字奇迹科技有限公司",
     * "北京基石信达科技有限公司"
     * ]}
     *
     * @param args
     */
    public static void main(String[] args) throws MalformedURLException, URISyntaxException {
        String json = "{\"media\": [\n" +
                "    \"北京数字奇迹科技有限公司\",\n" +
                "    \"北京基石信达科技有限公司\"\n" +
                "]}";

        Task task = new Task("http://127.0.0.1:8080/push", json);



    }
}
