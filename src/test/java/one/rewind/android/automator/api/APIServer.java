package one.rewind.android.automator.api;

import com.google.gson.reflect.TypeToken;
import one.rewind.android.automator.adapter.Adapter;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.deivce.AndroidDevice;
import one.rewind.android.automator.deivce.AndroidDeviceManager;
import one.rewind.android.automator.task.TaskFactory;
import one.rewind.android.automator.task.TaskHolder;
import one.rewind.io.server.Msg;
import one.rewind.json.JSON;
import one.rewind.txt.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Route;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static spark.Spark.path;
import static spark.Spark.post;


/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class APIServer {

    public static Logger logger = LogManager.getLogger(APIServer.class.getName());


    public static void main(String[] args) {

        path("/entrance", () -> {
            // 提交任务
            post("/submit", submit);

            // 获取指定Adapter登录的账号
            post("/getLoginAccountInfo", getLoginAccountInfo);
        });
    }

    public static Route submit = (request, response) -> {

        // 获取参数
        Map<String, Object> bodyJsonMap = JSON.fromJson(request.body(), new TypeToken<Map<String, Object>>() {
        }.getType());

        // 生成任务
        String taskId = StringUtil.uuid();
        String udid = String.valueOf(bodyJsonMap.get("udid"));
        String adapter_class_name = String.valueOf(bodyJsonMap.get("adapter_class_name"));
        String class_name = String.valueOf(bodyJsonMap.get("class_name"));
        int account_id = NumberUtils.toInt(String.valueOf(bodyJsonMap.get("class_name")), 0);
        List<String> params = (List<String>) bodyJsonMap.get("params");
        try {
            AndroidDeviceManager.getInstance().submit(TaskFactory.getInstance().generateTask(new TaskHolder(taskId, udid, adapter_class_name, class_name, account_id, params)));

        } catch (Exception e) {
            logger.error("Error submit task[{}] failure!,e", taskId, e);
        }

        return null;
    };


    /**
     * 获取当前设备登陆账号信息
     */
    public static Route getLoginAccountInfo = (request, response) -> {

        // 获取参数
        Map<String, String> bodyJsonMap = JSON.fromJson(request.body(), new TypeToken<Map<String, String>>() {
        }.getType());

        if (bodyJsonMap != null && StringUtils.isNotBlank(bodyJsonMap.get("udid"))) {

            String udid = bodyJsonMap.get("udid");

            // 默认获取微信的登录账号
            String adapterClassName = StringUtils.defaultString(bodyJsonMap.get("adapter_class_name"), WeChatAdapter.class.getName());

            Optional<AndroidDevice> op = AndroidDeviceManager.getInstance().deviceTaskMap.keySet().stream().filter(ad -> ad.udid.equals(udid)).findFirst();

            if (!op.isPresent()) {
                return new Msg<>(Msg.ILLEGAL_PARAMETERS);
            } else {
                AndroidDevice ad = op.get();
                Adapter adapter = ad.adapters.get(adapterClassName);
                if (adapter != null) {
                    return new Msg<>(adapter.account);
                }
            }
        } else {
            return new Msg<>(Msg.ILLEGAL_PARAMETERS);
        }
        return null;
    };
}
