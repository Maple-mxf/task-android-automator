package one.rewind.android.automator.test.db;

import one.rewind.data.raw.model.Author;
import one.rewind.db.util.Refactor;
import org.junit.Test;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class DBCreateTable {

    @Test
    public void createTable() throws Exception {


        //DBUtil.initDB(false);

        Refactor.createTable(Author.class);
        //Refactor.createTable(SysLog.class);
    }

}
