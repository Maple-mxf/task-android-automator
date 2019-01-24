package one.rewind.android.automator.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author maxuefeng[m17793873123@163.com]
 */
public class HttpUtil {

	/**
	 *
	 * @param requestUrl
	 * @param accessToken
	 * @param params
	 * @return
	 * @throws Exception
	 */
	public static String post(String requestUrl, String accessToken, String params)
			throws Exception {
		String contentType = "application/x-www-form-urlencoded";
		return HttpUtil.post(requestUrl, accessToken, contentType, params);
	}

	/**
	 *
	 * @param requestUrl
	 * @param accessToken
	 * @param contentType
	 * @param params
	 * @return
	 * @throws Exception
	 */
	public static String post(String requestUrl, String accessToken, String contentType, String params)
			throws Exception {
		String encoding = "UTF-8";
		if (requestUrl.contains("nlp")) {
			encoding = "GBK";
		}
		return HttpUtil.post(requestUrl, accessToken, contentType, params, encoding);
	}

	/**
	 *
	 * @param requestUrl
	 * @param accessToken
	 * @param contentType
	 * @param params
	 * @param encoding
	 * @return
	 * @throws Exception
	 */
	public static String post(String requestUrl, String accessToken, String contentType, String params, String encoding)
			throws Exception {
		String url = requestUrl + "?access_token=" + accessToken;
		return HttpUtil.postGeneralUrl(url, contentType, params, encoding);
	}

	/**
	 *
	 * @param generalUrl
	 * @param contentType
	 * @param params
	 * @param encoding
	 * @return
	 * @throws Exception
	 */
	public static String postGeneralUrl(String generalUrl, String contentType, String params, String encoding)
			throws Exception {
		URL url = new URL(generalUrl);

		// 打开和URL之间的连接
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		// 设置通用的请求属性
		connection.setRequestProperty("Content-Type", contentType);
		connection.setRequestProperty("Connection", "Keep-Alive");
		connection.setUseCaches(false);
		connection.setDoOutput(true);
		connection.setDoInput(true);

		// 得到请求的输出流对象
		DataOutputStream out = new DataOutputStream(connection.getOutputStream());
		out.write(params.getBytes(encoding));
		out.flush();
		out.close();

		// 建立实际的连接
		connection.connect();

		BufferedReader in;
		in = new BufferedReader(
				new InputStreamReader(connection.getInputStream(), encoding));

		String result = "";
		String getLine;
		while ((getLine = in.readLine()) != null) {
			result += getLine;
		}

		in.close();
		
		return result;
	}
}
