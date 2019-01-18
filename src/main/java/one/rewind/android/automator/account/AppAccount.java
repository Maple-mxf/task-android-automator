package one.rewind.android.automator.account;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.android.automator.adapter.Adapter;
import one.rewind.db.DBName;
import one.rewind.db.DaoManager;
import one.rewind.db.model.ModelL;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
@DBName("android_automator")
@DatabaseTable(tableName = "app_accounts")
public class AppAccount extends ModelL {

	// 搜索公众号限流
	public static long Default_Search_Public_Account_Frozen_Time = 72 * 3600 * 1000;

	// 点击 “全部消息过于频繁”
	public static long Default_Get_Public_Account_Essay_List_Frozen_Time = 24 * 3600 * 1000;

	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String src_id;

	// 用户名
	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String username;

	// 密码
	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String password;

	// 账号类型
	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String adapter_class_name;

	// 账号状态
	@DatabaseField(dataType = DataType.ENUM_STRING, width = 64)
	public Status status = Status.Normal;

	// 当前对应设备机器码
	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String udid;

	// APP的类型
	@DatabaseField(dataType = DataType.ENUM_STRING)
	public Adapter.AppType appType;

	// App info  包括app的类型等等
	public transient Adapter.AppInfo appInfo;

	/**
	 * 账号状态
	 */
	public enum Status {

		Normal,   // 正常状态
		Blocked,  // 账号被封
		Search_Public_Account_Frozen, // 查询公众号被限流
		Get_Public_Account_Essay_List_Frozen // 获取公众号历史文章被限流
	}

	/**
	 * @param adapter
	 * @return
	 * @throws Exception
	 */
	public static AppAccount getAccount(Adapter adapter) throws Exception {

		long t = System.currentTimeMillis();

		Dao<AppAccount, String> dao = DaoManager.getDao(AppAccount.class);
		return dao.queryBuilder().where()
				.eq("status", "Normal")
				.or().eq("status", "Search_Public_Account_Frozen").and().le("update_time", t - Default_Search_Public_Account_Frozen_Time)
				.or().eq("status", "Get_Public_Account_Essay_List_Frozen").and().le("update_time", t - Default_Get_Public_Account_Essay_List_Frozen_Time)
				.queryForFirst();

	}
}
