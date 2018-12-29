package one.rewind.android.automator.util;

import one.rewind.android.automator.exception.InvokingBaiduAPIException;
import one.rewind.android.automator.model.BaiduTokens;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.Date;

/**
 * @author maxuefeng[m17793873123@163.com]
 */
@SuppressWarnings("JavaDoc")
public class BaiduAPIUtil {

	/**
	 * 获取接口的信任信息
	 *
	 * @param ak
	 * @param sk
	 * @return
	 */
	public static String getAuth(String ak, String sk) {
		// 获取token地址
		String authHost = "https://aip.baidubce.com/oauth/2.0/token?";
		String getAccessTokenUrl = authHost
				// 1. grant_type为固定参数
				+ "grant_type=client_credentials"
				// 2. 官网获取的 API Key
				+ "&client_id=" + ak
				// 3. 官网获取的 Secret Key
				+ "&client_secret=" + sk;
		try {
			URL realUrl = new URL(getAccessTokenUrl);
			// 打开和URL之间的连接
			HttpURLConnection connection = (HttpURLConnection) realUrl.openConnection();
			connection.setRequestMethod("GET");
			connection.connect();

			// 定义 BufferedReader输入流来读取URL的响应
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			StringBuilder result = new StringBuilder();
			String line;
			while ((line = in.readLine()) != null) {
				result.append(line);
			}
			JSONObject jsonObject = new JSONObject(result.toString());
			return jsonObject.getString("access_token");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}


	public static BaiduTokens obtainToken() throws InvokingBaiduAPIException {
		synchronized (BaiduAPIUtil.class) {
			BaiduTokens result = null;
			try {
				result = Tab.tokenDao.
						queryBuilder().
						where().
						le("count", 500).
						queryForFirst();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			if (result == null) {
				throw new InvokingBaiduAPIException("当前没有可用的token");
			}
			result.count += 1;
			result.update_time = new Date();
			try {
				result.update();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return result;
		}
	}
}

