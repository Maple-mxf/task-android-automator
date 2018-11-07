package one.rewind.android.automator;

import com.j256.ormlite.dao.Dao;
import one.rewind.android.automator.adapter.DefaultWechatAdapter;
import one.rewind.android.automator.model.SubscribeAccount;
import one.rewind.android.automator.model.TaskType;
import one.rewind.android.automator.util.AndroidUtil;
import one.rewind.android.automator.util.DBUtil;
import one.rewind.db.DaoManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Create By 2018/10/19
 * Description   多设备管理
 */
public class DefaultAndroidDeviceManager {

    private static Dao<SubscribeAccount, String> subscribeDao;

    public volatile static boolean running = false;

    public static BlockingQueue<String> originalAccounts = new LinkedBlockingDeque<>();

    private DefaultAndroidDeviceManager() {
    }

    public static ConcurrentHashMap<String, AndroidDevice> devices = new ConcurrentHashMap<>();

    private static DefaultAndroidDeviceManager instance;

    public static final int DEFAULT_LOCAL_PROXY_PORT = 48454;

    public static DefaultAndroidDeviceManager getInstance() {
        synchronized (DefaultAndroidDeviceManager.class) {
            if (instance == null) {
                instance = new DefaultAndroidDeviceManager();
            }
            return instance;
        }
    }

    private static List<AndroidDevice> obtainAvailableDevices() {
        synchronized (DefaultAndroidDeviceManager.class) {
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
     * start all device
     * 任务队列初始化放在adapter中,更加便捷可控制
     *
     * @throws Exception
     */
    public void startManager() throws Exception {
        List<AndroidDevice> androidDevices = obtainAvailableDevices();

        for (AndroidDevice device : androidDevices) {

            TaskType taskType = calculateState(device.udid);

            DefaultWechatAdapter adapter =
                    new DefaultWechatAdapter.
                            Builder().
                            taskType(taskType).
                            device(device).
                            build();
            adapter.start();
        }
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
        long allSubscribe = subscribeDao.queryBuilder().where().eq("udid", udid).countOf();

        //未完成的公众号集合
        List<SubscribeAccount> notFinishR = subscribeDao.queryBuilder().where().
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
