package one.rewind.android.automator.task;

import one.rewind.android.automator.adapter.Adapter;

import java.util.concurrent.Callable;

/**
 * @author scisaga@gmail.com
 * @date 2019/1/14
 */
public abstract class Task<A extends Adapter> implements Callable<Boolean> {

	public A adapter;

	// Flag 在call()和调用方法中，显式调用，判断任务是否继续执行
	public volatile boolean stop = false;

	public abstract Boolean call() throws Exception;

}
