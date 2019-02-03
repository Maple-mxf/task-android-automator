package one.rewind.android.automator.deivce;

import one.rewind.android.automator.adapter.Adapter;
import one.rewind.android.automator.util.ShellUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se.vidstige.jadb.JadbConnection;
import se.vidstige.jadb.JadbDevice;
import se.vidstige.jadb.JadbException;
import se.vidstige.jadb.RemoteFile;
import se.vidstige.jadb.managers.PackageManager;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author scisaga@gmail.com
 * @date 2019/2/4
 */
public class AndroidUtil {

	private static final Logger logger = LogManager.getLogger(AndroidUtil.class.getName());

	/**
	 * 安装CA证书，否则无法解析https数据
	 *
	 * @param udid
	 * @throws IOException
	 * @throws JadbException
	 * @throws InterruptedException
	 */
	public static void installCA(String udid) throws IOException, JadbException {

		// 需要调用process 启动adb daemon, 否则第一次执行会出错

		Optional.ofNullable(getJadbDevice(udid)).ifPresent(d -> {
			try {

				// TODO 使用 adb 判断远端文件是否存在
				// ls /path/to/your/files* 1> /dev/null 2>&1;
				d.push(new File("ca.crt"), new RemoteFile("/sdcard/_certs/ca.crt"));
				Thread.sleep(2000);

			} catch (IOException | JadbException | InterruptedException e) {
				logger.error("[{}] Error setup wifi proxy", udid);
			}
		});
	}

	/**
	 * 设置设备Wifi代理
	 * <p>
	 * 设备需要连接WIFI，设备与本机器在同一网段
	 *
	 * @param udid
	 * @param local_ip
	 * @param proxyPort
	 * @throws IOException
	 * @throws JadbException
	 */
	public static void setupRemoteWifiProxy(String udid, String local_ip, int proxyPort) throws IOException, JadbException {

		Optional.ofNullable(getJadbDevice(udid)).ifPresent(d -> {

			try {
				execShell(d, "settings", "put", "global", "http_proxy", local_ip + ":" + proxyPort);
				execShell(d, "settings", "put", "global", "https_proxy", local_ip + ":" + proxyPort);
				// d.push(new File("ca.crt"), new RemoteFile("/sdcard/_certs/ca.crt"));
				Thread.sleep(2000);

			} catch (IOException | JadbException | InterruptedException e) {
				logger.error("[{}] Error setup wifi proxy", udid);
			}
		});
	}

	/**
	 * 移除Wifi Proxy
	 *
	 * @throws IOException
	 * @throws JadbException
	 * @throws InterruptedException
	 */
	public static void removeRemoteWifiProxy(String udid) throws IOException, JadbException {

		Optional.ofNullable(getJadbDevice(udid)).ifPresent(d -> {
			try {
				execShell(d, "settings", "delete", "global", "http_proxy");
				execShell(d, "settings", "delete", "global", "https_proxy");
				execShell(d, "settings", "delete", "global", "global_http_proxy_host");
				execShell(d, "settings", "delete", "global", "global_http_proxy_port");

				Thread.sleep(2000);

			} catch (IOException | JadbException | InterruptedException e) {
				logger.error("[{}] Error remove wifi proxy", udid);
			}
		});
	}

	/**
	 * 安装APK包
	 *
	 * @param apkPath 本地apk路径
	 */
	public static void installApk(String udid, String apkPath) throws IOException, JadbException {

		Optional.ofNullable(getJadbDevice(udid)).ifPresent(d -> {
			try {
				new PackageManager(d).install(new File(apkPath));
				Thread.sleep(2000);
			} catch (IOException | JadbException | InterruptedException e) {
				logger.error("[{}] Error install apk from {}", udid, apkPath);
			}
		});
	}

	/**
	 * 远程安装APK包
	 *
	 * @param apkPath 设备apk路径
	 */
	public static void installApkRemote(String udid, String apkPath) {
		String commandStr = "adb -s " + udid + " install " + apkPath;
		ShellUtil.exeCmd(commandStr);
	}

	/**
	 * 返回已经安装的应用程序
	 * TODO 应该返回列表
	 */
	public static void listApps(String udid) {

		String commandStr = "adb -s " + udid + " shell pm accounts packages -3";
		//-3为第三方应用 [-f] [-d] [-e] [-s] [-3] [-i] [-u]
		ShellUtil.exeCmd(commandStr);
	}

	/**
	 * 进入相应设备的shell
	 *
	 * @param udid
	 * @throws IOException
	 */
	public static void enterADBShell(String udid) throws IOException {
		String command = "adb -s " + udid + " shell";
		ShellUtil.exeCmd(command);
	}

