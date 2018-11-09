package one.rewind.android.automator.test;

import one.rewind.android.automator.adapter.LooseWechatAdapter;
import one.rewind.android.automator.manager.LooseDeviceManager;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class LooseManagerTest {

    @Test
    public void testLooseBoot() throws SQLException, ClassNotFoundException, IOException {

        System.out.println("一键启动!开箱即用.");

        LooseDeviceManager manager = LooseDeviceManager.getInstance();

        manager.startManager();

        Timer timer = new Timer(false);

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                System.out.println("定时执行释放内存操作!");
                List<LooseWechatAdapter> adapters = LooseDeviceManager.adapters;
                try {
                    for (LooseWechatAdapter adapter : adapters) {
                        adapter.clearMemory();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        timer.schedule(task, 0, 1000 * 60 * 5);
        System.in.read();
    }
}
