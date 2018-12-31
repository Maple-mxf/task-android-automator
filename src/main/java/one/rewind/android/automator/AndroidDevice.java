package one.rewind.android.automator;

import com.google.common.util.concurrent.AbstractService;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.AutomationName;
import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.filters.RequestFilter;
import net.lightbody.bmp.filters.RequestFilterAdapter;
import net.lightbody.bmp.filters.ResponseFilter;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import net.lightbody.bmp.mitm.PemFileCertificateSource;
import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;
import one.rewind.android.automator.util.AppInfo;
import one.rewind.android.automator.util.ShellUtil;
import one.rewind.android.automator.util.Tab;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import one.rewind.util.NetworkUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.remote.DesiredCapabilities;
import se.vidstige.jadb.JadbConnection;
import se.vidstige.jadb.JadbDevice;
import se.vidstige.jadb.JadbException;
import se.vidstige.jadb.managers.PackageManager;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Android 设备管理
 * <p>
 * 1. 通讯建立方式:
 * 第一步 本地启动 AppiumDriverLocalService 通过adb连接在目标Android设备上启动 AppiumServer
 * AppiumDriverLocalService --> (ADB) --> AppiumServer
 * 第二步 AppiumDriverLocalService 维护和目标Android设备的通讯, 并暴露本地服务地址
 * 第三部 通过AppiumDriverLocalService本地服务地址, 初始化AndroidDevice, 实现对设备的自动化操作
 * AndroidDriver --> AppiumDriverLocalService(HTTP) --> (ADB/HTTP Wired JSON) --> AppiumServer
 */
public class AndroidDevice extends AbstractService {

	private static final Logger logger = LogManager.getLogger(AndroidDevice.class.getName());

	public static class Task implements JSONable<Task> {

		public enum Type {
			Subscribe,
			Fetch
		}

		@Override
		public String toJSON() {
			return JSON.toJson(this);
		}
	}

	public enum Flag {
		// 单个设备的操作过多
		Frequent_Operation(1),
		// 单个设备当日订阅到达上限
		Upper_Limit(2);


		int state;

		Flag(int state) {
			this.state = state;
		}
	}

	public Flag flag;


	// 任务队列
	public Queue<String> queue = new ConcurrentLinkedQueue<>();

	//
	private boolean clickEffect;

	// 任务类型
	public Task.Type taskType = Task.Type.Subscribe;

	public boolean isClickEffect() {
		return clickEffect;
	}

	public void setClickEffect(boolean clickEffect) {
		this.clickEffect = clickEffect;
	}

	// 本地IP
	private static String LOCAL_IP;

	// 配置设定
	static {

		LOCAL_IP = NetworkUtil.getLocalIp();
		logger.info("Local IP: {}", LOCAL_IP);
	}

	// 设备 udid
	public String udid;

	// 本地代理服务器
	private BrowserMobProxy bmProxy;

	// Appium相关服务对象
	AppiumDriverLocalService service;


	public AndroidDriver driver; // 本地Driver

	// TODO  移动端代理端口
	private int proxyPort;

	// 本地 appium 服务端口
	private int appiumPort;

	// TODO 代码运行端代理端口
	public int localProxyPort;

	// TODO
	private URL serviceUrl;

	// 设备屏幕高度
	public int height;

	// 设备屏幕宽度
	public int width;


	/**
	 * 构造方法
	 *
	 * @param udid 设备udid
	 * @throws Exception
	 */
	public AndroidDevice(String udid) {

		this.udid = udid;
		this.appiumPort = Tab.appiumPort.getAndIncrement();
		this.proxyPort = Tab.proxyPort.getAndIncrement();
		this.localProxyPort = Tab.localProxyPort.getAndIncrement();
	}

	/**
	 * 获得设备的宽度
	 */
	public int getWidth() {
		return driver.manage().window().getSize().width;
	}

	/**
	 * 获得设备的高度
	 */
	public int getHeight() {
		return driver.manage().window().getSize().height;
	}

	/**
	 * 启动MITM代理服务
	 * <p>
	 * https://github.com/lightbody/browsermob-proxy
	 *
	 * @param port
	 */
	public void startProxy(int port) {

		// A 加载证书
		// 证书生成参考 openssl相关命令
        /*CertificateAndKeySource source = new PemFileCertificateSource(
                new File("ca.crt"), new File("pk.crt"), "sdyk");*/

		CertificateAndKeySource source = new PemFileCertificateSource(
				new File("/usr/local/ca.crt"), new File("/usr/local/pk.crt"), "sdyk");

		// B 让 MitmManager 使用刚生成的 root certificate
		ImpersonatingMitmManager mitmManager = ImpersonatingMitmManager.builder()
				.rootCertificateSource(source)
				.build();

		// C 初始化 bmProxy
		bmProxy = new BrowserMobProxyServer();
		bmProxy.setTrustAllServers(true);
		bmProxy.setMitmManager(mitmManager);
		bmProxy.start(port);
		proxyPort = bmProxy.getPort();

		logger.info("Proxy started @proxyPort {}", proxyPort);
	}

