package one.rewind.android.automator.deivce.action;

import one.rewind.android.automator.deivce.AndroidDevice;

import java.io.IOException;

/**
 * @author scisaga@gmail.com
 * @date 2019/2/4
 */
public class Stop extends Action {

	public Stop(AndroidDevice device) {
		super(device);
	}

	@Override
	public Boolean call() throws IOException {

		logger.info("[{}] stopping...", udid);

		// 停止 driver
		if (device.driver != null) device.driver.close();

		// 停止 Appium service运行
		if (device.service != null && device.service.isRunning()) device.service.stop();

		// 停止设备端的 appium
		device.stopRemoteAppiumServer();

		// 停止代理服务器
		device.stopProxy();

		logger.info("[{}] stopped", udid);

		return true;
	}
}