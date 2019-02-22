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


    public static void testRemoveWifiProxy(String udid) throws IOException, JadbException {
        AndroidUtil.execShell(udid, "settings", "delete", "global", "http_proxy");
        AndroidUtil.execShell(udid, "settings", "delete", "global", "global_http_proxy_host");
        AndroidUtil.execShell(udid, "settings", "delete", "global", "global_http_proxy_port");
        AndroidUtil.execShell(udid, "settings", "delete", "global", "https_proxy");
        AndroidUtil.execShell(udid, "settings", "delete", "global", "global_https_proxy_host");
        AndroidUtil.execShell(udid, "settings", "delete", "global", "global_https_proxy_port");
    }

    /**
     * 移除Wifi
     * @param args
     * @throws IOException
     * @throws JadbException
     */
    public static void main(String[] args) throws IOException, JadbException {
        testRemoveWifiProxy("ZX1G426B3V");
    }
}
