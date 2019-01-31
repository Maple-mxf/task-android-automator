package one.rewind.android.automator.task;

import com.j256.ormlite.dao.Dao;
import one.rewind.android.automator.AndroidDeviceManager;
import one.rewind.android.automator.account.Account;
import one.rewind.db.Daos;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;

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
     * 生成Task
     *
     * @param holder
     * @return
     */
    public Task generateTask(TaskHolder holder) {

        Task task = null;

        // A 检验holder参数
        try {
            // A1 检验task_class_name   指定任务类型
            if (StringUtils.isBlank(holder.class_name)) return null;

            // A2 检验设备是否存在
            if (StringUtils.isNotBlank(holder.udid)) {
                if (!AndroidDeviceManager.getInstance().deviceTaskMap.containsKey(holder.udid)) return null;
            }

            // A3 检验账号是否存在
            if (holder.account_id != 0) {
                Dao<Account, String> accountDao = Daos.get(Account.class);
                // 指定的账号不存在
                if (accountDao.queryBuilder().where().eq("id", holder.account_id).queryForFirst() == null) {
                    return null;
                }
            }

            // B 反射生成Task的实例
            Class<?> clazz = Class.forName(holder.class_name);

            Constructor<?> cons = clazz.getConstructor(TaskHolder.class, String[].class);

            int length = holder.params.size();
            String[] media = new String[length];
            for (int i = 0; i < length; i++) {
                media[i] = holder.params.get(i);
            }

            task = (Task) cons.newInstance(holder, media);

        } catch (Exception e) {
            logger.error("Error new instance of Task error. cause [{}] ", e);
        }
        return task;
    }

}
