package one.rewind.android.automator.adapter.wechat.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.annotation.DBName;
import one.rewind.db.model.ModelD;

@DBName(value = "android_automator")
@DatabaseTable(tableName = "wechat_moments")
public class WechatMoment extends ModelD {

	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String device_id;

	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String user_name;

	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String user_id;

	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String friend_name;

	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String friend_text;

	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String url;

	@DatabaseField(dataType = DataType.BYTE_ARRAY)
	public byte[] content;

	@DatabaseField(dataType = DataType.STRING, width = 32, defaultValue = "image")
	public String type;

	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String pub_time;

	public WechatMoment() {}

	public WechatMoment(String device_id, String user_id, String user_name) {
		this.device_id = device_id;
		this.user_id = user_id;
		this.user_name = user_name;
	}
}
