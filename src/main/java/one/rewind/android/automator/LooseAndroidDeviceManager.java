package one.rewind.android.automator;

import com.j256.ormlite.dao.Dao;
import one.rewind.android.automator.adapter.LooseWechatAdapter;
import one.rewind.android.automator.model.SubscribeAccount;
import one.rewind.android.automator.util.AndroidUtil;
import one.rewind.db.DaoManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Create By 2018/10/19
 * Description   多设备管理
 */
public class LooseAndroidDeviceManager {

    private static Dao<SubscribeAccount, String> subscribeDao;

    private LooseAndroidDeviceManager() {
    }

    public static ConcurrentHashMap<String, AndroidDevice> devices = new ConcurrentHashMap<>();

    private static LooseAndroidDeviceManager instance;

    public static final int DEFAULT_LOCAL_PROXY_PORT = 48454;

    public static LooseAndroidDeviceManager getInstance() {
        synchronized (LooseAndroidDeviceManager.class) {
            if (instance == null) {
                instance = new LooseAndroidDeviceManager();
            }
            return instance;
        }
    }

    private static List<AndroidDevice> obtainAvailableDevices() {
        synchronized (LooseAndroidDeviceManager.class) {
            List<AndroidDevice> availableDevices = new ArrayList<>();
            devices.forEach((k, v) -> {
                if (v.state.equals(AndroidDevice.State.INIT)) {
                    availableDevices.add(v);
                }
            });
            return availableDevices;
        }
    }

    //初始化设备
    static {
        try {
            subscribeDao = DaoManager.getDao(SubscribeAccount.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String[] var = AndroidUtil.obtainDevices();
        Random random = new Random();
        for (int i = 0; i < var.length; i++) {
            AndroidDevice device = new AndroidDevice(var[i], random.nextInt(50000));
            device.state = AndroidDevice.State.INIT;
            device.initApp(DEFAULT_LOCAL_PROXY_PORT + i);
            devices.put(var[i], device);
        }
    }

    /**
     * 任务队列初始化放在adapter中,更加便捷可控制
     */
    public void startManager() {
        List<AndroidDevice> androidDevices = obtainAvailableDevices();

        for (AndroidDevice device : androidDevices) {

            LooseWechatAdapter adapter =
                    new LooseWechatAdapter.
                            Builder().
                            device(device).
                            build();
            try {
                adapter.start();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("设备  " + device.udid + " :启动失败!");
            }
        }
    }

}
