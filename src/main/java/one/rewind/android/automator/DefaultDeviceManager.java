package one.rewind.android.automator;

import one.rewind.android.automator.adapter.WechatAdapter;
import one.rewind.android.automator.model.SubscribeAccount;
import one.rewind.android.automator.model.TaskType;
import one.rewind.android.automator.util.AndroidUtil;
import one.rewind.android.automator.util.DBUtil;
import one.rewind.db.DaoManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

/**
 * Create By 2018/10/19
 * Description   多设备管理
 */
public class DefaultDeviceManager {

    public volatile static boolean running = false;

    /**
     * 逻辑定义:   @1 TaskType: @Crawler-> 只在数据库中查找未完成抓取的公众号.当然也包括了未完成任务的公众号
     *
     * @2 TaskType: @Subscribe->只需要将当前one.rewind.android.automator.DefaultDeviceManager#originalAccounts
     * 集合中的所有数据进行关注.
     * 任务指定:  计算每一台设备(一台设备对应一个微信号) ;任务类型的根据每个设备关注的每个号进行动态计算,合理算出当前设备的任务类型
     * <p>
     * one.rewind.android.automator.DefaultDeviceManager#originalAccounts 为空则全部设备分配为数据抓取
     */
    public static BlockingQueue<String> originalAccounts = new LinkedBlockingDeque<>();

    private DefaultDeviceManager() {
    }

    public static ConcurrentHashMap<String, AndroidDevice> devices = new ConcurrentHashMap<>();

    private static DefaultDeviceManager instance;

    public static final int DEFAULT_LOCAL_PROXY_PORT = 48454;

    public static DefaultDeviceManager getInstance() {
        synchronized (DefaultDeviceManager.class) {
            if (instance == null) {
                instance = new DefaultDeviceManager();
            }
            return instance;
        }
    }

    /**
     * 获得可用的设备
     *
     * @return
     */
    private static List<AndroidDevice> obtainAvailableDevices() {
        synchronized (DefaultDeviceManager.class) {
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
            DBTab.subscribeDao = DaoManager.getDao(SubscribeAccount.class);
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
        WechatAdapter.executor = Executors.newFixedThreadPool(obtainAvailableDevices().size() + 2);
        uncertainAllotTask();
        while (!WechatAdapter.executor.isTerminated()) {
            WechatAdapter.executor.awaitTermination(300, TimeUnit.SECONDS);
            System.out.println("progress: % " + WechatAdapter.executor.isTerminated());
        }
    }

    /**
     * 任务分配   By Device
     * Dynamic set device state
     */
    private void uncertainAllotTask() throws Exception {
        running = true;
        List<AndroidDevice> availableDevices = obtainAvailableDevices();
        for (AndroidDevice device : availableDevices) {
            if (originalAccounts.size() == 0) {
                allotFormulateTask(device);
            } else {
                TaskType deviceTaskType = calculateState(device.udid);
                uncertainAllotTask(deviceTaskType, device);
            }

        }
    }


    /**
     * 分配固定的任务  crawler
     * 这种情景仅限于在任务队列的size = 0
     *
     * @param device
     * @see DefaultDeviceManager#originalAccounts
     */
    private void allotFormulateTask(AndroidDevice device) throws SQLException {
        device.setClickEffect(false);
        List<SubscribeAccount> accounts = DBTab.subscribeDao.queryBuilder().where().eq("udid", device.udid).
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
    private void uncertainAllotTask(TaskType taskType, AndroidDevice device) throws SQLException, InterruptedException {
        DBUtil.reset();
        int numToday = DBUtil.obtainSubscribeNumToday(device.udid);
        //清空任务队列
        device.queue.clear();
        if (TaskType.CRAWLER.equals(taskType)) {
            List<SubscribeAccount> var1 = DBTab.subscribeDao.queryBuilder().where().eq("udid", device.udid).and().eq("status", SubscribeAccount.CrawlerState.NOFINISH.status).query();
            for (SubscribeAccount account : var1) {
                device.queue.add(account.media_name);
            }
            uncertainAllotCrawlerTask(device);
        } else if (TaskType.SUBSCRIBE.equals(taskType)) {
            for (int i = 0; i < 40 - numToday; i++) {
                device.queue.add(originalAccounts.take());
            }
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
    private TaskType calculateState(String udid) throws Exception {

        //所有订阅公众号的总数量
        long allSubscribe = DBTab.subscribeDao.queryBuilder().where().eq("udid", udid).countOf();

        //未完成的公众号集合
        List<SubscribeAccount> notFinishR = DBTab.subscribeDao.queryBuilder().where().
                eq("udid", udid).and().
                eq("status", SubscribeAccount.CrawlerState.NOFINISH.status).
                query();

        int todaySubscribe = DBUtil.obtainSubscribeNumToday(udid);

        if (allSubscribe > 993 && todaySubscribe >= 40) {
            return TaskType.CRAWLER;
        } else if (allSubscribe < 993 && todaySubscribe >= 40) {
            return TaskType.CRAWLER;
        } else if (allSubscribe < 993) {
            if (notFinishR.size() == 0) {
                return TaskType.SUBSCRIBE;
            } else {
                return TaskType.CRAWLER;
            }
        }
        return null;
    }


}
