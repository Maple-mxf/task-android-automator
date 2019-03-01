package one.rewind.android.automator.test.db;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.j256.ormlite.dao.Dao;
import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.adapter.wechat.model.WechatAccountMediaSubscribe;
import one.rewind.android.automator.test.db.biz.UserInterface;
import one.rewind.android.automator.test.db.biz.UserInterfaceImpl;
import one.rewind.android.automator.util.Tab;
import one.rewind.db.Daos;
import one.rewind.db.RedissonAdapter;
import one.rewind.db.exception.DBInitException;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.*;
import org.redisson.api.map.event.EntryCreatedListener;
import org.redisson.api.map.event.EntryUpdatedListener;
import org.redisson.client.RedisClient;
import org.redisson.client.RedisClientConfig;
import org.redisson.client.RedisPubSubConnection;
import org.redisson.client.RedisPubSubListener;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.protocol.pubsub.PubSubType;
import org.redisson.config.Config;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * @author maxuefeng[m17793873123@163.com]
 */
public class RedissonTest {
    // 订阅端
    @Test
    public void redissonSubscribe1() throws IOException {

        RedisClientConfig config = new RedisClientConfig();

        config.setAddress("127.0.0.1", 6379).setPassword("123456");

        RedisClient client = RedisClient.create(config);

        RedisPubSubConnection subConnection = client.connectPubSub();

        final CountDownLatch latch = new CountDownLatch(2);

        subConnection.addListener(new RedisPubSubListener() {
            @Override
            public boolean onStatus(PubSubType type, String channel) {
                latch.countDown();
                return true;
            }

            @Override
            public void onPatternMessage(String pattern, String channel, Object message) {
            }

            @Override
            public void onMessage(String channel, Object msg) {
                System.out.println(channel);
                System.out.println(msg);
            }
        });
        subConnection.subscribe(StringCodec.INSTANCE, "testChannel");
        System.in.read();
    }


    // publish
    @Test
    public void redissonPublish() {
        Config tmp = new Config();
        tmp.useSingleServer().setAddress("redis://127.0.0.1:6379").setPassword("123456");
        RedissonClient client = Redisson.create(tmp);
        RTopic<Object> publishTopic = client.getTopic("task_queue");
        long k = publishTopic.publish("hello world");
        System.out.println("K: " + k);
    }

    // subscribe
    @Test
    public void redissonSubscribe() throws IOException {
        Config tmp = new Config();
        tmp.useSingleServer().setAddress("redis://127.0.0.1:6379").setPassword("123456");
        RedissonClient client = Redisson.create(tmp);
        RTopic<Object> topic = client.getTopic("task_queue");
        topic.addListener((channel, msg) -> {
            System.out.println("channel: " + channel);
            System.out.println("message:" + msg);
        });
        System.in.read();
    }

    public static void main(String[] args) {
        RedissonClient redisson = RedissonAdapter.redisson;

        RQueue<Object> h = redisson.getQueue("h");
    }

    @Test
    public void getSet() throws IOException {
        Config tmp = new Config();
        tmp.useSingleServer().setAddress("redis://10.0.0.157:6379").setPassword("123456");
        RedissonClient client = Redisson.create(tmp);
        RPriorityQueue<Object> queue = client.getPriorityQueue(Tab.TOPIC_MEDIA);

        for (Object var : queue) {
            System.out.println(var);
        }
    }

    @Test
    public void pushData2Queue() {
        Config tmp = new Config();
        tmp.useSingleServer().setAddress("redis://127.0.0.1:6379").setPassword("123456");
        RedissonClient client = Redisson.create(tmp);
        RPriorityQueue<Object> queue = client.getPriorityQueue(Tab.TOPIC_MEDIA);
//        queue.add("facebookreq_id20181224031231121");

        for (Object var : queue) {
            System.out.println(var);
        }
    }

    @Test
    public void testRpcRegister() throws IOException {
        Config tmp = new Config();
        tmp.useSingleServer().setAddress("redis://127.0.0.1:6379").setPassword("123456");
        RedissonClient client = Redisson.create(tmp);

        RRemoteService remoteService = client.getRemoteService();

        UserInterface bizUser = new UserInterfaceImpl();

        remoteService.register(UserInterface.class, bizUser);

        System.in.read();
    }

    @Test
    public void testRpcInvoke() {
        Config tmp = new Config();
        tmp.useSingleServer().setAddress("redis://127.0.0.1:6379").setPassword("123456");
        RedissonClient client = Redisson.create(tmp);
        RRemoteService remoteService = client.getRemoteService();
        UserInterface bizUser = remoteService.get(UserInterface.class);
        String say = bizUser.say();
        System.out.println(say);
    }

