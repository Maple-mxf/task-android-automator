package one.rewind.android.automator.manager;

import com.google.common.collect.Lists;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.adapter.LooseWechatAdapter2;
import one.rewind.android.automator.util.AndroidUtil;

import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Create By 2018/11/20
 * Description:
 */
public class Manager {

    /**
     * 存储无任务设备信息 利用建监听者模式实现设备管理
     */
    public BlockingQueue<LooseWechatAdapter2> idleAdapters = new LinkedBlockingQueue<>(Integer.MAX_VALUE);

    /**
     * 所有设备的信息
     */
    private List<AndroidDevice> devices = Lists.newArrayList();

    /**
     * 单例
     */
    private static Manager manager;


    private Manager() {
    }

    public static Manager getInstance() {
        if (manager == null) {
            manager = new Manager();
        }
        return manager;
    }

    /**
     * 初始化设备
     */
    private void init() {
        String[] var = AndroidUtil.obtainDevices();
        Random random = new Random();
        for (String aVar : var) {
            AndroidDevice device = new AndroidDevice(aVar, random.nextInt(50000));
            devices.add(device);
            LooseWechatAdapter2 adapter = new LooseWechatAdapter2(device);
            idleAdapters.add(adapter);
        }
    }

    /**
     * 异步启动设备
     */
    public void startManager() {
        init();
        for (AndroidDevice device : devices) {
            device.startAsync();
            idleAdapters.add(new LooseWechatAdapter2(device));
        }

    }

    public static void main(String[] args) {

        Manager manager = getInstance();
        manager.init();
        manager.startManager();

    }


    public void addIdleAdapter(LooseWechatAdapter2 adapter) {
        idleAdapters.add(adapter);
    }
}
