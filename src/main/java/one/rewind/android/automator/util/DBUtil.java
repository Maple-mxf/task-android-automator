package one.rewind.android.automator.util;

import com.j256.ormlite.dao.GenericRawResults;
import one.rewind.android.automator.model.Media;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author maxuefeng[m17793873123@163.com]
 */
public class DBUtil {

    public static void sendAccounts(Set<String> accounts, int page) {
        try {
            Set<String> collect = Tab.subscribeDao.
                    queryForAll().
                    stream().
                    map(ec -> ec.media_name).
                    collect(Collectors.toSet());

            List<Media> medias = Tab.mediaDao.queryBuilder().limit(30).offset((page - 1) * 30).query();

            for (Media media : medias) {
                if (!collect.contains(media.nick)) {
                    accounts.add(media.nick);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int obtainSubscribeNumToday(String udid) throws SQLException {
        GenericRawResults<String[]> results = Tab.subscribeDao.
                queryRaw("select count(id) as number from wechat_subscribe_account where `status` not in (2) and udid = ? and to_days(insert_time) = to_days(NOW())",
                        udid);
        String[] firstResult = results.getFirstResult();
        String var = firstResult[0];
        return Integer.parseInt(var);
    }
}
