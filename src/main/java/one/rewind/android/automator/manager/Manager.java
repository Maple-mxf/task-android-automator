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
import one.rewind.json.JSON;
import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONArray;
import org.redisson.api.RQueue;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import spark.Route;
import spark.Spark;

import javax.annotation.concurrent.ThreadSafe;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

/**
 * Create By 2018/11/20
 * Description:
 */
@ThreadSafe
public class Manager {

    /**
     * is restart
     * <p>
     * <p>
     * manager记录上一次的重启时间，达到阈值重启appium
     *
     * @see AndroidDevice#isLock
     */
    public static volatile boolean restart = false;

    /**
     * last restart date
     * <p>
     * 上一次的重启时间 ，间隔机制为每隔4小时重启appium   重启appium需要注意  手机不能锁屏
     * <p>
     * 锁频会造成appium重启失败
     *
     * @see AndroidDevice#isLock
     */
    private static volatile long lastRestart = new Date().getTime();

    /**
     * redis 客户端
     */
    public static RedissonClient redisClient = RedissonAdapter.redisson;

    /**
     * 存储无任务设备信息 利用建监听者模式实现设备管理
     */
    private BlockingQueue<WechatAdapter> idleAdapters = Queues.newLinkedBlockingDeque(Integer.MAX_VALUE);

    /**
     * 存储需要订阅的公众号 使用stack栈  先进后出
     */
    private Stack<String> mediaStack = new Stack<>();

    /**
     * 存储请求ID   对应redis中的数据
     * <p>
     * Thread safe
     */
    public static final List<String> REQUEST_ID_COLLECTION = Lists.newCopyOnWriteArrayList();


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

    public void startManager() throws InterruptedException, SQLException {


        // 初始化设备
        init();

        // 重置数据库数据
        reset();

        // 初始化
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
                // 如果在redis中存在任务  优先获取redis中的任务
//                priorityAllotAPITask(device, tmp);

                // 如果redis中的任务没有初始化成功  则换种方式初始化任务队列
                if (device.queue.size() == 0) {
                    for (int i = 0; i < tmp; i++) {
                        if (!mediaStack.empty()) {
                            // 如果没有数据了 先初始化订阅的公众号
                            startPage += 2;
                            initMediaStack();
                        }
                        if (!mediaStack.empty()) {
                            device.queue.add(mediaStack.pop());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 优先分配redis中存储的API接口的任务公众号  由于redisson的缘故；抛出NullPointException  需要try-catch消化异常继续进行
     *
     * @param device 设备实例
     * @param number 需要初始化任务队列的size
     */
    private void priorityAllotAPITask(AndroidDevice device, int number) {

        synchronized (this) {

            int requestIDs = REQUEST_ID_COLLECTION.size();

            if (requestIDs == 0) return;

            if (number == 0) return; // 处于等待的状态

            for (String tmpName : REQUEST_ID_COLLECTION) {

                // 任务已经分配完毕
                if (device.queue.size() == number) return;

                // tmpName存储的是已完成的任务集合
                String realName = exchange(tmpName);

                RQueue<String> taskQueue = redisClient.getQueue(realName);

                int tmpSize = taskQueue.size();

                // 继续进行下一个任务集合

                if (tmpSize == 0) continue;

                // 计算device还需要多少个公众号任务
                int var1 = number - device.queue.size();

                int var2;

                if (tmpSize > var1) {

                    var2 = var1;

                } else {
                    var2 = tmpSize;

                }
                // 任务添加
                for (int j = 0; j < var2; j++) {
                    device.queue.add(taskQueue.poll());
                }
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
                        eq("status", SubscribeMedia.State.NOT_FINISH.status).
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
                eq("status", SubscribeMedia.State.NOT_FINISH.status).
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
                    v.status = SubscribeMedia.State.FINISH.status;
                    v.number = (int) countOf;
                } else {
                    v.status = SubscribeMedia.State.NOT_FINISH.status;
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

        // 接受任务
        Spark.post("/push", manage.pushMedias);

        // 获取已完成的数据
        Spark.post("/medias", manage.medias);

        // 获取真实文章数据
//        Spark
    }

    /**
     * template:["芋道源码","淘宝网"]
     */
    private Route pushMedias = (req, res) -> {

        String body = req.body();

        try {
            JSONArray array = new JSONArray(body);

            String requestID = parseRequestID(array);

            // 将请求ID存储在内存集合中
            REQUEST_ID_COLLECTION.add(requestID);

            return new Msg<>(1, requestID);
        } catch (Exception e) {
            e.printStackTrace();
            return new Msg<>(0, "参数不合法");
        }

    };


    /**
     * By requestID obtain finish medias
     */
    private Route medias = (req, res) -> {

        String request_id = req.params("request_id");

        RSet<Object> result = redisClient.getSet(request_id);


        return new Msg<>(1, result);
    };


    /**
     * By media name obtain essays data
     */
    private Route essays = (req, res) -> {

        String body = req.body();

        JSONArray medias = JSON.fromJson(body, JSONArray.class);

        //medias is an array

        List<String> ids = Lists.newArrayList();

//        Tab.essayDao.queryBuilder()

        return null;
    };


    /**
     * @param array api接口传递的公众号数据
     */
    private String parseRequestID(JSONArray array) throws SQLException {

        synchronized (this) {

            String request_id = WechatAdapter.REQ_SUFFIX + DateUtil.timestamp();

            // 请求唯一标示 16K parse wechat adapter
            String requestID_$Finish = request_id + "_finish";

            // 没有完成任务的唯一请求
            String requestID_$Not_Finish = requestID_$Finish.replace("finish", "not_finish");

            // 已完成任务的集合
            RQueue<String> okRequestQueue = redisClient.getQueue(requestID_$Finish);

            // 未完成任务的集合
            RQueue<String> notOkRequestQueue = redisClient.getQueue(requestID_$Not_Finish);

            for (Object var : array) {

                String tmp = (String) var;

                SubscribeMedia media = Tab.subscribeDao.queryBuilder().where().eq("media_name", tmp).queryForFirst();

                if (media != null) {

                    // media可能是历史任务  也可能当前media的任务已经完成了

                    // 使用requestID作为redis的key   value存放一个有序集合

                    // 已经完成了任务，将当前的公众号名称存储到redis中

                    // media的状态可能是Finish(任务在DB中已经存在且完成) 也可能是NOT_EXIST(不存在)

                    if (media.status == SubscribeMedia.State.FINISH.status || media.status == SubscribeMedia.State.NOT_EXIST.status) {
                        okRequestQueue.add(media.media_name);
                    }

                    // else 处理任务的优先级  --
                } else {

                    // 针对于tmp进行处理  将tmp进行字符串包装 req_id

                    // 订阅成功后 存储数据库req_id
                    notOkRequestQueue.add(tmp + request_id);
                }
            }
            return request_id;
        }
    }

    /**
     * 转换redis队列名称  使用有限制，请注意
     *
     * @param collectionName
     * @return
     */
    @SuppressWarnings("uncheck")
    public static String exchange(String collectionName) {
        if (collectionName.endsWith("not_finish")) {
            return collectionName.replace("not_finish", "finish");
        } else if (collectionName.endsWith("finish")) {
            return collectionName.replace("finish", "not_finish");
        }
        return collectionName;
    }
}
