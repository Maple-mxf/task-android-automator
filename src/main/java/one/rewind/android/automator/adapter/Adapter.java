package one.rewind.android.automator.adapter;

import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.android.automator.exception.AndroidException;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public abstract class Adapter {

	public static final Logger logger = LogManager.getLogger(Adapter.class.getName());

	public static boolean NeedAccount = false;

	public AndroidDevice device;

	public AppInfo appInfo;

	// 当前使用的账号
	public Account account;

	public Adapter(AndroidDevice device) throws AndroidException.IllegalStatusException {
		this.device = device;
		device.addAdapter(this);
	}

	public Adapter(AndroidDevice device, Account account) throws AndroidException.IllegalStatusException {
		this.device = device;
		this.account = account;
		device.addAdapter(this);
	}

	public AndroidDevice getDevice() {
		return device;
	}

	public void setDevice(AndroidDevice device) {
		this.device = device;
	}

	/**
	 *
	 */
	public abstract void init() throws InterruptedException, AdapterException.OperationException, AccountException.NoAvailableAccount;

	/**
	 * 启动应用
	 */
	public void start() throws InterruptedException, AdapterException.OperationException, AccountException.NoAvailableAccount {
		device.startApp(appInfo);
	}

	/**
	 *
	 */
	public void restart() throws InterruptedException, AdapterException.OperationException, AccountException.NoAvailableAccount {
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
