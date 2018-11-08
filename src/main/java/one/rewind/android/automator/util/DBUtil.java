package one.rewind.android.automator.util;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.GenericRawResults;
import one.rewind.android.automator.adapter.WechatAdapter;
import one.rewind.android.automator.model.Essays;
import one.rewind.android.automator.model.SubscribeMedia;
import one.rewind.db.DaoManager;

import java.sql.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 */
public class DBUtil {

    private static Dao<SubscribeMedia, String> subscribeDao;

    private static Dao<Essays, String> essaysDao;

    private static Connection conn;

    static {
        try {
            subscribeDao = DaoManager.getDao(SubscribeMedia.class);
            essaysDao = DaoManager.getDao(Essays.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * @throws SQLException
     * @see WechatAdapter#subscribeWxAccount(java.lang.String)
     */
    public static void reset() throws SQLException {
        List<SubscribeMedia> accounts = subscribeDao.queryForAll();
        for (SubscribeMedia v : accounts) {
            try {
                if (v.status == 2) {
                    continue;
                }
                long countOf = essaysDao.queryBuilder().where().eq("media_name", v.media_name).countOf();
                if ((countOf - v.number) > 5) {
                    v.status = 0;
                } else {
                    v.status = 1;
                }
                v.update();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private static synchronized Connection getConnection() throws ClassNotFoundException, SQLException {

        if (conn == null) {
            Class.forName("com.mysql.jdbc.Driver");

            String url = "jdbc:mysql://192.168.164.15:3306/raw?useSSL=false";
            String user = "raw";
            String password = "raw";
            conn = DriverManager.getConnection(url, user, password);
        }
        return conn;
    }

    /**
     * 第一个参数是分页参数   每次限定20个公众号
     */
    public static void sendAccounts(Set<String> accounts, int page) {
        try {
            Connection connection = getConnection();
            PreparedStatement ps =
                    connection.prepareStatement("select name,nick from media limit ?,80");
            ps.setInt(1, page);
            ResultSet rs = ps.executeQuery();
            Set<String> collect = subscribeDao.queryForAll().stream().map(ec -> ec.media_name).collect(Collectors.toSet());
            while (rs.next()) {
                String media_nick = rs.getString("nick");
                if (!collect.contains(media_nick)) {
                    accounts.add(media_nick);
                }
            }
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取指定数量的公众账号
     *
     * @param accounts
     * @param page
     * @param var
     */
    public static int obtainFullData(Set<String> accounts, int page, int var) {
        while (accounts.size() <= var) {
            sendAccounts(accounts, page);
            ++page;
        }
        return page;
    }

    /**
     * 当前设备今天订阅了多少公众账号
     *
     * @param udid
     * @return
     */
    public static int obtainSubscribeNumToday(String udid) throws SQLException {
        GenericRawResults<String[]> results = subscribeDao.queryRaw("select count(id) as number from wechat_subscribe_account where udid = ? and to_days(insert_time) = to_days(NOW())",
                udid);
        String[] firstResult = results.getFirstResult();
        String var = firstResult[0];
        return Integer.parseInt(var);
    }
}
