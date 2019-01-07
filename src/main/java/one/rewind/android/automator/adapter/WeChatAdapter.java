package one.rewind.android.automator.adapter;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.*;
import okhttp3.internal.Util;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.AndroidDeviceManager;
import one.rewind.android.automator.callback.TaskStartCallback;
import one.rewind.android.automator.callback.TaskStopCallback;
import one.rewind.android.automator.model.SubscribeMedia;
import one.rewind.android.automator.util.AndroidUtil;
import one.rewind.android.automator.util.DateUtil;
import one.rewind.android.automator.util.Tab;
import one.rewind.db.RedissonAdapter;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;


/**
 * @author maxuefeng[m17793873123@163.com]
 * @see AndroidDevice
 */
public class WeChatAdapter extends AbstractWeChatAdapter {

	private static final RedissonClient client = RedissonAdapter.redisson;

	public WeChatAdapter(AndroidDevice device) {
		super(device);
	}

	private ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10, Util.threadFactory("thread-" + device.udid, false)));

	class Task implements Callable<Boolean> {

		/**
		 * @return isSuccess
		 * @throws Exception ex
		 */
		@Override
		public Boolean call() throws Exception {

			if (device.taskType != null) {

				if (device.taskType.equals(AndroidDevice.Task.Type.Fetch)) {

					for (String media : device.queue) {
						//  初始化记录  对应当前公众号
						lastPage.set(Boolean.FALSE);

						currentTitles.clear();

						SubscribeMedia var0 = Tab.subscribeDao.queryBuilder().where().eq("udid", device.udid).and().eq("media_name", media).queryForFirst();

						while (!lastPage.get()) {
							// 等于1说明非历史任务  retry代表是否重试
							digestionCrawler(media, var0.relative == 1);
						}
						logger.info("当前任务 : {}执行完成", media);
						// 当前公众号任务抓取完成之后需要到redis中进行处理数据

						// 异步通知redis
						callRedisAndChangeState(media);

						AndroidUtil.restartWechat(device);
					}
				} else if (device.taskType.equals(AndroidDevice.Task.Type.Subscribe)) {

					for (String media : device.queue) {

						digestionSubscribe(media);
					}
				}
			} else {
				if (device.flag != null) {
					if (device.flag.equals(AndroidDevice.Flag.Frequent_Operation)) {
						// 线程睡眠
						// 需要计算啥时候到达明天   到达明天的时候需要重新分配任务
						Date nextDay = DateUtil.buildDate();
						Date thisDay = new Date();
						long waitMills = Math.abs(nextDay.getTime() - thisDay.getTime());
						Thread.sleep(waitMills + 1000 * 60 * 5);
					} else if (device.flag.equals(AndroidDevice.Flag.Upper_Limit)) {
						// 当前设备订阅公众号数量到达上限
						return true;
					}
				}
			}

			return true;
		}


		private void callRedisAndChangeState(String mediaName) throws Exception {
			SubscribeMedia media = Tab.subscribeDao.
					queryBuilder().
					where().
					eq("media_name", mediaName).
					and().
					eq("udid", udid).
					queryForFirst();


			if (media != null) {

				if (!Strings.isNullOrEmpty(media.request_id) && media.request_id.contains(Tab.REQUEST_ID_SUFFIX)) {
					doCallRedis(media);
				}

				long countOf = Tab.essayDao.
						queryBuilder().
						where().
						eq("media_nick", mediaName).
						countOf();
				media.number = (int) countOf;
				media.status = (countOf == 0 ? SubscribeMedia.State.NOT_EXIST.status : SubscribeMedia.State.FINISH.status);
				media.status = 1;
				media.update_time = new Date();
				media.retry_count = 5;
				media.update();
			}
		}

		// 利用redis的消息发布订阅实现消息通知
		// publish subscribe
		private void doCallRedis(SubscribeMedia media) {
			String requestID = media.request_id;
			// topic name :requestIDk
			RTopic<Object> topic = client.getTopic(requestID);

			long k = topic.publish(media.media_name);

			logger.info("发布完毕！k: {}", k);
		}


		// 执行任务
		private void execute(TaskStartCallback startCallback, TaskStopCallback stopCallback) {

			startCallback.call(device);

			executeTask();

			stopCallback.call(device);
		}

		private void executeTask() {

			// execute task
		}
	}


	@Override
	public void start() {
		WeChatAdapter adapter = this;

		ListenableFuture<Boolean> future = service.submit(new Task());

		Futures.addCallback(future, new FutureCallback<Boolean>() {

			@Override
			public void onSuccess(@NullableDecl Boolean result) {

				// 清空任务队列
				device.queue.clear();
				AndroidDeviceManager.me().addIdleAdapter(adapter);
			}

			@Override
			public void onFailure(Throwable t) {
				//任务失败
				t.printStackTrace();
			}
		});
	}

	@Override
	@Deprecated
	public void stop() {
		//启动关闭线程池
		while (true) {
			service.shutdownNow();
			if (service.isShutdown()) return;
		}
	}
}
