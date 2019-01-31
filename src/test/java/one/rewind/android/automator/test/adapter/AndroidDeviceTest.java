package one.rewind.android.automator.test.adapter;

import one.rewind.android.automator.AndroidDevice;
import one.rewind.db.exception.DBInitException;
import one.rewind.json.JSON;
import org.junit.Test;

import java.sql.SQLException;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class AndroidDeviceTest {

    @Test
    public void testQueryDevice() throws DBInitException, SQLException {
        AndroidDevice device = AndroidDevice.getAndroidDeviceByUdid("ZX1G423DMM");
        System.out.println(JSON.toPrettyJson(device));
    }
}
