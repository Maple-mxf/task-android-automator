package one.rewind.android.automator.test.db;

import com.j256.ormlite.dao.Dao;
import one.rewind.android.automator.model.SubscribeAccount;
import one.rewind.android.automator.model.Essays;
import one.rewind.android.automator.model.Comments;
import one.rewind.db.DaoManager;
import one.rewind.db.Refacter;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
        Refacter.createTable(Comments.class);
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
        Dao<SubscribeAccount, String> dao = DaoManager.getDao(SubscribeAccount.class);
        long count = dao.queryBuilder().where().between("insert_time", zero, current).countOf();
        System.out.println(count);
    }


    @Test
    public void updateData() throws Exception {
        Dao<SubscribeAccount, String> dao = DaoManager.getDao(SubscribeAccount.class);

        SubscribeAccount subscribeAccount = dao.queryBuilder().where().
                eq("udid", "ZX1G22PQLH").
                and().
                eq("media_name", "北京理工大学研究生教育").
                queryForFirst();

        System.out.println(subscribeAccount);

        subscribeAccount.status = SubscribeAccount.CrawlerState.FINISH.status;

        System.out.println(subscribeAccount);

        subscribeAccount.update();
    }

    @Test
    public void byTimeQuery() throws Exception {
        Dao<SubscribeAccount, String> dao = DaoManager.getDao(SubscribeAccount.class);

        Date date = new Date();

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        String result = df.format(date);

        List<SubscribeAccount> insert_time = dao.queryBuilder().where().eq("insert_time", result).query();

        System.out.println(insert_time.size());

    }

}
