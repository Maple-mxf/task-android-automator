package one.rewind.android.automator.test.call;

import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ExecutorsPoolTest {

    private ExecutorService executor;

    private void setExecutor() {
        if (this.executor == null) {
            this.executor =
                    new ThreadPoolExecutor(0,
                            Integer.MAX_VALUE,
                            60,
                            TimeUnit.SECONDS,
                            new SynchronousQueue<>(),
                            threadFactory(UUID.randomUUID().toString().replace("-", "")
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


    public static void main(String[] args) {
        ReentrantReadWriteLock var = new ReentrantReadWriteLock();

        var.isWriteLocked();

        var.readLock().tryLock();

        var.writeLock().tryLock();
    }


}
