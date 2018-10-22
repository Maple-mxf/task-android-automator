package one.rewind.android.automator;

import com.j256.ormlite.dao.Dao;
import one.rewind.android.automator.adapter.WechatAdapter;
import one.rewind.android.automator.model.SubscribeAccount;
import one.rewind.android.automator.util.AndroidUtil;
import one.rewind.db.DaoManager;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Create By 2018/10/19
 * Description   多设备管理
 */
public class AndroidDeviceManager {

	/**
	 * 从接口传递过来的微信号
	 */
	public static BlockingQueue<String> originalAccounts = new LinkedBlockingDeque<>();

	private AndroidDeviceManager() {
	}

	Logger logger = LoggerFactory.getLogger(AndroidDeviceManager.class);

	/**
	 * key : udid
	 * value: state
	 */
	private static ConcurrentHashMap<String, AndroidDevice> devices = new ConcurrentHashMap<>();

	private static AndroidDeviceManager instance;

	public static final int DEFAULT_APPIUM_PORT = 47454;

	public static final int DEFAULT_LOCAL_PROXY_PORT = 48454;

	public static AndroidDeviceManager getInstance() {
		synchronized (AndroidDeviceManager.class) {
			if (instance == null) {
				instance = new AndroidDeviceManager();
			}
			return instance;
		}
	}

	/**
	 * 获得可用的设备
	 *
	 * @return
	 */
	private static List<AndroidDevice> obtainAvailableDevices() {
		synchronized (AndroidDeviceManager.class) {
			List<AndroidDevice> availableDevices = new ArrayList<>();
			devices.forEach((k, v) -> {
				if (v.state.equals(AndroidDevice.State.INIT)) {
					availableDevices.add(v);
				}
			});
			return availableDevices;
		}
	}


	/**
	 * Description:  获取到可用的设备   开启线程池进行任务分配
	 * 任务分配有两种: 关注公众号和抓取特定公众号文章
	 */
	public void allotTask(TaskType type) throws InterruptedException {
		List<AndroidDevice> availableDevices = obtainAvailableDevices();
		if (TaskType.SUBSCRIBE.equals(type)) {
			allotSubscribeTask(availableDevices);
		} else if (TaskType.CRAWLER.equals(type)) {
			allotCrawlerTask(availableDevices);
		}
		WechatAdapter.executor.shutdown();
		while (!WechatAdapter.executor.isTerminated()) {
			WechatAdapter.executor.awaitTermination(5, TimeUnit.SECONDS);
			System.out.println("progress:   done   %" + WechatAdapter.executor.getCompletedTaskCount());

		}
		logger.info("所有任务执行完毕");
		WechatAdapter.futures.forEach(v -> System.out.println("v:" + v));
	}

	/**
	 * Description:  分配订阅公众号的任务
	 *
	 * @param availableDevices
	 */
	private void allotSubscribeTask(List<AndroidDevice> availableDevices) {
		try {
			for (AndroidDevice device : availableDevices) {

				Dao<SubscribeAccount, String> dao = DaoManager.getDao(SubscribeAccount.class);

				//查询总共关注了多少公众号  今天关注了多少
				long countSub = dao.queryBuilder().where().
						eq("udid", device.udid).countOf();
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(new Date());
				calendar.set(Calendar.HOUR_OF_DAY, 0);
				calendar.set(Calendar.MINUTE, 0);
				calendar.set(Calendar.SECOND, 0);
				Date var0 = calendar.getTime();
				Date var1 = DateUtils.addDays(var0, 1);
				long currentSub = dao.queryBuilder().where().
						eq("udid", device.udid).and().
						between("insert_time", var0, var1).countOf();
				if (countSub >= 920 || currentSub >= 40) {
					continue;
				}

				//初始化设备的任务队列
				device.queue.clear();
				int rest = (int) (40 - currentSub);
				for (int i = 0; i < rest; i++) {
					if (originalAccounts.size() == 0) break;
					device.queue.add(originalAccounts.take());
				}
				//开启任务执行
				if (device.queue.size() == 0) return;
				device.setClickEffect(false);
				WechatAdapter adapter = new WechatAdapter(device);
				WechatAdapter.taskType = TaskType.SUBSCRIBE;
				adapter.start();
				logger.info("任务初始化完毕！！！");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * Description:  抓取文章任务
	 *
	 * @param availableDevices
	 */
	public void allotCrawlerTask(List<AndroidDevice> availableDevices) {
		WechatAdapter.taskType = TaskType.CRAWLER;
		try {
			for (AndroidDevice device : availableDevices) {
				Dao<SubscribeAccount, String> dao = DaoManager.getDao(SubscribeAccount.class);

				//任务队列
				device.queue.clear();
				List<SubscribeAccount> subscribeAccounts = dao.queryBuilder().where().eq("udid", device.udid).query();
				for (SubscribeAccount var : subscribeAccounts) {
					device.queue.add(var.media_name);
				}
				//重置点击生效标记
				device.setClickEffect(false);
				WechatAdapter adapter = new WechatAdapter(device);
				adapter.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 当前任务类型
	 */
	public enum TaskType {
		SUBSCRIBE(1),
		CRAWLER(2);

		private int type;

		TaskType(int type) {
			this.type = type;
		}
	}

	static {
		/**
		 * 初始化设备
		 */
		String[] var = AndroidUtil.obtainDevices();
		for (String v : var) {
			AndroidDevice device = new AndroidDevice(v, DEFAULT_APPIUM_PORT);
			device.state = AndroidDevice.State.INIT;
			devices.put(v, device);
		}
	}

}
