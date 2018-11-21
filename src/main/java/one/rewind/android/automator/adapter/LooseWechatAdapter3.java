package one.rewind.android.automator.adapter;

import com.google.common.util.concurrent.*;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.manager.Manager;
import one.rewind.android.automator.model.DBTab;
import one.rewind.android.automator.model.SubscribeMedia;
import one.rewind.android.automator.util.DateUtil;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

/**
 * Create By 2018/10/10
 * Description: 微信的自动化操作
 */
public class LooseWechatAdapter3 extends AbstractWechatAdapter {

    public LooseWechatAdapter3(AndroidDevice device) {
        super(device);
    }

    private ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));

    class Task implements Callable<Boolean> {

        @Override
        public Boolean call() throws Exception {

            switch (device.taskType) {
                case CRAWLER: {
                    int size = device.queue.size();
                    for (int i = 0; i < size; i++) {
                        String mediaName = device.queue.poll();
                        digestionCrawler(mediaName, true);
                        lastPage = false;
                        while (!lastPage) {
                            digestionCrawler(mediaName, true);
                        }
                        updateMediaState(mediaName, udid);
                        for (int j = 0; j < 5; j++) {
                            driver.navigate().back();
                            Thread.sleep(1000);
                        }
                    }
                    break;
                }
                case SUBSCRIBE: {
                    int size = device.queue.size();
                    for (int i = 0; i < size; i++) {
                        digestionSubscribe(device.queue.poll());
                    }
                    break;
                }
                case FINAL:
                    return false;//退出
                case WAIT: {
                    //线程睡眠
                    //需要计算啥时候到达明天   到达明天的时候需要重新分配任务
                    Date nextDay = DateUtil.buildDate();
                    Date thisDay = new Date();
                    long waitMills = Math.abs(nextDay.getTime() - thisDay.getTime());
                    Thread.sleep(waitMills + 1000 * 60 * 5);
                    break;
                }
            }
            return true;
        }
    }


    @Override
    public void start() {
        LooseWechatAdapter3 adapter = this;
        ListenableFuture<Boolean> future = service.submit(new Task());
        Futures.addCallback(future, new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(@NullableDecl Boolean result) {

                try {
                    if (Boolean.TRUE.equals(future.get())) {
                        Manager.getInstance().addIdleAdapter(adapter);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                //任务失败
                t.printStackTrace();
            }
        });
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
