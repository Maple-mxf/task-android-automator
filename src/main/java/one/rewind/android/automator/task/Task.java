package one.rewind.android.automator.task;

import one.rewind.android.automator.adapter.Adapter;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.db.model.ModelL;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author scisaga@gmail.com
 * @date 2019/1/14
 */
public abstract class Task<A extends Adapter> implements Callable<Boolean> {

	public static final Logger logger = LogManager.getLogger(Task.class.getName());

	public A adapter;

	public TaskHolder holder;

	// Flag 在call()和调用方法中，显式调用，判断任务是否继续执行
	public volatile boolean stop = false;

	public List<Runnable> doneCallbacks = new ArrayList<>();

	public List<Runnable> successCallbacks = new ArrayList<>();

	public List<Runnable> failureCallbacks = new ArrayList<>();

	/**
	 *
	 * @param holder
	 */
	public Task(TaskHolder holder, String... params) throws IllegalParamsException {
		this.holder = holder;
	}

	// 任务的生命周期
	public abstract Boolean call() throws AdapterException.NoResponseException, AdapterException.OperationException,
			AdapterException.IllegalStateException, AccountException.NoAvailableAccount, InterruptedException, IOException;

	/**
	 * 参数异常
	 */
	public static class IllegalParamsException extends Exception {
		public IllegalParamsException(String params) {
			super(params);
		}
	}
}
