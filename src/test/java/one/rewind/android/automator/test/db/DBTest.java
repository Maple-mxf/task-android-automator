package one.rewind.android.automator.test.db;

import com.google.common.collect.Lists;
import com.j256.ormlite.dao.Dao;
import one.rewind.android.automator.AndroidDeviceManager;
import one.rewind.android.automator.model.*;
import one.rewind.android.automator.util.MD5Util;
import one.rewind.android.automator.util.Tab;
import one.rewind.db.DaoManager;
import one.rewind.db.Refacter;
import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONArray;
import org.junit.Test;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author maxuefeng[m17793873123@163.com]
 */
public class DBTest {

	@Test
	public void setupTables() throws Exception {
		String packageName = "one.rewind.android.automator.model";
		Refacter.dropTables(packageName);
		Refacter.createTables(packageName);
	}

	@Test
	public void setupTable() throws Exception {
		Refacter.dropTable(Comments.class);
		Refacter.createTable(Comments.class);
		Refacter.dropTable(Essays.class);
		Refacter.createTable(Essays.class);
	}


	@Test
	public void setupRawTable() throws Exception {
		Refacter.createTable(BaiduToken.class);
	}


	@Test
	public void testSQLInject() throws Exception {
		Dao<Essays, String> dao = DaoManager.getDao(Essays.class);
		long countOf = dao.queryBuilder().where().eq("media_nick", "IPP评论").countOf();
		System.out.println(countOf);
	}

	@Test
	public void queryByCondition() throws Exception {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		Date zero = calendar.getTime();
		Date current = DateUtils.addDays(zero, 1);
		Dao<SubscribeMedia, String> dao = DaoManager.getDao(SubscribeMedia.class);
		long count = dao.queryBuilder().where().between("insert_time", zero, current).countOf();
		System.out.println(count);
	}


	@Test
	public void updateData() throws Exception {
		Dao<SubscribeMedia, String> dao = DaoManager.getDao(SubscribeMedia.class);

		SubscribeMedia subscribeMedia = dao.queryBuilder().where().
				eq("udid", "ZX1G22PQLH").
				and().
				eq("media_name", "北京理工大学研究生教育").
				queryForFirst();

		System.out.println(subscribeMedia);

		subscribeMedia.status = SubscribeMedia.State.FINISH.status;

		System.out.println(subscribeMedia);

		subscribeMedia.update();
	}


	@Test
	public void byTimeQuery() throws Exception {
		Dao<SubscribeMedia, String> dao = DaoManager.getDao(SubscribeMedia.class);

		Date date = new Date();

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

		String result = df.format(date);

		Date parse = df.parse(result);

		List<SubscribeMedia> insert_time = dao.queryBuilder().where().eq("insert_time", parse).query();

		System.out.println(insert_time.size());
	}


	/**
	 * tokens.put("rDztaDallgGp5GkiZ7mPBUwo", "em7eA1tsCXyqm0HdD83dMwsyG0gSU77n");
	 * tokens.put("dZy8t8zi1j8G0j5yKlz5geQM", "1EKo8m3NheGQZM35gYEUNLjhnkQa9o9R");
	 * tokens.put("y66vnud58pLDi0qi5NDVBnIg", "IEQpqda0jL4u2uFOgLL1TTo6fDSR42em");
	 * tokens.put("e40LU71rtvxEsD6bVlb9U3wV", "zVK9qmK3B2hhWiw3WuYmGIHNHgXR6tgh");
	 *
	 * @throws Exception
	 */
	@Test
	public void initBaiduTokens() throws Exception {
		BaiduToken token1 = new BaiduToken();
		token1.app_k = "rDztaDallgGp5GkiZ7mPBUwo";
		token1.app_s = "em7eA1tsCXyqm0HdD83dMwsyG0gSU77n";
		token1.count = 0;
		token1.insert();
	}


	@Test
	public void testQueryByQuery() throws SQLException {
		Calendar instance = Calendar.getInstance();
		instance.set(Calendar.HOUR_OF_DAY, 0);
		instance.set(Calendar.MINUTE, 0);
		instance.set(Calendar.SECOND, 0);
		Date time = instance.getTime();


		List<SubscribeMedia> query = Tab.subscribeDao.queryBuilder().where().eq("udid", "ZX1G42BX4R").and().ge("insert_time", time).query();

		System.out.println(query);
	}

