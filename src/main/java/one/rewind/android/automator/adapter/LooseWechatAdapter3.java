package one.rewind.android.automator.adapter;

import com.google.common.util.concurrent.*;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.manager.Manager;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.concurrent.Callable;
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
                        digestionCrawler(device.queue.poll(), true);
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
                case FINAL: {

                    break;
                }
                case WAIT: {
                    //线程睡眠
                    break;
                }
            }

            return true;
        }
    }


    @Override
    void start() {
        LooseWechatAdapter3 adapter = this;
        ListenableFuture<Boolean> future = service.submit(new Task());
        Futures.addCallback(future, new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(@NullableDecl Boolean result) {
                Manager.getInstance().addIdleAdapter(adapter);
            }

            @Override
            public void onFailure(Throwable t) {
                //任务失败
                t.printStackTrace();
            }
        });
    }
}
