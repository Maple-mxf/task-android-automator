package one.rewind.android.automator.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.DBName;
import one.rewind.db.model.ModelL;

@DBName(value = "android_automator")
@DatabaseTable(tableName = "wechat_groups")
public class WechatGroup extends ModelL {

	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String device_id;

	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String user_name;

	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String user_id;

	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String group_name;

	@DatabaseField(dataType = DataType.BYTE_ARRAY)
	public byte[] content;

	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String type;

	public WechatGroup() {}

	public WechatGroup(String device_id, String user_id, String user_name) {
		this.device_id = device_id;
		this.user_id = user_id;
		this.user_name = user_name;
	}
}
