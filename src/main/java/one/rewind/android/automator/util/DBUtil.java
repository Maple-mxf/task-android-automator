package one.rewind.android.automator.util;

import com.google.common.collect.Lists;
import com.j256.ormlite.dao.Dao;
import one.rewind.android.automator.model.SubscribeAccount;
import one.rewind.db.DaoManager;

import java.sql.*;
import java.util.List;
import java.util.Objects;

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

            String url = "jdbc:mysql://192.168.164.11:3306/raw?useSSL=false";
            String user = "raw";
            String password = "raw";

            return DriverManager.getConnection(url, user, password);
        } else {
            return conn;
        }

    }

    public static List<String> sendAccounts(List<String> accounts, int page) {

        try {
            Connection connection = getConnection();
            /**
             * 第一个参数是分页参数   每次限定20个公众号
             */
            PreparedStatement ps =
                    connection.prepareStatement("select name,nick from media limit ?,80");
            ps.setInt(1, page);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String media_nick = rs.getString("nick");
                long count = subscribeDao.queryBuilder().where().eq("media_name", media_nick).countOf();
                if (count == 0) {
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
    public static void obtainFullData(List<String> accounts, int page, int var) {
        while (accounts.size() <= var) {
            accounts.addAll(Objects.requireNonNull(sendAccounts(accounts, page)));
            ++page;
        }
    }
}
