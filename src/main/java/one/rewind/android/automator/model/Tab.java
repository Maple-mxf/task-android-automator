package one.rewind.android.automator.model;


import com.j256.ormlite.dao.Dao;
import one.rewind.db.DaoManager;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Create By  2018/10/24
 * Description:
 * <p>
 * Because of the program of accounts from java client to send ,The program can not
 * Know a few account,so must Tag the current data flag
 */

public class Tab {

    public static Dao<Essays, String> essayDao;

    public static Dao<SubscribeMedia, String> subscribeDao;

    public static Dao<BaiduTokens, String> tokenDao;

    public static Dao<Media, String> mediaDao;

    // 本地appium端口  为了不使得端口冲突   port自增

    public static AtomicInteger port = new AtomicInteger(50000);

    static {
        try {
            essayDao = DaoManager.getDao(Essays.class);
            subscribeDao = DaoManager.getDao(SubscribeMedia.class);
            tokenDao = DaoManager.getDao(BaiduTokens.class);
            mediaDao = DaoManager.getDao(Media.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
