package one.rewind.android.automator.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.DBName;
import one.rewind.db.model.ModelL;

/**
 * Create By 2018/11/7
 * Description:
 *
 * @author maxuefeng [m17793873123@163.com]
 */
@DBName(value = "raw")
@DatabaseTable(tableName = "baidu_tokens")
public class BaiduTokens extends ModelL {

    @DatabaseField(dataType = DataType.STRING, width = 255, unique = true)
    public String app_k;

    @DatabaseField(dataType = DataType.STRING, width = 255, unique = true)
    public String app_s;

    @DatabaseField(dataType = DataType.INTEGER, defaultValue = "0")
    public int count;
}
