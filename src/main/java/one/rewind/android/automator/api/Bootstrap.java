package one.rewind.android.automator.api;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.AndroidDeviceManager;
import one.rewind.android.automator.model.RequestRecord;
import one.rewind.android.automator.model.SubscribeMedia;
import one.rewind.android.automator.util.DateUtil;
import one.rewind.android.automator.util.Tab;
import one.rewind.io.server.Msg;
import one.rewind.json.JSON;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.redisson.api.RPriorityQueue;
import org.redisson.api.RTopic;
import spark.Route;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static one.rewind.db.RedissonAdapter.redisson;
import static spark.Spark.port;
import static spark.Spark.post;

/**
 * 每次往redis中存储任务的时候,通知topic定时更新TASK_CACHE中的数据 作为简单缓存来使用
 *
 * @author maxuefeng [m17793873123@163.com]
 */
@ThreadSafe
public class Bootstrap {

	private static final Set<String> TASK_CACHE = Sets.newConcurrentHashSet();

	private static final ImmutableSet<String> STRINGS = ImmutableSet.<String>builder().build();

	private static final String TASK_MSG = "Android-Automator-Task_Msg";

	public static void main(String[] args) {

		AndroidDeviceManager manage = AndroidDeviceManager.me();

		// 启动任务
		manage.run();

		port(56789);

		// 采集接口
		post("/fetch", fetch, JSON::toJson);

		// 订阅接口
		post("/subscribe", subscribe, JSON::toJson);
	}


	/**
	 * 图像识别服务  客户端先将图片转换为byte数组  再将byte数组转换为字符串
	 */
	private static Route ocrService = (req, resp) -> {

		return null;
	};

	/**
	 * request body param {"media":[""]}
	 * 公众号订阅
	 */
	private static Route subscribe = (req, resp) -> {

		String body = req.body();

		if (Strings.isNullOrEmpty(body)) return new Msg<>(0, "请检查您的参数！");

		JSONObject json = new JSONObject(body);

		if (!json.has("media")) return new Msg<>(0, "请检查您的参数！");

		JSONArray mediasArray = json.getJSONArray("media");

		if (mediasArray == null || mediasArray.length() == 0) return new Msg<>(0, "请检查您的参数！");

		String topic = parseTopic(json);

		if (Strings.isNullOrEmpty(topic)) return new Msg<>(0, "请检查Topic参数！");

		String udid = null;
		// 指定设备执行任务
		if (json.has("udid")) {

			if (parseDeviceExist(json.getString("udid"))) return new Msg<>(0, "设备不存在！");  // TODO 需要获取当前设备订阅的任务是否处于

			udid = json.getString("udid");
		}

		Map data = parseMedia(mediasArray, topic, udid);

		return new Msg<>(1, data);
	};


	/**
	 * request body param {"udid":"xxxxx","media":["xxxx"]}
	 * 数据采集
	 */
	private static Route fetch = (req, resp) -> {

		String body = req.body();

		if (Strings.isNullOrEmpty(body)) return new Msg<>(0, "请检查您的参数！");

		JSONObject json = new JSONObject(body);

		if (!json.has("media")) return new Msg<>(0, "请检查您的参数！");

		JSONArray mediasArray = json.getJSONArray("media");

		if (mediasArray == null || mediasArray.length() == 0) return new Msg<>(0, "请检查您的参数！");

		String topic = parseTopic(json);

		if (Strings.isNullOrEmpty(topic)) return new Msg<>(0, "请检查Topic参数！");

		String udid = null;
		// 指定设备执行任务
		if (json.has("udid")) {

			if (parseDeviceExist(json.getString("udid"))) return new Msg<>(0, "设备不存在！");  // TODO 需要获取当前设备订阅的任务是否处于

			udid = json.getString("udid");
		}

		Map data = parseMedia(mediasArray, topic, udid);

		return new Msg<>(1, data);
	};


	/**
	 * 解析Topic
	 *
	 * @param origin 原始JSON参数
	 * @return 返回Topic
	 */
	private static String parseTopic(JSONObject origin) {
		if (origin.has("topic")) {

			String topic = origin.getString("topic");

			if (Strings.isNullOrEmpty(topic)) {
				return null;
			}

			if (!redisson.getSet(Tab.TOPICS).contains(topic)) return null;

			return topic;
		} else {
			//
			return Tab.REQUEST_ID_SUFFIX + DateUtil.timestamp();
		}
	}

	// 校验设备是否存在
	private static boolean parseDeviceExist(String udid) {

		AndroidDeviceManager manager = AndroidDeviceManager.me();

		boolean has = false;

		for (AndroidDevice device : manager.devices) {
			if (device.udid.equalsIgnoreCase(udid) || (device.flag != null && !device.flag.equals(AndroidDevice.Flag.Upper_Limit)))
				has = true;
		}
		return !has;
	}


