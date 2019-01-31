package one.rewind.android.automator.adapter.wechat.model;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.Daos;
import one.rewind.db.annotation.DBName;
import one.rewind.db.model.ModelL;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author maxuefeng[m17793873123@163.com]
 */
@DBName(value = "raw")
@DatabaseTable(tableName = "wechat_account_media_subscribes")
public class WechatAccountMediaSubscribe extends ModelL {

    @DatabaseField(dataType = DataType.INTEGER, index = true, canBeNull = false, uniqueCombo = true)
    public int account_id; // 账号ID

    @DatabaseField(dataType = DataType.STRING, width = 32, index = true, canBeNull = false, uniqueCombo = true)
    public String media_id; // 系统自定义 id

    @DatabaseField(dataType = DataType.STRING, width = 32, index = true, canBeNull = false)
    public String media_name; // 微信号

    @DatabaseField(dataType = DataType.STRING, width = 32, index = true, canBeNull = false)
    public String media_nick; // 公号名称

    public WechatAccountMediaSubscribe() {
    }

    /**
     * @param account_id
     * @param media_id
     * @param media_name
     * @param media_nick
     */
    public WechatAccountMediaSubscribe(int account_id, String media_id, String media_name, String media_nick) {
        this.account_id = account_id;
        this.media_id = media_id;
        this.media_name = media_name;
        this.media_nick = media_nick;
    }

    /**
     * 获取订阅的公众号
     *
     * @param account_id
     * @return
     * @throws Exception
     */
    public static List<String> getSubscribeMediaIds(int account_id) throws Exception {
        Dao<WechatAccountMediaSubscribe, String> dao = Daos.get(WechatAccountMediaSubscribe.class);
        return dao.queryBuilder().where().eq("account_id", account_id).query().stream().map(ams -> ams.media_id).collect(Collectors.toList());
    }
}
