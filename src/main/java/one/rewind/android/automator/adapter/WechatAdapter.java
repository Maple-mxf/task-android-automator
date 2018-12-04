package one.rewind.android.automator.adapter;

import com.google.common.util.concurrent.*;
import joptsimple.internal.Strings;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.manager.Manager;
import one.rewind.android.automator.model.Tab;
import one.rewind.android.automator.model.SubscribeMedia;
import one.rewind.android.automator.util.AndroidUtil;
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
public class WechatAdapter extends AbstractWechatAdapter {

    public static final String REQ_SUFFIX = "$req_";

    // 获取真实的mediaName
    public static String realMedia(String mediaName) {

        if (!Strings.isNullOrEmpty(mediaName)) {
            if (mediaName.contains(REQ_SUFFIX)) {
                // 确认为api接口中传递过来的数据
                // 字符串处理为正常的字符串
                int tmpIndex = mediaName.indexOf(REQ_SUFFIX);

                return mediaName.substring(0, tmpIndex);
            } else {
                return mediaName;
            }
        } else {
            return mediaName;
        }
    }

    // 获取请求ID
    public static String requestID(String mediaName) {

        if (!Strings.isNullOrEmpty(mediaName)) {
            if (mediaName.contains(REQ_SUFFIX)) {
                int tmpIndex = mediaName.indexOf(REQ_SUFFIX);
                return mediaName.substring(tmpIndex);

            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public WechatAdapter(AndroidDevice device) {
        super(device);
    }

    private ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));

    class Task implements Callable<Boolean> {

        @Override
        public Boolean call() throws Exception {

            switch (device.taskType) {
                case CRAWLER: {
                    for (String var : device.queue) {

                        //  初始化记录  对应当前公众号
                        lastPage.set(Boolean.FALSE);
                        previousEssayTitles.clear();

                        while (!lastPage.get()) {
                            digestionCrawler(var, true);
                        }
                        System.out.println("one/rewind/android/automator/adapter/WechatAdapter.java location: 40 Line !");
                        updateMediaState(var, udid);

                        AndroidUtil.restartWechatAPP(device);
                    }
                    break;
                }
                case SUBSCRIBE: {
                    for (String var : device.queue) {

                        // 处理数据
//                        String realMedia = realMedia(var);

                        digestionSubscribe(var);
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

    private void updateMediaState(String mediaName, String udid) throws Exception {
        SubscribeMedia account = Tab.subscribeDao.
                queryBuilder().
                where().
                eq("media_name", mediaName).
                and().
                eq("udid", udid).
                queryForFirst();

        if (account != null) {
            long countOf = Tab.essayDao.
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
