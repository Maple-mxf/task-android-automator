package one.rewind.android.automator.adapter;

import com.google.common.collect.Lists;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.model.TaskType;
import one.rewind.android.automator.util.AndroidUtil;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Create By 2018/10/10
 * Description: 微信的自动化操作
 */
public class DefaultWechatAdapter extends AbstractWechatAdapter {

    public DefaultWechatAdapter(AndroidDevice device) {
        super(device);
    }

    public class Start implements Callable<Boolean> {

        private boolean retry = false;

        public synchronized boolean getRetry() {
            return retry;
        }

        public synchronized void setRetry(boolean retry) {
            this.retry = retry;
        }

        @Override
        public Boolean call() {
            assert taskType != null;
            if (TaskType.SUBSCRIBE.equals(taskType)) {
                try {
                    for (String var : device.queue) {
                        digestionSubscribe(var);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (TaskType.CRAWLER.equals(taskType)) {
                try {
                    for (String var : device.queue) {
                        lastPage = false;
                        digestionCrawler(var, getRetry());
                        AndroidUtil.updateProcess(var, device.udid);
                        //返回到主界面
                        for (int i = 0; i < 5; i++) {
                            driver.navigate().back();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
    }

    public void start(boolean retry) {
        Start start = new Start();
        start.setRetry(retry);
        Future<Boolean> future = executor.submit(start);
        submit(future);
        logger.info("任务提交完毕！");
    }

    public static List<Future<?>> futures = Lists.newArrayList();

    /**
     * 将执行结果添加进来
     *
     * @param future
     */
    public void submit(Future<?> future) {
        futures.add(future);
    }


}
