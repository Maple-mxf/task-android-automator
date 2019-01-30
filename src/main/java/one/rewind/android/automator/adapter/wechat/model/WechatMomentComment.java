package one.rewind.android.automator.adapter.wechat.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.annotation.DBName;
import one.rewind.db.model.ModelD;

@DBName(value = "android_automator")
@DatabaseTable(tableName = "wechat_moment_comments")
public class WechatMomentComment extends ModelD {

	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String device_id;

	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String user_name;

	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String user_id;

	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String comment;

	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String comment_user;

	public WechatMomentComment() {}

	public WechatMomentComment(String device_id, String user_id, String user_name) {
		this.device_id = device_id;
		this.user_id = user_id;
		this.user_name = user_name;
	}
}
