package one.rewind.android.automator.util;

import com.j256.ormlite.dao.GenericRawResults;
import one.rewind.android.automator.adapter.DefaultWechatAdapter;
import one.rewind.android.automator.model.BaiduTokens;
import one.rewind.android.automator.model.DBTab;
import one.rewind.android.automator.model.SubscribeMedia;

import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.stream.Collectors;


public class DBUtil {


    private static Connection conn;

    /**
     * @throws SQLException
     * @see DefaultWechatAdapter#subscribeMedia(java.lang.String)
     */
    public static void reset() throws SQLException {
        List<SubscribeMedia> accounts = DBTab.subscribeDao.queryForAll();
        for (SubscribeMedia v : accounts) {
            try {
                if (v.status == 2) {
                    continue;
                }
                long countOf = DBTab.essayDao.
                        queryBuilder().
                        where().
                        eq("media_nick", v.media_name).
                        countOf();
                if ((countOf >= v.number || Math.abs(v.number - countOf) <= 5) && countOf > 0) {
                    v.retry_count = 5;
                    v.status = SubscribeMedia.CrawlerState.FINISH.status;
                    v.number = (int) countOf;
                } else {
                    v.status = SubscribeMedia.CrawlerState.NOFINISH.status;
                    v.retry_count = 0;
                    if (v.number == 0) v.number = 100;
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

    private static void sendAccounts(Set<String> accounts, int page) {
        try {
            Connection connection = getConnection();
            PreparedStatement ps =
                    connection.prepareStatement("select name,nick from media limit ?,80");
            ps.setInt(1, page);
            ResultSet rs = ps.executeQuery();
            Set<String> collect = DBTab.subscribeDao.
                    queryForAll().
                    stream().
                    map(ec -> ec.media_name).
                    collect(Collectors.toSet());

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

    public static void obtainFullData(Set<String> accounts, int page, int var) {
        while (accounts.size() <= var) {
            sendAccounts(accounts, page);
            ++page;
        }
    }

    public static int obtainSubscribeNumToday(String udid) throws SQLException {
        GenericRawResults<String[]> results = DBTab.subscribeDao.
                queryRaw("select count(id) as number from wechat_subscribe_account where udid = ? and to_days(insert_time) = to_days(NOW())",
                        udid);
        String[] firstResult = results.getFirstResult();
        String var = firstResult[0];
        return Integer.parseInt(var);
    }


    public static class ResetTokenState extends TimerTask {
        @Override
        public void run() {
            try {
                List<BaiduTokens> tokens = DBTab.tokenDao.queryForAll();

                for (BaiduTokens v : tokens) {
                    v.count = 0;
                    v.update_time = new Date();
                    v.update();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void startTimer() {
            Timer timer = new Timer(false);
            TimerTask task = new ResetTokenState();
            timer.schedule(task, buildDate(), 1000 * 60 * 60 * 24);
        }

        Date buildDate() {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            Date time = calendar.getTime();

            if (time.before(new Date())) {
                return addDay(time, 1);
            }
            return time;
        }

        Date addDay(Date date, int days) {
            Calendar startDT = Calendar.getInstance();
            startDT.setTime(date);
            startDT.add(Calendar.DAY_OF_MONTH, days);
            return startDT.getTime();
        }
    }
}
