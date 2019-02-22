package one.rewind.android.automator.test.task;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class TaskTest {

    public static void main(String[] args) throws InterruptedException, ExecutionException {

        new Thread(() -> {
            try {

                ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));

                ListenableFuture<Void> future = service.submit(new Task());

                future.addListener(() -> {

                }, service);

                future.get();

            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

        }).start();


        Thread.sleep(1000000);
    }


    static class Task implements Callable<Void> {
        @Override
        public Void call() throws Exception {

            while (true) {
                System.out.println("hello world");
            }


        }
    }

}
