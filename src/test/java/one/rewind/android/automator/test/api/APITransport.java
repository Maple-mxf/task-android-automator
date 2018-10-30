package one.rewind.android.automator.test.api;

import com.google.common.collect.Lists;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.AndroidDeviceManager;
import one.rewind.android.automator.adapter.WechatAdapter;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Create By  2018/10/23
 * Description:
 */

public class APITransport {


	public static Connection conn;


	public static synchronized Connection getConnection() throws ClassNotFoundException, SQLException {

		if (conn == null) {
			Class.forName("com.mysql.jdbc.Driver");

			String url = "jdbc:mysql://192.168.164.11:3306/raw?useSSL=false";
			String user = "raw";
			String password = "raw";

			return DriverManager.getConnection(url, user, password);
		} else {
			return conn;
		}

	}


	public List<String> sendAccounts(int page) {

		try {
			Connection connection = getConnection();
			/**
			 * 第一个参数是分页参数   每次限定20个公众号
			 */
			PreparedStatement ps =
					connection.prepareStatement("select name,nick from media limit ?,80");
			ps.setInt(1, page);

			ResultSet rs = ps.executeQuery();

			List<String> accounts = Lists.newArrayList();
			while (rs.next()) {

				String media_nick = rs.getString("nick");

//				String media_name = rs.getString("name");

//				map.put("media_nick", media_nick);
//				map.put("media_id", media_name);

				accounts.add(media_nick);
			}
			return accounts;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Test
	public void testQueryData() throws MalformedURLException, URISyntaxException {
		// 0
//		String accounts = sendAccounts(1);

		/*Task task = new Task("http://127.0.0.1:8080/api/accounts", accounts);

		Task.Response response = task.getResponse();

		if (response.isActionDone()) {
			System.out.println("请求成功");
		} else {
			System.out.println("请求失败");
		}*/


		List<String> strings = sendAccounts(4);

		System.out.println(strings.size());

		System.out.println(strings);

	}


	@Test
	public void feedProgram() throws InterruptedException {
		if (AndroidDeviceManager.running) {
			return;
		}


		AndroidDeviceManager manager = AndroidDeviceManager.getInstance();

		//   账号限制
		int var = 10000;

		for (int i = 4; i < var / 20; i++) {

			/*if (i % 1000 == 0) {
				var = countAccounts() / 20;
			}*/
			List<String> accounts = sendAccounts(i);

			accounts.forEach(v -> AndroidDeviceManager.originalAccounts.add(v));

			manager.allotTask();

			if (i == 6) return;
		}
	}


	/**
	 * 统计公众号数量
	 */
	public static int countAccounts() throws SQLException, ClassNotFoundException {
		Connection connection = getConnection();

		ResultSet rs = connection.createStatement().executeQuery("select count(id) as count from media");

		if (rs.next()) {
			return rs.getInt("count");
		} else {
			return 0;
		}
	}

	@Test
	public void testAccountsCount() throws SQLException, ClassNotFoundException {
		int i = countAccounts();
		System.out.println(i);
	}


	//从现有数据库中查询微信公众号进行数据抓取    只存在已经抓取文章任务  没有订阅公众号任务
	@Test
	public void proceedProgram() throws ClassNotFoundException, InterruptedException {

		Class.forName("one.rewind.android.automator.AndroidDeviceManager");

		List<AndroidDevice> availableDevices = AndroidDeviceManager.obtainAvailableDevices();

		AndroidDeviceManager manager = AndroidDeviceManager.getInstance();

		manager.allotCrawlerTask(availableDevices, false);

		WechatAdapter.executor.shutdown();

		while (!WechatAdapter.executor.isTerminated()) {
			WechatAdapter.executor.awaitTermination(800, TimeUnit.SECONDS);
			System.out.println("progress:   done   %" + WechatAdapter.executor.isTerminated());
		}
	}
}
