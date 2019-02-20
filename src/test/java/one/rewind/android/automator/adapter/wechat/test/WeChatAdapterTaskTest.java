package one.rewind.android.automator.adapter.wechat.test;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.j256.ormlite.dao.Dao;
import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.adapter.wechat.task.GetMediaEssaysTask;
import one.rewind.android.automator.adapter.wechat.task.GetSelfSubscribeMediaTask;
import one.rewind.android.automator.adapter.wechat.task.SubscribeMediaTask;
import one.rewind.android.automator.adapter.wechat.task.UnsubscribeMediaTask;
import one.rewind.android.automator.deivce.AndroidDeviceManager;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AndroidException;
import one.rewind.android.automator.exception.TaskException;
import one.rewind.android.automator.task.Task;
import one.rewind.android.automator.task.TaskFactory;
import one.rewind.android.automator.task.TaskHolder;
import one.rewind.db.Daos;
import one.rewind.db.RedissonAdapter;
import one.rewind.db.exception.DBInitException;
import one.rewind.db.model.Model;
import one.rewind.txt.StringUtil;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.document.StringField;
import org.junit.Before;
import org.junit.Test;
import org.redisson.api.RList;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * @author scisaga@gmail.com
 * @date 2019/2/10
 */
public class WeChatAdapterTaskTest {

    @Before
    public void initAndroidDeviceManager() throws Exception {

        Model.getAll(Account.class).forEach(a -> {
            a.occupied = false;
            try {
                a.update();
            } catch (DBInitException | SQLException e) {
                e.printStackTrace();
            }
        });

        AndroidDeviceManager.getInstance().detectDevices();
    }

    @Test
    public void testGetSelfSubscribePublicAccountTest() throws InterruptedException, AndroidException.NoAvailableDevice, TaskException.IllegalParameters, AccountException.AccountNotLoad {

        TaskHolder holder = new TaskHolder(StringUtil.uuid(), WeChatAdapter.class.getName(), GetSelfSubscribeMediaTask.class.getName());

        Task task = TaskFactory.getInstance().generateTask(holder);

        AndroidDeviceManager.getInstance().submit(task);

        Thread.sleep(10000000);

    }

    @Test
    public void testSubscribe() throws InterruptedException, AndroidException.NoAvailableDevice, TaskException.IllegalParameters, AccountException.AccountNotLoad {

        TaskHolder holder = new TaskHolder(StringUtil.uuid(), WeChatAdapter.class.getName(), SubscribeMediaTask.class.getName(), Arrays.asList("黄生看金融"));

        Task task = TaskFactory.getInstance().generateTask(holder);

        AndroidDeviceManager.getInstance().submit(task);

        Thread.sleep(10000000);

    }

    @Test
    public void testUnsubscribe() throws InterruptedException, AndroidException.NoAvailableDevice, TaskException.IllegalParameters, AccountException.AccountNotLoad {

        TaskHolder holder1 = new TaskHolder(StringUtil.uuid(), WeChatAdapter.class.getName(), UnsubscribeMediaTask.class.getName(), Arrays.asList("拍拍贷"));

        Task task1 = TaskFactory.getInstance().generateTask(holder1);

        AndroidDeviceManager.getInstance().submit(task1);

        Thread.sleep(10000000);
    }


    @Test
    public void testSubscribeAndGetEssays() throws InterruptedException, AndroidException.NoAvailableDevice, TaskException.IllegalParameters, AccountException.AccountNotLoad {

        TaskHolder holder = new TaskHolder(StringUtil.uuid(), WeChatAdapter.class.getName(), SubscribeMediaTask.class.getName(), Arrays.asList("雷帝触网"));

        Task task = TaskFactory.getInstance().generateTask(holder);

        //AndroidDeviceManager.getInstance().submit(task);

        AndroidDeviceManager.getInstance().submit(
                TaskFactory.getInstance().generateTask(
                        new TaskHolder(StringUtil.uuid(), WeChatAdapter.class.getName(), GetMediaEssaysTask.class.getName(), Arrays.asList("雷帝触网"))));

        Thread.sleep(10000000);
    }


    /**
     * 测试订阅多个media
     */
    @Test
    public void testMultiSubscribeMedia() throws InterruptedException, DBInitException, SQLException {

        String[] udids = AndroidDeviceManager.getAvailableDeviceUdids();

        for (String udid : udids) {
            execute(udid);
        }

        Thread.sleep(10000000);
    }


    public void execute(String udid) throws DBInitException, SQLException {

        RedissonClient redisClient = RedissonAdapter.redisson;
        Dao<Account, String> accountDao = Daos.get(Account.class);

        // 查询登录在当前设备的所有的账号ID
        List<Integer> accountIds = accountDao.queryBuilder().where().eq("udid", udid).query().stream().map(t -> t.id).collect(Collectors.toList());

        // 存储集合名称
        List<String> collectionNames = Lists.newArrayList();

        // 获取当前设备和账号对应的集合（存储需要订阅公众号）的名称
        accountIds.forEach(id -> Optional.ofNullable(id).ifPresent(i -> collectionNames.add(udid + "-" + i)));

        // 一次执行其中的每个任务
        collectionNames.forEach(m -> {

            // 获取当前对应的media队列
            RQueue<String> mediaQueue = redisClient.getQueue(m);

            // 获取账号ID  用于指定账号
            int accountId = Integer.parseInt(m.replace(udid + "-", ""));

            // 迭代器遍历
            Iterator<String> iterator = mediaQueue.iterator();

            try {

                while (iterator.hasNext()) {
                    // 获取mediaNick
                    String mediaNick = iterator.next();

                    // 生成任务    <指定设备>  <指定账号>
                    TaskHolder holder = new TaskHolder(StringUtil.uuid(), udid, WeChatAdapter.class.getName(), SubscribeMediaTask.class.getName(), accountId, Arrays.asList(mediaNick));
                    Task task = TaskFactory.getInstance().generateTask(holder);

                    // 任务提交
                    AndroidDeviceManager.getInstance().submit(task);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    /**
     * 订阅任务
     * <p>
     * 控制任务生成，定时提交
     */
    class Subscriber implements Runnable {

        private String udid;

        public Subscriber(String udid) {
            this.udid = udid;
        }

        @Override
        public void run() {

            RedissonClient redisClient = RedissonAdapter.redisson;

            try {
                Dao<Account, String> accountDao = Daos.get(Account.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}































