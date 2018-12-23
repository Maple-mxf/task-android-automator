package one.rewind.android.automator.test.db;

import com.j256.ormlite.dao.Dao;
import one.rewind.android.automator.model.Essays;
import one.rewind.android.automator.model.Tab;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class DataTransfer {


    public static void main(String[] args) {

        // 数据存储
        Dao<Essays, String> dao = Tab.essayDao;
    }
}
