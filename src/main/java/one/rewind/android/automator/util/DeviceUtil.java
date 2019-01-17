package one.rewind.android.automator.util;

import io.appium.java_client.android.AndroidDriver;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.adapter.WeChatAdapter;
import one.rewind.android.automator.ocr.OCRParser;
import one.rewind.android.automator.ocr.TesseractOCRParser;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.UUID;

/**
 * @author maxuefeng[m17793873123@163.com]
 */
public class DeviceUtil {


	// 重启手机APP
	public static void restartWechat(AndroidDevice device) throws InterruptedException {
		DeviceUtil.clearMemory(device.udid);
		DeviceUtil.activeWechat(device);
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
			WeChatAdapter.screenshot(device.driver);

//			JSONObject jsonObject = TesseractOCRParser.imageOcr(path + fileName, false);
			List<OCRParser.TouchableTextArea> textAreas = TesseractOCRParser.getInstance().imageOcr(path + fileName, false);
			JSONObject jsonObject = null;

			JSONArray array = jsonObject.getJSONArray("words_result");

			for (Object o : array) {

				JSONObject v = (JSONObject) o;

				String words = v.getString("words");
				if (words.contains("微信没有响应") || words.contains("关闭应用")) {
					WeChatAdapter.clickPoint(517, 1258, 1000, device.driver);
					break;
				}
				if (words.contains("要将其关闭吗") && words.contains("微信无响应")) {
					//点击确定  这个截图和上面的截图是有点不太一样的
					WeChatAdapter.clickPoint(1196, 1324, 1000, device.driver);
					break;

				}
				if (words.contains("系统繁忙") && words.contains("请稍后再试")) {
					WeChatAdapter.clickPoint(1110, 1342, 5000, device.driver);
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
