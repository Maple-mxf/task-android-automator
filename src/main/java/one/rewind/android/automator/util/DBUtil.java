package one.rewind.android.automator.util;

import com.j256.ormlite.dao.Dao;
import one.rewind.android.automator.model.SubscribeAccount;
import one.rewind.db.DaoManager;

import java.sql.*;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 */
public class DBUtil {

    public static Dao<SubscribeAccount, String> subscribeDao;

    public static Connection conn;

    static {
        try {
            subscribeDao = DaoManager.getDao(SubscribeAccount.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static synchronized Connection getConnection() throws ClassNotFoundException, SQLException {

        if (conn == null) {
            Class.forName("com.mysql.jdbc.Driver");

            String url = "jdbc:mysql://192.168.164.15:3306/raw?useSSL=false";
            String user = "raw";
            String password = "raw";
            conn = DriverManager.getConnection(url, user, password);
        }
        return conn;
    }

    public static Set<String> sendAccounts(Set<String> accounts, int page) {

        try {
            Connection connection = getConnection();
            /**
             * 第一个参数是分页参数   每次限定20个公众号
             */
            PreparedStatement ps =
                    connection.prepareStatement("select name,nick from media limit ?,80");
            ps.setInt(1, page);

            ResultSet rs = ps.executeQuery();

            List<String> collect = subscribeDao.queryForAll().stream().map(ec -> ec.media_name).collect(Collectors.toList());

            while (rs.next()) {
                String media_nick = rs.getString("nick");
                if (!collect.contains(media_nick)) {
                    accounts.add(media_nick);
                }
            }
            return accounts;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
            accounts.addAll(Objects.requireNonNull(sendAccounts(accounts, page)));
            ++page;
        }
        return page;
    }
}
