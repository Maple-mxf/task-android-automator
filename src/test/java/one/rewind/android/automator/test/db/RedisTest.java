package one.rewind.android.automator.test.db;

import one.rewind.android.automator.model.TaskFailRecord;
import one.rewind.db.RedissonAdapter;
import org.junit.Test;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * 描述：
 * 作者：MaXFeng
 * 时间：2018/10/17
 */
public class RedisTest {

	@Test
	public void testTakeObject() {
		RedissonClient redisClient = RedissonAdapter.redisson;
		RMap<String, TaskFailRecord> notFinishTask = redisClient.getMap("NotFinishTask");
		System.out.println(notFinishTask);
		notFinishTask.forEach((k, v) -> System.out.println("k: " + k + "   v:" + v));
	}

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
}
