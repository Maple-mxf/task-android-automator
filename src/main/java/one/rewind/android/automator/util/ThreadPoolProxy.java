package one.rewind.android.automator.util;


import java.util.concurrent.*;

/**
 * Create By  2018/10/18
 * Description  线程管理
 */
public class ThreadPoolProxy {
	ThreadPoolExecutor mThreadPoolExecutor;

	private int corePoolSize;
	private int maximumPoolSize;
	private long keepAliveTime;

	public ThreadPoolProxy(int corePoolSize, int maximumPoolSize, long keepAliveTime) {
		this.corePoolSize = corePoolSize;
		this.maximumPoolSize = maximumPoolSize;
		this.keepAliveTime = keepAliveTime;
		initExecutor();
	}

	private ThreadPoolExecutor initExecutor() {
		if (mThreadPoolExecutor == null) {
			synchronized (ThreadPoolProxy.class) {
				if (mThreadPoolExecutor == null) {

					TimeUnit unit = TimeUnit.MILLISECONDS;
					ThreadFactory threadFactory = Executors.defaultThreadFactory();
					RejectedExecutionHandler handler = new ThreadPoolExecutor.AbortPolicy();
					LinkedBlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();

					mThreadPoolExecutor = new ThreadPoolExecutor(
							corePoolSize,//核心线程数
							maximumPoolSize,//最大线程数
							keepAliveTime,//保持时间
							unit,//保持时间对应的单位
							workQueue,
							threadFactory,//线程工厂
							handler);//异常捕获器
				}
			}
		}
		return mThreadPoolExecutor;
	}


	/**
	 * 执行任务
	 */
	public void executeTask(Runnable r) {
		mThreadPoolExecutor.execute(r);
	}

	/**
	 * 提交任务
	 */
	public Future<?> commitTask(Runnable r) {
		return mThreadPoolExecutor.submit(r);
	}

	/**
	 * 删除任务
	 */
	public void removeTask(Runnable r) {
		mThreadPoolExecutor.remove(r);
	}


	public boolean isStopped() {
		return this.mThreadPoolExecutor.isTerminated();
	}

}
