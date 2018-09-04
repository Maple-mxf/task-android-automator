package com.sdyk.android.automator.model;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.DBName;
import one.rewind.db.DaoManager;

@DBName(value = "wechat")
@DatabaseTable(tableName = "wechat_public_accounts")
public class WechatPublicAccount extends Model {

	@DatabaseField(dataType = DataType.STRING, width = 32, unique = true, canBeNull = false)
	public String wechat_id;

	@DatabaseField(dataType = DataType.STRING, width = 32, unique = true, canBeNull = false)
	public String name;

	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String content;

	@DatabaseField(dataType = DataType.INTEGER)
	public int essay_count = 0;

	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String subject;

	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String trademark;

	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String phone;

	public WechatPublicAccount() {}

	public static WechatPublicAccount getByName(String name) throws Exception {
		Dao<WechatPublicAccount, String> dao = DaoManager.getDao(WechatPublicAccount.class);
		return dao.queryBuilder().where().eq("name", name).queryForFirst();
	}
}
