package one.rewind.android.automator.util;

import io.appium.java_client.android.AndroidDriver;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.adapter.AbstractWeChatAdapter;
import one.rewind.android.automator.adapter.BaiDuOCRAdapter;
import one.rewind.android.automator.model.SubscribeMedia;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.UUID;

/**
 * @author maxuefeng[m17793873123@163.com]
 */
@SuppressWarnings("JavaDoc")
public class AndroidUtil {


	// 重启手机APP
	public static void restartWechat(AndroidDevice device) throws InterruptedException {
		AndroidUtil.clearMemory(device.udid);
		AndroidUtil.activeWechat(device);
	}


	/**
	 * 获取当前可用设备的udid
	 * <p>
	 * 排除offline状态的设备
	 *
	 * @return
	 */
	public static String[] obtainDevices() {

		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();

		try {

			Process p = Runtime.getRuntime().exec("adb devices");
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
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

		String r = sb.toString().replace("List of devices attached", "").replace("\t", "");

		return r.split("device");
	}

	/**
	 * @param driver
	 * @throws InterruptedException
	 */
	public static void closeEssay(AndroidDriver driver) throws InterruptedException {

		driver.navigate().back();
		Thread.sleep(1000);
	}

	/**
	 * 关闭App可能会存在一些无法预料的问题   比如手机出来一层透明层  此时closeApp方法调用可能会不起作用
	 *
	 * @param device
	 * @throws Exception
	 */
	public static void closeApp(AndroidDevice device) {
		try {
			//截图
			String filePrefix = UUID.randomUUID().toString();
			String fileName = filePrefix + ".png";
			String path = System.getProperty("user.dir") + "/screen/";
			AbstractWeChatAdapter.screenshot(fileName, path, device.driver);
			JSONObject jsonObject = BaiDuOCRAdapter.imageOcr(path + fileName,false);
			JSONArray array = (JSONArray) jsonObject.get("words_result");
			for (Object o : array) {

				JSONObject v = (JSONObject) o;

				String words = v.getString("words");
				if (words.contains("微信没有响应") || words.contains("关闭应用")) {
					AbstractWeChatAdapter.clickPoint(517, 1258, 1000, device.driver);
					break;
				}
				if (words.contains("要将其关闭吗") && words.contains("微信无响应")) {
					//点击确定  这个截图和上面的截图是有点不太一样的
					AbstractWeChatAdapter.clickPoint(1196, 1324, 1000, device.driver);
					break;
				}
				if (words.contains("系统繁忙") && words.contains("请稍后再试")) {
					AbstractWeChatAdapter.clickPoint(1110, 1342, 5000, device.driver);
					break;
				}
			}
			clearMemory(device.udid);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 激活应用
	 *
	 * @param device
	 */
	public static void activeWechat(AndroidDevice device) throws InterruptedException {
		device.startActivity("com.tencent.mm", ".ui.LauncherUI");
		Thread.sleep(8000);
	}

	/**
	 * 更新未完成的公众号数据
	 */
	public static void updateProcess(String mediaName, String udid) throws Exception {

		SubscribeMedia account = Tab.subscribeDao.
				queryBuilder().
				where().
				eq("media_name", mediaName).
				and().
				eq("udid", udid).
				queryForFirst();

		if (account != null) {
			long countOf = Tab.essayDao.
					queryBuilder().
					where().
					eq("media_nick", mediaName).
					countOf();
			account.number = (int) countOf;
			account.status = (countOf == 0 ? SubscribeMedia.State.NOT_EXIST.status : SubscribeMedia.State.FINISH.status);
			account.status = 1;
			account.update_time = new Date();
			account.retry_count = 5;
			account.update();
		}
	}

	/**
	 * @param udid
	 */
	public static void clearMemory(String udid) {
		try {
			ShellUtil.shutdownProcess(udid, "com.tencent.mm");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
