package one.rewind.android.automator.test.wechat;

import com.google.common.collect.ImmutableMap;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.AutomationName;
import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class NewFriends {

	public static void main(String arg[]) throws MalformedURLException, InterruptedException {

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

		//从txt中按行导入
		String[] friendArray = readToString("C:\\test\\newFriend.txt");
		for (int i = 0; i < friendArray.length; i++) {
			System.out.println(friendArray[i]);
			//验证和备注是否要修改，在下方for循环中相应修改
			//System.out.println(friendArray[i + 1]);
			//i++;
			//System.out.println(friendArray[i+1]);
			//i++;
		}

		//设定验证信息和备注
		String yanzheng = "验证";
		String beizhu = "备注";

		Thread.sleep(1500);

		WebElement add = driver.findElementByAccessibilityId("更多功能按钮");//点击发现
		add.click();

		Thread.sleep(1500);

		WebElement newfriend = driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'添加朋友')]"));
		newfriend.click();

		Thread.sleep(1500);

		driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'微信号/QQ号/手机号')]")).click();

		Thread.sleep(1500);


		for (int j = 0; j < friendArray.length; j++) {
			driver.findElement(By.xpath("//android.widget.EditText[contains(@text,'微信号/QQ号/手机号')]")).sendKeys(friendArray[j]);//填入添加人的号码
			Thread.sleep(1500);

			driver.findElementByClassName("android.widget.TextView").click();//点击搜索
			Thread.sleep(1800);

			//如果用户不存在
			List<WebElement> lis2 = (List<WebElement>) driver.findElementsByAccessibilityId("该用户不存在");
			if (lis2.size()!=0){
				driver.findElementByAccessibilityId("返回").click();//返回
				System.out.println(friendArray[j]);
				continue;
			}

			driver.findElementByClassName("android.widget.Button").click();//添加到通讯录
			Thread.sleep(1500);

			List<WebElement> temp = (List<WebElement>) driver.findElementsByClassName("android.widget.EditText");
			temp.get(0).clear();
			temp.get(0).sendKeys(yanzheng);//重填验证
			Thread.sleep(300);

			temp.get(1).clear();
			temp.get(1).sendKeys(beizhu);//重填备注
			Thread.sleep(1000);

			driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'发送')]")).click();//发送
			Thread.sleep(1500);

			driver.findElementByAccessibilityId("返回").click();//返回
			Thread.sleep(1500);

			driver.findElementByClassName("android.widget.EditText").clear();//清空
			Thread.sleep(500);

		}


	}


	public static String[] readToString(String filePath) {
		File file = new File(filePath);
		Long filelength = file.length(); // 获取文件长度
		byte[] filecontent = new byte[filelength.intValue()];
		try {
			FileInputStream in = new FileInputStream(file);
			in.read(filecontent);
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String[] fileContentArr = new String(filecontent).split("\r\n");

		return fileContentArr;// 返回文件内容,默认编码
	}
}
