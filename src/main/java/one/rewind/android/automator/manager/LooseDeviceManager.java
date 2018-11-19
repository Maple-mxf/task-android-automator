package one.rewind.android.automator.manager;

import com.google.common.collect.Lists;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.adapter.LooseWechatAdapter2;
import one.rewind.android.automator.model.BaiduTokens;
import one.rewind.android.automator.model.DBTab;
import one.rewind.android.automator.model.SubscribeMedia;
import one.rewind.android.automator.util.AndroidUtil;
import one.rewind.android.automator.util.DateUtil;
import one.rewind.db.DaoManager;
import org.apache.commons.lang3.time.DateUtils;

import java.sql.SQLException;
import java.util.*;

/**
 * Create By 2018/10/19
 * Description
 *
 * @see DefaultDeviceManager 集中式线程任务分配  放弃此方案
 * @see LooseDeviceManager 分散式线程任务分配
 */
public class LooseDeviceManager {


    private List<AndroidDevice> devices = Lists.newArrayList();

    private static final int DEFAULT_LOCAL_PROXY_PORT = 48454;


    private void init() {
        try {
            DBTab.subscribeDao = DaoManager.getDao(SubscribeMedia.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String[] var = AndroidUtil.obtainDevices();
        Random random = new Random();
        for (int i = 0; i < var.length; i++) {
            AndroidDevice device = new AndroidDevice(var[i], random.nextInt(50000));
            device.state = AndroidDevice.State.INIT;
            device.initApp(DEFAULT_LOCAL_PROXY_PORT + i);
            devices.add(device);
        }
    }

    public void startManager() throws SQLException {
        init();
        reset();
        for (AndroidDevice device : devices) {
            LooseWechatAdapter2 adapter = new LooseWechatAdapter2(device);

            try {
                adapter.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        new ResetTokenState().startTimer();
    }

    private void reset() throws SQLException {
        List<SubscribeMedia> accounts = DBTab.subscribeDao.queryForAll();
        for (SubscribeMedia v : accounts) {
            try {
                if (v.status == 2 || v.status == 1 || v.retry_count >= 5) {
                    continue;
                }

                long countOf = DBTab.essayDao.
                        queryBuilder().
                        where().
                        eq("media_nick", v.media_name).
                        countOf();
                if ((countOf >= v.number || Math.abs(v.number - countOf) <= 5) && countOf > 0) {
                    v.retry_count = 5;
                    v.status = SubscribeMedia.CrawlerState.FINISH.status;
                    v.number = (int) countOf;
                } else {
                    v.status = SubscribeMedia.CrawlerState.NOFINISH.status;
                    v.retry_count = 0;
                    if (v.number == 0) v.number = 100;
                }
                v.update();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    class ResetTokenState extends TimerTask {
        @Override
        public void run() {
            try {
                List<BaiduTokens> tokens = DBTab.tokenDao.queryForAll();

                for (BaiduTokens v : tokens) {
                    if (!DateUtils.isSameDay(v.update_time, new Date())) {
                        v.count = 0;
                        v.update_time = new Date();
                        v.update();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void startTimer() {
            Timer timer = new Timer(false);
            TimerTask task = new ResetTokenState();
            Date nextDay = DateUtil.buildDate();
            timer.schedule(task, nextDay, 1000 * 60 * 60 * 24);
        }
    }
}
