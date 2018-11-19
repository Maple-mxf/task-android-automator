package one.rewind.android.automator.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.data.raw.model.base.ModelD;
import one.rewind.db.DBName;

/**
 * Create By 2018/11/19
 * Description:
 */
@DBName(value = "raw")
@DatabaseTable(tableName = "media")
public class Media extends ModelD {

    @DatabaseField(dataType = DataType.INTEGER)
    public int platform_id;

    @DatabaseField(dataType = DataType.STRING)
    public String platform;

    @DatabaseField(dataType = DataType.STRING)
    public String src_id;

    @DatabaseField(dataType = DataType.STRING)
    public String avatar;

    @DatabaseField(dataType = DataType.STRING)
    public String name;

    @DatabaseField(dataType = DataType.STRING)
    public String nick;

    @DatabaseField(dataType = DataType.STRING)
    public String tags;

    @DatabaseField(dataType = DataType.STRING)
    public String content;

    @DatabaseField(dataType = DataType.INTEGER)
    public int fav_count;

    @DatabaseField(dataType = DataType.INTEGER)
    public int fans_count;

    @DatabaseField(dataType = DataType.INTEGER)
    public int essay_count;

    @DatabaseField(dataType = DataType.STRING)
    public String subject;

    @DatabaseField(dataType = DataType.STRING)
    public String trademark;

    @DatabaseField(dataType = DataType.STRING)
    public String phone;

    @DatabaseField(dataType = DataType.STRING)
    public String email;

    @DatabaseField(dataType = DataType.STRING)
    public String weibo;

    @DatabaseField(dataType = DataType.STRING)
    public String qq;

    @DatabaseField(dataType = DataType.STRING)
    public String wechat;

    @DatabaseField(dataType = DataType.STRING)
    public String wechat_media;


}
