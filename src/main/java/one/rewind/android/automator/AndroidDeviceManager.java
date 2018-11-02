package one.rewind.android.automator;

import com.j256.ormlite.dao.Dao;
import one.rewind.android.automator.adapter.WechatAdapter;
import one.rewind.android.automator.model.SubscribeAccount;
import one.rewind.android.automator.model.TaskFailRecord;
import one.rewind.android.automator.model.TaskType;
import one.rewind.android.automator.util.AndroidUtil;
import one.rewind.db.DaoManager;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * Create By 2018/10/19
 * Description   多设备管理
 */
public class AndroidDeviceManager {

    private static Dao<SubscribeAccount, String> subscribeDao;

    private static Dao<TaskFailRecord, String> failRecordDao;

    public volatile static boolean running = false;

    /**
     * 逻辑定义:   @1 TaskType: @Crawler-> 只在数据库中查找未完成抓取的公众号.当然也包括了未完成任务的公众号
     *
     * @2 TaskType: @Subscribe->只需要将当前one.rewind.android.automator.AndroidDeviceManager#originalAccounts
     * 集合中的所有数据进行关注.
     * 任务指定:  计算每一台设备(一台设备对应一个微信号) ;任务类型的根据每个设备关注的每个号进行动态计算,合理算出当前设备的任务类型
     * <p>
     * one.rewind.android.automator.AndroidDeviceManager#originalAccounts 为空则全部设备分配为数据抓取
     */
    public static BlockingQueue<String> originalAccounts = new LinkedBlockingDeque<>();

    private AndroidDeviceManager() {
    }

    /**
     * key : udid
     * value: state
     */
    public static ConcurrentHashMap<String, AndroidDevice> devices = new ConcurrentHashMap<>();

    private static AndroidDeviceManager instance;

    public static final int DEFAULT_LOCAL_PROXY_PORT = 48454;

    public static AndroidDeviceManager getInstance() {
        synchronized (AndroidDeviceManager.class) {
            if (instance == null) {
                instance = new AndroidDeviceManager();
            }
            return instance;
        }
    }

    /**
     * 获得可用的设备
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

    //初始化设备
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
     * 启动
     *
     * @throws Exception
     */
    public void startManager() throws Exception {
        boolean flag = true;
        while (flag) {
            WechatAdapter.executor = Executors.newFixedThreadPool(obtainAvailableDevices().size() + 2);
            uncertainAllotTask();
            while (!WechatAdapter.executor.isTerminated()) {
                WechatAdapter.executor.awaitTermination(300, TimeUnit.SECONDS);
                System.out.println("progress: % " + WechatAdapter.executor.isTerminated());
            }
            if (subscribeDao.queryBuilder().countOf() > 100000) {
                flag = false;
            }
        }
    }

    /**
     * 更细致的任务分配   By Device
     * Dynamic set device state
     */
    public void uncertainAllotTask() throws Exception {
        running = true;
        List<AndroidDevice> availableDevices = obtainAvailableDevices();
        for (AndroidDevice device : availableDevices) {
            if (originalAccounts.size() == 0) {
                allotFormulateTask(device);
            } else {
                TaskType deviceTaskType = calcuState(device.udid);
                uncertainAllotTask(deviceTaskType, device);
            }

        }
    }


    /**
     * 分配固定的任务  crawler
     * 这种情景仅限于在任务队列的size == 0
     *
     * @param device
     * @see one.rewind.android.automator.AndroidDeviceManager#originalAccounts
     */
    private void allotFormulateTask(AndroidDevice device) throws SQLException {
        device.setClickEffect(false);
        List<SubscribeAccount> accounts = subscribeDao.queryBuilder().where().eq("udid", device.udid).
                and().
                eq("status", SubscribeAccount.CrawlerState.NOFINISH.status).
                query();
        if (accounts.size() > 0) {
            for (SubscribeAccount account : accounts) {
                device.queue.add(account.media_name);
            }
        }
        WechatAdapter adapter = new WechatAdapter(device);
        adapter.setTaskType(TaskType.CRAWLER);
        adapter.start(true);

    }


    /**
     * 当前设备任务 数据抓取
     * 一:  从失败任务中提取公众号
     * 二:  从订阅记录表中提取公众号
     *
     * @param taskType
     * @param device
     */
    private void uncertainAllotTask(TaskType taskType, AndroidDevice device) throws SQLException {
        //清空任务队列
        device.queue.clear();
        List<TaskFailRecord> records = failRecordDao.queryBuilder().where().eq("udid", device.udid).query();
        List<SubscribeAccount> subscribeAccounts = subscribeDao.
                queryBuilder().
                where().
                eq("udid", device.udid).
                query();
        //初始化任务队列
        if (records != null && records.size() != 0) {
            records.forEach(v -> device.queue.add(v.wxPublicName));
        }

        subscribeAccounts.forEach(v -> device.queue.add(v.media_name));

        if (TaskType.CRAWLER.equals(taskType)) {
            uncertainAllotCrawlerTask(device);
        } else if (TaskType.SUBSCRIBE.equals(taskType)) {
            uncertainAllotSubscribeTask(device);
        }
    }

    private void uncertainAllotCrawlerTask(AndroidDevice device) {
        device.setClickEffect(false);
        WechatAdapter adapter = new WechatAdapter(device);
        adapter.setTaskType(TaskType.CRAWLER);
        adapter.start(true);
    }

    private void uncertainAllotSubscribeTask(AndroidDevice device) {
        device.setClickEffect(false);
        WechatAdapter adapter = new WechatAdapter(device);
        adapter.setTaskType(TaskType.SUBSCRIBE);
        adapter.start(false);
    }


    /**
     * 主要计算设备当前应该处于的一个状态
     *
     * @param udid
     * @return
     * @throws Exception
     */
    public TaskType calcuState(String udid) throws Exception {

        //所有订阅公众号的总数量
        long allSubscribe = subscribeDao.queryBuilder().where().eq("udid", udid).countOf();

        //未完成的公众号集合
        List<SubscribeAccount> notFinishR = subscribeDao.queryBuilder().where().
                eq("udid", udid).
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

}
