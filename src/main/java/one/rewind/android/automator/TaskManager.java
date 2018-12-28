package one.rewind.android.automator;

import com.google.common.collect.Queues;
import one.rewind.db.RedissonAdapter;
import one.rewind.json.JSON;
import org.json.JSONObject;
import org.redisson.api.RedissonClient;

import java.util.Optional;
import java.util.Queue;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class TaskManager {

	private static RedissonClient redissonClient = RedissonAdapter.redisson;

	/**
	 * 全局唯一的任务队列
	 */
	public static final Queue<String> taskQueue = Queues.newConcurrentLinkedQueue();

	private TaskManager() {
	}

	private static TaskManager taskManager;


	public static TaskManager me() {
		return Optional.ofNullable(taskManager).orElseGet(() -> {
			taskManager = new TaskManager();
			return taskManager;
		});
	}


	// 任务状态的管理  --- 达到松耦合  层次清晰

	// 从DB中加载历史任务

	// 接口数据缓存

	public String taskCache(String json) {

		JSONObject result = JSON.fromJson(json, JSONObject.class);


		return "";
	}
}
