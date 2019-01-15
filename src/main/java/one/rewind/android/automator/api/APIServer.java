package one.rewind.android.automator.api;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.AndroidDeviceManager;
import one.rewind.android.automator.model.SubscribeMedia;
import one.rewind.android.automator.util.DateUtil;
import one.rewind.android.automator.util.Tab;
import one.rewind.io.server.Msg;
import one.rewind.json.JSON;
import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.redisson.api.RPriorityQueue;
import org.redisson.api.RSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Route;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static one.rewind.android.automator.model.SubscribeMedia.State;
import static one.rewind.db.RedissonAdapter.redisson;
import static spark.Spark.port;
import static spark.Spark.post;

/**
 * @author maxuefeng [m17793873123@163.com]
 * <p>
 * 1: 记录每次的请求并且缓存 加速后面请求的响应时间
 */
@ThreadSafe
@Deprecated
public class APIServer {

	private static Logger logger = LoggerFactory.getLogger(APIServer.class);

	// 指定设备制定制定任务

	// request body param {"udid":"xxxxx","media":["xxxx"]}
	// response body param {"msg":"xxx","alreadyCompleting":["xxxxx"],"alreadyCompleted":["xxxxx"],"code":1}

	private static Route specific = (req, resp) -> {
		// 参数校验
		String body = req.body();
		if (Strings.isNullOrEmpty(body)) return new Msg<>(0, "请检查您的参数！");
		JSONObject result = new JSONObject(body);

		if (!result.has("udid") || !result.has("media")) return new Msg<>(0, "请检查您的参数！");

		String udid = result.getString("udid");

		if (!parseDeviceExist(udid)) return new Msg<>(0, "设备不存在或者！");  // TODO 需要获取当前设备订阅的任务是否处于

		// topic校验
		String topic = parseTopic(result);

		JSONArray mediasArray = result.getJSONArray("media");
		if (mediasArray == null || mediasArray.length() == 0 || Strings.isNullOrEmpty(topic)) {
			return new Msg<>(0, "请检查您的参数！");
		}

		// media校验

		// 校验规则  如果API接口传输过来的任务已经被订阅
		//          当前任务是否已经完成,如果未完成,将当前公众号放入到alreadyCompleting中
		//                           如果已完成,获取当前任务的最后一次的采集任务,如果相对于今天仍然是未完成任务那么需要抓取最新的文章

		// 已经完成的任务
		List<String> alreadyCompleted = Lists.newArrayList();

		// 正在排队的任务
		List<String> alreadyCompleting = Lists.newArrayList();

		parseMedia(mediasArray, alreadyCompleted, alreadyCompleting, topic, udid);

		Map<String, Object> data = Maps.newHashMap();

		data.put("alreadyCompleted", alreadyCompleted);

		data.put("alreadyCompleting", alreadyCompleting);

		data.put("udid", udid);

		data.put("topic", topic);

		return new Msg<>(1, data);
	};

	private static Route fetch2 = (req, resp) -> {
		String body = req.body();
		if (Strings.isNullOrEmpty(body)) return new Msg<>(0, "请检查您的参数！");
		JSONObject result = new JSONObject(body);

		if (!result.has("media")) return new Msg<>(0, "请检查您的参数！");

		// topic校验
		String topic = parseTopic(result);

		JSONArray mediasArray = result.getJSONArray("media");
		if (mediasArray == null || mediasArray.length() == 0 || Strings.isNullOrEmpty(topic)) {
			return new Msg<>(0, "请检查您的参数！");
		}

		// ------------------参数校验完毕-------------------------
		// 已经完成的任务
		List<String> alreadyCompleted = Lists.newArrayList();

		// 正在排队的任务
		List<String> alreadyCompleting = Lists.newArrayList();

		parseMedia(mediasArray, alreadyCompleted, alreadyCompleting, topic, null);

		Map<String, Object> data = Maps.newHashMap();

		data.put("alreadyCompleted", alreadyCompleted);

		data.put("alreadyCompleting", alreadyCompleting);

		data.put("topic", topic);
		return new Msg<>(1, data);
	};

	/**
	 * 每个客户端一个Topic
	 * <p>
	 * {"media":["facebook"],"topic":"req_id20181225084704623"}
	 */
	@Deprecated
	private static Route fetch = (req, resp) -> {
		String body = req.body();
		if (Strings.isNullOrEmpty(body)) return new Msg<>(0, "请检查您的参数！");
		JSONObject result = new JSONObject(body);
		if (!result.has("media")) {
			return new Msg<>(0, "请检查您的参数！");
		}
		JSONArray mediasArray = result.getJSONArray("media");
		if (mediasArray == null || mediasArray.length() == 0) return new Msg<>(0, "请检查您的参数！");

		String topic;

		String reqTopic = null;
		boolean hasTopic = result.has("topic");

		if (hasTopic) {
			reqTopic = result.getString("topic");
		}
		if (!Strings.isNullOrEmpty(reqTopic)) {

			if (!redisson.getSet(Tab.TOPICS).contains(reqTopic)) {
				return new Msg<>(0, "请检查您的Topic!");
			} else {
				topic = reqTopic;
			}
		} else {
			topic = Tab.REQUEST_ID_SUFFIX + DateUtil.timestamp();
		}
		// 获取当前topic对应的media  优先级队列   先进先出
		RPriorityQueue<Object> topicMedia = redisson.getPriorityQueue(Tab.TOPIC_MEDIA);
		// 已经完成的任务
		List<String> alreadyCompleted = Lists.newArrayList();
		List<String> alreadyCompleting = Lists.newArrayList();

		// 数据缓存到redis中
		for (Object tmp : mediasArray) {

			String var = (String) tmp;
			if (contain(var)) {
				// no add
				alreadyCompleting.add(var);
				continue;
			}

			// 需要验证是否是历史任务？
			// 如果是历史任务   判断是否是否完成？
			//               1 已完成；发布消息通知订阅者
			//               2 未完成：改动数据库中的req_id字段  这种公众号执行可能存在很高的延迟
			SubscribeMedia media = Tab.subscribeDao.queryBuilder().where().eq("media_name", var).queryForFirst();
			if (media == null) {
				// 数据形式应该是   公众号名称+topic
				topicMedia.add(var + topic);
				logger.info("任务：公众号{}已加载到redis中...等待执行");

			} else {
				if (media.status == State.FINISH.status) {

					// 已完成任务
					logger.info("公众号{}加入okSet,状态为:{}", media.media_name, media.status);

					alreadyCompleted.add(media.media_name);

				} else if (media.status == State.NOT_FINISH.status) {

					// status: 0 未完成   但是已经订阅
					media.request_id = topic;
					media.update();
					logger.info("公众号{}已经订阅！任务尚未完成，状态为{}", media.media_name, media.status);
				}
			}


		}

		Map<String, Object> data = Maps.newHashMap();
		RSet<String> tmpTopics = redisson.getSet(Tab.TOPICS);
		tmpTopics.add(topic);

		data.put("topic", topic);
		data.put("alreadyCompleted", alreadyCompleted);
		data.put("alreadyCompleting", alreadyCompleting);
		return new Msg<>(1, data);
	};

