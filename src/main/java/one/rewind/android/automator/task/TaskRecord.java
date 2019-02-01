package one.rewind.android.automator.task;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.annotation.DBName;
import one.rewind.db.model.ModelL;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
@DBName(value = "android_automator")
@DatabaseTable(tableName = "task_records")
public class TaskRecord extends ModelL {

    @DatabaseField(dataType = DataType.STRING, width = 32)
    public String task_id;

    @DatabaseField(dataType = DataType.STRING, width = 128)
    public String udid;

    @DatabaseField(dataType = DataType.INTEGER, width = 11)
    public int account_id; // =0 时 不需要Account

    @DatabaseField(dataType = DataType.STRING, columnDefinition = "MEDIUMTEXT")
    public String content; // 保存出错的堆栈信息

    public TaskRecord() {}

    public TaskRecord(TaskHolder holder) {
        this.task_id = holder.id;
        this.account_id = holder.account_id;
        this.udid = holder.udid;
    }
}
