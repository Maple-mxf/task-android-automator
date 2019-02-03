package one.rewind.android.automator.deivce.action;

import one.rewind.android.automator.deivce.AndroidDevice;
import one.rewind.android.automator.deivce.AndroidUtil;
import one.rewind.android.automator.util.ShellUtil;

/**
 * @author scisaga@gmail.com
 * @date 2019/2/4
 */
public class Reboot extends Action {

	public Reboot(AndroidDevice device) {
		super(device);
	}

	@Override
	public Boolean call() throws Exception {

		logger.info("[{}] rebooting...", udid);

		// TODO 这么写肯定有问题 必须拿到对应的InputStream
		AndroidUtil.enterADBShell(udid);
		ShellUtil.exeCmd("reboot");

		device.status = AndroidDevice.Status.DeviceBooting;

		Thread.sleep(120000);

		// enterADBShell(udid);

		// 尝试重新连接  重新连接 TODO  adb usb命令会影响其他USB连接？或者导致其他正在运行的手机出现丢失session的情况
		ShellUtil.exeCmd("adb usb");

		// 摁电源键 adb shell input keyevent 26

		// 滑动解锁 TODO 如果不设定密码 就可以不用解锁了 默认到Home界面
		ShellUtil.exeCmd("adb shell input swipe 300 1000 300 500");

		// 输入pin密码 adb shell input text 1234
		ShellUtil.exeCmd("adb shell input text " + AndroidDevice.PIN_PASSWORD);

		// 点击OK键
		device.touch(1114, 2114, 3000);

		logger.info("[{}] rebooted", udid);

		device.flags.add(AndroidDevice.Flag.NewReboot);

		return true;
	}
}
