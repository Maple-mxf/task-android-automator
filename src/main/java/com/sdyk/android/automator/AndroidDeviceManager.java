package com.sdyk.android.automator;

import org.aspectj.weaver.ast.And;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 多设备管理
 * TODO 尚未完成
 */
public class AndroidDeviceManager {

	private static AndroidDeviceManager instance;

	public static int DefaultAppiumPort = 47100;
	public static int DefaultLocalProxyPort = 48100;

	public static void getInstance() {
		synchronized (AndroidDeviceManager.class) {
			if(instance == null) {
				instance = new AndroidDeviceManager();
			}
		}
	}

	public Map<String, AndroidDevice> devices = new HashMap<>();

	/**
	 *
	 */
	private AndroidDeviceManager() {

	}

	/**
	 *
	 * @param udid
	 * @throws Exception
	 */
	public synchronized void initDevice(String udid, AndroidDevice.Flag... flag) throws Exception {

		int appiumPort = DefaultAppiumPort + devices.keySet().size();
		AndroidDevice device = new AndroidDevice(udid, appiumPort);

		if(Arrays.asList(flag).contains(AndroidDevice.Flag.Proxy)) {
			int localProxyPort = DefaultLocalProxyPort + devices.keySet().size();
			device.startProxy(localProxyPort);
		}

		devices.put(udid, device);
	}

	/**
	 *
	 * @param udid
	 * @return
	 */
	public synchronized AndroidDevice getDevice(String udid) throws Exception {

		AndroidDevice device = devices.get(udid);
		if(device == null) {
			throw new Exception("Android device:[" + udid + "] not exists.");
		}

		return device;
	}

	/**
	 *
	 * @param udid
	 */
	public synchronized void stopDevice(String udid) {
		AndroidDevice device = devices.get(udid);
		if(device != null) {
			device.stop();
			devices.remove(udid);
		}
	}
}
