package one.rewind.android.automator.route;

import one.rewind.android.automator.APIMainServer;
import one.rewind.android.automator.AndroidDeviceManager;
import one.rewind.io.server.Msg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.Route;

/**
 * Create By  2018/10/18
 * Description:  批量处理公众号
 * <p>
 * <p>
 */

public class PublicAccountsHandler {

	public static final Logger logger = LogManager.getLogger(APIMainServer.class.getName());

	/**
	 * 批量从接口接受所有公众号
	 * JSON数据格式：
	 * [
	 * {"media_id ":"0xoikmbfhgy","media_nick":"芋道源码"},
	 * {"media_id ":"0xoikmbfhgy","media_nick":"今日头条"}
	 * ]
	 */
	public static Route postAccounts = (Request req, Response res) -> {

		String bodyParams = req.body();

		JSONArray jsonArray = new JSONArray(bodyParams);

		return storageAccounts(jsonArray);
	};


	/**
	 * <p>初始化任务队列   执行任务</p>
	 *
	 * @param array 公众号数组
	 */
	private static Object storageAccounts(JSONArray array) {
		if (AndroidDeviceManager.running) {
			return new Msg<>(0);
		}

		try {
			array.forEach(v -> {
				JSONObject var = (JSONObject) v;
				AndroidDeviceManager.originalAccounts.add(var.getString("media_nick"));
			});

			AndroidDeviceManager manager = AndroidDeviceManager.getInstance();

			manager.allotTask();
			return new Msg<>();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("任务执行错误   代码坐标：one.rewind.android.automator.route.PublicAccountsHandler.storageAccounts");
		}
		return null;
	}
}
