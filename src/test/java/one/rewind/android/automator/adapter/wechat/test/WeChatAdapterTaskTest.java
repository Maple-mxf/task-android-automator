package one.rewind.android.automator.adapter.wechat.test;

import com.google.common.util.concurrent.*;
import com.j256.ormlite.dao.Dao;
import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.adapter.Adapter;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.adapter.wechat.model.WechatAccountMediaSubscribe;
import one.rewind.android.automator.adapter.wechat.task.*;
import one.rewind.android.automator.deivce.AndroidDevice;
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
import one.rewind.txt.StringUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author scisaga@gmail.com
 * @date 2019/2/10
 */
public class WeChatAdapterTaskTest {

    public static Logger logger = LogManager.getLogger(WeChatAdapterTaskTest.class.getName());

    @Before
    public void initAndroidDeviceManager() throws Exception {

        Account.getAll(Account.class).forEach(a -> {
            a.occupied = false;
            try {
                a.update();
            } catch (DBInitException | SQLException e) {
                e.printStackTrace();
            }
        });

        AndroidDevice.getAll(AndroidDevice.class).forEach(ad -> {
            ad.status = AndroidDevice.Status.New;
            try {
                ad.update();
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

        TaskHolder holder = new TaskHolder(StringUtil.uuid(), WeChatAdapter.class.getName(), SubscribeMediaTask.class.getName(), Arrays.asList("雷帝触网"));

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

        AndroidDeviceManager.getInstance().submit(
                TaskFactory.getInstance().generateTask(
                        new TaskHolder(StringUtil.uuid(),
                                WeChatAdapter.class.getName(),
                                GetMediaEssaysTask1.class.getName(),
                                Arrays.asList("寒飞论债"))
                ));

        Thread.sleep(10000000);
    }

    @Test
    public void testMultiSubscribeMedia2() throws InterruptedException, AccountException.AccountNotLoad, AndroidException.NoAvailableDevice, TaskException.IllegalParameters {

        List<String> udids = Arrays.asList("ZX1G426B3V", "ZX1G22MMSQ", "ZX1G227PZ7");

        List<String> pa_nicks = Arrays.asList("黄生看金融");

        for (String udid : udids) {
            for (String nick : pa_nicks) {
                TaskHolder holder = new TaskHolder(StringUtil.uuid(), udid, WeChatAdapter.class.getName(), SubscribeMediaTask.class.getName(), Arrays.asList(nick));
                AndroidDeviceManager.getInstance().submit(TaskFactory.getInstance().generateTask(holder));
            }
        }

        Thread.sleep(1000000000);
    }


    /**
     * 测试订阅多个media
     */
    @Test
    public void testMultiSubscribeMedia() throws InterruptedException {
        Set<AndroidDevice> devices = AndroidDeviceManager.getInstance().deviceTaskMap.keySet();

        ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(30));

        for (AndroidDevice ad : devices) {

            Adapter adapter = ad.adapters.get(WeChatAdapter.class.getName());
            System.err.println(adapter.account);

            ListenableFuture<Void> explosion = service.submit(new Subscriber(ad.udid, adapter.account.id, 45));

            Futures.addCallback(explosion,
                    new FutureCallback() {
                        @Override
                        public void onSuccess(@Nullable Object result) {
                            logger.info("Success Subscriber!");
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            logger.info("Error Subscriber execute task failure !");
                        }
                    }, service
            );
        }

        Thread.sleep(1000000000);
    }


    /**
     * 订阅任务
     * <p>
     * 控制任务生成，定时提交
     * 微信号每天能15-30个公众号   TODO 在生成任务之前先加载已经关注的公众号
     */
    class Subscriber implements Callable<Void> {

        private String udid;

        private int accountId;
        private int mediaNumber;

        public Subscriber(String udid, int accountId, int mediaNumber) {
            this.udid = udid;
            this.accountId = accountId;
            this.mediaNumber = mediaNumber;
        }

        @Override
        public Void call() {
            String queueName = this.udid + "-" + this.accountId;

            RedissonClient redisClient = RedissonAdapter.redisson;

            // 获取当前对应的media队列
            RQueue<String> mediaQueue = redisClient.getQueue(queueName);

            // 每天限制订阅？个公众号
            int count = 0;

            for (String var : mediaQueue) {
                // 生成任务    <指定设备>  <指定账号>
                TaskHolder holder = new TaskHolder(StringUtil.uuid(), udid, WeChatAdapter.class.getName(), SubscribeMediaTask.class.getName(), accountId, Arrays.asList(var));
                Task task = TaskFactory.getInstance().generateTask(holder);

                // 任务提交
                try {
                    AndroidDeviceManager.getInstance().submit(task);
                    count++;
                    if (count == mediaNumber) break;

                } catch (Exception e) {
                    // 当前任务提交失败
                    logger.info("Error task submit failure, ", e);
                }
            }
            return null;
        }


    }


    @Test
    public void testGetEssaysTask() throws InterruptedException, DBInitException, SQLException {

        AndroidDeviceManager deviceManager = AndroidDeviceManager.getInstance();

        Dao<WechatAccountMediaSubscribe, String> accountMediaDao = Daos.get(WechatAccountMediaSubscribe.class);

        List<AndroidDevice> devices = deviceManager.deviceTaskMap.keySet().stream().filter(d -> d.status != AndroidDevice.Status.Failed).collect(Collectors.toList());

        for (AndroidDevice d : devices) {

            Adapter adapter = d.adapters.get(WeChatAdapter.class.getName());

            // 当前设备登录微信账号对应的公众号名称
            WechatAccountMediaSubscribe mediaAccount = accountMediaDao.queryBuilder()
                    .where()
                    .eq("account_id", adapter.account.id)
                    .queryForFirst();
            try {
                // 提交任务
                deviceManager.submit(TaskFactory.getInstance().generateTask(
                        new TaskHolder(StringUtil.uuid(),
                                d.udid,
                                WeChatAdapter.class.getName(),
                                GetMediaEssaysTask1.class.getName(),
                                Arrays.asList(mediaAccount.media_nick))
                ));
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Error submit task failure!");
                System.err.println(GetMediaEssaysTask1.class.getName());
            }
        }
        Thread.sleep(10000000);
    }


    @Test
    public void testGetEssaysTask2() throws InterruptedException, DBInitException, SQLException, AccountException.AccountNotLoad, AndroidException.NoAvailableDevice, TaskException.IllegalParameters {

        AndroidDeviceManager.getInstance().submit(
                TaskFactory.getInstance().generateTask(
                        new TaskHolder(StringUtil.uuid(),
                                "ZX1G22PQLH",
                                null,
                                GetMediaEssaysTask1.class.getName(),
                                Arrays.asList("华夏时报"))
                ));

        AndroidDeviceManager.getInstance().submit(
                TaskFactory.getInstance().generateTask(
                        new TaskHolder(StringUtil.uuid(),
                                "ZX1G22PQLH",
                                null,
                                GetMediaEssaysTask1.class.getName(),
                                Arrays.asList("主力君说股"))
                ));

        Thread.sleep(10000000);
    }


    @Test
    public void testMonitorRealTimeTask() throws InterruptedException, TaskException.IllegalParameters, AndroidException.NoAvailableDevice, AccountException.AccountNotLoad {

        AndroidDeviceManager.getInstance().submit(
                TaskFactory.getInstance().generateTask(
                        new TaskHolder(StringUtil.uuid(),
                                WeChatAdapter.class.getName(),
                                MonitorRealTimeTask.class.getName(),
                                Arrays.asList())
                ));

        Thread.sleep(10000000);
    }

}































