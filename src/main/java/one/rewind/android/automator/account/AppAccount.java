package one.rewind.android.automator.account;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.db.DBName;
import one.rewind.db.model.ModelL;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
@DBName("raw")
@DatabaseTable(tableName = "app_accounts")
public class AppAccount extends ModelL {

	// 用户名
	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String username;

	// 密码
	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String password;

	// 账号类型
	@DatabaseField(dataType = DataType.ENUM_STRING, width = 32)
	public Type type;

	// 账号状态
	@DatabaseField(dataType = DataType.ENUM_STRING, width = 32)
	public Status status = Status.Normal;

	// 对应设备机器码
	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String udid;

	// 账号登陆过的设备   一个账号在一台设备上登录
	public transient AndroidDevice device;


	/**
	 * 账号类型
	 */
	public enum Type {
		Wechat,        // 微信
		Dingding,    // 钉钉
		Weibo    // 微博
	}

	/**
	 * 账号状态
	 */
	public enum Status {

		Normal,  // 正常状态
		Blocked, // 账号被封
		RateLimit // 限流

	}
}
