package one.rewind.android.automator.manager;

import com.google.common.collect.Lists;
import com.j256.ormlite.dao.GenericRawResults;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.adapter.LooseWechatAdapter3;
import one.rewind.android.automator.model.DBTab;
import one.rewind.android.automator.model.SubscribeMedia;
import one.rewind.android.automator.model.TaskType;
import one.rewind.android.automator.util.AndroidUtil;
import one.rewind.android.automator.util.DBUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * Create By 2018/11/20
 * Description:
 */
public class Manager {

    /**
     * 存储无任务设备信息 利用建监听者模式实现设备管理
     */
    private BlockingQueue<LooseWechatAdapter3> idleAdapters = new LinkedBlockingQueue<>(Integer.MAX_VALUE);

    /**
     * 所有设备的信息
     */
    private List<AndroidDevice> devices = Lists.newArrayList();

    /**
     * 单例
     */
    private static Manager manager;


    private Manager() {
    }

    public static Manager getInstance() {
        if (manager == null) {
            manager = new Manager();
        }
        return manager;
    }

    /**
     * 初始化设备
     */
    private void init() {
        String[] var = AndroidUtil.obtainDevices();
        Random random = new Random();
        for (String aVar : var) {
            AndroidDevice device = new AndroidDevice(aVar, random.nextInt(50000));
            devices.add(device);
            LooseWechatAdapter3 adapter = new LooseWechatAdapter3(device);
            idleAdapters.add(adapter);
        }
    }

    /**
     * 异步启动设备
     */
    public void startManager() throws InterruptedException {
        init();
        for (AndroidDevice device : devices) {
            device.startAsync();
            idleAdapters.add(new LooseWechatAdapter3(device));
        }

        while (true) {
            //阻塞线程
            LooseWechatAdapter3 adapter = idleAdapters.take();
            execute(adapter);
        }

    }


    /**
     * 分配任务
     *
     * @param adapter
     */
    private void execute(LooseWechatAdapter3 adapter) {
        try {
            //计算任务类型
            adapter.getDevice().taskType = calculateTaskType(adapter.getDevice().udid);
            //初始化任务队列
            switch (adapter.getDevice().taskType) {
                case SUBSCRIBE:
                    initSubscribeQueue(adapter.getDevice());
                    break;
                case CRAWLER:
                    initCrawlerQueue(adapter.getDevice());
                    break;
                case WAIT:
                    //手机睡眠
                    break;
                case FINAL:
                    //手机设备移除
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void addIdleAdapter(LooseWechatAdapter3 adapter) {
        idleAdapters.add(adapter);
    }
    

    /**
     * 订阅任务分配
     *
     * @param device 设备
     * @throws SQLException
     */
    private void initSubscribeQueue(AndroidDevice device) throws SQLException {
        int numToday = DBUtil.obtainSubscribeNumToday(device.udid);
        if (numToday >= 40) {
            device.taskType = TaskType.WAIT;
        } else {
            int tmp = 40 - numToday;
            try {
                for (int i = 0; i < tmp; i++) {

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 从数据库中取数据
     *
     * @param device
     * @throws SQLException
     */
    private void initCrawlerQueue(AndroidDevice device) throws SQLException {
        List<SubscribeMedia> accounts =
                DBTab.subscribeDao.
                        queryBuilder().
                        where().
                        eq("udid", device.udid).
                        and().
                        eq("status", SubscribeMedia.CrawlerState.NOFINISH.status).
                        query();
        if (accounts.size() == 0) {
            device.taskType = TaskType.WAIT;
            return;
        }
        device.queue.addAll(accounts.stream().map(v -> v.media_name).collect(Collectors.toSet()));
    }


    /**
     * 计算任务类型
     *
     * @param udid
     * @return
     * @throws Exception
     */
    private TaskType calculateTaskType(String udid) throws Exception {

        long allSubscribe = DBTab.subscribeDao.queryBuilder().where().eq("udid", udid).countOf();

        List<SubscribeMedia> notFinishR = DBTab.subscribeDao.queryBuilder().where().
                eq("udid", udid).and().
                eq("status", SubscribeMedia.CrawlerState.NOFINISH.status).
                query();

        int todaySubscribe = obtainSubscribeNumToday(udid);

        if (allSubscribe >= 993) {
            if (notFinishR.size() == 0) {
                return TaskType.FINAL;   //当前设备订阅的公众号已经到上限
            }
            return TaskType.CRAWLER;
        } else if (todaySubscribe >= 40) {

            if (notFinishR.size() == 0) {
                return TaskType.WAIT;
            }
            return TaskType.CRAWLER;
        } else {
            if (notFinishR.size() == 0) {
                return TaskType.SUBSCRIBE;
            } else {
                return TaskType.CRAWLER;
            }
        }
    }

    /**
     * 获取当前设备今天订阅了多少公众号
     *
     * @param udid
     * @return
     * @throws SQLException
     */
    private int obtainSubscribeNumToday(String udid) throws SQLException {
        GenericRawResults<String[]> results = DBTab.subscribeDao.
                queryRaw("select count(id) as number from wechat_subscribe_account where `status` not in (2) and udid = ? and to_days(insert_time) = to_days(NOW())",
                        udid);
        String[] firstResult = results.getFirstResult();
        String var = firstResult[0];
        return Integer.parseInt(var);
    }
}
