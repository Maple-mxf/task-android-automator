package one.rewind.android.automator;

import com.google.common.base.Strings;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.*;
import com.j256.ormlite.dao.GenericRawResults;
import one.rewind.android.automator.adapter.WeChatAdapter;
import one.rewind.android.automator.model.SubscribeMedia;
import one.rewind.android.automator.util.AndroidUtil;
import one.rewind.android.automator.util.DBUtil;
import one.rewind.android.automator.util.Tab;
import one.rewind.db.RedissonAdapter;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.redisson.api.RPriorityQueue;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/**
 * @author maxuefeng[m17793873123@163.com]
 */
@ThreadSafe
public class AndroidDeviceManager {

	private static Logger logger = LoggerFactory.getLogger(AndroidDeviceManager.class);

	/**
	 * guava线程池
	 */
	private ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));

	/**
	 * redis 客户端
	 */
	private static RedissonClient redisClient = RedissonAdapter.redisson;

	/**
	 * 存储无任务设备信息 利用建监听者模式实现设备管理
	 */
	private BlockingQueue<WeChatAdapter> idleAdapters = Queues.newLinkedBlockingDeque(Integer.MAX_VALUE);

	/**
	 *
	 */
	public Stack<String> mediaStack = new Stack<>();

	/**
	 * 所有设备的信息
	 */
	public List<AndroidDevice> devices = new ArrayList<>();

	/**
	 * 初始分页参数
	 */
	private static int startPage = 20;

	/**
	 * 单例
	 */
	private static AndroidDeviceManager manager;

	public static AndroidDeviceManager me() {
		synchronized (AndroidDeviceManager.class) {
			if (manager == null) {
				manager = new AndroidDeviceManager();
			}
		}
		return manager;
	}

	/**
	 *
	 */
	private AndroidDeviceManager() {
	}

	/**
	 * 初始化设备
	 */
	private void init() {

		String[] var = AndroidUtil.obtainDevices();

		for (String aVar : var) {
			AndroidDevice device = new AndroidDevice(aVar);
			logger.info("udid: " + device.udid);
			devices.add(device);
			logger.info("添加device {} 到容器中", device.udid);
		}
	}

	private static void obtainFullData(Set<String> accounts, int page, int var) {
		// TODO 可能出现死循环
		while (accounts.size() <= var) {
			DBUtil.sendAccounts(accounts, page);
			++page;
		}
	}

	/**
	 * 加载数据库中,上一次未完成的任务
	 */
	public void initMediaStack() {
		Set<String> set = Sets.newHashSet();
		obtainFullData(set, startPage, AndroidUtil.obtainDevices().length);
		mediaStack.addAll(set);
	}


	private void execute(WeChatAdapter adapter) {
		try {
			logger.info("start executed");
			//计算任务类型
			adapter.getDevice().taskType = calculateTaskType(adapter);

			System.out.println("当前设备 : " + adapter.getDevice().udid + "的任务类型是: " + adapter.getDevice().taskType);
			//初始化任务队列
			switch (adapter.getDevice().taskType) {
				case Subscribe: {
					// 只分配一个任务去订阅   订阅完了之后立马切换到数据采集任务
					initSubscribeSingleQueue(adapter.getDevice());
					break;
				}
				case Fetch: {
					initCrawlerQueue(adapter.getDevice());
					break;
				}
				default:
					logger.info("当前没有匹配到任何任务类型!");
			}
			adapter.start();
		} catch (Exception e) {
			logger.error("初始化任务失败！");
		}
	}


	public void addIdleAdapter(WeChatAdapter adapter) {
		synchronized (this) {
			this.idleAdapters.add(adapter);
		}
	}

	/**
	 * 初始化订阅任务
	 *
	 * @param device d
	 * @throws SQLException
	 */
	private void initSubscribeSingleQueue(AndroidDevice device) throws SQLException {
		device.queue.clear();

		if (mediaStack.isEmpty()) {
			// 如果没有数据了 先初始化订阅的公众号
			startPage += 2;
			initMediaStack();
		}
		// 今日订阅了多少个号
		int numToday = DBUtil.obtainSubscribeNumToday(device.udid);

		System.out.println("今天订阅了" + numToday + "个号");
		// 处于等待状态
		if (numToday > 40) {

			device.flag = AndroidDevice.Flag.Upper_Limit;

			device.taskType = null;

		} else {
			RPriorityQueue<String> taskQueue = redisClient.getPriorityQueue(Tab.TOPIC_MEDIA);

			System.out.println("redis中的任务队列的数据是否为空? " + taskQueue.size());

			String redisTask = redisTask(device.udid);

			// 如果可以从redis中加到任务
			if (StringUtils.isNotBlank(redisTask)) {
				device.queue.add(redisTask);
			} else {
				device.queue.add(mediaStack.pop());
			}
		}
	}

	/**
	 * 从redis中加载任务
	 *
	 * @param originUdid 设备udid标识
	 * @return 返回任务
	 */
	private String redisTask(String originUdid) {
		RPriorityQueue<String> taskQueue = redisClient.getPriorityQueue(Tab.TOPIC_MEDIA);

		for (String var : taskQueue) {
			if (var.contains(Tab.UDID_SUFFIX)) {

				String udid = Tab.udid(var);

				if (!Strings.isNullOrEmpty(udid) && originUdid.equals(udid)) {
					taskQueue.remove(var);
					return var;
				}
			} else {
				taskQueue.remove(var);
				return var;
			}
		}
		return null;
	}


	/**
	 * 从MySQL中初始化任务
	 *
	 * @param device d
	 * @throws SQLException sql e
	 */
	private void initCrawlerQueue(AndroidDevice device) throws SQLException {
		device.queue.clear();
		SubscribeMedia media =
				Tab.subscribeDao.
						queryBuilder().
						where().
						eq("udid", device.udid).
						and().
						eq("status", SubscribeMedia.State.NOT_FINISH.status).
						queryForFirst();

		System.out.println("当前设备 " + device.udid + "分配的任务是: " + media);
		// 相对于现在没有完成的任务
		if (media == null) {
			device.taskType = null;
			// 处于等待状态
			device.flag = AndroidDevice.Flag.Upper_Limit;
			return;
		}
		// 限制初始化一个任务
		device.queue.add(media.media_name);
	}


	/**
	 * 计算任务类型
	 *
	 * @param adapter
	 * @return
	 * @throws Exception
	 */
	private AndroidDevice.Task.Type calculateTaskType(WeChatAdapter adapter) throws Exception {

		String udid = adapter.getDevice().udid;

		long allSubscribe = Tab.subscribeDao.queryBuilder().where().eq("udid", udid).countOf();

		List<SubscribeMedia> notFinishR = Tab.subscribeDao.queryBuilder().where().
				eq("udid", udid).and().
				eq("status", SubscribeMedia.State.NOT_FINISH.status).
				query();

		int todaySubscribe = obtainSubscribeNumToday(udid);

		if (allSubscribe >= 993) {
			if (notFinishR.size() == 0) {
				adapter.getDevice().flag = AndroidDevice.Flag.Upper_Limit;
				return null;   //当前设备订阅的公众号已经到上限
			}
			return AndroidDevice.Task.Type.Fetch;
		} else if (todaySubscribe >= 40) {
			if (notFinishR.size() == 0) {
				adapter.getDevice().flag = AndroidDevice.Flag.Frequent_Operation;
				return null;
			}
			return AndroidDevice.Task.Type.Fetch;
		} else {
			adapter.getDevice().flag = null;
			// 当前设备订阅的号没有到达上限则分配订阅任务  有限分配订阅接口任务
			if (notFinishR.size() == 0) {
				return AndroidDevice.Task.Type.Subscribe;
			} else {
				return AndroidDevice.Task.Type.Fetch;
			}
		}
	}

	/**
	 * 计算今日订阅了多少公众号
	 *
	 * @param udid
	 * @return
	 * @throws SQLException
	 */
	private int obtainSubscribeNumToday(String udid) throws SQLException {
		GenericRawResults<String[]> results = Tab.subscribeDao.
				queryRaw("select count(id) as number from wechat_subscribe_account where `status` not in (2) and udid = ? and to_days(insert_time) = to_days(NOW())",
						udid);
		String[] firstResult = results.getFirstResult();
		String var = firstResult[0];
		return Integer.parseInt(var);
	}

	private void reset() throws SQLException {
		RQueue<Object> taskMedia = redisClient.getQueue(Tab.TOPIC_MEDIA);
		List<SubscribeMedia> accounts = Tab.subscribeDao.queryForAll();
		for (SubscribeMedia v : accounts) {

			if (v.status == 2 && v.number == 0) {
				if (v.request_id != null) {
					// 重试
					taskMedia.add(v.media_name + v.request_id);
				}
				// 删除记录
				Tab.subscribeDao.delete(v);
			}

			if (v.status == 0) {
				if (v.number >= 100) {
					v.number = v.number * 2;
				} else {
					v.number = 100 * 2;
				}
			}
//			try {
//				if (v.status == 2 || v.status == 1 || v.retry_count >= 5) {
////                    if (v.number != 0) continue;
//					// 有些公众号一片文章也没有
//					continue;
//				}
//
//				long countOf = Tab.essayDao.
//						queryBuilder().
//						where().
//						eq("media_nick", v.media_name).
//						countOf();
//				if ((countOf >= v.number || Math.abs(v.number - countOf) <= 5) && countOf > 0) {
//					v.retry_count = 5;
//					v.status = SubscribeMedia.State.FINISH.status;
//					v.number = (int) countOf;
//				} else {
//					v.status = SubscribeMedia.State.NOT_FINISH.status;
//					v.retry_count = 0;
//					if (v.number == 0) v.number = 100;
//				}
//				v.update();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
		}
	}


	public static class ManagerTask implements Callable<Boolean> {
		@Override
		public Boolean call() throws Exception {

			AndroidDeviceManager manager = AndroidDeviceManager.me();

			// 初始化设备
			manager.init();

			// 重置数据库数据
			manager.reset();

			// 初始化
			manager.initMediaStack();

			for (AndroidDevice device : manager.devices) {
				WeChatAdapter adapter = new WeChatAdapter(device);
				adapter.startupDevice();
				manager.idleAdapters.add(adapter);
			}

			do {
				WeChatAdapter adapter = manager.idleAdapters.take();
				// 获取到休闲设备进行任务执行
				manager.execute(adapter);
			} while (true);
		}
	}

	// 任务启动入口

	public void run() {
		ListenableFuture<Boolean> result = this.service.submit(new ManagerTask());

		Futures.addCallback(result, new FutureCallback<Boolean>() {
			@Override
			public void onSuccess(@NullableDecl Boolean result) {
				logger.info("execute success ok!");
			}

			@Override
			public void onFailure(Throwable t) {
				logger.info("execute failed Not OK Please focus on this");
			}
		});
	}

}
