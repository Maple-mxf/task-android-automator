package one.rewind.android.automator.adapter;

import com.google.common.collect.Sets;
import com.j256.ormlite.dao.GenericRawResults;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.model.DBTab;
import one.rewind.android.automator.model.SubscribeMedia;
import one.rewind.android.automator.model.TaskType;
import one.rewind.android.automator.util.AndroidUtil;
import one.rewind.android.automator.util.DBUtil;
import one.rewind.android.automator.util.DateUtil;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Create By 2018/10/10
 * Description: 微信的自动化操作
 */
public class LooseWechatAdapter2 extends AbstractWechatAdapter {

    private ExecutorService executor;

    //使用线程安全队列防止订阅重读的公众号
    private static BlockingQueue<String> subscribeQueue = new LinkedBlockingDeque<>();

    static {
        //必须传入set防止重复
        Set<String> set = Sets.newHashSet();

        DBUtil.obtainFullData(set, 10, AndroidUtil.obtainDevices().length * 40);

        subscribeQueue.addAll(set);
    }

    private void setExecutor() {
        if (this.executor == null) {
            this.executor =
                    new ThreadPoolExecutor(0,
                            Integer.MAX_VALUE,
                            60,
                            TimeUnit.SECONDS,
                            new SynchronousQueue<>(),
                            threadFactory("waa-" + UUID.randomUUID().toString().replace("-", "")
                            ));
        }
    }

    private ThreadFactory threadFactory(final String name) {
        return runnable -> {
            Thread result = new Thread(runnable, name);
            result.setDaemon(false);
            return result;
        };
    }

    public LooseWechatAdapter2(AndroidDevice device) {
        super(device);
    }

    public void start() {
        this.setExecutor();
        Task task = new Task();
        this.executor.execute(task);
    }


    class Task implements Runnable {

        private boolean retry;

        boolean getRetry() {
            return retry;
        }

        void setRetry(boolean retry) {
            this.retry = retry;
        }

        @Override
        public void run() {
            execute();
        }

        private void execute() {
            setCountVal();
            boolean flag = true;
            try {
                while (flag) {

                    //计算任务类型
                    taskType = calculateTaskType(device.udid);

                    if (TaskType.FINAL.equals(taskType)) {
                        flag = false;  //当前设备订阅的公众号抓取的文章已经达到了上限
                        continue;
                    }

                    if (TaskType.WAIT.equals(taskType)) {
                        //需要计算啥时候到达明天   到达明天的时候需要重新分配任务
                        Date nextDay = DateUtil.buildDate();
                        Date thisDay = new Date();
                        long waitMills = Math.abs(nextDay.getTime() - thisDay.getTime());
                        Thread.sleep(waitMills + 1000 * 60 * 5);
                        taskType = TaskType.SUBSCRIBE;
                    }

                    this.setRetry(TaskType.CRAWLER.equals(taskType));

                    //开始任务
                    if (TaskType.SUBSCRIBE.equals(taskType)) {
                        initSubscribeQueue();
                        int length = device.queue.size();
                        for (int i = 0; i < length; i++) {
                            digestionSubscribe(device.queue.poll());
                        }
                    } else if (TaskType.CRAWLER.equals(taskType)) {
                        initCrawlerQueue();
                        int length = device.queue.size();
                        for (int i = 0; i < length; i++) {
                            String mediaName = device.queue.poll();
                            lastPage = false;
                            while (!lastPage) {
                                digestionCrawler(mediaName, getRetry());
                            }
                            updateMediaState(mediaName, udid);
                            for (int j = 0; j < 5; j++) {
                                driver.navigate().back();
                                Thread.sleep(1000);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        private void initSubscribeQueue() throws SQLException {
            int numToday = DBUtil.obtainSubscribeNumToday(device.udid);
            if (numToday >= 40) {
                taskType = TaskType.WAIT;
            } else {
                int tmp = 40 - numToday;
                try {
                    for (int i = 0; i < tmp; i++) {
                        device.queue.add(subscribeQueue.take());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void initCrawlerQueue() throws SQLException {
            List<SubscribeMedia> accounts =
                    DBTab.subscribeDao.
                            queryBuilder().
                            where().
                            eq("udid", device.udid).
                            and().
                            eq("status", SubscribeMedia.CrawlerState.NOFINISH.status).
                            query();
            if (accounts.size() == 0) {
                taskType = TaskType.WAIT;
                return;
            }
            device.queue.addAll(accounts.stream().map(v -> v.media_name).collect(Collectors.toSet()));
        }
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

    private void updateMediaState(String mediaName, String udid) throws Exception {
        SubscribeMedia account = DBTab.subscribeDao.
                queryBuilder().
                where().
                eq("media_name", mediaName).
                and().
                eq("udid", udid).
                queryForFirst();

        if (account != null) {
            long countOf = DBTab.essayDao.
                    queryBuilder().
                    where().
                    eq("media_nick", mediaName).
                    countOf();
            account.number = (int) countOf;
            account.status = (countOf == 0 ? SubscribeMedia.CrawlerState.NOMEDIANAME.status : SubscribeMedia.CrawlerState.FINISH.status);
            account.status = 1;
            account.update_time = new Date();
            account.retry_count = 5;
            account.update();
        }
    }
}
