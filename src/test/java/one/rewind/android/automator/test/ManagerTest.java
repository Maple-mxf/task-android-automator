package one.rewind.android.automator.test;

import one.rewind.android.automator.manager.Manager;
import org.junit.Test;

import java.sql.SQLException;

/**
 * Create By 2018/11/21
 * Description:
 */
public class ManagerTest {


    @Test
    public void testInitDevice() throws InterruptedException, SQLException {
        Manager manager = Manager.me();

        manager.startManager();
    }
}
