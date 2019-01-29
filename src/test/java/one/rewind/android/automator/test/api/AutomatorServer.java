package one.rewind.android.automator.test.api;

import com.google.gson.reflect.TypeToken;
import one.rewind.android.automator.AndroidDeviceManager;
import one.rewind.android.automator.task.Task;
import one.rewind.android.automator.task.TaskFactory;
import one.rewind.android.automator.task.TaskHolder;
import one.rewind.json.JSON;
import spark.Route;

import java.util.Map;

import static spark.Spark.port;
import static spark.Spark.post;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class AutomatorServer {


    public static void main(String[] args) {

        port(30006);

        post("/", feed, JSON::toJson);
    }


    // 将参数生成TaskHolder
    public static Route feed = (request, response) -> {

        String paramBody = request.body();

        // udid class_name account_id params
        Map<String, Object> params = JSON.fromJson(paramBody, new TypeToken<Map<String, Object>>() {
        }.getType());

        //
        TaskHolder holder = new TaskHolder(String.valueOf(params.get("id")), String.valueOf(params.get("udid")), String.valueOf(params.get("class_name")));

        Task task = TaskFactory.getInstance().generateTask(holder);

        // 任务提交
        AndroidDeviceManager.getInstance().submit(task);

        return null;
    };
}
