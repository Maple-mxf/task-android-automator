package one.rewind.android.automator.device.test;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.remote.AutomationName;
import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import one.rewind.android.automator.adapter.Adapter;
import one.rewind.util.FileUtil;
import org.junit.Test;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.remote.DesiredCapabilities;

/**
 * @author scisaga@gmail.com
 * @date 2019/2/22
 */
public class AppiumTest {

	@Test
	public void testBuildLocalService() throws InterruptedException {

		Adapter.AppInfo appInfo = new Adapter.AppInfo("com.tencent.mm", ".ui.LauncherUI");

		// A 定义Service Capabilities
		DesiredCapabilities serviceCapabilities = new DesiredCapabilities();

		serviceCapabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android");
		// serviceCapabilities.setCapability(MobileCapabilityType.UDID, udid); // udid是设备的唯一标识
		serviceCapabilities.setCapability(MobileCapabilityType.NEW_COMMAND_TIMEOUT, 0); // 永不超时

		AppiumDriverLocalService service = new AppiumServiceBuilder()
				.withCapabilities(serviceCapabilities)
				.usingPort(42755)
				//.withArgument(GeneralServerFlag.LOG_LEVEL, "info")
				/*.withArgument(GeneralServerFlag.SESSION_OVERRIDE, "true")*/ // TODO  session覆盖问题解决
				.build();
		service.start();


		DesiredCapabilities capabilities = new DesiredCapabilities();
		capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android");
		capabilities.setCapability("app", ""); // 必须设定 否则无法启动Driver
		capabilities.setCapability("appPackage", appInfo.appPackage);   // App包名 必须设定 否则无法启动Driver
		capabilities.setCapability("appActivity", appInfo.appActivity); // App启动Activity 必须设定 否则无法启动Driver
		//capabilities.setCapability("fastReset", false);
		capabilities.setCapability("fullReset", false);
		capabilities.setCapability("noReset", true);

		capabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, AutomationName.ANDROID_UIAUTOMATOR2);

		// TODO 下面两行代码如果不添加 是否不能进入小程序？
		/*String webViewAndroidProcessName = "com.tencent.mm:tools";
		webViewAndroidProcessName = "com.tencent.mm:appbrand0"; // App中的加载WebView的进程名
		capabilities.setCapability("chromeOptions", ImmutableMap.of("androidProcess", webViewAndroidProcessName));*/
		capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, "ZX1G426B3V");
		capabilities.setCapability(MobileCapabilityType.UDID, "ZX1G426B3V");

//other caps
		AndroidDriver<AndroidElement> driver = new AndroidDriver<>(service.getUrl(), capabilities);

		capabilities = new DesiredCapabilities();
		capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android");
		capabilities.setCapability("app", ""); // 必须设定 否则无法启动Driver
		capabilities.setCapability("appPackage", appInfo.appPackage);   // App包名 必须设定 否则无法启动Driver
		capabilities.setCapability("appActivity", appInfo.appActivity); // App启动Activity 必须设定 否则无法启动Driver
		//capabilities.setCapability("fastReset", false);
		capabilities.setCapability("fullReset", false);
		capabilities.setCapability("noReset", true);

		capabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, AutomationName.ANDROID_UIAUTOMATOR2);

		// TODO 下面两行代码如果不添加 是否不能进入小程序？
		/*String webViewAndroidProcessName = "com.tencent.mm:tools";
		webViewAndroidProcessName = "com.tencent.mm:appbrand0"; // App中的加载WebView的进程名
		capabilities.setCapability("chromeOptions", ImmutableMap.of("androidProcess", webViewAndroidProcessName));*/
		capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, "ZX1G22MMSQ");
		capabilities.setCapability(MobileCapabilityType.UDID, "ZX1G22MMSQ");

//other caps
		AndroidDriver<AndroidElement> driver2 = new AndroidDriver<>(service.getUrl(), capabilities);

		Thread.sleep(10000);

		byte[] img = driver.getScreenshotAs(OutputType.BYTES);
		FileUtil.writeBytesToFile(img, "tmp/1.png");
		byte[] img2 = driver2.getScreenshotAs(OutputType.BYTES);
		FileUtil.writeBytesToFile(img2, "tmp/2.png");

	}
}
