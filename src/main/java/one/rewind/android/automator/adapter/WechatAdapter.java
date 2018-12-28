package one.rewind.android.automator.adapter;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.*;
import net.lightbody.bmp.filters.RequestFilter;
import net.lightbody.bmp.filters.ResponseFilter;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.AndroidDeviceManager;
import one.rewind.android.automator.model.Comments;
import one.rewind.android.automator.model.Essays;
import one.rewind.android.automator.model.SubscribeMedia;
import one.rewind.android.automator.util.AndroidUtil;
import one.rewind.android.automator.util.DateUtil;
import one.rewind.android.automator.util.MD5Util;
import one.rewind.android.automator.util.Tab;
import one.rewind.db.RedissonAdapter;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.json.JSONArray;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;


/**
 * @author maxuefeng[m17793873123@163.com]
 */
public class WechatAdapter extends AbstractWechatAdapter {

	private static final RedissonClient client = RedissonAdapter.redisson;

	public WechatAdapter(AndroidDevice device) {
		super(device);
	}

	private ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));

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

						previousEssayTitles.clear();

						SubscribeMedia var0 = Tab.subscribeDao.queryBuilder().where().eq("udid", device.udid).and().eq("media_name", media).queryForFirst();

						while (!lastPage.get()) {
							// 等于1说明非历史任务  retry代表是否重试
							digestionCrawler(media, var0.relative == 1);
						}
						logger.info("one/rewind/android/automator/adapter/WechatAdapter.java location: 40 Line !");
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
	}


	@Override
	public void start() {
		WechatAdapter adapter = this;
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

	// 启动Device
	public void startUpDevice() {
		Optional.of(this.device).ifPresent(t -> {
			t.startProxy(t.localProxyPort);
			t.setupWifiProxy();
			logger.info("Starting....Please wait!");
			try {

				RequestFilter requestFilter = (request, contents, messageInfo) -> {
					return null;
				};

				Stack<String> content_stack = new Stack<>();
				Stack<String> stats_stack = new Stack<>();
				Stack<String> comments_stack = new Stack<>();

				ResponseFilter responseFilter = (response, contents, messageInfo) -> {

					String url = messageInfo.getOriginalUrl();

					if (contents != null && (contents.isText() || url.contains("https://mp.weixin.qq.com/s"))) {

						// 正文
						if (url.contains("https://mp.weixin.qq.com/s")) {
							t.setClickEffect(true);
							/*System.err.println(" : " + url);*/
							content_stack.push(contents.getTextContents());
						}
						// 统计信息
						else if (url.contains("getappmsgext")) {
							t.setClickEffect(true);
							/*System.err.println(" :: " + url);*/
							stats_stack.push(contents.getTextContents());
						}
						// 评论信息
						else if (url.contains("appmsg_comment?action=getcomment")) {
							t.setClickEffect(true);
							/*System.err.println(" ::: " + url);*/
							comments_stack.push(contents.getTextContents());
						}

						if (content_stack.size() > 0) {

							t.setClickEffect(true);
							String content_src = content_stack.pop();
							Essays we = null;
							try {
								if (stats_stack.size() > 0) {
									String stats_src = stats_stack.pop();
									we = new Essays().parseContent(content_src).parseStat(stats_src);
								} else {
									we = new Essays().parseContent(content_src);
									we.view_count = 0;
									we.like_count = 0;
								}
							} catch (Exception e) {
								logger.error("文章解析失败！", e);
							}

							assert we != null;

							we.insert_time = new Date();
							we.update_time = new Date();
							we.media_content = we.media_nick;
							we.platform = "WX";
							we.media_id = MD5Util.MD5Encode(we.platform + "-" + we.media_name, "UTF-8");
							we.platform_id = 1;
							we.fav_count = 0;
							we.forward_count = 0;
							we.images = new JSONArray(we.parseImages(we.content)).toString();
							we.id = MD5Util.MD5Encode(we.platform + "-" + we.media_name + we.title, "UTF-8");

							try {
								we.insert();
							} catch (Exception ignored) {

							}
							if (comments_stack.size() > 0) {
								String comments_src = comments_stack.pop();
								List<Comments> comments_ = null;
								try {
									comments_ = Comments.parseComments(we.src_id, comments_src);
								} catch (ParseException e) {
									logger.error("----------------------");
								}
								comments_.stream().forEach(c -> {
									try {
										c.insert();
									} catch (Exception e) {
										logger.error("----------------评论插入失败！重复key----------------");
									}
								});
							}
						}
					}
				};
				t.setProxyRequestFilter(requestFilter);
				t.setProxyResponseFilter(responseFilter);
				// 启动device
				t.startAsync();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
}
