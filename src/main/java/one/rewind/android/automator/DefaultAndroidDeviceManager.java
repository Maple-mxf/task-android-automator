package one.rewind.android.automator;


import com.j256.ormlite.dao.Dao;
import one.rewind.android.automator.adapter.DefaultWechatAdapter;
import one.rewind.android.automator.exception.AndroidCollapseException;
import one.rewind.android.automator.model.SubscribeAccount;
import one.rewind.android.automator.model.TaskFailRecord;
import one.rewind.android.automator.model.TaskType;
import one.rewind.android.automator.util.AndroidUtil;
import one.rewind.db.DaoManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Create By 2018/11/03
 * Description:
 * <p>
 * <p>
 * use the single object created
 * For the device is not occur problem
 */
public class DefaultAndroidDeviceManager {

    private static Logger logger = LoggerFactory.getLogger(DefaultAndroidDeviceManager.class);

    private DefaultAndroidDeviceManager() {
    }

    private static DefaultAndroidDeviceManager deviceManager;

    public static DefaultAndroidDeviceManager createInstance() {
        if (deviceManager == null) {
            deviceManager = new DefaultAndroidDeviceManager();
        }
        return deviceManager;
    }

    private static Dao<SubscribeAccount, String> subscribeDao;

    private static Dao<TaskFailRecord, String> failRecordDao;

    private static final int DEFAULT_LOCAL_PROXY_PORT = 48454;

    /**
     * key is the device key(udid),value is the device Object message;
     * The collection is put device message
     */
    private static ConcurrentHashMap<String, AndroidDevice> devices = new ConcurrentHashMap<>();

    /**
     * obtain available android devices
     *
     * @return
     */
    public static List<AndroidDevice> obtainAvailableDevices() {
        synchronized (AndroidDeviceManager.class) {
            List<AndroidDevice> availableDevices = new ArrayList<>();
            devices.forEach((k, v) -> {
                if (v.state.equals(AndroidDevice.State.INIT)) {
                    availableDevices.add(v);
                }
            });
            return availableDevices;
        }
    }

    static {
        try {
            subscribeDao = DaoManager.getDao(SubscribeAccount.class);
            failRecordDao = DaoManager.getDao(TaskFailRecord.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String[] var = AndroidUtil.obtainDevices();
        Random random = new Random();
        for (int i = 0; i < var.length; i++) {
            AndroidDevice device = new AndroidDevice(var[i], random.nextInt(50000));
            device.state = AndroidDevice.State.INIT;
            device.initApp(DEFAULT_LOCAL_PROXY_PORT + i);
            devices.put(var[i], device);
        }
    }


    /**
     * By the device udid to start device
     * If State is INIT state; The device should start wechat app
     * If State is CLOSE state; The device should init device appium
     * After state is Init,Device should be start the wechat app
     * After device state is started,should be to allot device task queue
     *
     * @param udid
     * @see DefaultAndroidDeviceManager #static{}
     */
    public void startDeviceByUdid(String udid) throws InterruptedException {
        AndroidDevice androidDevice = devices.get(udid);
        if (androidDevice == null) {
            logger.error("This Device {} Is Not Exist; Please Your Device Udid", udid);
            return;
        }
        switch (androidDevice.state) {
            case INIT: {
                AndroidUtil.activeWechat(androidDevice);
                break;
            }
            case CLOSE: {

            }
            case RUNNING: {

            }
            case IDLE: {

            }
            default: {
                logger.info("Start The Device {} Is Success", udid);
            }
        }
    }

    /**
     * Resize Is For The Class Task Queue
     * Otherwise The Task Queue Size Not Zero
     */
    private static void resize() {

    }

    /**
     * 计算设备的任务类型
     *
     * @param udid
     * @return
     * @throws Exception
     */
    public TaskType calculationState(String udid) throws Exception {

        //所有订阅公众号的总数量
        long allSubscribe = subscribeDao.queryBuilder().where().eq("udid", udid).countOf();

        //未完成的公众号集合
        List<SubscribeAccount> notFinishR = subscribeDao.queryBuilder().where().
                eq("udid", udid).and().
                eq("status", SubscribeAccount.CrawlerState.NOFINISH.status).
                query();

        //今日订阅的公众号数量
        Date date = new Date();

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        String nowStr = df.format(date);

        long todaySubscribe = subscribeDao.queryBuilder().where()
                .eq("udid", udid)
                .eq("insert_time", nowStr)
                .countOf();

        long hasFailRecord = failRecordDao.queryBuilder().where().eq("udid", udid).countOf();

        if (allSubscribe > 993 && todaySubscribe >= 40) {
            return TaskType.CRAWLER;
        } else if (allSubscribe < 993 && todaySubscribe >= 40) {
            return TaskType.CRAWLER;
        } else if (allSubscribe < 993) {
            if (notFinishR.size() == 0) {
                if (hasFailRecord == 0) {
                    return TaskType.SUBSCRIBE;
                } else {
                    return TaskType.CRAWLER;
                }
            } else {
                return TaskType.CRAWLER;
            }
        }
        return null;
    }

    /**
     * 启动入口
     */
    public void boot() {
        devices.forEach((k, v) -> {
            try {
                TaskType taskType = calculationState(v.udid);
                DefaultWechatAdapter adapter = new DefaultWechatAdapter.
                        Builder().
                        device(v).
                        executor(Executors.newFixedThreadPool(1)).
                        taskType(taskType).
                        build();

                adapter.start(new Callback() {
                    @Override
                    public void onFailure(AndroidCollapseException e, Queue<String> queue) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onSuccess() {
                        logger.info("start success ...");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    /**
     * 关闭入口
     */
    public void shutdownNow(String udid) {
        AndroidDevice androidDevice = devices.get(udid);

        if (androidDevice == null) {
            logger.error("Not Has This Device : {}", udid);
            return;
        }

        AndroidDevice.State state = androidDevice.state;

        switch (state) {
            case IDLE:
            case RUNNING:
            case INIT:
            case CLOSE:
            default: {

            }
        }
    }
}
