package one.rewind.android.automator;

import com.j256.ormlite.dao.Dao;
import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.exception.AndroidException;
import one.rewind.android.automator.task.Task;
import one.rewind.android.automator.util.ShellUtil;
import one.rewind.db.DaoManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author maxuefeng[m17793873123@163.com]
 */
public class AndroidDeviceManager {

    private static final Logger logger = LogManager.getLogger(AndroidDeviceManager.class.getName());

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

    // 默认的Device对应的Adapter类的全路径
    public static List<String> DefaultAdapterClassNameList = new ArrayList<>();

    static {
        DefaultAdapterClassNameList.add(WeChatAdapter.class.getName());
    }


    // Adapter - AndroidDevice 记录表，记录哪些设备可使用特定类型的Adapter
    public ConcurrentHashMap<String, List<AndroidDevice>> adapterAndroidDeviceMap = new ConcurrentHashMap<>();


    // 所有设备的任务
    public ConcurrentHashMap<String, BlockingQueue<Task>> deviceTaskMap = new ConcurrentHashMap<>();

    /**
     * 所有设备的信息
     */
    public List<AndroidDevice> androidDevices = new ArrayList<>();


    /**
     *
     */
    private AndroidDeviceManager() {

    }

    /**
     * 初始化设备
     */
    private void initialize() throws Exception {

        Dao<AndroidDevice, String> deviceDao = DaoManager.getDao(AndroidDevice.class);

        // A 先找设备
        String[] udids = getAvailableDeviceUdids();

        for (String udid : udids) {

            // A1 创建 AndroidDevice 对象
            AndroidDevice device = new AndroidDevice(udid);
            logger.info("udid: " + device.udid);

            androidDevices.add(device);
            logger.info("add device [{}] in device container", device.udid);

            // A2 TODO 同步数据库对应记录
            deviceDao.delete(device);
            device.insert();
        }

        // B 加载默认的Adapters
        for (AndroidDevice ad : androidDevices) {

            deviceTaskMap.put(ad.udid, new LinkedBlockingDeque<>());

            for (String className : DefaultAdapterClassNameList) {

                Class<?> clazz = Class.forName(className);

                Constructor<?> cons;

                Field[] fields = clazz.getFields();
                boolean needAccount = false;

                for (Field field : fields) {
                    if (field.getName().equals("NeedAccount")) {
                        needAccount = field.getBoolean(clazz);
                        break;
                    }
                }

                // 如果Adapter必须使用Account
                if (needAccount) {

                    cons = clazz.getConstructor(AndroidDevice.class, Account.class);

                    Account account = Account.getAccount(ad.udid, className);

                    if (account != null) {
                        cons.newInstance(ad, account);
                    }
                    // 找不到账号，对应设备无法启动
                    else {
                        ad.status = AndroidDevice.Status.Failed;
                    }

                } else {
                    cons = clazz.getConstructor(AndroidDevice.class);
                    cons.newInstance(ad);
                }
            }

            ad.idleCallbacks.add((d) -> {
                try {
                    assign(d);
                } catch (InterruptedException | AndroidException.IllegalStatusException e) {
                    logger.error("Error assign task to Device[{}], ", d.udid, e);
                    d.status = AndroidDevice.Status.Failed;
                }
            });
        }

        // C 设备INIT
        androidDevices.parallelStream()
                .filter(d -> d.status != AndroidDevice.Status.Failed)
                .forEach(d -> {
                    try {
                        d.start();
                    } catch (AndroidException.IllegalStatusException e) {
                        logger.error("Start Device[{}] failed, ", d.udid, e);
                    }
                });
    }

    /**
     * @param ad
     * @throws InterruptedException
     * @throws AndroidException.IllegalStatusException
     */
    public void assign(AndroidDevice ad) throws InterruptedException, AndroidException.IllegalStatusException {

        Task task = deviceTaskMap.get(ad.udid).take();
        ad.submit(task);
    }

    /**
     * @param task
     */
    public void submit(Task task) {

        if (task.holder == null || task.holder.adapter_name == null) return;

        // task.holder.udid; // 是否指定设备
        // task.holder.adapter_name; // 指定App
        // task.holder.account_id; // 指定账户

        // 合法模式
        // 1. adapter_name
        // 2. udid adapter_name
        // 3. adapter_name account_id

        // 其他异常举例
        // 订阅公众号任务 预分派的Device account_id处于限流状态


    }

    /**
     * 加载数据库中,上一次未完成的任务
     */
	/* public void initMediaStack() {
		Set<String> set = Sets.newHashSet();
		obtainFullData(set, startPage, DeviceUtil.obtainDevices().length);
		mediaStack.addAll(set);
	}


	private void run(WeChatAdapter adapter) {
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
		AccountMediaSubscribe media =
				Tab.subscribeDao.
						queryBuilder().
						where().
						eq("udid", device.udid).
						and().
						eq("status", AccountMediaSubscribe.State.NOT_FINISH.status).
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

		List<AccountMediaSubscribe> notFinishR = Tab.subscribeDao.queryBuilder().where().
				eq("udid", udid).and().
				eq("status", AccountMediaSubscribe.State.NOT_FINISH.status).
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
			List<AccountMediaSubscribe> accounts = Tab.subscribeDao.queryForAll();
			for (AccountMediaSubscribe v : accounts) {

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
				manager.run(adapter);
			} while (true);
		}
	}

	// 任务启动入口

	public void run() {
		ListenableFuture<Boolean> result = this.service.submit(new Task());

		Futures.addCallback(result, new FutureCallback<Boolean>() {
			@Override
			public void onSuccess(@NullableDecl Boolean result) {
				logger.info("run success ok!");
			}

			@Override
			public void onFailure(Throwable t) {
				logger.info("run failed Not OK Please focus on this");
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
}






















