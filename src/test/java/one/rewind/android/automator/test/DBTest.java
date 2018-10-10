package one.rewind.android.automator.test;

import one.rewind.android.automator.model.WechatEssay;
import one.rewind.android.automator.model.WechatEssayComment;
import one.rewind.db.Refacter;
import org.junit.Test;

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
}
