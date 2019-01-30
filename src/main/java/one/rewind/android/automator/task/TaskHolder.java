package one.rewind.android.automator.task;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.annotation.DBName;
import one.rewind.db.model.ModelD;
import one.rewind.db.persister.JSONableListPersister;

import java.util.Date;
import java.util.List;

/**
 * @author scisaga@gmail.com
 * @date 2019/1/24
 */
@DBName(value = "raw")
@DatabaseTable(tableName = "tasks")
public class TaskHolder extends ModelD {

    public TaskHolder(){}

    @DatabaseField(dataType = DataType.STRING, width = 128)
    public String udid;

    @DatabaseField(dataType = DataType.STRING, width = 128)
    public String adapter_class_name; // 任务类型 全称 带包路径

    @DatabaseField(dataType = DataType.STRING, width = 128)
    public String class_name; // 任务类型 全称 带包路径

    @DatabaseField(dataType = DataType.INTEGER, width = 11)
    public int account_id; // =0 时 不需要Account

    @DatabaseField(dataType = DataType.STRING, width = 128)
    public String topic_name; // 如果需要发布到redisson topic 需要提供该值

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

    /**
     * 不指定 设备 不指定账号
     * @param id
     * @param adapter_class_name
     * @param class_name
     */
    public TaskHolder(String id, String adapter_class_name, String class_name) {
        this(id, null, adapter_class_name, class_name, 0);
    }

    /**
     * 不需要指定account
     * @param id
     * @param udid
     * @param class_name
     */
    public TaskHolder(String id, String udid, String adapter_class_name, String class_name) {
        this(id, udid, adapter_class_name, class_name, 0);
    }

    /**
     * @param id
     * @param udid
     * @param class_name
     * @param account_id
     */
    public TaskHolder(String id, String udid, String adapter_class_name, String class_name, int account_id) {

        this.id = id;
        this.udid = udid;
        this.adapter_class_name = adapter_class_name;
        this.class_name = class_name;
        this.account_id = account_id;
    }

    /**
     * @param content
     */
    public void r(String content) {
        try {
            TaskRecord record = new TaskRecord(this);
            record.content = content;
            record.insert();
        } catch (Exception e) {
            Task.logger.error("Error insert record, ", e);
        }
    }

}
