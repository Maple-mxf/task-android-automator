package one.rewind.android.automator.test.api;

import com.google.gson.reflect.TypeToken;
import one.rewind.android.automator.AndroidDeviceManager;
import one.rewind.android.automator.task.Task;
import one.rewind.android.automator.task.TaskFactory;
import one.rewind.android.automator.task.TaskHolder;
import one.rewind.io.server.Msg;
import one.rewind.json.JSON;
import org.apache.commons.lang3.math.NumberUtils;
import spark.Route;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static spark.Spark.port;
import static spark.Spark.post;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class AutomationServer {

    public static volatile AndroidDeviceManager deviceManager = AndroidDeviceManager.getInstance();


    public static void main(String[] args) throws Exception {

        port(30002);

        // 启动device
        deviceManager.detectDevices();

        // 程序feed
        post("/feed", feed, JSON::toJson);
    }


    // 将参数生成TaskHolder
    public static Route feed = (request, response) -> {

        String httpBody = request.body();

        // 1 获取初步参数
        Map<String, Object> httpParams = JSON.fromJson(httpBody, new TypeToken<Map<String, Object>>() {
        }.getType());

        // 2 解析为TaskHolder

        // udid
        String udid = httpParams.get("udid") == null ? null : String.valueOf(httpParams.get("udid"));

        // adapter_class_name    Adapter类的全路径名称
        String adapter_class_name = String.valueOf(httpParams.get("adapter_class_name"));

        // class_name    TasK类的全路径名称
        String class_name = String.valueOf(httpParams.get("class_name"));

        // account_id  账号ID   如果为空 则为默认值0
        int account_id = NumberUtils.toInt(String.valueOf(httpParams.get("account_id")), 0);

        // topic_name redis的topic名称    TODO Holder构函数加入params
        String topic_name = String.valueOf(httpParams.get("topic_name"));

        // List<String>   params可以是多个媒体的名称
        List<String> params = (List<String>) httpParams.get("params");

        // 生成TaskHolder
        TaskHolder holder = new TaskHolder(UUID.randomUUID().toString(), udid, adapter_class_name, class_name, account_id, params);

        // 生成Task
        Task task = TaskFactory.getInstance().generateTask(holder);

        // 提交任务
        AndroidDeviceManager.SubmitInfo submitInfo = deviceManager.submit(task);

        return new Msg<>(Msg.SUCCESS, submitInfo);
    };
}
