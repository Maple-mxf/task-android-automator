package one.rewind.android.automator.test.db;

import one.rewind.android.automator.util.Tab;
import one.rewind.db.RedissonAdapter;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RPriorityQueue;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * @author maxuefeng[m17793873123@163.com]
 */
public class RedisTest {


    @Test
    public void testQueue() throws InterruptedException {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(10);
        for (int i = 0; i < 4; i++) {
            queue.add(i);
        }
        Integer take = queue.take();
        System.out.println(take);
        System.out.println("queue size: " + queue.size());
    }

    @Test
    public void testPrioryQueue() {
        RedissonClient client = RedissonAdapter.redisson;

        RPriorityQueue<Object> priorityQueue = client.getPriorityQueue(Tab.TOPIC_MEDIA);

        System.out.println(priorityQueue.size());

        System.out.println(priorityQueue.poll());
    }

    @Test
    public void testPublishTopic() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379").setPassword("123456");
        RedissonClient client = Redisson.create(config);
        RTopic<Object> iTopic = client.getTopic("custom_topic");
        iTopic.publish("hello  world");
    }

    @Test
    public void testSubscribeTopic() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379").setPassword("123456");
        RedissonClient client = Redisson.create(config);
        RTopic<Object> iTopic = client.getTopic("custom_topic");
        iTopic.addListener((channel, msg) -> {
            System.out.println(channel);
            System.out.println(msg);
        });
    }

    public void test2(String... args) {
        Class<? extends String[]> aClass = args.getClass();
        boolean array = aClass.isArray();
        System.out.println(args.getClass());
        System.out.println(args.getClass());
    }

    @Test
    public void test1() {
        test2("1");
    }

    @Test
    public void testConnection() {
        RedissonClient client = RedissonAdapter.redisson;

        RTopic<Object> topic = client.getTopic("topic");

        topic.publish("hello world");
    }
}
