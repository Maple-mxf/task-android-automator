package com.sdyk.android.automator.test;

import com.sdyk.android.automator.model.WechatEssay;
import com.sdyk.android.automator.model.WechatEssayComment;
import one.rewind.db.Refacter;
import org.junit.Test;

public class DBTest {

	@Test
	public void setupTables() throws Exception {
		Refacter.dropTables("com.sdyk.android.automator.model");
		Refacter.createTables("com.sdyk.android.automator.model");
	}

	@Test
	public void setupTable() throws Exception {
		Refacter.dropTable(WechatEssayComment.class);
		Refacter.createTable(WechatEssayComment.class);
		Refacter.dropTable(WechatEssay.class);
		Refacter.createTable(WechatEssay.class);
	}
}