	@Test
	public void perfectEssays() throws Exception {

		Class.forName("com.mysql.jdbc.Driver");
		Connection conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/raw?useSSL=false", "root", "root");

		PreparedStatement ps1 = conn.prepareStatement("select * from essays limit ?,20");

		PreparedStatement ps2 = conn.prepareStatement("update essays set media_id=?,set images=? where id=?");


		int page = 0;
		boolean flag = true;
		while (flag) {
			ps1.setInt(1, page);
			ResultSet rs = ps1.executeQuery();
			while (rs.next()) {
				String id = rs.getString("id");
				String platform = rs.getString("platform");
				String media_nick = rs.getString("media_nick");
				String content = rs.getString("content");
				String images = new JSONArray(getImgStr(content)).toString();
				String media_id = MD5Util.MD5Encode(platform + "-" + media_nick, "UTF-8");
				ps2.setString(1, media_id);
				ps2.setString(2, images);
				ps2.setString(3, id);
			}
			page++;
			if (page == 2976) {
				flag = false;
			}
		}


//        Dao<Essays, String> dao = Tab.essayDao;

//        while (relativeFlag) {
//            List<Essays> result = dao.queryBuilder().limit(20).offset((page - 1) * 30).query();
//            for (Essays var : result) {
//                var.media_id = MD5Util.MD5Encode(var.platform + "-" + var.media_nick, "UTF-8");
//                var.update();
//                String content = var.content;
//                Set<String> imgStr = getImgStr(content);
//                JSONArray imageOcr = new JSONArray(imgStr);
//                var.images = imageOcr.toString();
//                var.update();
//            }
//
//            page++;
//            if (page == 2976) {
//                relativeFlag = false;
//            }
//        }
		conn.close();
	}


	/**
	 * 得到网页中图片的地址
	 */
	public Set<String> getImgStr(String htmlStr) {
		Set<String> pics = new HashSet<String>();
		String img = "";
		Pattern p_image;
		Matcher m_image;
		// String regEx_img = "<img.*src=(.*?)[^>]*?>"; //图片链接地址
		String regEx_img = "<img.*src\\s*=\\s*(.*?)[^>]*?>";
		p_image = Pattern.compile
				(regEx_img, Pattern.CASE_INSENSITIVE);
		m_image = p_image.matcher(htmlStr);
		while (m_image.find()) {
			// 得到<img />数据
			img = m_image.group();
// 匹配<img>中的src数据
			Matcher m = Pattern.compile("src\\s*=\\s*\"?(.*?)(\"|>|\\s+)").matcher(img);
			while (m.find()) {
				pics.add(m.group(1));
			}
		}
		return pics;
	}

	public static void main(String[] args) throws ClassNotFoundException, SQLException {

		Class.forName("com.mysql.jdbc.Driver");
		Connection conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/raw?useSSL=false", "root", "root");
		conn.setAutoCommit(false);

		PreparedStatement ps1 = conn.prepareStatement("select id from essays  group by id having count(id)>1");
		PreparedStatement ps2 = conn.prepareStatement("select * from essays where id=?");
		PreparedStatement ps3 = conn.prepareStatement("delete from essays where id=? and insert_time=?");

		List<String> ids = Lists.newArrayList();
		ResultSet rs1 = ps1.executeQuery();
		while (rs1.next()) {
			ids.add(rs1.getString("id"));
		}

		int k = 0;

		for (String id : ids) {
			ps2.setString(1, id);

			//查询出来重复的数据
			ResultSet result = ps2.executeQuery();

			int i = 0;
			while (result.next()) {
				if (i != 0) {
					ps3.setString(1, id);
					ps3.setDate(2, result.getDate("insert_time"));
					int row = ps3.executeUpdate();
					System.out.println(row);
					System.out.println("k: " + k);
					k++;
				}
//                System.out.println(result.getString("id"));
//                System.out.println(result.getString("insert_time"));
				i++;

			}
		}

		conn.commit();
		conn.close();
	}

	@Test
	public void test2() {
		AndroidDeviceManager manager = AndroidDeviceManager.getInstance();

		manager.initMediaStack();

		for (String var : manager.mediaStack) {
			System.out.println(var);
			System.out.println(manager.mediaStack.size());
		}
	}

	@Test
	public void test3() throws Exception {
		SubscribeMedia media =
				Tab.subscribeDao.
						queryBuilder().
						where().
						eq("udid", "ZX1G22PQLH").
						and().
						eq("status", SubscribeMedia.State.NOT_FINISH.status).
						queryForFirst();
		System.out.println(media.media_name);

	}
}