	/**
	 * 解析任务
	 *
	 * @param media 任务数组
	 * @param topic 主题
	 * @param udid  设备机器码
	 */
	private static Map<String, Object> parseMedia(JSONArray media, String topic, String udid) {
		try {
			Map<String, Object> data = Maps.newHashMap();

			List<Object> result = Lists.newArrayList();
			// 处理相对任务   历史任务的完成状态仅仅相对于过去 相对于现在仍旧是未完成任务
			for (Object var0 : media) {
				// 带插入数据库的请求记录
				RequestRecord requestRecord = new RequestRecord();

				Map<String, Object> single = Maps.newHashMap();

				String tmp = (String) var0;

				requestRecord.media = tmp;
				requestRecord.insert_time = new Date();
				requestRecord.udid = udid;

				// 1 当前任务是否已订阅
				SubscribeMedia var1 = Tab.subscribeDao.queryBuilder().where().eq("media_name", tmp).queryForFirst();

				if (var1 != null) {
					requestRecord.is_follow = true;
					requestRecord.is_queue = false;

					// 任务状态为0:  未完成
					// 任务状态为1:  已完成
					// 任务状态为2:  不存在

					if (SubscribeMedia.State.NOT_FINISH.status == var1.status) {
						// 任务未完成

						// 上一次采集时间
						requestRecord.last = var1.update_time;
						requestRecord.is_finish = false;
						requestRecord.is_finish_history = false;


					} else if (SubscribeMedia.State.FINISH.status == var1.status) {
						//
						int interval = relativeTask(var1);
						if (interval == 0) {
							requestRecord.last = new Date();
							requestRecord.is_finish = true;
							requestRecord.is_finish_history = false;
						} else {
							requestRecord.last = var1.update_time;
							requestRecord.is_finish = false;
							requestRecord.is_finish_history = true;

							// 设备任务初始化的时候一直会存数据库中存储
							// 获取当前公众号最早的发布时间
							// select pub_date from essays where media_nick = '' and min(pub_date) = pub_date
							// update数据记录
							var1.update_time = new Date();
							var1.status = SubscribeMedia.State.NOT_FINISH.status;
							var1.retry_count = 0;
							// 相对于现在处于未完成当前任务
							var1.relative = 0;
							var1.update();
							// 根据时间的相对差距采集最新更新的数据  采集最新的文章结束的标记是发布时间
							// setLastPage = true   mediaName需要解析去掉1:topic 2:suffix 3:真实udid

						}
					} else if (SubscribeMedia.State.NOT_EXIST.status == var1.status) {
						// 公众号不存在
					}
				} else {
					String var2 = contain(tmp, topic);

					// 当前任务正在排队
					if (!var2.equals(topic)) {
						requestRecord.is_queue = true;
						topic = var2;

					} else {
						requestRecord.is_queue = false;
						// 当前任务没有排队 并且当前任务也非历史任务  分配到制定设备去做
						// 将当前任务存放到redis中指定设备去做   #realMedia
						// mediaName + topic + "_udid" + udid
						// 将任务提交到redis中

						if (StringUtils.isNotBlank(udid)) {
							// 附带udid
							redisson.getPriorityQueue(Tab.TOPIC_MEDIA).add(tmp + topic + Tab.UDID_SUFFIX + udid);

						} else {
							// 没有udid
							redisson.getPriorityQueue(Tab.TOPIC_MEDIA).add(tmp + topic);
						}
					}
				}
				requestRecord.topic = topic;
				requestRecord.insert();

				// 给客户端的返回结果
				single.put("media", requestRecord.media);
				single.put("is_finish", requestRecord.is_finish);
				single.put("is_follow", requestRecord.is_follow);
				single.put("in_queue", requestRecord.is_queue);
				single.put("is_finish_history", requestRecord.is_finish_history);
				single.put("last", requestRecord.last == null ? "" : DateFormatUtils.format(requestRecord.last, "yyyy-MM-dd HH:mm:ss"));
				single.put("udid", requestRecord.udid);
				result.add(single);
			}

			// topic 存储redis
			redisson.getSet(Tab.TOPICS).add(topic);

			data.put("topic", topic);
			data.put("media_result", result);
			return data;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 队列中是否包含
	 *
	 * @param mediaName
	 * @param topic
	 * @return
	 */
	@Deprecated
	private static String contain(String mediaName, String topic) {

		RPriorityQueue<String> topicMedia = redisson.getPriorityQueue(Tab.TOPIC_MEDIA);

		for (String var : topicMedia) {
			if (var.startsWith(mediaName + Tab.REQUEST_ID_SUFFIX)) {
				// 获取真实Topic
				topic = var.replace(mediaName, "");
			}
		}
		return topic;
	}

	/**
	 * @param media 任务对象
	 * @return 返回是否历史任务  0 否  1是
	 */
	private static int relativeTask(SubscribeMedia media) {

		Date lastedDate = media.update_time;

		if (DateUtils.isSameDay(lastedDate, new Date())) {

			return 0;
		} else {
			return 1;
		}
	}

	/**
	 * 监听redis队列
	 */
	public static void startListen() {

		final RTopic<Object> taskMsgTopic = redisson.getTopic(TASK_MSG);

		taskMsgTopic.addListener((channel, msg) -> {

			System.out.println(msg);

//			更新 TASK_CACHE 集合的数据
			TASK_CACHE.clear();

			final RPriorityQueue<String> var = redisson.getPriorityQueue(Tab.TOPIC_MEDIA);

			final String[] tasks = (String[]) var.toArray();

			for (String task : tasks) {
				final String media = Tab.realMedia(task);
				TASK_CACHE.add(media);
			}
		});
	}
}
