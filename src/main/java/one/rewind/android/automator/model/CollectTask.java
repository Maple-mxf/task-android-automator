package one.rewind.android.automator.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.DBName;
import one.rewind.db.model.ModelL;

import java.util.Date;

/**
 * 采集任务 监控爬虫用途
 *
 * @author maxuefeng [m17793873123@163.com]
 */

@DBName(value = "raw")
@DatabaseTable(tableName = "collect_task")
public class CollectTask extends ModelL {

    // 提交者
    @DatabaseField(dataType = DataType.STRING)
    public String submission;

    @DatabaseField(dataType = DataType.STRING)
    public String media;

    @DatabaseField(dataType = DataType.STRING)
    public String udid;

    @DatabaseField(dataType = DataType.DATE)
    public Date start_time;

    // 是否结束
    @DatabaseField(dataType = DataType.BOOLEAN)
    public Boolean end;

    // 客户端IP地址
    @DatabaseField(dataType = DataType.STRING)
    public String ip;
}
