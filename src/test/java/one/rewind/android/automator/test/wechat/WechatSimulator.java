package one.rewind.android.automator.test.wechat;

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
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;


/**
 * @author maxuefeng[m17793873123@163.com]
 */
public class WechatSimulator {

	public static AndroidDriver getStart(String udid, String appPackage, String appActivity) throws MalformedURLException, InterruptedException {

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

		//ormlite jsoup

		URL serviceUrl = service.getUrl();

		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability("app", "");
		capabilities.setCapability("appPackage", appPackage); // appPackage = "com.tencent.mm"; App包名，以微信为例
		capabilities.setCapability("appActivity", appActivity); // appActivity = ".ui.LauncherUI"; App启动Activity，以微信为例
		capabilities.setCapability("fastReset", false);
		capabilities.setCapability("fullReset", false);
		capabilities.setCapability("noReset", true);

		capabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, AutomationName.ANDROID_UIAUTOMATOR2);
		//capabilities.setCapability("chromeOptions", ImmutableMap.of("androidProcess", webViewAndroidProcessName));
		// webViewAndroidProcessName = "com.tencent.mm:appbrand0"; App中的加载WebView的进程名

		capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, udid);

		//driver1 = new AndroidDriver<MobileElement>(url, capabilities);
		//AndroidDriver<MobileElement> driver = new AndroidDriver<>(serviceUrl, capabilities);

		AndroidDriver driver = new AndroidDriver(new URL("http://127.0.0.1:4723/wd/hub"), capabilities);

		Thread.sleep(1500);

		return driver;
	}


	public void getIntoFriend(AndroidDriver driver) throws InterruptedException {
		WebElement sells = driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'发现')]"));//点击发现
		sells.click();

		Thread.sleep(1700);
		List<WebElement> lis = driver.findElementsByClassName("android.widget.LinearLayout");//点击朋友圈
		WebElement targetEle = lis.get(1);//获取列表中第一个元素
		targetEle.click();

		Thread.sleep(2500);
	}


	public int getWidth(AndroidDriver driver) {
		int width = driver.manage().window().getSize().width;
		return width;
	}

	public int getHeight(AndroidDriver driver) {
		int height = driver.manage().window().getSize().height;
		return height;
	}


	public void getPic(AndroidDriver driver, int width, int height) throws IOException, InterruptedException {
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

				TouchAction back = new TouchAction(driver).tap(PointOption.point((int) StartService.mul(a, width), (int) StartService.mul(b, height)));//退出大图
				back.perform();


				Thread.sleep(2500);
			}


			TouchAction action1 = new TouchAction(driver).press(PointOption.point((int) StartService.mul(c, width), (int) StartService.mul(d, height))).waitAction().moveTo(PointOption.point((int) StartService.mul(c, width), (int) StartService.mul(e, height))).release();
			action1.perform();
			//向上滑动整个页面

			Thread.sleep(2500);

		}
	}


	public void getSimpleFriend(String friend, AndroidDriver driver) throws InterruptedException {

		WebElement sells = driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'通讯录')]"));//进入通讯录
		sells.click();

		Thread.sleep(1500);

		for (int i = 0; i < 25; i++) {//找到friend变量的好友并点击
			List<WebElement> lis2 = (List<WebElement>) driver.findElementsByAccessibilityId(friend);
			if (lis2.size() != 0) {
				driver.findElementByAccessibilityId(friend).click();
				break;
			}
			TouchAction action1 = new TouchAction(driver).press(PointOption.point(200, 1024)).waitAction().moveTo(PointOption.point(200, 176)).release();
			action1.perform();
		}

		Thread.sleep(1500);

		driver.findElement(By.xpath("//android.widget.Button[contains(@text,'发消息')]")).click();//进入聊天界面

		Thread.sleep(2000);
	}


	public void sendMsg(AndroidDriver driver, String msg) throws InterruptedException {
		driver.findElementByClassName("android.widget.EditText").sendKeys(msg);//sendKeys
		Thread.sleep(1500);
		driver.findElement(By.xpath("//android.widget.Button[contains(@text,'发送')]")).click();//发送
		Thread.sleep(1500);
	}


	public void getIntoText(AndroidDriver driver, int width) throws FileNotFoundException, InterruptedException {
		String relativepath = System.getProperty("user.dir") + "/writefile.txt";
		PrintWriter out = new PrintWriter(relativepath);

		for (int i = 0; i < 1; i++) {
			List<WebElement> lis3 = (List<WebElement>) driver.findElementsByClassName("android.template.View");
			for (int j = lis3.size() - 1; j >= 0; j--) {
				int x = (lis3.get(j).getLocation().getX());
				if (x < width / 2) {//对方的信息
					TouchAction longtouch1 = new TouchAction(driver);
					Point center = lis3.get(j).getLocation();
					longtouch1.longPress(PointOption.point(center.getX(), center.getY())).release().perform();
					Thread.sleep(1500);
					driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'复制')]")).click();
					Thread.sleep(1500);
					String centertext = driver.getClipboardText();
					out.write("op:" + centertext + "\t");
					out.write("\n");
					System.out.println("op:" + centertext + "\t");
				} else if (x > width / 2) {//自己的信息
					TouchAction longtouch2 = new TouchAction(driver);
					Point center1 = lis3.get(j).getLocation();
					longtouch2.longPress(PointOption.point(center1.getX(), center1.getY())).release().perform();
					Thread.sleep(1500);
					driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'复制')]")).click();
					Thread.sleep(1500);
					String centertext2 = driver.getClipboardText();
					out.write("me:" + centertext2 + "\t");
					System.out.println("me:" + centertext2 + "\t");
				}
			}
			//向上滑动
			TouchAction action1 = new TouchAction(driver).press(PointOption.point(200, 176)).waitAction().moveTo(PointOption.point(200, 1166)).release();
			action1.perform();
			Thread.sleep(1500);
		}

		out.flush();
		out.close();
	}




	public static void addFriendsByFile(AndroidDriver driver, String filePath) throws InterruptedException {
		//从txt中按行导入
		String[] friendArray = readToString(filePath);//filePath = "C:\\test\\newFriend.txt"
		for (int i = 0; i < friendArray.length; i++) {
			System.out.println(friendArray[i]);
			//验证和备注是否要修改，在下方for循环中相应修改，对应txt文件中的行
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

		outer:
		for (int j = 0; j < friendArray.length; j++) {

			driver.findElement(By.xpath("//android.widget.EditText[contains(@text,'微信号/QQ号/手机号')]")).sendKeys(friendArray[j]);//填入添加人的号码
			Thread.sleep(1500);

			driver.findElementByClassName("android.widget.TextView").click();//点击搜索
			Thread.sleep(1300);

			//如果用户不存在
			List<WebElement> lis2 = (List<WebElement>) driver.findElements(By.xpath("//android.widget.TextView[contains(@text,'该用户不存在')]"));
			if (lis2.size() != 0) {
				driver.findElementByAccessibilityId("返回").click();//返回
				System.out.println(friendArray[j]);
				Thread.sleep(1000);
				driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'微信号/QQ号/手机号')]")).click();
				Thread.sleep(1500);
				continue outer;
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

	public void getIntoGroupByName(AndroidDriver driver, String groupname) {

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


	public static void main(String[] arg) throws MalformedURLException, InterruptedException {
		String udid = "192.168.98.101:5555";
		String appPackage = "com.tencent.mm";
		String appActivity = ".ui.LauncherUI";
		String webViewAndroidProcessName = "com.tencent.mm:appbrand0";
		String friend = "";
		String msg = "";
		String file = "C:\\\\test\\\\newFriend.txt";
		AndroidDriver driver = WechatSimulator.getStart(udid, appPackage, appActivity);
		WechatSimulator.addFriendsByFile(driver,file);
	}
}
