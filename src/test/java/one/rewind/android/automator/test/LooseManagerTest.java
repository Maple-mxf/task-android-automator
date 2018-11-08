package one.rewind.android.automator.test;

import one.rewind.android.automator.LooseDeviceManager;
import org.junit.Test;

import java.sql.SQLException;

public class LooseManagerTest {

    @Test
    public void testLooseBoot() throws SQLException, ClassNotFoundException {

        System.out.println("一键启动!开箱即用.");

        LooseDeviceManager manager = LooseDeviceManager.getInstance();

        manager.startManager();

        System.out.println("等待任务执行");
    }
}