	/**
	 * 设置代理请求过滤器
	 *
	 * @param filter
	 */
	public void setProxyRequestFilter(RequestFilter filter) {
		if (bmProxy == null) return;
		bmProxy.addFirstHttpFilterFactory(new RequestFilterAdapter.FilterSource(filter, 16777216));
	}

	/**
	 * 设置代理返回过滤器
	 *
	 * @param filter
	 */
	public void setProxyResponseFilter(ResponseFilter filter) {
		if (bmProxy == null) return;
		bmProxy.addResponseFilter(filter);
	}

	/**
	 * 停止代理
	 */
	public void stopProxy() {
		Optional.ofNullable(bmProxy).ifPresent(t -> t.stop());
	}

	/**
	 * 设置设备Wifi代理
	 * <p>
	 * 设备需要连接WIFI，设备与本机器在同一网段
	 */
	public void setupWifiProxy() {

		try {

			JadbConnection jadb = new JadbConnection();

			// TODO
			// 需要调用process 启动adb daemon, 否则第一次执行会出错

			List<JadbDevice> devices = jadb.getDevices();

			for (JadbDevice d : devices) {

				if (d.getSerial().equals(udid)) {

					execShell(d, "settings", "put", "global", "http_proxy", LOCAL_IP + ":" + proxyPort);
					execShell(d, "settings", "put", "global", "https_proxy", LOCAL_IP + ":" + proxyPort);
					// d.push(new File("ca.crt"), new RemoteFile("/sdcard/_certs/ca.crt"));
					Thread.sleep(2000);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 移除Wifi Proxy
	 */
	public void removeWifiProxy() {

		try {

			JadbConnection jadb = new JadbConnection();

			List<JadbDevice> devices = jadb.getDevices();

			for (JadbDevice d : devices) {

				if (d.getSerial().equals(udid)) {

					execShell(d, "settings", "delete", "global", "http_proxy");
					execShell(d, "settings", "delete", "global", "https_proxy");
					execShell(d, "settings", "delete", "global", "global_http_proxy_host");
					execShell(d, "settings", "delete", "global", "global_http_proxy_port");

					Thread.sleep(2000);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 安装APK包
	 *
	 * @param udid
	 * @param fileName
	 */
	public void installApk(String udid, String fileName) {

		try {

			JadbConnection jadb = new JadbConnection();

			List<JadbDevice> devices = jadb.getDevices();

			for (JadbDevice d : devices) {

				if (d.getSerial().equals(udid)) {

					new PackageManager(d).install(new File(fileName));
					Thread.sleep(2000);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 远程安装APK包
	 *
	 * @param apkPath
	 */
	public void installApk(String apkPath) {
		String commandStr = "adb -s " + udid + " install " + apkPath;
		ShellUtil.exeCmd(commandStr);
	}

	/**
	 * 执行远程设备shell命令
	 *
	 * @param d
	 * @param command
	 * @param args
	 * @throws IOException
	 * @throws JadbException
	 */
	public static void execShell(JadbDevice d, String command, String... args) throws IOException, JadbException {

		InputStream is = d.executeShell(command, args);

		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder builder = new StringBuilder();

		String line;
		try {

			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append("\n"); //appende a new line
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		logger.info(builder.toString());
	}

	/**
	 * 初始化AppiumDriver
	 *
	 * @throws Exception
	 */
	public void initAppiumServiceAndDriver(AppInfo info) throws MalformedURLException, InterruptedException {

		// A 定义Service Capabilities
		DesiredCapabilities serviceCapabilities = new DesiredCapabilities();

		serviceCapabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android");
		serviceCapabilities.setCapability(MobileCapabilityType.UDID, udid); // udid是设备的唯一标识
		serviceCapabilities.setCapability(MobileCapabilityType.NEW_COMMAND_TIMEOUT, 3600);

		// B 定义AppiumService
		service = new AppiumServiceBuilder()
				.withCapabilities(serviceCapabilities)
				.usingPort(appiumPort)
				.withArgument(GeneralServerFlag.LOG_LEVEL, "info")
				.withAppiumJS(new File("/usr/local/lib/node_modules/appium/build/lib/main.js")) // TimeBomb!!!
				/*.withArgument(GeneralServerFlag.SESSION_OVERRIDE, "true")*/ // TODO
				.build();

		service.start();

		Thread.sleep(5000);

		serviceUrl = service.getUrl();

		logger.info("Appium Service URL: {}", serviceUrl);

		// C 定义Driver Capabilities
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability("app", "");
		capabilities.setCapability("appPackage", info.appPackage); // App包名
		capabilities.setCapability("appActivity", info.appActivity); // App启动Activity
		capabilities.setCapability("fastReset", false);
		capabilities.setCapability("fullReset", false);
		capabilities.setCapability("noReset", true);

		capabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, AutomationName.ANDROID_UIAUTOMATOR2);

		// TODO 下面两行代码如果不添加 是否不能进入小程序？
        /*String webViewAndroidProcessName = "com.tencent.mm:tools";
        webViewAndroidProcessName = "com.tencent.mm:appbrand0"; // App中的加载WebView的进程名
        capabilities.setCapability("chromeOptions", ImmutableMap.of("androidProcess", webViewAndroidProcessName));*/

		capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, udid);

		driver = new AndroidDriver(new URL("http://127.0.0.1:" + appiumPort + "/wd/hub"), capabilities);

		Thread.sleep(15000);

		// 设置宽高
		this.width = getWidth();
		this.height = getHeight();
	}

	/**
	 * 返回已经安装的应用程序
	 * TODO 应该返回列表
	 */
	public void listApps() {

		String commandStr = "adb -s " + udid + " shell pm accounts packages -3";
		//-3为第三方应用 [-f] [-d] [-e] [-s] [-3] [-i] [-u]
		ShellUtil.exeCmd(commandStr);
	}

	/**
	 * 以app的包名为参数，卸载选择的应用程序
	 *
	 * @param appPackage 卸载选择的app com.ss.android.ugc.aweme
	 */
	public void uninstallApp(String appPackage) {
		String commandStr = "adb -s " + udid + " uninstall " + appPackage;
		ShellUtil.exeCmd(commandStr);
	}

	/**
	 * 打开应用
	 *
	 * @param appPackage  包名
	 * @param appActivity 主窗体名
	 */
	public void startActivity(String appPackage, String appActivity) {
		String commandStr = "adb -s " + udid + " shell am start " + appPackage + "/" + appActivity;
		ShellUtil.exeCmd(commandStr);
	}

	/**
	 * 打开应用
	 *
	 * @param appInfo 应用信息
	 */
	public void startActivity(AppInfo appInfo) {
		String commandStr = "adb -s " + udid + " shell am start " + appInfo.appPackage + "/" + appInfo.appActivity;
		ShellUtil.exeCmd(commandStr);
	}


	@Override
	protected void doStart() {
		try {
			AppInfo appInfo = AppInfo.get(AppInfo.Defaults.WeChat);
			this.initAppiumServiceAndDriver(appInfo);
			Thread.sleep(3000);

		} catch (MalformedURLException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void start() {
		this.doStart();
	}

	@Override
	protected void doStop() {

		// 停止service运行
		if (service.isRunning()) service.stop();

		// driver not close
	}


	//  清空缓存日志
	public void clearCacheLog() {
		try {
			String command = "adb -s " + this.udid + " logcat -c -b events";
			Runtime.getRuntime().exec(command);
			logger.info("清空设备 {} 的缓存日志");
		} catch (Exception ignore) {
			logger.error(ignore);
		}
	}

	// 清空所有日志
	public void clearAllLog() {
		try {
			String command = "adb -s " + this.udid + " logcat -c -b main -b events -b radio -b system";
			Runtime.getRuntime().exec(command);
			logger.info("清空设备 {} 的全部系统级别日志");
		} catch (Exception e) {
			logger.error(e);
		}
	}

	/**
	 * 重启appium
	 * <p>
	 * 场景: 设备运行时间过程, 没有响应
	 * <p>
	 * TODO 尚未实现
	 */
	public void restart() throws IOException {
		// 停止client端
		doStop();
		// 停止server端
		stopAppiumServer();

		// initApp();
	}

	/**
	 * 重启移动端appium
	 *
	 * @throws IOException
	 */
	public void stopAppiumServer() throws IOException {
		String command1 = "adb -s " + this.udid + " shell am force-stop io.appium.settings";
		Runtime.getRuntime().exec(command1);

		String command2 = "adb -s " + this.udid + " shell am force-stop io.appium.uiautomator2.server";
		Runtime.getRuntime().exec(command2);
		logger.info("Restart {} Appium Server", udid);
	}
}
