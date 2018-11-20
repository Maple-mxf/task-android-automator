package one.rewind.android.automator.test.guava.device;

import com.google.common.util.concurrent.AbstractService;

/**
 * Create By 2018/11/20
 * Description:
 */
public class IAndroidDevice extends AbstractService {

    @Override
    protected void doStart() {
        //启动设备
        //开启任务    计算任务    任务分配   任务
        //query
    }

    @Override
    protected void doStop() {
    }

    public static void main(String[] args) {
        IAndroidDevice device = new IAndroidDevice();
        device.startAsync();
        System.out.println(device.state());
        device.stopAsync();
        System.out.println(device.state());
        device.notifyStarted();
    }




}
