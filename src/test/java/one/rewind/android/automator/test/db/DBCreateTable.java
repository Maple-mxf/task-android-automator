package one.rewind.android.automator.test.db;

import one.rewind.db.util.Refactor;
import one.rewind.io.requester.task.TaskHolder;
import org.junit.Test;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class DBCreateTable {

    @Test
    public void createTable() throws Exception {


        //DBUtil.initDB(false);

        Refactor.createTable(TaskHolder.class);
        //Refactor.createTable(SysLog.class);
    }

}
