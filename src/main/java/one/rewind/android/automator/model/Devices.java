package one.rewind.android.automator.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.data.raw.model.base.ModelL;
import one.rewind.db.DBName;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
@DBName("raw")
@DatabaseTable(tableName = "devices")
public class Devices extends ModelL {

    @DatabaseField(dataType = DataType.STRING)
    public String udid;

    // 异常数量
    @DatabaseField(dataType = DataType.LONG)
    public Long exception_num;

    // 采集页面数量
    @DatabaseField(dataType = DataType.LONG)
    public Long collect_page_num;

    // 采集步骤执行量
    @DatabaseField(dataType = DataType.LONG)
    public Long execute_step_num;
}
