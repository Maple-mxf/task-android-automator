package one.rewind.android.automator.adapter;

import com.google.common.collect.Sets;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.model.DBTab;
import one.rewind.android.automator.model.SubscribeMedia;
import one.rewind.android.automator.model.TaskType;
import one.rewind.android.automator.util.AndroidUtil;
import one.rewind.android.automator.util.DBUtil;
import one.rewind.android.automator.util.ShellUtil;

import java.sql.SQLException;
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
        this.executor =
                new ThreadPoolExecutor(0,
                        Integer.MAX_VALUE,
                        60,
                        TimeUnit.SECONDS,
                        new SynchronousQueue<>(),
                        threadFactory(UUID.randomUUID().toString().replace("-", "")
                        ));
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

    /**
     * 清空手机缓存
     *
     * @throws Exception
     */
    public void clearMemory() throws Exception {
        ShellUtil.shutdownProcess(this.device.udid, "com.tencent.mm");
        AndroidUtil.activeWechat(this.device);
    }

    public void start() throws Exception {
        this.setExecutor();
        Task task = new Task();
        this.taskType = calculateTaskType(this.device.udid);
        task.setRetry(TaskType.CRAWLER.equals(this.taskType));
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
            if (TaskType.SUBSCRIBE.equals(taskType)) {
                try {
                    if (device.queue.size() == 0) {
                        initSubscribeQueue();
                    }
                    for (String var : device.queue) {
                        digestionSubscribe(var, false);
                    }
                    taskType = TaskType.CRAWLER;
                    execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (TaskType.CRAWLER.equals(taskType)) {
                try {
                    if (device.queue.size() == 0) {
                        initCrawlerQueue();
                    }
                    device.queue.clear();
                    device.queue.add("淘迷网");
                    for (String var : device.queue) {
                        lastPage = false;
                        digestionCrawler(var, getRetry());
//                        AndroidUtil.updateProcess(var, device.udid);
                        clearMemory();
                    }
                    taskType = TaskType.SUBSCRIBE;
                    execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (TaskType.WAIT.equals(taskType)) {
                System.out.println("当前设备没有可执行的任务");
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
