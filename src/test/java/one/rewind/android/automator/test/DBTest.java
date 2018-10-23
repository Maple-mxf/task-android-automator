package one.rewind.android.automator.test;

import com.j256.ormlite.dao.Dao;
import one.rewind.android.automator.model.SubscribeAccount;
import one.rewind.android.automator.model.WechatEssay;
import one.rewind.android.automator.model.WechatEssayComment;
import one.rewind.db.DaoManager;
import one.rewind.db.Refacter;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

public class DBTest {

	@Test
	public void setupTables() throws Exception {
		String packageName = "one.rewind.android.automator.model";
		Refacter.dropTables(packageName);
		Refacter.createTables(packageName);
	}

	@Test
	public void setupTable() throws Exception {
		Refacter.dropTable(WechatEssayComment.class);
		Refacter.createTable(WechatEssayComment.class);
		Refacter.dropTable(WechatEssay.class);
		Refacter.createTable(WechatEssay.class);
	}


	@Test
	public void setupRawTable() throws Exception {
		Refacter.createTable(SubscribeAccount.class);
	}


	@Test
	public void testSQLInject() throws Exception {
		Dao<WechatEssay, String> dao = DaoManager.getDao(WechatEssay.class);
		long countOf = dao.queryBuilder().where().eq("wechat_name", "IPP评论").countOf();
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

}
