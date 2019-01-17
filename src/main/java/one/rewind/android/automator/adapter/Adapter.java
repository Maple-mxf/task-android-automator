package one.rewind.android.automator.adapter;

import one.rewind.android.automator.AndroidDevice;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public abstract class Adapter {

	static final Logger logger = LogManager.getLogger(Adapter.class.getName());

	public AndroidDevice device;

	public AppInfo appInfo;

	public Adapter(AndroidDevice device) {
		this.device = device;
	}

	public AndroidDevice getDevice() {
		return device;
	}

	public void setDevice(AndroidDevice device) {
		this.device = device;
	}

	/**
	 * 启动应用
	 */
	public void start() {
		device.startApp(appInfo);
	}

	/**
	 *
	 */
	public void restart() {
		device.stopApp(appInfo);
		device.startApp(appInfo);
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
