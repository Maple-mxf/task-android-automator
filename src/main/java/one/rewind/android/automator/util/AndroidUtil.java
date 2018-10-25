package one.rewind.android.automator.util;

import com.j256.ormlite.dao.Dao;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.touch.offset.PointOption;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.exception.InvokingBaiduAPIException;
import one.rewind.android.automator.model.Essays;
import one.rewind.android.automator.model.TaskFailRecord;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.UUID;

/**
 * Create By 2018/10/15
 * Description   操作安卓常用方法
 */
@SuppressWarnings("JavaDoc")
public class AndroidUtil {


	/**
	 * @param name
	 * @throws InterruptedException
	 */
	public static void enterEssaysPage(String name, AndroidDriver driver) throws InterruptedException {


		driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'通讯录')]")).click();

		Thread.sleep(1000);

		driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'公众号')]")).click();

		Thread.sleep(1000);

		driver.findElement(By.xpath("//android.widget.ImageButton[contains(@content-desc,'搜索')]")).click();

		Thread.sleep(1000);

		// 搜索
		driver.findElement(By.className("android.widget.EditText")).sendKeys(name);

		AndroidUtil.clickPoint(720, 150, 0, driver);

		AndroidUtil.clickPoint(1350, 2250, 2000, driver);

		// 进入公众号
		AndroidUtil.clickPoint(720, 360, 1000, driver);

		driver.findElement(By.xpath("//android.widget.ImageButton[contains(@content-desc,'聊天信息')]")).click();

		Thread.sleep(1000);

		AndroidUtil.slideToPoint(720, 1196, 720, 170, driver, 1000);

		driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'全部消息')]")).click();

		Thread.sleep(10000); // TODO 此处时间需要调整

	}

	/**
	 * 截图
	 *
	 * @param fileName
	 * @param path
	 */
	public static void screenshot(String fileName, String path, AndroidDriver driver) {
		try {
			File screenFile = ((TakesScreenshot) driver)
					.getScreenshotAs(OutputType.FILE);
			FileUtils.copyFile(screenFile, new File(path + fileName));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 点击固定的位置
	 *
	 * @param xOffset
	 * @param yOffset
	 * @param sleepTime 睡眠时间
	 * @throws InterruptedException
	 */
	public static void clickPoint(int xOffset, int yOffset, int sleepTime, AndroidDriver driver) throws InterruptedException {
		new TouchAction(driver).tap(PointOption.point(xOffset, yOffset)).perform();
		if (sleepTime > 0) {
			Thread.sleep(sleepTime);
		}
	}

	/**
	 * 点击返回
	 * 如果是在文章页面点击返回的时候需要判别是否是点击有效   如果无效，则不会点击返回
	 *
	 * @throws InterruptedException
	 */
	public static void returnPrevious(AndroidDriver driver) throws InterruptedException {
		driver.findElement(By.xpath("//android.widget.ImageView[contains(@content-desc,'返回')]")).click();
		Thread.sleep(1000);
	}

	/**
	 * 下滑到指定位置
	 *
	 * @param startX
	 * @param startY
	 * @param endX
	 * @param endY
	 */
	public static void slideToPoint(int startX, int startY, int endX, int endY, AndroidDriver driver, int sleepTime) throws InterruptedException {
		new TouchAction(driver).press(PointOption.point(startX, startY))
				.waitAction()
				.moveTo(PointOption.point(endX, endY))
				.release()
				.perform();
		if (sleepTime > 0) {
			Thread.sleep(sleepTime);
		}
	}

	/**
	 * 返回到主界面   只针对于未爬完的公众号
	 *
	 * @throws InterruptedException
	 */
	public static void exit2Main(AndroidDriver driver) throws InterruptedException {

		for (int i = 0; i < 3; i++) {
			// 点击返回
			driver.findElement(By.xpath("//android.widget.ImageView[contains(@content-desc,'返回')]")).click();
			Thread.sleep(1000);
		}
	}


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


	public static void closeEssay(AndroidDriver driver) throws InterruptedException {
		driver.navigate().back();
		Thread.sleep(1000);
	}

	/**
	 * 关闭App可能会存在一些无法预料的问题   比如手机出来一层透明层  此时closeApp方法调用可能会不起作用
	 *
	 * @param driver
	 * @throws InvokingBaiduAPIException
	 * @throws InterruptedException
	 */
	public static void closeApp(AndroidDriver driver) throws InvokingBaiduAPIException, InterruptedException {
		//截图
		String filePrefix = UUID.randomUUID().toString();
		String fileName = filePrefix + ".png";
		String path = System.getProperty("user.dir") + "/screen/";
		AndroidUtil.screenshot(fileName, path, driver);

		JSONObject jsonObject = BaiduAPIUtil.executeImageRecognitionRequest(path + fileName);

		JSONArray array = (JSONArray) jsonObject.get("words_result");


		for (Object o : array) {

			JSONObject v = (JSONObject) o;

			String words = v.getString("words");
			if (words.contains("关闭应用")) {
				AndroidUtil.clickPoint(521, 1256, 1000, driver);
				break;
			}
			if (words.contains("要将其关闭吗")) {
				//点击确定  这个截图和上面的截图是有点不太一样的
				AndroidUtil.clickPoint(1196, 1377, 1000, driver);
				break;
			}
		}

		driver.closeApp();
		/**
		 * close之后再按一次home键  防止微信没有启动
		 */
		for (int i = 0; i < 5; i++) {
			driver.navigate().back();
		}
	}

	public static TaskFailRecord retry(String wxPublicName, Dao<Essays, String> dao2, String udid, Dao<TaskFailRecord, String> dao1) throws Exception {

		long count = dao2.queryBuilder().where().eq("media_nick", wxPublicName).countOf();

		TaskFailRecord temp = dao1.queryBuilder().where().eq("wxPublicName", wxPublicName).queryForFirst();

		if (temp == null) {
			TaskFailRecord record = new TaskFailRecord();

			record.deviceUdid = udid;

			record.wxPublicName = wxPublicName;

			record.finishNum = (int) count;

			int var = (int) count % 6;

			if (var >= 3) {
				record.slideNumByPage = (int) (count / 6) + 2;
			} else {
				record.slideNumByPage = (int) (count / 6) + 1;
			}
			record.insert();

			return record;
		} else {
			temp.finishNum = (int) count;

			int var = (int) count % 6;

			if (var >= 3) {
				temp.slideNumByPage = (int) (count / 6) + 2;
			} else {
				temp.slideNumByPage = (int) (count / 6) + 1;
			}

			temp.update();
			return temp;
		}
	}

	/**
	 * 激活应用
	 *
	 * @param device
	 */
	public static void activeWechat(AndroidDevice device) throws InterruptedException {
		device.startActivity("com.tencent.mm", ".ui.LauncherUI");
		Thread.sleep(10000);
	}
}