    @Test
    public void priorityQueuePoll() {
        Config tmp = new Config();
        tmp.useSingleServer().setAddress("redis://127.0.0.1:6379").setPassword("123456");
        RedissonClient client = Redisson.create(tmp);

        RPriorityQueue<Object> queue = client.getPriorityQueue("i-queue");

        queue.add(2);
        queue.add(3);
        queue.add(4);
        queue.add(5);

        for (int i = 0; i < 10; i++) {
            System.out.println("第" + i + "次" + queue.poll());
        }
    }

    @Test
    public void allotMedia() throws IOException, DBInitException, SQLException {

        Dao<Account, String> accountDao = Daos.get(Account.class);

        List<String> mediaNicks = FileUtils.readLines(new File("tmp/media.txt"), "UTF-8");

        ConcurrentLinkedQueue<String> queues = Queues.newConcurrentLinkedQueue();

        queues.addAll(mediaNicks);

        Collection<String> devices = getDevices();


        RedissonClient redisClient = RedissonAdapter.redisson;

        for (String device : devices) {


            // 查询登录在当前设备的所有的账号ID
            List<Integer> accountIds = accountDao.queryBuilder().where().eq("udid", device).query().stream().map(t -> t.id).collect(Collectors.toList());

            // 存储集合名称
            List<String> collectionNames = Lists.newArrayList();

            // 每个账号存储一份
            accountIds.forEach(id -> Optional.ofNullable(id).ifPresent(i -> collectionNames.add(device + "-" + i)));


            // 保存一份副本
            List<String> tmp = Lists.newArrayList();
            for (int i = 0; i < 90; i++) {
                tmp.add(queues.poll());
            }

            // 向集合中添加数据
            collectionNames.forEach(t -> {
                RQueue<Object> currentMediaCollection = redisClient.getQueue(t);
                currentMediaCollection.addAll(tmp);
            });

        }

        RList<Object> other = redisClient.getList("other");

        other.addAll(queues);
    }


    public static Collection<String> getDevices() throws DBInitException, SQLException {

        Dao<Account, String> accountDao = Daos.get(Account.class);
        List<Account> accounts = accountDao.queryForAll();
        Set<String> udids = Sets.newHashSet();

        accounts.forEach(a -> udids.add(a.udid));

        udids.remove("ZX1G323GNB"); // 去掉AD-1
        udids.remove("ZX1G22RN7F"); // 去掉AD-9

        return udids;
    }


    // ZX1G22B42S-24
    // ZX1G22MMSQ-9
    @Test
    public void test0() throws DBInitException, SQLException {
        RedissonClient redisClient = RedissonAdapter.redisson;

        String udid = "ZX1G22PQLH";

        int accountId = 4;

        //
        RQueue<String> queue1 = redisClient.getQueue(udid + "-" + accountId);

        queue1.clear();

        RQueue<String> queue12 = redisClient.getQueue(udid + "-" + 40);


        Dao<WechatAccountMediaSubscribe, String> subscribeDao = Daos.get(WechatAccountMediaSubscribe.class);


        queue12.forEach(m -> {
            try {
                WechatAccountMediaSubscribe var =
                        subscribeDao.queryBuilder().where()
                                .eq("media_nick", m)
                                .and()
                                .eq("account_id", 4)
                                .queryForFirst();

                if (var == null) {
                    queue1.add(m);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }


    // ZX1G227PZ7-28
    @Test
    public void test1() throws DBInitException, SQLException {
        RedissonClient redisClient = RedissonAdapter.redisson;

        String udid = "ZX1G22PQLH";
        int account_id = 4;
        //
        RQueue<String> queue1 = redisClient.getQueue(udid + "-" + account_id);
        Dao<WechatAccountMediaSubscribe, String> subscribeDao = Daos.get(WechatAccountMediaSubscribe.class);

        queue1.forEach(m -> {
            try {
                WechatAccountMediaSubscribe var =
                        subscribeDao.queryBuilder().where()
                                .eq("media_nick", m)
                                .and()
                                .eq("account_id", account_id)
                                .queryForFirst();

                if (var != null) {
                    queue1.remove(m);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    public void testListenMap() throws InterruptedException {

        RedissonClient client = RedissonAdapter.redisson;

        RMapCache<Object, Object> realTimeMsg = client.getMapCache("realTimeMessage");

        realTimeMsg.addListener((EntryUpdatedListener<String, String>) event -> {

            System.out.println("key: " + event.getKey());

            System.out.println("value: " + event.getValue());

            System.out.println("oldValue");
        });

        realTimeMsg.addListener((EntryCreatedListener<String, String>) event -> {

            System.out.println("key: " + event.getKey());

            System.out.println("value: " + event.getValue());

        });

        Thread.sleep(10000000);
    }

    @Test
    public void testAddData2Map() {
        RedissonClient client = RedissonAdapter.redisson;

        RMapCache<Object, Object> realTimeMsg = client.getMapCache("realTimeMessage");

        realTimeMsg.put("kkk", "asdasdsa");
    }
}































