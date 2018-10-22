package one.rewind.android.automator.util;

import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.touch.offset.PointOption;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * Create By 2018/10/15
 * Description   操作安卓常用方法
 */
@SuppressWarnings("JavaDoc")
public class AndroidUtil {

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
	public static void slideToPoint(int startX, int startY, int endX, int endY, AndroidDriver driver) {
		new TouchAction(driver).press(PointOption.point(startX, startY))
				.waitAction()
				.moveTo(PointOption.point(endX, endY))
				.release()
				.perform();
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

}
