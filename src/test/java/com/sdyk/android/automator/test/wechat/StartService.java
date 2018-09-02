package com.sdyk.android.automator.test.wechat;


import com.google.common.collect.ImmutableMap;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.AutomationName;
import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import io.appium.java_client.touch.offset.PointOption;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;


import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;


public class StartService {


	public static void main(String arg[]) throws IOException, SQLException, InterruptedException {

		String udid = "ZX1G323GNB";
		String appPackage = "com.tencent.mm";
		String appActivity = ".ui.LauncherUI";
		String webViewAndroidProcessName = "com.tencent.mm:appbrand0";

		/*
		AppiumDriver<?> driver1;
		final String URL_STRING = "http://127.0.0.1:4723/wd/hub";
		URL url = new URL(URL_STRING);
		*/

		DesiredCapabilities serverCapabilities = new DesiredCapabilities();

		serverCapabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android");
		serverCapabilities.setCapability(MobileCapabilityType.UDID, udid); // udid是设备的唯一标识
		serverCapabilities.setCapability(MobileCapabilityType.NEW_COMMAND_TIMEOUT, 3600);

		AppiumDriverLocalService service = new AppiumServiceBuilder()
				.withCapabilities(serverCapabilities)
				.usingAnyFreePort()
				.withArgument(GeneralServerFlag.LOG_LEVEL, "util")
				.build();

		service.start();

		URL serviceUrl = service.getUrl();
		//ormlite jsoup
		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability("app", "");
		capabilities.setCapability("appPackage", appPackage); // appPackage = "com.tencent.mm"; App包名，以微信为例
		capabilities.setCapability("appActivity", appActivity); // appActivity = ".ui.LauncherUI"; App启动Activity，以微信为例
		capabilities.setCapability("fastReset", false);
		capabilities.setCapability("fullReset", false);
		capabilities.setCapability("noReset", true);

		capabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, AutomationName.ANDROID_UIAUTOMATOR2);
		capabilities.setCapability("chromeOptions", ImmutableMap.of("androidProcess", webViewAndroidProcessName));
		// webViewAndroidProcessName = "com.tencent.mm:appbrand0"; App中的加载WebView的进程名

		capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, udid);

		//driver1 = new AndroidDriver<MobileElement>(url, capabilities);
		//AndroidDriver<MobileElement> driver = new AndroidDriver<>(serviceUrl, capabilities);

		AndroidDriver driver = new AndroidDriver(new URL("http://127.0.0.1:4723/wd/hub"), capabilities);

		Thread.sleep(1700);

		//点击第一个聊天窗口
		// driver.findElement(By.xpath("//android.widget.LinearLayout[2]")).click();
		//输入发送的文字
		// driver.findElement(By.xpath("//android.widget.EditText[1]")).sendKeys("123");

		//driver.findElementById("com.tencent.mm:id/d75").click();

		//driver.findElementById("com.tencent.mm:id/cdh").getCenter();

		WebElement sells = driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'发现')]"));//点击发现
		sells.click();

		Thread.sleep(1700);
		List<WebElement> lis = driver.findElementsByClassName("android.widget.LinearLayout");//点击朋友圈
		WebElement targetEle = lis.get(1);//获取列表中第一个元素
		targetEle.click();

		Thread.sleep(2500);

		//根据屏幕大小转化
		int width = driver.manage().window().getSize().width;
		int height = driver.manage().window().getSize().height;

		//点击的点占屏幕的比例
		double a = 0.625;
		double b = 0.78125;
		double c = 0.2604;
		double d = 0.9109;
		double e = 0.1375;

		for (int i = 0; i <= 15; i++) {
			List<WebElement> lis2 = (List<WebElement>) driver.findElementsByAccessibilityId("图片");//获取图片数量，存入list
			int length = lis2.size();
			for (int j = 0; j < length; j++) {//从list中取出并截图
				WebElement pic1 = lis2.get(j);
				pic1.click();

				Thread.sleep(3500);

				File srcFile = driver.getScreenshotAs(OutputType.FILE);

				FileUtils.copyFile(srcFile, new File("screenshot" + i + j + ".png"));//截屏

				Thread.sleep(2500);

				TouchAction back = new TouchAction(driver).tap(PointOption.point((int) mul(a, width), (int) mul(b, height)));//退出大图
				back.perform();


				Thread.sleep(2500);
			}


			TouchAction action1 = new TouchAction(driver).press(PointOption.point((int) mul(c, width), (int) mul(d, height))).waitAction().moveTo(PointOption.point((int) mul(c, width), (int) mul(e, height))).release();
			action1.perform();
			//向上滑动整个页面

			Thread.sleep(2500);

		}


	}

	//double的乘法
	public static double mul(double d1, double d2) {
		BigDecimal b1 = new BigDecimal(d1);
		BigDecimal b2 = new BigDecimal(d2);
		return b1.multiply(b2).doubleValue();
	}

	//List lis = driver.findElementsByClassName("android.widget.relative");//获取ImageView的所有元素

	//WebElement targetEle = (WebElement) lis.get(3);//获取列表中第一个元素


	//找按钮并点击
	//WebElement search = driver.findElementByAndroidUIAutomator("new UiSelector().className(\"android.widget.Button\").text(\"发现\")");
	//search.click();


		/*
		Boolean discover=driver.findElementById("com.tencent.mm:id/cdh").getAttribute("clickable").equals("false");
		if(discover)
		{
		}
		*/

	//单击
		/*
		driver.findElementById("com.android.calculator2:id/op_add").click();
				driver.findElementById("com.android.calculator2:id/digit_2").click();
		driver.findElementById("com.android.calculator2:id/eq").click();
		*/


}

