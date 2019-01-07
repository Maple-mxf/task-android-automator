package one.rewind.android.automator.adapter;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.*;
import okhttp3.internal.Util;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.AndroidDeviceManager;
import one.rewind.android.automator.callback.Callback;
import one.rewind.android.automator.model.SubscribeMedia;
import one.rewind.android.automator.model.TaskLog;
import one.rewind.android.automator.util.AndroidUtil;
import one.rewind.android.automator.util.DateUtil;
import one.rewind.android.automator.util.Tab;
import one.rewind.db.RedissonAdapter;
import org.apache.commons.lang3.StringUtils;
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


	private TaskLog taskLog;

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
			execute(begin -> {

				WeChatAdapter adapter = (WeChatAdapter) begin;

				adapter.lastPage.set(Boolean.FALSE);

				adapter.currentTitles.clear();

				// peek
				String media = adapter.device.queue.peek();

				if (StringUtils.isNotBlank(media) && (AndroidDevice.Task.Type.Fetch.equals(adapter.device.taskType) || AndroidDevice.Task.Type.Subscribe.equals(adapter.device.taskType))) {

					adapter.taskLog = new TaskLog();

					String topic = Tab.topic(media);

					media = Tab.realMedia(media);

					// 1代表订阅  2代表数据采集任务
					adapter.taskLog.buildLog(null, media, topic, adapter.device.udid, adapter.device.taskType.equals(AndroidDevice.Task.Type.Subscribe) ? 1 : 2);
				}
			}, end -> {
				try {

					WeChatAdapter adapter = (WeChatAdapter) end;
					// peek
					String media = adapter.device.queue.peek();

					if ((AndroidDevice.Task.Type.Fetch.equals(adapter.device.taskType) || AndroidDevice.Task.Type.Subscribe.equals(adapter.device.taskType)) && StringUtils.isNotBlank(media)) {

						logger.info("当前任务 : {}执行完成", media);

						callRedisAndChangeState(media);

						AndroidUtil.restartWechat(device);

						// 执行成功
						adapter.taskLog.executeSuccess();
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			});
			return true;
		}


		// 执行任务
		private void execute(Callback startCallback, Callback stopCallback) throws Exception {

			startCallback.call(WeChatAdapter.this);

			executeTask();

			stopCallback.call(WeChatAdapter.this);
		}


		// execute task
		private void executeTask() throws Exception {

			if (device.taskType != null) {

				if (device.taskType.equals(AndroidDevice.Task.Type.Fetch)) {

					String media = device.queue.poll();

					SubscribeMedia var0 = Tab.subscribeDao.queryBuilder().where().eq("udid", device.udid).and().eq("media_name", media).queryForFirst();

					while (!lastPage.get()) {
						// 等于1说明非历史任务  retry代表是否重试
						digestionCrawler(media, var0.relative == 1);
					}

				} else if (device.taskType.equals(AndroidDevice.Task.Type.Subscribe)) {

					for (String media : device.queue) {

						digestionSubscribe(media);
					}
				}
			} else {
				if (device.flag != null) {

					if (device.flag.equals(AndroidDevice.Flag.Frequent_Operation)) {

						// 需要计算啥时候到达明天   到达明天的时候需要重新分配任务
						Date nextDay = DateUtil.buildDate();
						Date thisDay = new Date();
						long waitMills = Math.abs(nextDay.getTime() - thisDay.getTime());
						Thread.sleep(waitMills + 1000 * 60 * 5);
					} else if (device.flag.equals(AndroidDevice.Flag.Upper_Limit)) {
						// 当前设备订阅公众号数量到达上限
						logger.info("当前设备{}订阅的公众号已经达到上限", udid);
					}
				}
			}
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

	}


	@Override
	public void start() {
		WeChatAdapter adapter = this;

		ListenableFuture<Boolean> future = service.submit(new Task());

		Futures.addCallback(future, new FutureCallback<Boolean>() {

			@Override
			public void onSuccess(@NullableDecl Boolean result) {

				// 清空任务队列
				if (!AndroidDevice.Flag.Upper_Limit.equals(adapter.device.flag)) {

					AndroidDeviceManager.me().addIdleAdapter(adapter);

				}
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
