package one.rewind.android.automator.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.data.raw.model.base.ModelL;
import one.rewind.db.DBName;

import java.util.Date;

/**
 * 统计安卓的日志信息 以任务为单位进行记录   任务执行前   任务执行后
 *
 * @author maxuefeng [m17793873123@163.com]
 * @see one.rewind.android.automator.AndroidDevice
 */
@DBName(value = "raw")
@DatabaseTable(tableName = "task_log")
public class TaskLog extends ModelL {

	/**
	 * 设备序列号
	 */
	@DatabaseField(dataType = DataType.STRING)
	public String udid;

	/**
	 * 媒体ID
	 */
	@DatabaseField(dataType = DataType.STRING)
	public String media_id;

	/**
	 * 媒体名称
	 */
	@DatabaseField(dataType = DataType.STRING)
	public String media_nick;

	/**
	 * 执行的步数
	 */
	@DatabaseField(dataType = DataType.INTEGER)
	public int step = 0;

	/**
	 * 抛出异常的次数
	 */
	@DatabaseField(dataType = DataType.INTEGER)
	public int error = 0;

	/**
	 * redis主题ID
	 */
	@DatabaseField(dataType = DataType.STRING)
	public String topic;

	/**
	 * 任务执行是否成功
	 */
	@DatabaseField(dataType = DataType.BOOLEAN)
	public boolean success = false;

	/**
	 * 任务类型
	 */
	@DatabaseField(dataType = DataType.INTEGER)
	public int type;

	public TaskLog() {}

	/**
	 *
	 * @param media_id
	 * @param media_nick
	 * @param topic
	 * @param udid
	 * @param type
	 */
	public TaskLog(String media_id, String media_nick, String topic, String udid, int type) {

		this.media_id = media_id;
		this.media_nick = media_nick;
		this.topic = topic;
		this.udid = udid;
		this.type = type;
	}

	public void buildLog(String media_id, String media_nick, String topic, String udid, int type) {

		this.error = 0;

		this.media_id = media_id;

		this.media_nick = media_nick;

		this.step = 0;

		this.topic = topic;

		this.success = false;

		this.udid = udid;

		this.type = type;
	}

	public void executeSuccess() {
		try {
			this.insert_time = new Date();
			this.update_time = new Date();
			this.success = true;
			this.insert();
		} catch (Exception ignore) {
			ignore.printStackTrace();
		}
	}

	public void executeFailure() {
		try {
			this.insert_time = new Date();
			this.update_time = new Date();
			this.success = false;
		} catch (Exception ignore) {
			ignore.printStackTrace();
		}
	}

	public void error() {
		this.error++;
	}

	public void step() {
		this.step++;
	}
}
