package one.rewind.android.automator.task;

import one.rewind.android.automator.adapter.Adapter;
import one.rewind.db.model.ModelL;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author scisaga@gmail.com
 * @date 2019/1/14
 */
public abstract class Task<A extends Adapter> extends ModelL implements Callable<Boolean> {

	static final Logger logger = LogManager.getLogger(Task.class.getName());

	public A adapter;

	// Flag 在call()和调用方法中，显式调用，判断任务是否继续执行
	public volatile boolean stop = false;

	public List<Runnable> doneCallbacks = new ArrayList<>();

	public List<Runnable> successCallbacks = new ArrayList<>();

	public List<Runnable> failureCallbacks = new ArrayList<>();


	// 任务的生命周期
	public abstract Boolean call() throws Exception;

}
