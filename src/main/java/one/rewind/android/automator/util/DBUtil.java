package one.rewind.android.automator.util;

import com.j256.ormlite.dao.GenericRawResults;
import one.rewind.android.automator.model.DBTab;
import one.rewind.android.automator.model.Media;

import java.sql.*;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class DBUtil {

    public static void sendAccounts(Set<String> accounts, int page) {
        try {
            Set<String> collect = DBTab.subscribeDao.
                    queryForAll().
                    stream().
                    map(ec -> ec.media_name).
                    collect(Collectors.toSet());

            List<Media> medias = DBTab.mediaDao.queryBuilder().limit(30).offset((page - 1) * 30).query();

            Iterator<Media> iterator = medias.iterator();

            while (iterator.hasNext()) {
                if (!collect.contains(iterator.next().nick)) {
                    accounts.add(iterator.next().nick);
                }
            }
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
                queryRaw("select count(id) as number from wechat_subscribe_account where `status` not in (2) and udid = ? and to_days(insert_time) = to_days(NOW())",
                        udid);
        String[] firstResult = results.getFirstResult();
        String var = firstResult[0];
        return Integer.parseInt(var);
    }
}
