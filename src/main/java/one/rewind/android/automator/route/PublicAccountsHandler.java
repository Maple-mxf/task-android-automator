package one.rewind.android.automator.route;

import one.rewind.android.automator.APIMainServer;
import one.rewind.db.RedissonAdapter;
import one.rewind.io.server.Msg;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import spark.Request;
import spark.Response;
import spark.Route;

/**
 * Create By  2018/10/18
 * Description:  批量处理公众号
 * <p>
 * <p>
 * 所有的公众号集合 original_collection                          RMap
 * 临时存储集合   temp_collection                               RMap
 * 待关注的公众号集合 unsubscribe_collection                     BlockQueue
 * 设备完成关注微信公众号标记集合 ${device_udid}_subscribe         Set
 */

public class PublicAccountsHandler {

	public static final Logger logger = LogManager.getLogger(APIMainServer.class.getName());

	private static RedissonClient redisClient;

	static {
		redisClient = RedissonAdapter.redisson;
	}


	/**
	 * 批量从接口接受所有公众号
	 * <p>
	 * JSON数据格式：
	 * [
	 * {"media_id ":"0xoikmbfhgy","media_nick":"芋道源码"},
	 * {"media_id ":"0xoikmbfhgy","media_nick":"今日头条"}
	 * ]
	 */
	public static Route postAccounts = (Request req, Response res) -> {

		String bodyParams = req.body();

		JSONArray jsonArray = new JSONArray(bodyParams);

		storageAccounts(jsonArray);

		return new Msg<>();
	};


	/**
	 * 存储处理微信公众号
	 * 针对于已经在任务队列中的公众号不进行二次存储  忽略掉重复的公众号
	 *
	 * @param array 公众号数组
	 */
	private static void storageAccounts(JSONArray array) {

		synchronized (PublicAccountsHandler.class) {
			RMap<String, String> original_collection = redisClient.getMap("original_collection");

			RMap<String, String> temp_collection = redisClient.getMap("temp_collection");

			RBlockingDeque<String> unsubscribe_collection = redisClient.getBlockingDeque("unsubscribe_collection");

			if (temp_collection.isExists()) temp_collection.clear();

			array.forEach(v -> {

				JSONObject o = (JSONObject) v;

				String media_id = o.getString("media_id");

				String media_nick = o.getString("media_nick");

				if (!original_collection.isExists() || StringUtils.isEmpty(original_collection.get(media_id))) {
					temp_collection.put(media_id, media_nick);
				}
			});

			if (temp_collection.isExists() && temp_collection.size() > 0)
				temp_collection.forEach((k, v) -> unsubscribe_collection.add(v));

			original_collection.putAll(temp_collection);

		}
	}
}
