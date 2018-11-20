package one.rewind.android.automator.test.call;

import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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


    public static void main(String[] args) {
        ReentrantReadWriteLock var = new ReentrantReadWriteLock();

        var.isWriteLocked();

        var.readLock().tryLock();

        var.writeLock().tryLock();
    }


}
