package one.rewind.android.automator.device.test;

import one.rewind.android.automator.deivce.AndroidDevice;
import one.rewind.android.automator.deivce.AndroidUtil;
import org.junit.Test;
import org.openqa.selenium.By;
import se.vidstige.jadb.JadbException;

import java.io.IOException;

/**
 * @author scisaga@gmail.com
 * @date 2019/2/10
 */
public class GeneralDeviceTest {

	AndroidDevice device = new AndroidDevice("ZX1G22PQLH");

	@Test
	public void testSetWifiProxy() throws IOException, JadbException {

		AndroidUtil.setupRemoteWifiProxy(device.udid, "reid.red", 60103);

	}

	@Test
	public void testRemoveWifiProxy() throws IOException, JadbException {

		AndroidUtil.removeRemoteWifiProxy(device.udid);

	}

	@Test
	public void testClick() {
		device.driver.findElement(By.xpath("//android.widget.Button[contains(@text,'取消')]")).click();
	}
}
