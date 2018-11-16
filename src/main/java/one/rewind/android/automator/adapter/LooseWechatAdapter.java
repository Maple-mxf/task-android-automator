package one.rewind.android.automator.adapter;

import com.google.common.collect.Sets;
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
public class LooseWechatAdapter extends AbstractWechatAdapter {

    private ExecutorService executor;

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

    private static ThreadFactory threadFactory(final String name) {
        return runnable -> {
            Thread result = new Thread(runnable, name);
            result.setDaemon(false);
            return result;
        };
    }

    private LooseWechatAdapter(AndroidDevice device) {
        super(device);
    }

    public void start() throws Exception {

        //采取非递归手段
        this.setExecutor();

        boolean flag = true;

        while (flag) {

            this.taskType = calculateTaskType(this.device.udid);

            if (TaskType.FINAL.equals(this.taskType)) flag = false;  //当前设备订阅的公众号抓取的文章已经达到了上限

            if (TaskType.WAIT.equals(this.taskType)) {
                //需要计算啥时候到达明天   到达明天的时候需要重新分配任务
                Date nextDay = DateUtil.buildDate();
                Date thisDay = new Date();
                long waitMills = Math.abs(nextDay.getTime() - thisDay.getTime());
                Thread.sleep(waitMills + 1000 * 60 * 5);
                this.taskType = TaskType.SUBSCRIBE;
            }
            Task task = new Task();

            task.setRetry(TaskType.CRAWLER.equals(this.taskType));

            this.executor.execute(task);
        }
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
            if (TaskType.SUBSCRIBE.equals(taskType)) {
                try {
                    initSubscribeQueue();
                    int length = device.queue.size();
                    for (int i = 0; i < length; i++) {
                        digestionSubscribe(device.queue.poll());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (TaskType.CRAWLER.equals(taskType)) {
                try {
                    initCrawlerQueue();
                    int length = device.queue.size();
                    for (int i = 0; i < length; i++) {
                        String mediaName = device.queue.poll();
                        lastPage = false;
                        digestionCrawler(mediaName, getRetry());
                        AndroidUtil.updateProcess(mediaName, device.udid);
                        for (int j = 0; j < 5; j++) {
                            driver.navigate().back();
                            Thread.sleep(1000);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


        private void initSubscribeQueue() throws SQLException {
            int numToday = DBUtil.obtainSubscribeNumToday(device.udid);
            if (numToday >= 40) {
                taskType = TaskType.WAIT;
            } else {
                Set<String> mediaNicks = Sets.newHashSet();
                DBUtil.obtainFullData(mediaNicks, 10, 40 - numToday);
                device.queue.addAll(mediaNicks);
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


    private static TaskType calculateTaskType(String udid) throws Exception {

        long allSubscribe = DBTab.subscribeDao.queryBuilder().where().eq("udid", udid).countOf();

        List<SubscribeMedia> notFinishR = DBTab.subscribeDao.queryBuilder().where().
                eq("udid", udid).and().
                eq("status", SubscribeMedia.CrawlerState.NOFINISH.status).
                query();

        int todaySubscribe = DBUtil.obtainSubscribeNumToday(udid);

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

    public static class Builder {

        private AndroidDevice device;

        public Builder device(AndroidDevice device) {
            this.device = device;
            return this;
        }

        public LooseWechatAdapter build() {
            return new LooseWechatAdapter(this.device);
        }
    }

}
