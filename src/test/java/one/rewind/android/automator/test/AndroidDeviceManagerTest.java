package one.rewind.android.automator.test;

import one.rewind.android.automator.manager.AndroidDeviceManager;
import org.junit.Test;

import java.sql.SQLException;

/**
 * Create By 2018/11/21
 * Description:
 */
public class AndroidDeviceManagerTest {


    @Test
    public void testInitDevice() throws InterruptedException, SQLException {

        AndroidDeviceManager manager = AndroidDeviceManager.me();

        manager.startManager();
    }
}