	private static boolean contain(String mediaName) {
		RPriorityQueue<String> topicMedia = redisson.getPriorityQueue(Tab.TOPIC_MEDIA);
		for (String var : topicMedia) {
			if (var.startsWith(mediaName + Tab.REQUEST_ID_SUFFIX)) return true;
		}
		return false;
	}


	// 解析Topic

	private static String parseTopic(JSONObject var0) {

		if (var0.has("topic")) {

			String topic = var0.getString("topic");

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

		AndroidDeviceManager manager = AndroidDeviceManager.getInstance();

		boolean has = false;

		for (AndroidDevice device : manager.devices) {
			if (device.udid.equalsIgnoreCase(udid) || (device.status != null && !device.status.equals(AndroidDevice.Status.Exceed_Subscribe_Limit)))
				has = true;
		}
		return has;
	}


	// 解析任务

	private static void parseMedia(JSONArray media, List<String> alreadyCompleted, List<String> alreadyCompleting, String topic, String udid) {
		try {
			// 处理相对任务   历史任务的完成状态仅仅相对于过去 相对于现在仍旧是未完成任务

			for (Object var0 : media) {

				String tmp = (String) var0;

				// 1 当前任务是否已订阅
				SubscribeMedia var1 = Tab.subscribeDao.queryBuilder().where().eq("media_name", tmp).queryForFirst();
				if (var1 != null) {

					// 任务状态为0:  未完成
					// 任务状态为1:  已完成
					// 任务状态为2:  不存在

					if (State.NOT_FINISH.status == var1.status) {
						alreadyCompleting.add(tmp);
					} else if (State.FINISH.status == var1.status) {
						//
						int interval = relativeTask(var1);
						if (interval == 0) {
							alreadyCompleted.add(tmp);
						} else {
							// 设备任务初始化的时候一直会存数据库中存储
							// 获取当前公众号最早的发布时间
							// select pub_date from essays where media_nick = '' and min(pub_date) = pub_date
							// update数据记录
							var1.update_time = new Date();
							var1.status = State.NOT_FINISH.status;
							var1.retry_count = 0;
							// 相对于现在处于未完成当前任务
							var1.relative = 0;
							var1.update();
							// 根据时间的相对差距采集最新更新的数据  采集最新的文章结束的标记是发布时间
							// setLastPage = true   mediaName需要解析去掉1:topic 2:suffix 3:真实udid

						}
					} else if (State.NOT_EXIST.status == var1.status) {
						// 公众号不存在
					}
				} else {

					// 当前任务正在排队
					if (contain(tmp)) {
						alreadyCompleting.add(tmp);
					} else {
						// 当前任务没有排队 并且当前任务也非历史任务  分配到制定设备去做
						// 将当前任务存放到redis中指定设备去做   #realMedia
						// mediaName + topic + "_udid" + udid

						// 将任务提交到redis中
						if (Strings.isNullOrEmpty(udid)) {
							// 附带udid
							redisson.getPriorityQueue(Tab.TOPIC_MEDIA).add(tmp + Tab.REQUEST_ID_SUFFIX + topic + Tab.UDID_SUFFIX + udid);
						} else {
							// 没有udid
							redisson.getPriorityQueue(Tab.TOPIC_MEDIA).add(tmp + Tab.REQUEST_ID_SUFFIX + topic);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// 2

	}

	// 相对任务判断  需要计算离最后一次数据更新的时间间隔

	@Deprecated
	private static int relativeTask(SubscribeMedia media) {

		Date lastedDate = media.update_time;

		if (DateUtils.isSameDay(lastedDate, new Date())) {

			return 0;
		} else {
			// 计算两个日期之间差了多少天
			long var0 = new Date().getTime() - lastedDate.getTime();

			return (int) var0 / 24 * 60 * 60 * 1000;
		}
	}


	// 新增了相对历史任务 一个公众相对于昨天来说已经完成了   但是相对于现在这个公众号还未完成

	public static void main(String[] args) {

		AndroidDeviceManager manage = AndroidDeviceManager.getInstance();

		// 启动任务
		manage.run();

		port(56789);

		// 不需要制定设备去执行任务
		post("/fetch", fetch2, JSON::toJson);

		// 指定设备去执行任务
		post("/specific", specific, JSON::toJson);
	}

}