	/**
	 * 摁电源键
	 *
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void clickPower(String udid) throws IOException, InterruptedException {
		String powerCommand = "adb -s " + udid + " shell input keyevent 26";
		ShellUtil.exeCmd(powerCommand);
		Thread.sleep(2000);
	}


	/**
	 * 杀死进程
	 *
	 * @param packageName
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void shutdownProcess(String udid, String packageName) {
		String command = "adb -s " + udid + " shell am force-stop " + packageName;
		ShellUtil.exeCmd(command);
	}

	/**
	 * 以app的包名为参数，卸载选择的应用程序
	 *
	 * @param appPackage 卸载选择的app com.ss.android.ugc.aweme
	 */
	public static void uninstallApp(String udid, String appPackage) {
		String commandStr = "adb -s " + udid + " uninstall " + appPackage;
		ShellUtil.exeCmd(commandStr);
	}

	/**
	 * 打开应用
	 *
	 * @param appPackage  包名
	 * @param appActivity 主窗体名
	 */
	public static void startApp(String udid, String appPackage, String appActivity) {
		String commandStr = "adb -s " + udid + " shell am start " + appPackage + "/" + appActivity;
		ShellUtil.exeCmd(commandStr);
	}

	/**
	 * 打开应用
	 *
	 * @param appInfo 应用信息
	 */
	public static void startApp(String udid, Adapter.AppInfo appInfo) {
		String commandStr = "adb -s " + udid + " shell am start " + appInfo.appPackage + "/" + appInfo.appActivity;
		ShellUtil.exeCmd(commandStr);
	}

	/**
	 * @param appInfo
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void stopApp(String udid, Adapter.AppInfo appInfo) {
		shutdownProcess(udid, appInfo.appPackage);
	}

	/**
	 * 清空缓存日志
	 */
	public static void clearCacheLog(String udid) throws IOException {
		String command = "adb -s " + udid + " logcat -c -b events";
		ShellUtil.exeCmd(command);
		logger.info("[{}] clean cache log events", udid);
	}

	/**
	 * 清空所有日志
	 */
	public static void clearAllLog(String udid) {
		try {
			String command = "adb -s " + udid + " logcat -c -b main -b events -b radio -b system";
			ShellUtil.exeCmd(command);
			logger.info("[{}] clean all log events", udid);
		} catch (Exception e) {
			logger.error(e);
		}
	}

	/**
	 * 查看所有的第三方应用进程  adb command: adb shell pm list packages -3
	 * package:io.appium.settings
	 * package:com.mgyapp.android
	 * package:io.appium.uiautomator2.server
	 * package:com.tencent.mm
	 * package:io.appium.uiautomator2.server.test
	 * package:com.mgyun.shua.protector
	 * package:io.appium.unlock
	 * <p>
	 * 返回桌面 am start -a android.intent.action.MAIN -c android.intent.category.HOME
	 * 清理app进程 am kill <package_name>
	 */
	public static void killAllBackgroundProcess(String udid) throws IOException {

		// 查看所有的第三方应用
		String packageList = ShellUtil.exeCmd("adb shell pm list packages -3");

		String[] ps = packageList.split("package:");

		List<String> packages = new ArrayList<>();
		for (String var : ps) {
			String var0 = var.replaceAll("\n", "");
			if (StringUtils.isNotBlank(var0)) {
				packages.add(var0);
			}
		}

		// 排除appium的进程
		// [io.appium.uiautomator2.server]
		// [io.appium.uiautomator2.server.test]
		// [io.appium.settings]
		// TODO
		packages.forEach(p -> {
			if (!p.equals("io.appium.uiautomator2.server") && !p.equals("io.appium.uiautomator2.server.test") && !p.equals("io.appium.settings")) {
				shutdownProcess(udid, p);
			}
		});
	}

	/**
	 *
	 * @param udid
	 * @return
	 * @throws IOException
	 * @throws JadbException
	 */
	private static JadbDevice getJadbDevice(String udid) throws IOException, JadbException {

		JadbConnection jadb = new JadbConnection();

		List<JadbDevice> devices = jadb.getDevices();

		for (JadbDevice d : devices) {

			if (d.getSerial().equals(udid)) {
				return d;
			}
		}

		return null;
	}

	/**
	 *
	 * @param udid
	 * @param command
	 * @param args
	 * @throws IOException
	 * @throws JadbException
	 */
	public static void execShell(String udid, String command, String... args) throws IOException, JadbException {

		Optional<JadbDevice> oj = Optional.ofNullable(getJadbDevice(udid));

		if(oj.isPresent()) {
			execShell(oj.get(), command, args);
		}
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
	private static void execShell(JadbDevice d, String command, String... args) throws IOException, JadbException {

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
}