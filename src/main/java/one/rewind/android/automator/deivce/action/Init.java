package one.rewind.android.automator.deivce.action;

import one.rewind.android.automator.adapter.Adapter;
import one.rewind.android.automator.callback.AndroidDeviceCallBack;
import one.rewind.android.automator.deivce.AndroidDevice;
import one.rewind.android.automator.deivce.AndroidUtil;
import one.rewind.android.automator.util.Tab;
import one.rewind.db.exception.DBInitException;
import se.vidstige.jadb.JadbException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

/**
 * @author scisaga@gmail.com
 * @date 2019/2/4
 */
public class Init extends Action {

	public Init(AndroidDevice device) {
		super(device);
	}

	public Boolean call() throws InterruptedException, IOException, JadbException, DBInitException, SQLException {

		logger.info("[{}] Init...", udid);

		device.appiumPort = Tab.appiumPort.getAndIncrement();
		device.proxyPort = Tab.proxyPort.getAndIncrement();
		device.localProxyPort = Tab.localProxyPort.getAndIncrement();

		// 安装CA
		// installCA();

		// 启动代理
		logger.info("[{}] start proxy...", udid);
		device.startProxy();

		// 设置设备Wifi代理
		logger.info("[{}] setup wifi proxy...", udid);
		AndroidUtil.setupRemoteWifiProxy(udid, device.local_ip, device.proxyPort);

		// 启动相关服务
		logger.info("[{}] start appium local service / driver ...", udid);
		device.initAppiumServiceAndDriver(new Adapter.AppInfo("com.tencent.mm", ".ui.LauncherUI"));

		//
		for(AndroidDeviceCallBack.InitCallBack callBack : device.initCallbacks) {
			callBack.call(device);
		}

		device.init_time = new Date();

		logger.info("[{}] init success", udid);

		return true;
	}
}