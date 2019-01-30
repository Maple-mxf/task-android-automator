package one.rewind.android.automator.test.db;

import one.rewind.data.raw.model.Comment;
import one.rewind.db.Refacter;
import org.junit.Test;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class DBCreateTable {

    @Test
    public void createTable() throws Exception {
        Refacter.createTable(Comment.class);
    }

}
