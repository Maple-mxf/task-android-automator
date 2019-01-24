package one.rewind.android.automator.task;

import com.j256.ormlite.dao.Dao;
import one.rewind.android.automator.AndroidDeviceManager;
import one.rewind.android.automator.account.Account;
import one.rewind.db.DaoManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class TaskFactory {

    private static final Logger logger = LogManager.getLogger(TaskFactory.class.getName());

    /**
     * 单例
     */
    private static TaskFactory instance;

    public static TaskFactory getInstance() {
        synchronized (TaskFactory.class) {
            if (instance == null) {
                instance = new TaskFactory();
            }
        }
        return instance;
    }

    /**
     * @param holder
     * @return
     */
    public Task generateTask(TaskHolder holder) {

        Task task = null;

        try {
            // A 检验holder参数

            // A1 检验adapter name   必须指定
            if (StringUtils.isBlank(holder.adapter_name)) return null;

            // A2 检验设备是否存在
            if (!AndroidDeviceManager.getInstance().deviceTaskMap.containsKey(holder.udid)) return null;

            // A3 检验账号是否存在
            if (holder.account_id != 0) {
                Dao<Account, String> accountDao = DaoManager.getDao(Account.class);
                // 指定的账号不存在
                if (accountDao.queryBuilder().where().eq("id", holder.account_id).queryForFirst() == null) {
                    return null;
                }
            }
            // B 反射创建Task的实例  TODO?
            if (holder.params.size() == 1) {
                
            }
        } catch (Exception e) {

        }

        return task;
    }

}
