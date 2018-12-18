package one.rewind.android.automator.test.model;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.data.raw.model.base.ModelD;
import one.rewind.data.raw.util.JSONableListPersister;
import one.rewind.db.DBName;
import one.rewind.db.DaoManager;

import java.util.List;


/**
 * @author maxuefeng[m17793873123@163.com]
 */
@DBName(value = "raw")
@DatabaseTable(tableName = "raw_essay_comments")
public class EssayComment extends ModelD {

    @DatabaseField(dataType = DataType.STRING, width = 32, canBeNull = false)
    public String essay_id;

    @DatabaseField(dataType = DataType.STRING, width = 32, canBeNull = false)
    public String uid;

    @DatabaseField(dataType = DataType.STRING, width = 1024, canBeNull = false)
    public String content;

    @DatabaseField(dataType = DataType.STRING, width = 32, canBeNull = false)
    public String toUid;

    @DatabaseField(persisterClass = JSONableListPersister.class, width = 2048)
    public EssayComment f_comment;

    @DatabaseField(dataType = DataType.INTEGER, width = 32)
    public int like_count;

    @DatabaseField(dataType = DataType.INTEGER, width = 2, canBeNull = false)
    public int status;

    public EssayComment() {
    }

    /**
     *
     * @param id
     * @return
     * @throws Exception
     */
    public static EssayComment getById(String  id) throws Exception {
        Dao<EssayComment, String> dao = DaoManager.getDao(EssayComment.class);
        return dao.queryForId(id);
    }

    /**
     * 查询某一文章下所有的评论
     * @param essay_id
     * @param size 数量
     * @return
     * @throws Exception
     */
    public static List<EssayComment> findByEsssyId(String essay_id, long size, long offset) throws Exception {
        Dao<EssayComment, String> dao = DaoManager.getDao(EssayComment.class);
        return dao.queryBuilder()
                .limit(size).offset(offset)
                .where()
                .eq("media_name", essay_id)
                .query();
    }

    /**
     * 查询某一文章下所有的评论count
     * @param essay_id
     * @return
     * @throws Exception
     */
    public static long getCountByEsssyId(String essay_id) throws Exception {
        Dao<EssayComment, String> dao = DaoManager.getDao(EssayComment.class);
        return dao.queryBuilder()
                .where()
                .eq("media_name", essay_id)
                .countOf();
    }


    /**
     * 获取被点赞的评论数量
     * @param uid
     * @param size
     * @param offset
     * @return
     * @throws Exception
     */
    public static List<EssayComment> getLikeComments(String uid, long size, long offset) throws Exception {
        Dao<EssayComment, String> dao = DaoManager.getDao(EssayComment.class);
        return dao.queryBuilder().limit(size).offset(offset).where()
                .gt("like_count", 0).and().eq("uid", uid)
                .query();
    }

    /**
     *
     * @param uid
     * @return
     * @throws Exception
     */
    public static long getLikeCommentCountByUid(String uid) throws Exception {
        Dao<EssayComment, String> dao = DaoManager.getDao(EssayComment.class);
        return dao.queryBuilder().where()
                .gt("like_count", 0).and().eq("uid", uid)
                .countOf();
    }

}
