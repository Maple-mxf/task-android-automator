package one.rewind.android.automator.route;

import one.rewind.android.automator.APIMainServer;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.AndroidDeviceManager;
import one.rewind.io.server.Msg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.List;

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
	 * 继续执行已经关注的公众号
	 */
	public static Route oldAccounts = (Request req, Response res) -> {

		if (AndroidDeviceManager.running) {
			return new Msg<>(0, "程序正在运行,请稍后再试！");
		}
		new Thread(() -> {

		}).start();
		return new Msg<>(1, "请求成功！程序即将执行您的请求，请稍后");
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


	/**
	 * 恢复任务的进行    这种场景是web服务停止了一段时间  突然又启动
	 * <p>
	 * 1：数据库查询未完成的  ---->  完成未完成的公众号
	 * 2：在另一个未完成记录表中查询详细的文章抓取信息。
	 * 3：获取设备和公众号的对应关系
	 * 4：如何设备存在继续抓   不存在移除不存在设备对应的公众号
	 * <p>
	 */
	public static Route recovery = (Request req, Response res) -> {

		Class.forName("one.rewind.android.automator.AndroidDeviceManager");

		List<AndroidDevice> availableDevices = AndroidDeviceManager.obtainAvailableDevices();

		AndroidDeviceManager manager = AndroidDeviceManager.getInstance();

		manager.allotCrawlerTask(availableDevices, false);

		return new Msg<>(1, "恢复成功");
	};


	public static Route readJson = (Request req, Response res) -> {

		String jsonString = req.body();

		JSONArray jsonArray = new JSONArray(jsonString);

		System.out.println(jsonArray);
		return new Msg<>(0, "请求成功");
	};
}
