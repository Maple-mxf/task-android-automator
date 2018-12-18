package one.rewind.android.automator.test.call;

import com.google.common.util.concurrent.*;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.*;


/**
 * @author maxuefeng[m17793873123@163.com]
 */
public class ExecutorsPoolTest {

    private ExecutorService executor;

    private void setExecutor() {
        if (this.executor == null) {
            this.executor = Executors.newFixedThreadPool(1, threadFactory());
        }
    }

    private static ThreadFactory threadFactory() {
        return runnable -> {
            Thread result = new Thread(runnable, "waa");
            result.setDaemon(false);
            return result;
        };
    }


    class Task implements Runnable {
        @Override
        public void run() {
            int i = 0;
            boolean flag = true;
            while (flag) {
                System.out.println("i:" + i);
                i++;
                if (i == 1000) {
                    flag = false;
                }
            }
        }
    }

    @Test
    public void testExecutorRestore() throws InterruptedException {
        setExecutor();
        executor.execute(new Task());
        while (executor.awaitTermination(1, TimeUnit.SECONDS)) {
            System.out.println("第1次任务提交执行等待中");
        }
        executor.execute(new Task());
        while (executor.awaitTermination(1, TimeUnit.SECONDS)) {
            System.out.println("第2次任务提交执行等待中");
        }
    }

    @Test
    public void testGuavaCall() throws IOException {
        ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
        ListenableFuture<String> explosion = service.submit(() -> {
            int a = 1;

            boolean flag = true;

            while (flag) {
                System.out.println("i: " + a);
                a++;
                if (a == 1000000) {
                    flag = false;
                }
            }
            return "executed";
        });
        Futures.addCallback(explosion, new FutureCallback<String>() {
            @Override
            public void onSuccess(@NullableDecl String result) {
                System.out.println("executed success");
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
                System.out.println("executed failed");
            }
        }, service);
        System.in.read();
    }
}
