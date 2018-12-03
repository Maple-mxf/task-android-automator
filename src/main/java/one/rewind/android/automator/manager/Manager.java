package one.rewind.android.automator.manager;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.j256.ormlite.dao.GenericRawResults;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.adapter.WechatAdapter;
import one.rewind.android.automator.model.BaiduTokens;
import one.rewind.android.automator.model.DBTab;
import one.rewind.android.automator.model.SubscribeMedia;
import one.rewind.android.automator.model.TaskType;
import one.rewind.android.automator.util.AndroidUtil;
import one.rewind.android.automator.util.DBUtil;
import one.rewind.android.automator.util.DateUtil;
import one.rewind.io.server.Msg;
import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONArray;
import spark.Route;
import spark.Spark;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

/**
 * Create By 2018/11/20
 * Description:
 */
public class Manager {

    /**
     * 存储无任务设备信息 利用建监听者模式实现设备管理
     */
    private BlockingQueue<WechatAdapter> idleAdapters = Queues.newLinkedBlockingDeque(Integer.MAX_VALUE);

    /**
     * 存储需要订阅的公众号 使用stack栈  先进后出
     */
    private Stack<String> mediaStack = new Stack<>();

    /**
     * API接口传输过来的公众号信息  如果这个队列不为空  优先抓取这个这个队列中的公众号
     */
    private Queue<String> apiMedias = Queues.newConcurrentLinkedQueue();

    /**
     * 所有设备的信息
     */
    private List<AndroidDevice> devices = Lists.newArrayList();

    /**
     * 初始分页参数
     */
    private static int startPage = 20;

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

    private void initMediaStack() {
        Set<String> set = Sets.newHashSet();
        DBUtil.obtainFullData(set, startPage, AndroidUtil.obtainDevices().length * 40);
        mediaStack.addAll(set);
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
        }
    }

    /**
     * 异步启动设备
     */
    public void startManager() throws InterruptedException, SQLException {

        init();

        reset();

        initMediaStack();


        for (AndroidDevice device : devices) {
            device.startAsync();
            idleAdapters.add(new WechatAdapter(device));
        }

        startTimer(); //开启恢复百度API  token 状态

        while (true) {

            //阻塞线程
            WechatAdapter adapter = idleAdapters.take();
            //获取到休闲设备进行任务执行
            execute(adapter);
        }

    }


    private void execute(WechatAdapter adapter) {
        try {
            //计算任务类型
            adapter.getDevice().taskType = calculateTaskType(adapter.getDevice().udid);
            //初始化任务队列
            switch (adapter.getDevice().taskType) {
                case SUBSCRIBE: {
                    initSubscribeQueue(adapter.getDevice());
                    break;
                }
                case CRAWLER: {
                    initCrawlerQueue(adapter.getDevice());
                    break;
                }
                default:
                    System.out.println("没有匹配的任务类型");
            }
            adapter.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void addIdleAdapter(WechatAdapter adapter) {
        synchronized (this) {
            this.idleAdapters.add(adapter);
        }
    }


    private void initSubscribeQueue(AndroidDevice device) throws SQLException {
        int numToday = DBUtil.obtainSubscribeNumToday(device.udid);
        if (numToday >= 40) {
            device.taskType = TaskType.WAIT;
        } else {
            int tmp = 40 - numToday;
            try {
                for (int i = 0; i < tmp; i++) {
                    if (!mediaStack.empty()) {
                        //如果没有数据了 先初始化订阅的公众号
                        startPage += 2;
                        initMediaStack();
                    }
                    if (!mediaStack.empty()) {
                        device.queue.add(mediaStack.pop());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

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

    private int obtainSubscribeNumToday(String udid) throws SQLException {
        GenericRawResults<String[]> results = DBTab.subscribeDao.
                queryRaw("select count(id) as number from wechat_subscribe_account where `status` not in (2) and udid = ? and to_days(insert_time) = to_days(NOW())",
                        udid);
        String[] firstResult = results.getFirstResult();
        String var = firstResult[0];
        return Integer.parseInt(var);
    }

    private void reset() throws SQLException {
        List<SubscribeMedia> accounts = DBTab.subscribeDao.queryForAll();
        for (SubscribeMedia v : accounts) {
            try {
                if (v.status == 2 || v.status == 1 || v.retry_count >= 5) {
                    continue;
                }

                long countOf = DBTab.essayDao.
                        queryBuilder().
                        where().
                        eq("media_nick", v.media_name).
                        countOf();
                if ((countOf >= v.number || Math.abs(v.number - countOf) <= 5) && countOf > 0) {
                    v.retry_count = 5;
                    v.status = SubscribeMedia.CrawlerState.FINISH.status;
                    v.number = (int) countOf;
                } else {
                    v.status = SubscribeMedia.CrawlerState.NOFINISH.status;
                    v.retry_count = 0;
                    if (v.number == 0) v.number = 100;
                }
                v.update();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private void startTimer() {
        Timer timer = new Timer(false);
        Date nextDay = DateUtil.buildDate();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    List<BaiduTokens> tokens = DBTab.tokenDao.queryForAll();
                    for (BaiduTokens v : tokens) {
                        if (!DateUtils.isSameDay(v.update_time, new Date())) {
                            v.count = 0;
                            v.update_time = new Date();
                            v.update();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, nextDay, 1000 * 60 * 60 * 24);
    }

    public static void main(String[] args) throws InterruptedException, SQLException {

        Manager manage = getInstance();

        manage.startManager(); //开启任务执行

        Spark.get("/push", manage.pushMedias);
    }

    /**
     * template:["芋道源码","淘宝网"]
     */
    private Route pushMedias = (req, res) -> {

        String body = req.body();

        JSONArray array = new JSONArray(body);

        for (Object v : array) {
            apiMedias.add((String) v);
        }
        return new Msg<>(1);
    };


    /**
     * 如果公众号已经被订阅了,移除队列
     *
     * @param media
     */
    private void parsingMedia(String media) {
        try {
            SubscribeMedia result = DBTab.subscribeDao.queryBuilder().where().eq("media_name", media).queryForFirst();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
