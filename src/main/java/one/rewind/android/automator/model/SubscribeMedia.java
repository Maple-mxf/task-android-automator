package one.rewind.android.automator.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.DBName;
import one.rewind.db.model.ModelL;

/**
 * @author maxuefeng[m17793873123@163.com]
 */
@DBName(value = "raw")
@DatabaseTable(tableName = "wechat_subscribe_account")
public class SubscribeMedia extends ModelL {

	@DatabaseField(dataType = DataType.STRING, width = 32, index = true, canBeNull = false)
	public String udid;

	@DatabaseField(dataType = DataType.STRING, width = 32, index = true, canBeNull = false)
	public String media_name;

	@DatabaseField(dataType = DataType.STRING, width = 32, index = true)
	public String media_id;

	@DatabaseField(dataType = DataType.INTEGER)
	public int status;

	//文章总量
	@DatabaseField(dataType = DataType.INTEGER)
	public int number;

	//重试次数
	@DatabaseField(dataType = DataType.INTEGER)
	public int retry_count;

	@DatabaseField(dataType = DataType.STRING)
	public String request_id;


	// 此字段用来标记公众号的相对状态,相对于过去处于完成状态 相对于现在处于未完成状态
	// 1表示相对于过去处于完成状态 0表示相对于过去处于未完成状态
	@DatabaseField(dataType = DataType.INTEGER)
	public int relative;


	public enum State {

		FINISH(1),

		// 表示搜索不到当前公众号
		NOT_EXIST(2),

		NOT_FINISH(0);

		public int status;

		State(int status) {
			this.status = status;
		}
	}

}
