package one.rewind.android.automator.test.api;

import one.rewind.json.JSON;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Create By  2018/10/23
 * Description:
 */

public class APITransport {

	public String sendAccounts(int page) {

		try {
			Class.forName("com.mysql.jdbc.Driver");

			String url = "jdbc:mysql://192.168.164.11:3306/raw?useSSL=false";
			String user = "raw";
			String password = "raw";

			Connection connection = DriverManager.getConnection(url, user, password);

			/**
			 * 第一个参数是分页参数   每次限定20个公众号
			 */
			PreparedStatement ps =
					connection.prepareStatement("select name,nick from media limit ?,20");
			ps.setInt(1, page);

			ResultSet rs = ps.executeQuery();

			Map[] v = new Map[20];
			int i = 0;
			while (rs.next()) {

				Map<String, String> map = new HashMap<>();

				String media_nick = rs.getString("nick");

				String media_name = rs.getString("name");

				map.put("media_nick", media_nick);
				map.put("media_id", media_name);
				v[i] = map;

				++i;
			}
			System.out.println(JSON.toJson(v));
			return JSON.toJson(v);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Test
	public void testQueryData() throws MalformedURLException, URISyntaxException {
		// 0
		String accounts = sendAccounts(1);

		/*Task task = new Task("http://127.0.0.1:8080/api/accounts", accounts);

		Task.Response response = task.getResponse();

		if (response.isActionDone()) {
			System.out.println("请求成功");
		} else {
			System.out.println("请求失败");
		}*/

	}


	public void feedProgram(){

	}
}
