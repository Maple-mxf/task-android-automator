package one.rewind.android.automator.adapter;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.*;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.manager.Manager;
import one.rewind.android.automator.model.SubscribeMedia;
import one.rewind.android.automator.model.Tab;
import one.rewind.android.automator.util.AndroidUtil;
import one.rewind.android.automator.util.DateUtil;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.redisson.api.RSet;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

/**
 * Create By 2018/10/10
 * Description: 微信的自动化操作
 */
public class WechatAdapter extends AbstractWechatAdapter {

    public static final String REQ_SUFFIX = "$req_";

    public WechatAdapter(AndroidDevice device) {
        super(device);
    }

    private ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));

    class Task implements Callable<Boolean> {

        /**
         * @return isSuccess
         * @throws Exception ex
         */
        @Override
        public Boolean call() throws Exception {

            switch (device.taskType) {
                case CRAWLER: {
                    for (String media : device.queue) {

                        //  初始化记录  对应当前公众号
                        lastPage.set(Boolean.FALSE);

                        previousEssayTitles.clear();

                        while (!lastPage.get()) {
                            digestionCrawler(media, true);
                        }
                        System.out.println("one/rewind/android/automator/adapter/WechatAdapter.java location: 40 Line !");

                        // 当前公众号任务抓取完成之后需要到redis中进行处理数据

                        // 异步通知redis
                        doCallRedisAndChangeState(media);

                        AndroidUtil.restartWechatAPP(device);
                    }
                    break;
                }
                case SUBSCRIBE: {
                    for (String media : device.queue) {
                        digestionSubscribe(media);
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


        private void doCallRedisAndChangeState(String mediaName) throws Exception {


            SubscribeMedia media = Tab.subscribeDao.
                    queryBuilder().
                    where().
                    eq("media_name", mediaName).
                    and().
                    eq("udid", udid).
                    queryForFirst();


            if (media != null) {

                if (!Strings.isNullOrEmpty(media.request_id)) {
                    doCallRedis(media);
                }

                long countOf = Tab.essayDao.
                        queryBuilder().
                        where().
                        eq("media_nick", mediaName).
                        countOf();
                media.number = (int) countOf;
                media.status = (countOf == 0 ? SubscribeMedia.State.NOT_EXIST.status : SubscribeMedia.State.FINISH.status);
                media.status = 1;
                media.update_time = new Date();
                media.retry_count = 5;
                media.update();
            }
        }

        // 消息通知

        private void doCallRedis(SubscribeMedia media) {

            String requestID = media.request_id;

            // 获取到已完成的队列  此时不会出现NullPointException
            requestID += "_finish";

            RSet<Object> var = Manager.redisClient.getSet(requestID);

            var.add(media.media_name);
        }
    }


    @Override
    public void start() {
        WechatAdapter adapter = this;
        ListenableFuture<Boolean> future = service.submit(new Task());

        Futures.addCallback(future, new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(@NullableDecl Boolean result) {

                try {
                    if (Boolean.TRUE.equals(future.get())) {
                        Manager.me().addIdleAdapter(adapter);
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

    @Override
    public void stop() {

        //启动关闭线程池
        while (true) {
            service.shutdownNow();
            if (service.isShutdown()) return;
        }
    }
}
