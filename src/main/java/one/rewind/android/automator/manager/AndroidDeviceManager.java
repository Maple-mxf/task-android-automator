package one.rewind.android.automator.manager;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.*;
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
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.redisson.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Route;
import spark.Spark;

import javax.annotation.concurrent.ThreadSafe;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author maxuefeng[m17793873123@163.com]
 */
@ThreadSafe
public class AndroidDeviceManager {

    private ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));

    private static Logger logger = LoggerFactory.getLogger(AndroidDeviceManager.class);

    /**
     * redis 客户端
     */
    public static RedissonClient redisClient = RedissonAdapter.redisson;

    /**
     * 存储无任务设备信息 利用建监听者模式实现设备管理
     */
    private BlockingQueue<WechatAdapter> idleAdapters = Queues.newLinkedBlockingDeque(Integer.MAX_VALUE);

    /**
     */
    private Stack<String> mediaStack = new Stack<>();

    /**
     * 所有设备的信息
     */
    private List<AndroidDevice> devices = new ArrayList<>();

    /**
     * 初始分页参数
     */
    private static int startPage = 20;

    /**
     * 单例
     */
    private static AndroidDeviceManager manager;

    private AndroidDeviceManager() {
    }

    public static AndroidDeviceManager me() {
        if (manager == null) {
            manager = new AndroidDeviceManager();
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
        for (String aVar : var) {
            AndroidDevice device = new AndroidDevice(aVar);
            logger.info("udid: " + device.udid);
            devices.add(device);
            logger.info("添加device {} 到容器中", device.udid);
        }
    }

    @Deprecated
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

        //开启恢复百度API  token 状态
        resetOCRToken();

        while (true) {

            logger.info("任务执行......");

            WechatAdapter adapter = idleAdapters.take();

            logger.info("获取到Adapter....");
            // 获取到休闲设备进行任务执行
            execute(adapter);

        }
    }

    // 开启任务

    private void execute(WechatAdapter adapter) {
        try {
            logger.info("start executed");

            //计算任务类型
            adapter.getDevice().taskType = calculateTaskType(adapter);
            //初始化任务队列
            switch (adapter.getDevice().taskType) {
                case SUBSCRIBE: {
                    // 只分配一个任务去订阅   订阅完了之后立马切换到数据采集任务
                    initSubscribeSingleQueue(adapter.getDevice());
                    break;
                }
                case CRAWLER: {
                    initCrawlerQueue(adapter.getDevice());
                    break;
                }
                default:
                    logger.info("当前没有匹配到任何任务类型!");
            }
            adapter.start();
        } catch (Exception e) {
            logger.error("初始化任务失败！");
        }
    }


    public void addIdleAdapter(WechatAdapter adapter) {
        synchronized (this) {
            this.idleAdapters.add(adapter);
        }
    }


    // TODO
    private void initSubscribeSingleQueue(AndroidDevice device) throws SQLException {

        device.queue.clear();
        // 计算今日还能订阅多少
        int numToday = DBUtil.obtainSubscribeNumToday(device.udid);

        // 处于等待状态
        if (numToday > 40) {
            device.taskType = TaskType.WAIT;
        } else {

            // 从redis中加载数据
            RPriorityQueue<String> priorityQueue = redisClient.getPriorityQueue(Tab.TOPIC_MEDIA);

            // TODO
            if (priorityQueue.size() == 0) {

                if (mediaStack.isEmpty()) {
                    // 如果没有数据了 先初始化订阅的公众号
                    startPage += 2;
                    initMediaStack();
                }
                device.queue.add(mediaStack.pop());
            } else {

                // redis 存在API接口数据  直接加载redis中的数据
                device.queue.add(priorityQueue.poll());
            }
        }
    }


    /**
     * 优先分配redis中存储的API接口的任务公众号  由于redisson的缘故；抛出NullPointException  需要try-catch消化异常继续进行
     *
     * @param device 设备实例
     * @param number 需要初始化任务队列的size
     */
    @Deprecated
    private void priorityAllotAPITask(AndroidDevice device, int number) {

        RQueue<String> requests = redisClient.getQueue(Tab.REQUESTS);

        if (!requests.isExists() || requests.size() == 0 || number == 0) return;   // 最近没有任务  number == 0 当前设备处于等待的状态

        synchronized (this) {

            for (String requestID : requests) {

                // 任务已经分配完毕
                if (device.queue.size() == number) return;

                // realName存储的是未完成任务的数据
                String realName = requestID + Tab.NO_OK_TASK_PROCESS_SUFFIX;

                RList<String> taskList = redisClient.getList(realName);

                int tmpSize = taskList.size();

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
                List<String> medias = taskList.readAll();
                // 任务添加
                for (int j = 0; j < var2; j++) {
                    device.queue.add(medias.get(j));
                }
            }
        }
    }

    // 从MySQL中初始化任务
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

    //
    private TaskType calculateTaskType(WechatAdapter adapter) throws Exception {

        String udid = adapter.getDevice().udid;

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
            // 当前设备订阅的号没有到达上限则分配订阅任务  有限分配订阅接口任务
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
//                    if (v.number != 0) continue;
                    // 有些公众号一片文章也没有
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

    }


    // 启动入口

    public static void main(String[] args) {

        AndroidDeviceManager manage = me();

        ListenableFuture<Boolean> result = manage.service.submit(new ManagerTask());

        Futures.addCallback(result, new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(@NullableDecl Boolean result) {
                logger.info("execute success ok!");
            }

            @Override
            public void onFailure(Throwable t) {
                logger.info("execute failed Not OK Please focus on this");
            }
        });
        logger.info("开启web服务!");
        Spark.port(8080);
        Spark.post("/push", manage.load, JSON::toJson);
        logger.info("web 服务开启完成!");
    }


    private Route load = (req, resp) -> {
        // 参数校验
        String body = req.body();
        if (Strings.isNullOrEmpty(body)) return new Msg<>(0, "请检查您的参数！");
        JSONObject result = new JSONObject(body);
        JSONArray mediasArray = result.getJSONArray("media");
        if (mediasArray == null || mediasArray.length() == 0) return new Msg<>(0, "请检查您的参数！");

        // 数据加载到redis
        String topic = Tab.REQUEST_ID_SUFFIX + DateUtil.timestamp();

        // 获取当前topic对应的media  优先级队列   先进先出
        RPriorityQueue<Object> topicMedia = redisClient.getPriorityQueue(Tab.TOPIC_MEDIA);

        List<String> alreadyCompleted = Lists.newArrayList();

        // 数据缓存到redis中
        for (Object tmp : mediasArray) {

            String var = (String) tmp;

            // 需要验证是否是历史任务？
            // 如果是历史任务   判断是否是否完成？
            //               1 已完成；发布消息通知订阅者
            //               2 未完成：改动数据库中的req_id字段  这种公众号执行可能存在很高的延迟
            SubscribeMedia media = Tab.subscribeDao.queryBuilder().where().eq("media_name", var).queryForFirst();
            if (media == null) {
                // 数据形式应该是   公众号名称+topic
                topicMedia.add(var + topic);
                logger.info("任务：公众号{}已加载到redis中...等待执行");

            } else {
                if (media.status == SubscribeMedia.State.FINISH.status) {

                    // 已完成任务
                    logger.info("公众号{}加入okSet,状态为:{}", media.media_name, media.status);

                    // 发布主题  这个地方发布主题是错误的   存在BUG  客户端会因为时序问题接收不到数据  TODO
//                    RTopic<Object> tmpTopic = redisClient.getTopic(topic);
//                    long k = tmpTopic.publish(media.media_name);
//                    logger.info("发布完毕！k: {} ; 主题名称： {}", k, tmpTopic);
                    alreadyCompleted.add(media.media_name);

                } else if (media.status == SubscribeMedia.State.NOT_FINISH.status) {

                    // status: 0 未完成   但是已经订阅
                    media.request_id = topic;
                    media.update();
                    logger.info("公众号{}已经订阅！任务尚未完成，状态为{}", media.media_name, media.status);
                }
            }


        }
        Map<String, Object> data = Maps.newHashMap();
        data.put("topic", topic);
        data.put("alreadyCompleted", alreadyCompleted);
        return new Msg<>(1, data);
    };


    static class ManagerTask implements Callable<Boolean> {
        @Override
        public Boolean call() throws Exception {

            AndroidDeviceManager manager = AndroidDeviceManager.me();

            // 初始化设备
            manager.init();

            // 重置数据库数据
            manager.reset();

            // 初始化
            manager.initMediaStack();

            for (AndroidDevice device : manager.devices) {
                device.startAsync();
                manager.idleAdapters.add(new WechatAdapter(device));
            }

            //开启恢复百度API  token 状态
            manager.resetOCRToken();

            while (true) {

                WechatAdapter adapter = manager.idleAdapters.take();

                // 获取到休闲设备进行任务执行
                manager.execute(adapter);
            }
        }
    }
}
