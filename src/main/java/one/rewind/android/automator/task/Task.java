package one.rewind.android.automator.task;

import one.rewind.android.automator.adapter.Adapter;
import one.rewind.android.automator.exception.AdapterException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * 任务重试
 * adapter异常处理
 * 任务执行
 *
 * @author scisaga@gmail.com
 * @date 2019/1/14
 */
public abstract class Task<A extends Adapter> implements Callable<Boolean> {

    public static final Logger logger = LogManager.getLogger(Task.class.getName());

    public A adapter;

    public TaskHolder holder;

    // Flag 在call()和调用方法中，显式调用，判断任务是否继续执行
    public volatile boolean stop = false;

    // 任务完成回调
    public List<Runnable> doneCallbacks = new ArrayList<>();

    // 任务失败回调
    public List<Runnable> failureCallbacks = new ArrayList<>();

    /**
     * @param holder
     */
    public Task(TaskHolder holder, String... params) throws IllegalParamsException {
        this.holder = holder;
    }

    // 任务的生命周期
    public Boolean call() throws InterruptedException, IOException, AdapterException.OperationException {

        execute();

        runCallbacks(doneCallbacks);

        return Boolean.TRUE;
    }

    public abstract void execute() throws InterruptedException, IOException, AdapterException.OperationException;

    /**
     * 参数异常
     */
    public static class IllegalParamsException extends Exception {
        public IllegalParamsException(String params) {
            super(params);
        }
    }

    public void runCallbacks(List<Runnable> callbacks) {
        callbacks.forEach(c -> new Thread(c).start());
    }
}
