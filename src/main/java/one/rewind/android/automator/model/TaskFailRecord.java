package one.rewind.android.automator.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.data.raw.model.base.ModelL;
import one.rewind.db.DBName;

/**
 * 描述： 失败任务记录类
 * 作者：MaXFeng
 * 时间：2018/10/16
 */

@DBName(value = "raw")
@DatabaseTable(tableName = "wechat_task_fail_record")
public class TaskFailRecord extends ModelL {

	public TaskFailRecord() {
	}

	/**
	 * 已经抓取文章的数量
	 */
	@DatabaseField(dataType = DataType.INTEGER, width = 11, canBeNull = false)
	public int finishNum;

	/**
	 * 下一次抓取需要滑动多少页
	 */
	@DatabaseField(dataType = DataType.INTEGER, width = 11, canBeNull = false)
	public int slideNumByPage;

	/**
	 * 当前微信公众号的名称
	 */
	@DatabaseField(dataType = DataType.STRING, width = 32, index = true, canBeNull = false)
	public String wxPublicName;

	/**
	 * 那个设备关注了当前公众号
	 */
	@DatabaseField(dataType = DataType.STRING, width = 11, canBeNull = false)
	public String deviceUdid;
}
