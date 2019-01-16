package one.rewind.android.automator.util;

import io.appium.java_client.android.AndroidDriver;
import one.rewind.android.automator.adapter.AbstractWeChatAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 */
public class ShellUtil {

	public static void exeCall(String fileLac) {
		Runtime rt = Runtime.getRuntime();
		Process p = null;
		try {
			p = rt.exec(fileLac);
		} catch (Exception e) {
			System.out.println("open failure");
		}
	}

	public static void exeCmd(String commandStr) {

		BufferedReader br = null;
		try {
			Process p = Runtime.getRuntime().exec(commandStr);
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			StringBuilder sb = new StringBuilder();
			while ((line = br.readLine()) != null) {
				sb.append(line).append("\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 进入相应设备的shell
	 *
	 * @param udid
	 * @throws IOException
	 */
	private static void enterADBShell(String udid) throws IOException {
		String command = "abd -s " + udid + " shell";
		exeCmd(command);
	}


	// reboot
	@Deprecated
	public static void reboot(String udid) throws IOException, InterruptedException {

		enterADBShell(udid);

		exeCmd("reboot");

		Thread.sleep(120000);

		enterADBShell(udid);
		//滑动解锁
		exeCmd("adb shell input swipe 300 1000 300 500");
	}

	/**
	 * 杀死进程
	 *
	 * @param udid
	 * @param packageName
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void shutdownProcess(String udid, String packageName) throws IOException, InterruptedException {
		String command = "adb -s " + udid + " shell am force-stop " + packageName;
		Runtime.getRuntime().exec(command);
		Thread.sleep(5000);
	}

	/**
	 * 摁电源键
	 *
	 * @param udid
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void clickPower(String udid) throws IOException, InterruptedException {
		String powerCommand = "adb -s " + udid + " shell input keyevent 26";
		Runtime runtime = Runtime.getRuntime();
		runtime.exec(powerCommand);
		Thread.sleep(2000);
	}

	/**
	 * 唤醒设备
	 *
	 * @param udid
	 * @param driver
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void notifyDevice(String udid, AndroidDriver driver) throws IOException, InterruptedException {
		Runtime runtime = Runtime.getRuntime();
		clickPower(udid);
		Thread.sleep(2000);
		//滑动解锁  728 2356  728 228  adb shell -s  input swipe  300 1500 300 200
		String unlockCommand = "adb -s " + udid + " shell input swipe  300 1500 300 200";
		runtime.exec(unlockCommand);
		Thread.sleep(2000);
		//输入密码adb -s ZX1G42BX4R shell input text szqj  adb -s ZX1G42BX4R shell input swipe 300 1000 300 500
		String loginCommand = "adb -s " + udid + " shell input text szqj";
		runtime.exec(loginCommand);
		Thread.sleep(4000);
		//点击确认
		AbstractWeChatAdapter.clickPoint(1350, 2250, 6000, driver); //TODO 时间适当调整
		Thread.sleep(2000);
	}
}
