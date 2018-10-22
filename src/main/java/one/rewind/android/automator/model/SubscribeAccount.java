package one.rewind.android.automator.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.data.raw.model.base.ModelL;
import one.rewind.db.DBName;

/**
 * Create By  2018/10/20
 * Description 订阅公众号
 */
@DBName(value = "raw")
@DatabaseTable(tableName = "wechat_subscribe_account")
public class SubscribeAccount extends ModelL {


	@DatabaseField(dataType = DataType.STRING, width = 32, index = true, canBeNull = false)
	public String udid;


	@DatabaseField(dataType = DataType.STRING, width = 32, index = true, canBeNull = false)
	public String media_name;


	@DatabaseField(dataType = DataType.STRING, width = 32, index = true, canBeNull = false)
	public String media_id;


}
