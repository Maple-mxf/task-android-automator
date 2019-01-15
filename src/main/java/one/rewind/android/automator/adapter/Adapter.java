package one.rewind.android.automator.adapter;

import one.rewind.android.automator.AndroidDevice;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class Adapter {

	static final Logger logger = LogManager.getLogger(Adapter.class.getName());

	AndroidDevice device;
	String udid;
	public AppInfo appInfo;

	public Adapter(AndroidDevice device) {
		this.device = device;
		this.udid = device.udid;
	}

	public AndroidDevice getDevice() {
		return device;
	}

	public void setDevice(AndroidDevice device) {
		this.device = device;
	}

	public void startApp() throws Exception {

		// A
		if(device == null) throw new Exception("Device is null");

		// B TODO 检验设备状态，抛出对应异常

		// C 启动App
		device.startActivity(appInfo);
	}

	/**
	 *
	 */
	public static class AppInfo implements JSONable<AppInfo> {

		public String appPackage;
		public String appActivity;

		public AppInfo(String appPackage, String appActivity) {
			this.appPackage = appPackage;
			this.appActivity = appActivity;
		}

		@Override
		public String toJSON() {
			return JSON.toJson(this);
		}
	}
}
