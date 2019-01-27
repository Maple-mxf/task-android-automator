package one.rewind.android.automator.service;

import com.google.gson.reflect.TypeToken;
import one.rewind.json.JSON;
import spark.Route;

import java.util.Map;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class APIMainServer {

	protected APIMainServer(){}


	public static void main(String[] args) {

	}


	/**
	 * 将body中的参数转换位TaskHolder
	 * <p>
	 * TaskFactory将TaskHolder转换为Task
	 */
	public static Route fetch = (request, response) -> {

		String paramBody = request.body();

		Object o = JSON.fromJson(paramBody, new TypeToken<Map<String, Object>>() {
		}.getType());

		return null;
	};
}
