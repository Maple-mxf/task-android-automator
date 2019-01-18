package one.rewind.android.automator.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.DBName;
import one.rewind.db.model.ModelL;

import java.util.Date;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
@DBName(value = "android_automator")
@DatabaseTable(tableName = "request_record")
public class RequestRecord extends ModelL {
	/**
	 * 任务公众号
	 */
	@DatabaseField(dataType = DataType.STRING)
	public String media;

	/**
	 * 返回的topic
	 */
	@DatabaseField(dataType = DataType.STRING)
	public String topic;

	/**
	 * 是否关注
	 */
	@DatabaseField(dataType = DataType.BOOLEAN)
	public boolean is_follow;

	/**
	 * 是否放在队列
	 */
	@DatabaseField(dataType = DataType.BOOLEAN)
	public boolean in_queue;

	/**
	 * 相对于当前时间是否完成
	 */
	@DatabaseField(dataType = DataType.BOOLEAN)
	public boolean is_finish;

	/**
	 * 是历史任务  但是县归于今天来说没有完成
	 */
	@DatabaseField(dataType = DataType.BOOLEAN)
	public boolean is_finish_history;

	/**
	 * 上一次采集时间
	 */
	@DatabaseField(dataType = DataType.DATE)
	public Date last;

	/**
	 * 设备机器码
	 */
	@DatabaseField(dataType = DataType.STRING)
	public String udid;
}
