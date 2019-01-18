package one.rewind.android.automator;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import one.rewind.android.automator.adapter.Adapter;
import one.rewind.android.automator.util.ShellUtil;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.annotation.concurrent.ThreadSafe;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * @author maxuefeng[m17793873123@163.com]
 */
@ThreadSafe
public class AndroidDeviceManager {

	private static Logger logger = LogManager.getLogger(AndroidDeviceManager.class);

	/**
	 * guava线程池
	 */
	private ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));


	// Adapter - AndroidDevice 记录表，记录哪些设备可使用特定类型的Adapter
	public ConcurrentHashMap<String, List<AndroidDevice>> adapterAndroidDeviceMap = new ConcurrentHashMap<>();


	// 所有设备的任务
	public ConcurrentHashMap<AndroidDevice, Queue<String>> task = new ConcurrentHashMap<>();

	/**
	 * 所有设备的信息
	 */
	public static List<AndroidDevice> androidDevices = new ArrayList<>();

	/**
	 * 单例
	 */
	private static AndroidDeviceManager instance;

	public static AndroidDeviceManager getInstance() {
		synchronized (AndroidDeviceManager.class) {
			if (instance == null) {
				instance = new AndroidDeviceManager();
			}
		}
		return instance;
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

		String[] udids = getAvailableDeviceUdids();

		for (String udid : udids) {
			AndroidDevice device = new AndroidDevice(udid);
			logger.info("udid: " + device.udid);

			androidDevices.add(device);
			logger.info("添加device " + device.udid + " 到容器中");
		}
	}

	/*private static void obtainFullData(Set<String> accounts, int page, int var) {
		// TODO 可能出现死循环
		while (accounts.size() <= var) {
			DBUtil.sendAccounts(accounts, page);
			++page;
		}
	}*/

	/**
	 * 加载数据库中,上一次未完成的任务
	 */
/*	public void initMediaStack() {
		Set<String> set = Sets.newHashSet();
		obtainFullData(set, startPage, DeviceUtil.obtainDevices().length);
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
					distributionSubscribeTask(adapter.getDevice());
					break;
				}
				case Fetch: {
					distributionFetchTask(adapter.getDevice());
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

	*//**
	 * 初始化订阅任务
	 *
	 * @param device d
	 * @throws SQLException e
	 *//*
	private void distributionSubscribeTask(AndroidDevice device) throws SQLException {
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

			device.status = AndroidDevice.Status.Exceed_Subscribe_Limit;

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

	*//**
	 * 从redis中加载任务
	 *
	 * @param originUdid 设备udid标识
	 * @return 返回任务
	 *//*
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


	*//**
	 * 从MySQL中初始化任务
	 *
	 * @param device d
	 * @throws SQLException sql e
	 *//*
	private void distributionFetchTask(AndroidDevice device) throws SQLException {
		device.queue.clear();
		SubscribeMedia media =
				Tab.subscribeDao.
						queryBuilder().
						where().
						eq("udid", device.udid).
						and().
						eq("status", SubscribeMedia.State.NOT_FINISH.status).
						queryForFirst();

		// 相对于现在没有完成的任务
		if (media == null) {
			device.taskType = null;
			// 处于等待状态
			device.status = AndroidDevice.Status.Exceed_Subscribe_Limit;
			return;
		}
		// 限制初始化一个任务
		device.queue.add(media.media_name);
	}


	*//**
	 * 计算任务类型
	 *
	 * @param adapter
	 * @return
	 * @throws Exception
	 *//*
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
				adapter.getDevice().status = AndroidDevice.Status.Exceed_Subscribe_Limit;
				return null;   //当前设备订阅的公众号已经到上限
			}
			return AndroidDevice.Task.Type.Fetch;
		} else if (todaySubscribe >= 40) {
			if (notFinishR.size() == 0) {
				adapter.getDevice().status = AndroidDevice.Status.Operation_Too_Frequent;
				return null;
			}
			return AndroidDevice.Task.Type.Fetch;
		} else {
			adapter.getDevice().status = null;
			// 当前设备订阅的号没有到达上限则分配订阅任务  有限分配订阅接口任务
			if (notFinishR.size() == 0) {
				return AndroidDevice.Task.Type.Subscribe;
			} else {
				return AndroidDevice.Task.Type.Fetch;
			}
		}
	}

	*//**
	 * 计算今日订阅了多少公众号
	 *
	 * @param udid
	 * @return
	 * @throws SQLException
	 *//*
	private int obtainSubscribeNumToday(String udid) throws SQLException {
		GenericRawResults<String[]> results = Tab.subscribeDao.
				queryRaw("select count(id) as number from wechat_subscribe_account where `status` not in (2) and udid = ? and to_days(insert_time) = to_days(NOW())",
						udid);
		String[] firstResult = results.getFirstResult();
		String var = firstResult[0];
		return Integer.parseInt(var);
	}

	private void reset() {
		try {
			RQueue<Object> taskMedia = redisClient.getQueue(Tab.TOPIC_MEDIA);
			List<SubscribeMedia> accounts = Tab.subscribeDao.queryForAll();
			for (SubscribeMedia v : accounts) {

				if (v.status == 2 && v.number == 0) {
					if (v.topic != null) {
						// 重试
						taskMedia.add(v.media_name + v.topic);
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
				v.update();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	static class Task implements Callable<Boolean> {
		@Override
		public Boolean call() throws Exception {

			AndroidDeviceManager manager = AndroidDeviceManager.getInstance();

			// 初始化设备
			manager.init();

			// 重置数据库数据
			manager.reset();

			// 初始化
			manager.initMediaStack();

			for (AndroidDevice device : manager.androidDevices) {
				WeChatAdapter adapter = new WeChatAdapter(device);
				adapter.setupDevice();
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
		ListenableFuture<Boolean> result = this.service.submit(new Task());

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
	}*/

	/**
	 * 获取可用的设备 udid 列表
	 *
	 * @return
	 */
	public static String[] getAvailableDeviceUdids() {

		ShellUtil.exeCmd("adb"); // 有可能需要先启动 adb 服务器

		ShellUtil.exeCmd("adb usb"); // 有可能需要刷新 adb udb 连接

		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();

		try {

			Process p = Runtime.getRuntime().exec("adb androidDevices");
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		String r = sb.toString().replace("List of androidDevices attached", "").replace("\t", "");

		return r.split("device");
	}


	/**
	 * 加载所有的AndroidDevice
	 *
	 * Android Device 和 AppAccount之间存在弱引用的关系（逻辑上定义的弱引用关系）
	 *
	 *
	 */
	static {

		// A 加载所有安卓设备
		String[] udids = getAvailableDeviceUdids();

		for (String udid : udids) {

			AndroidDevice device = new AndroidDevice(udid);

			androidDevices.add(device);
		}

		// B 加载所有设备的任务   AndroidDevice--->Account--->media  name
		for (AndroidDevice device : androidDevices) {

		}
	}

	/**
	 * 在当前机器上登录过的账号查询   根据Appinfo查询
	 */
	public static void queryAccountByUdid(String udid, Adapter.AppInfo appInfo) {
	}
}
