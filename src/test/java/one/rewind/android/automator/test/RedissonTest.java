package one.rewind.android.automator.test;

import com.google.common.collect.Queues;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;
import org.redisson.client.RedisClientConfig;
import org.redisson.client.RedisPubSubConnection;
import org.redisson.client.RedisPubSubListener;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.protocol.pubsub.PubSubType;
import org.redisson.config.Config;
import spark.Spark;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

/**
 * Create By 2018/12/10
 * Description:
 */
public class RedissonTest {
    // 订阅端
    @Test
    public void redissonSubscribe1() throws IOException {

        RedisClientConfig config = new RedisClientConfig();

        config.setAddress("localhost", 6379).setPassword("123456");

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
        BlockingQueue<String> queue = Queues.newLinkedBlockingQueue();
        new Thread(() -> {
            try {
                queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        Spark.port(8080);
        Spark.get("/hello", (req, resp) -> "hello world");
    }
}
