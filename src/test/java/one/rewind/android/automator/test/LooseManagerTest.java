package one.rewind.android.automator.test;

import one.rewind.android.automator.manager.LooseDeviceManager;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;

public class LooseManagerTest {

    @Test
    public void testLooseBoot() throws SQLException, ClassNotFoundException, IOException {

        System.out.println("一键启动!开箱即用.");

        LooseDeviceManager manager = LooseDeviceManager.getInstance();

        manager.startManager();

        System.in.read();
    }

    @Test
    public void testLooseBoot2() throws SQLException, ClassNotFoundException, IOException {

        System.out.println("一键启动!开箱即用.");

        LooseDeviceManager manager = LooseDeviceManager.getInstance();

        manager.startManager2();

        System.in.read();
    }
}
