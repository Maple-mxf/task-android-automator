package one.rewind.android.automator.deivce;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.adapter.Adapter;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AndroidException;
import one.rewind.android.automator.exception.TaskException;
import one.rewind.android.automator.log.SysLog;
import one.rewind.android.automator.task.Task;
import one.rewind.db.exception.DBInitException;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import one.rewind.util.NetworkUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

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

    // 所有设备的任务
    public ConcurrentHashMap<AndroidDevice, BlockingQueue<Task>> deviceTaskMap = new ConcurrentHashMap<>();

    // 控制Device执行命令的线程池
    private ThreadPoolExecutor executor;

    public ListeningExecutorService executorService;

    /**
     *
     */
    private AndroidDeviceManager() {

        executor = new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>());
        executor.setThreadFactory(new ThreadFactoryBuilder()
                .setNameFormat("ADM-%d").build());

        executorService = MoreExecutors.listeningDecorator(executor);

        // TODO 需要先执行 adb
    }

    /**
     * 应该每隔10s 执行一次
     *
     * @throws Exception
     */
    public void detectDevices() throws Exception {

        // A 先找设备
        String[] udids = getAvailableDeviceUdids();

        List<AndroidDevice> devices = new ArrayList<>();

        for (String udid : udids) {

            // A1 创建 AndroidDevice 对象
            if (deviceTaskMap.keySet().stream().map(d -> d.udid).collect(Collectors.toList()).contains(udid)) {

                // 此时假设对应Device已经序列化
                logger.info("Device {} already initialized.", udid);

            }
            // A2 找到没有识别的设备
            else {

                AndroidDevice device = AndroidDevice.getAndroidDeviceByUdid(udid);

                device.status = AndroidDevice.Status.New;

                devices.add(device);
            }
        }

        // B 加载默认的Adapters
        for (AndroidDevice ad : devices) {

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

                // 如果Adapter必须使用Account  反射创建Adapter对象
                if (needAccount) {

                    cons = clazz.getConstructor(AndroidDevice.class, Account.class);

                    Account account = Account.getAccount(ad.udid, className);

                    if (account != null) {

                        // 在new的时候就已经把自身添加到AndroidDevice中了
                        cons.newInstance(ad, account);
                    }
                    // 找不到账号，对应设备无法启动
                    else {
                        logger.error("Device:[{}] Add Failed, No available account for Adapter:[{}].", ad.name, className);
                        SysLog.log("Device [" + ad.udid + "] Add Failed, No available account for " + className);
                        ad.status = AndroidDevice.Status.Failed;
                        ad.update();
                    }
                } else {

                    cons = clazz.getConstructor(AndroidDevice.class);
                    cons.newInstance(ad);
                }
            }

            // 添加到容器中 并添加队列
            deviceTaskMap.put(ad, new LinkedBlockingQueue<>());
            logger.info("Add Device:[{}] to device container", ad.udid);

            ad.addIdleCallback(AndroidDeviceManager.this::assign);

            // 设备INIT
            try {

                logger.info("Try to start Device:[{}]", ad.udid);

                ad.start();

            } catch (AndroidException.IllegalStatusException | SQLException | DBInitException e) {
                logger.error("Unable to start Device:[{}]", ad.udid, e);
            }

            /*executor.submit(() -> {
                try {

                    logger.info("Try to start Device:[{}]", ad.udid);

                    ad.start();

                } catch (AndroidException.IllegalStatusException | SQLException | DBInitException e) {
                    logger.error("Unable to start Device:[{}]", ad.udid, e);
                }
            });*/

            // 添加 idle 回掉方法 获取执行任务

        }
    }

    /**
     * 从队列中拿任务
     *
     * @param ad
     * @throws InterruptedException
     * @throws AndroidException.IllegalStatusException
     */
    private void assign(AndroidDevice ad) throws
            InterruptedException,
            AndroidException.IllegalStatusException,
            DBInitException,
            SQLException,
            AndroidException.NoSuitableAdapter,
            AccountException.AccountNotLoad,
            TaskException.IllegalParameters,
            AndroidException.NoAvailableDevice {

        AndroidDevice device = deviceTaskMap.keySet().stream().filter(d -> d.udid.equals(ad.udid)).findFirst().orElse(null);

        if (device != null) {

            Task task = deviceTaskMap.get(device).take();

            logger.info("[{}] take Task[{}] from queue", device.udid, task.getInfo());

            // 提交任务
            ad.submit(task);
        }
    }


    /**
     * @param task
     */
    public SubmitInfo submit(Task task) throws InterruptedException, AndroidException.NoAvailableDevice, TaskException.IllegalParameters, AccountException.AccountNotLoad {

        if (task == null) return new SubmitInfo(false);

        if (task.h == null || task.h.class_name == null) throw new TaskException.IllegalParameters();

        String adapterClassName = task.h.adapter_class_name;
        if (StringUtils.isBlank(adapterClassName)) throw new TaskException.IllegalParameters();
        AndroidDevice device;

        // A 指定 account_id
        if (task.h.account_id != 0) {
            List<AndroidDevice> tmpDevices = deviceTaskMap.keySet().stream()
                    .filter(d -> {
                        Adapter adapter = d.adapters.get(adapterClassName);
                        if (adapter == null) return false;
                        if (adapter.account == null) return false;
                        return adapter.account.id == task.h.account_id;
                    })
                    .collect(Collectors.toList());
            if (tmpDevices.size() == 0) throw new AccountException.AccountNotLoad(task.h.account_id);

            device = tmpDevices.get(0);
        }
        // B 指定udid
        else if (task.h.udid != null) {

            device = deviceTaskMap.keySet().stream()
                    .filter(d -> d.udid.equals(task.h.udid))
                    .collect(Collectors.toList())
                    .get(0);

            if (device != null && !device.adapters.containsKey(adapterClassName))
                device = null;

            if (device == null) throw new AndroidException.NoAvailableDevice();
        }
        // C
        else {
            device = getDevice(adapterClassName);
        }

        logger.info("Assign [{}] --> [{}]", task.getInfo(), device.name);

        // 将当前任务放在队列的头部
        deviceTaskMap.get(device).put(task);

        return new SubmitInfo(task, device);
    }

    /**
     * 选择任务最少的Device 保证公平性 TODO  Device处于Init状态是否可以接受任务？
     *
     * @param AdapterClassName
     * @return
     */
    public AndroidDevice getDevice(String AdapterClassName) throws AndroidException.NoAvailableDevice {

        List<AndroidDevice> devices = deviceTaskMap.keySet().stream()
                .filter(d ->
                        (d.status == AndroidDevice.Status.New ||
                                d.status == AndroidDevice.Status.Init ||
                                d.status == AndroidDevice.Status.Idle ||
                                d.status == AndroidDevice.Status.Busy) &&
                                d.adapters.get(AdapterClassName) != null)
                .map(d -> new AbstractMap.SimpleEntry<>(d, deviceTaskMap.get(d).size()))
                .sorted(Map.Entry.comparingByValue())
                .limit(1)
                .map(AbstractMap.SimpleEntry::getKey)
                .collect(Collectors.toList());

        if (devices.size() == 1) {
            return devices.get(0);
        }

        throw new AndroidException.NoAvailableDevice();
    }

    /**
     *
     */
    public static class SubmitInfo implements JSONable<SubmitInfo> {

        public boolean success = true;

        String localIp = NetworkUtil.getLocalIp();
        String id;
        String task_class_name;
        int account_id;
        String topic_name;
        AndroidDevice androidDevice;

        /**
         *
         */
        public SubmitInfo() {
        }

        /**
         * @param success
         */
        public SubmitInfo(boolean success) {
            this.success = success;
        }

        /**
         * @param task
         * @param androidDevice
         */
        public SubmitInfo(Task task, AndroidDevice androidDevice) {
            this.id = task.h.id;
            this.account_id = task.h.account_id;
            this.task_class_name = task.h.class_name;
            this.androidDevice = androidDevice;
        }

        @Override
        public String toJSON() {
            return JSON.toJson(this);
        }
    }


    /**
     * 获取可用的设备 udid 列表
     *
     * @return
     */
    public static String[] getAvailableDeviceUdids() {

        /*ShellUtil.exeCmd("adb"); // 有可能需要先启动 adb 服务器

        ShellUtil.exeCmd("adb usb"); // 有可能需要刷新 adb udb 连接*/

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        try {

            Process p = Runtime.getRuntime().exec("adb devices");
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            System.out.println(sb.toString());

            logger.info(sb.toString());
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
        String r = sb.toString().replace("List of devices attached", "").replace("\t", "");

        return r.split("device");
    }
}






















