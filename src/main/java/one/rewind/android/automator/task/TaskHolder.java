package one.rewind.android.automator.task;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.DBName;
import one.rewind.db.model.ModelD;
import one.rewind.db.persister.JSONableListPersister;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author scisaga@gmail.com
 * @date 2019/1/24
 */
@DBName(value = "android_automator")
@DatabaseTable(tableName = "tasks")
public class TaskHolder extends ModelD {

    @DatabaseField(dataType = DataType.STRING, width = 128)
    public String udid;

    @DatabaseField(dataType = DataType.STRING, width = 128)
    public String task_class_name; // 任务类型 全称 带包路径

    @DatabaseField(dataType = DataType.INTEGER, width = 11)
    public int account_id; // =0 时 不需要Account

    @DatabaseField(persisterClass = JSONableListPersister.class, width = 512)
    public List<String> params; // 全称

    @DatabaseField(dataType = DataType.DATE)
    public Date create_time;

    @DatabaseField(dataType = DataType.DATE)
    public Date start_time;

    @DatabaseField(dataType = DataType.DATE)
    public Date end_time;

    @DatabaseField(dataType = DataType.BOOLEAN)
    public boolean success = false;

    @DatabaseField(dataType = DataType.STRING, columnDefinition = "MEDIUMTEXT")
    public String error; // 保存出错的堆栈信息

    // 保存每一步执行的记录
    @DatabaseField(persisterClass = JSONableListPersister.class, columnDefinition = "MEDIUMTEXT")
    public List<String> records = new ArrayList<>();

    /**
     * @param id
     * @param udid
     * @param task_class_name
     */
    public TaskHolder(String id, String udid, String task_class_name) {
        this(id, udid, task_class_name, 0);
    }

    /**
     * @param id
     * @param udid
     * @param task_class_name
     * @param account_id
     */
    public TaskHolder(String id, String udid, String task_class_name, int account_id) {

        this.id = id;
        this.udid = udid;
        this.task_class_name = task_class_name;
        this.account_id = account_id;
    }

    /**
     * @param record
     */
    public void addRecord(String record) {
        this.records.add(record);
    }

}
