package one.rewind.android.automator.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.data.raw.model.base.ModelD;
import one.rewind.db.DBName;

/**
 * Create By 2018/11/7
 * Description:
 */
@DBName(value = "raw")
@DatabaseTable(tableName = "baidu_tokens")
public class BaiduTokens extends ModelD {

    @DatabaseField(dataType = DataType.STRING, width = 255, unique = true)
    public String app_k;

    @DatabaseField(dataType = DataType.STRING, width = 255, unique = true)
    public String app_s;

    @DatabaseField(dataType = DataType.INTEGER, defaultValue = "0")
    public int count;
}
