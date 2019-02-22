package one.rewind.android.automator.device.test;

import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.deivce.AndroidDevice;
import one.rewind.android.automator.deivce.AndroidDeviceManager;
import one.rewind.android.automator.deivce.AndroidUtil;
import one.rewind.db.exception.DBInitException;
import one.rewind.db.model.Model;
import one.rewind.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.openqa.selenium.By;
import se.vidstige.jadb.JadbException;

import java.io.IOException;
import java.sql.SQLException;

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

    /**
     *
     * @throws Exception
     */
    @Test
    public void multiDeviceTest() throws Exception {

        Account.getAll(Account.class).forEach(a -> {
            a.occupied = false;
            try {
                a.update();
            } catch (DBInitException | SQLException e) {
                e.printStackTrace();
            }
        });

        AndroidDevice.getAll(AndroidDevice.class).forEach(ad -> {
            ad.status = AndroidDevice.Status.New;
            try {
                ad.update();
            } catch (DBInitException | SQLException e) {
                e.printStackTrace();
            }
        });

        AndroidDeviceManager.getInstance().detectDevices();

        Thread.sleep(60000);

        /*AndroidDeviceManager.getInstance().deviceTaskMap.keySet().stream().forEach(
                ad -> {
                    try {
                        byte[] screen = ad.screenshot();
                        FileUtil.writeBytesToFile(screen, "tmp/screen/" + ad.udid + ".png");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );*/


    }
}
