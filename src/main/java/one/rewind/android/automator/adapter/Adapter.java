package one.rewind.android.automator.adapter;

import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.account.AppAccount;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;


public abstract class Adapter {

	static final Logger logger = LogManager.getLogger(Adapter.class.getName());

	public static boolean NeedAccount = false;

	public AndroidDevice device;

	public AppInfo appInfo;

	// 当前使用的账号
	public AppAccount account;

	public Adapter(AndroidDevice device) {
		this.device = device;
	}

	public Adapter(AndroidDevice device, AppAccount account) {
		this.device = device;
		this.account = account;
	}

	public AndroidDevice getDevice() {
		return device;
	}

	public void setDevice(AndroidDevice device) {
		this.device = device;
	}


	// TODO 声明成abstract？  每个Adapter的具体处理方式也不一样
	public void init() {

		// A 启动Adapter   启动Adapter之后确认在APP首页
		start();

		// B 验证是否在微信首页
		boolean home = false;
		try {
			device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'微信')]")).click();
			home = true;
		} catch (Exception ignore) {
		}


		// C 需要根据具体的Appinfo来判定当前页在什么位置？

		// C1 登录页 ---> 输入账户密码  --->点击登录 ---> 当前页是否存在拖拽或者验证码之内的安全验证

		// D 如果不在首页，则一定在Adapter登录页(针对于WeChat而言 不在首页则一定在登录页)
		if (!home) {


		} else {

			// D1  如果在首页 判断是否有App更新提示

			// D2  如果存在更新提示的弹窗  则需要关掉

			// D3 验证当前账号是否跟Adapter中初始化的账号一直

			// D4 Adapter初始化完毕 ，可以正常执行任务
		}
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
		public AppType appType;

		public AppInfo(String appPackage, String appActivity, AppType appType) {
			this.appPackage = appPackage;
			this.appActivity = appActivity;
			this.appType = appType;
		}

		@Override
		public String toJSON() {
			return JSON.toJson(this);
		}
	}

	/**
	 * 账号类型
	 */
	public enum AppType {

		QQ("QQ"),
		WeiBo("WeiBo"),
		TouTiao("Toutiao"),
		Contacts("Contacts"),
		Dingding("Dingding"),
		WeChat("WeChat");

		private String appName;


		AppType(String appName) {
			this.appName = appName;
		}
	}
}
