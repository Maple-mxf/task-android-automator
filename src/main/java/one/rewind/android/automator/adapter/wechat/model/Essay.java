package one.rewind.android.automator.adapter.wechat.model;

import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.DBName;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
@DBName("android_automator")
@DatabaseTable(tableName = "essays")
public class Essay extends one.rewind.data.raw.model.Essay {
}
