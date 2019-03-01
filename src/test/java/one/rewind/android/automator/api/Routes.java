package one.rewind.android.automator.api;

import com.google.common.collect.ImmutableMap;
import com.google.gson.reflect.TypeToken;
import com.j256.ormlite.dao.Dao;
import one.rewind.android.automator.adapter.Adapter;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.deivce.AndroidDevice;
import one.rewind.android.automator.deivce.AndroidDeviceManager;
import one.rewind.android.automator.task.Task;
import one.rewind.android.automator.task.TaskFactory;
import one.rewind.android.automator.task.TaskHolder;
import one.rewind.db.Daos;
import one.rewind.io.server.Msg;
import one.rewind.json.JSON;
import one.rewind.txt.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Route;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.BlockingQueue;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class Routes {

    public static Logger logger = LogManager.getLogger(APIServer.class.getName());


    public enum OutputType {
        JSON,
        CSV
    }

    /**
     * 爬虫概览信息
     */
    public static Route overview = (request, response) -> {
        try {
            Dao<TaskHolder, String> taskDao = Daos.get(TaskHolder.class);

            // TotalTaskNum
            long totalTaskNum = taskDao.queryBuilder().distinct().countOf();

            // SuccessTaskNum
            long successTaskNum = taskDao.queryBuilder().distinct().where().eq("success", true).countOf();

            // FailureTaskNum
            long failureTaskNum = taskDao.queryBuilder().distinct().where().eq("success", false).countOf();

            // InQueueTaskNumber
            Map<AndroidDevice, BlockingQueue<Task>> var = AndroidDeviceManager.getInstance().deviceTaskMap;
            int inQueueTaskNumber = var.keySet().stream().mapToInt(t -> var.get(t).size()).sum();

            // ExecutedTime
            String executedTime = AndroidDeviceManager.startTime.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss"));

            Map<String, Object> data = ImmutableMap.of(
                    "totalTaskNum", totalTaskNum,
                    "successTaskNum", successTaskNum,
                    "failureTaskNum", failureTaskNum,
                    "inQueueTaskNumber", inQueueTaskNumber,
                    "executedTime", executedTime);

            return new Msg<>(data);
        } catch (Exception e) {
            logger.error("Error Server error ", e);
            return new Msg<>(Msg.FAILURE);
        }
    };

    public static Route submit = (request, response) -> {

        // 获取参数
        Map<String, Object> bodyJsonMap = JSON.fromJson(request.body(), new TypeToken<Map<String, Object>>() {
        }.getType());

        // 生成任务
        String taskId = StringUtil.uuid();

        String udid = (String) bodyJsonMap.get("udid");

        String adapter_class_name = String.valueOf(bodyJsonMap.get("adapter_class_name"));

        String class_name = String.valueOf(bodyJsonMap.get("class_name"));

        int account_id = NumberUtils.toInt(String.valueOf(bodyJsonMap.get("class_name")), 0);

        List<String> params = (List<String>) bodyJsonMap.get("params");

        try {

            AndroidDeviceManager.getInstance().submit(TaskFactory.getInstance().generateTask(new TaskHolder(taskId, udid, adapter_class_name, class_name, account_id, params)));

            logger.info("Success submit task[{}] ok! ", taskId);

        } catch (Exception e) {
            logger.error("Error submit task[{}] failure!,e", taskId, e);
        }

        return new Msg<>(taskId).toJSON();
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
                return new Msg<>(Msg.ILLEGAL_PARAMETERS).toJSON();
            } else {
                AndroidDevice ad = op.get();
                Adapter adapter = ad.adapters.get(adapterClassName);
                if (adapter != null) {
                    return new Msg<>(adapter.account).toJSON();
                }
            }
        } else {
            return new Msg<>(Msg.ILLEGAL_PARAMETERS).toJSON();
        }
        return null;
    };

    /**
     * 切换账号
     */
    public static Route switchAccount = (request, response) -> {

        // 获取参数
        Map<String, String> bodyJsonMap = JSON.fromJson(request.body(), new TypeToken<Map<String, String>>() {
        }.getType());

        String accountId;
        String udid;
        String className;
        if (bodyJsonMap != null && StringUtils.isNotBlank((accountId = bodyJsonMap.get("accountId"))) && StringUtils.isNotBlank(udid = bodyJsonMap.get("udid")) && StringUtils.isNotBlank(className = bodyJsonMap.get("className"))) {

            String taskId = StringUtil.uuid();
            
            Task task = TaskFactory.getInstance().generateTask(new TaskHolder(taskId, udid, null, className, 0, Arrays.asList(accountId)));

            AndroidDeviceManager.getInstance().submit(task);

            logger.info("Success submit task ok!");

            return new Msg<>(taskId).toJSON();

        } else {

            logger.error("Error submit task failure!");
            return new Msg<>(Msg.ILLEGAL_PARAMETERS).toJSON();
        }
    };

    /**
     * 查看某一个设备的运行情况
     */
    public static Route operator = (request, response) -> {

        // 设备ID
        String udid;

        // 获取参数
        Map<String, String> jsonMap = request.params();

        // 参数校验
        if (StringUtils.isBlank((udid = jsonMap.get("udid"))) ||
                AndroidDeviceManager.getInstance().deviceTaskMap.keySet().stream().noneMatch(t -> udid.equalsIgnoreCase(t.udid))) {
            return new Msg<>(Msg.ILLEGAL_PARAMETERS);
        }

        // 获取startTime   默认是一月
        Date sd = StringUtils.isBlank(jsonMap.get("sd")) ? new Date(new Date().getTime() - 24 * 3600 * 1000) : new Date(Long.valueOf(jsonMap.get("sd")));

        // 获取EndTime   默认是现在
        Date ed = StringUtils.isBlank(jsonMap.get("ed")) ? new Date() : new Date(Long.valueOf(jsonMap.get("ed")));

        // 获取gap
        long gap = StringUtils.isBlank(jsonMap.get("gap")) ? 3600 : Long.valueOf(jsonMap.get("gap"));

        if ((ed.getTime() - sd.getTime()) < gap || (ed.getTime() - sd.getTime()) > 30L * 24 * 3600 * 1000) {
            return new Msg<>(Msg.ILLEGAL_PARAMETERS);
        }

        // 获取输出类型
        OutputType ot = getOutputType(jsonMap);


        return null;
    };

    public static OutputType getOutputType(Map<String, String> jsonMap) {
        // 输出类型
        String ot = jsonMap.get("ot");
        return StringUtils.isBlank(ot) ? OutputType.JSON : (ot.equalsIgnoreCase("csv") ? OutputType.CSV : OutputType.JSON);
    }
}




































