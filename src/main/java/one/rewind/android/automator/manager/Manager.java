package one.rewind.android.automator.manager;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.j256.ormlite.dao.GenericRawResults;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.adapter.WechatAdapter;
import one.rewind.android.automator.model.BaiduTokens;
import one.rewind.android.automator.model.SubscribeMedia;
import one.rewind.android.automator.model.Tab;
import one.rewind.android.automator.model.TaskType;
import one.rewind.android.automator.util.AndroidUtil;
import one.rewind.android.automator.util.DBUtil;
import one.rewind.android.automator.util.DateUtil;
import one.rewind.db.RedissonAdapter;
import one.rewind.io.server.Msg;
import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONArray;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RSortedSet;
import org.redisson.api.RedissonClient;
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
     * is restart
     * <p>
     * <p>
     * manager记录上一次的重启时间，达到阈值重启appium
     */
    public static volatile boolean restart = false;

    /**
     * last restart date
     * <p>
     * 上一次的重启时间 ，间隔机制为每隔4小时重启appium   重启appium需要注意  手机不能锁频
     */
    private static volatile long lastRestart = new Date().getTime();

    /**
     * redis 客户端
     */
    private RedissonClient redisClient = RedissonAdapter.redisson;

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

    public static Manager me() {
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

        resetOCRToken(); //开启恢复百度API  token 状态

        while (true) {

            // 阻塞线程

            // 需要定期重启appium 重启代理等等

            // 2 停止代理
            // 3 关闭本地appium
            // 4 退出当前方法
            // 5 API接口传递过来的数据持久化


            WechatAdapter adapter = idleAdapters.take();

            // 获取到休闲设备进行任务执行
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
                Tab.subscribeDao.
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

        long allSubscribe = Tab.subscribeDao.queryBuilder().where().eq("udid", udid).countOf();

        List<SubscribeMedia> notFinishR = Tab.subscribeDao.queryBuilder().where().
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
        GenericRawResults<String[]> results = Tab.subscribeDao.
                queryRaw("select count(id) as number from wechat_subscribe_account where `status` not in (2) and udid = ? and to_days(insert_time) = to_days(NOW())",
                        udid);
        String[] firstResult = results.getFirstResult();
        String var = firstResult[0];
        return Integer.parseInt(var);
    }

    private void reset() throws SQLException {
        List<SubscribeMedia> accounts = Tab.subscribeDao.queryForAll();
        for (SubscribeMedia v : accounts) {
            try {
                if (v.status == 2 || v.status == 1 || v.retry_count >= 5) {
                    continue;
                }

                long countOf = Tab.essayDao.
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


    // reset 百度API token状态

    private void resetOCRToken() {
        Timer timer = new Timer(false);
        Date nextDay = DateUtil.buildDate();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    List<BaiduTokens> tokens = Tab.tokenDao.queryForAll();
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

//        timer.schedule();
    }

    public static void main(String[] args) throws InterruptedException, SQLException {

        Manager manage = me();

        manage.startManager(); //开启任务执行

        Spark.get("/push", manage.pushMedias);
    }

    /**
     * template:["芋道源码","淘宝网"]
     */
    private Route pushMedias = (req, res) -> {

        String body = req.body();

        try {
            JSONArray array = new JSONArray(body);

            for (Object v : array) {
                apiMedias.add((String) v);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Msg<>(0, "参数不合法");
        }
        return new Msg<>(1);
    };

    /**
     * @param array api接口传递的公众号数据
     */
    private void parseRequestID(JSONArray array) throws SQLException {

        // 请求唯一标示
        String requestID = WechatAdapter.REQ_SUFFIX + UUID.randomUUID().toString().replaceAll("-", "");

        // 作为临时集合辅助操作业务逻辑
        RSortedSet<String> tmpSet = redisClient.getSortedSet(requestID + "_tmp");

        for (Object var : array) {

            String tmp = (String) var;

            tmpSet.add(tmp);

            SubscribeMedia media = Tab.subscribeDao.queryBuilder().where().eq("media_name", tmp).queryForFirst();

            if (media != null) {

                // media可能是历史任务  也可能当前media的任务已经完成了

                // 使用requestID作为redis的key   value存放一个有序集合  每一个元素的分数作为当前的时间戳

                // 已经完成了任务，将当前的公众号名称存储到redis中

                if (media.status == SubscribeMedia.CrawlerState.FINISH.status || media.status == SubscribeMedia.CrawlerState.NOMEDIANAME.status) {

                    RScoredSortedSet<Object> sortedSet = redisClient.getScoredSortedSet(requestID);

                    // 时间戳作为分数  排序规则
                    sortedSet.add(new Date().getTime(), media.media_name);

                }
            } else {

                // 添加到任务队列中    tmp拼接一个字符串作为request标示   tmp$#{requestID}
                apiMedias.add(tmp + requestID);

                // 插入到数据中作为业务字段辅助操作   java技术栈$req_KgfhazvcbfrteUHjsdf90
            }
        }
    }

}
