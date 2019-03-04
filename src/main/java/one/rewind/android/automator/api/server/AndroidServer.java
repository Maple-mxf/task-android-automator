package one.rewind.android.automator.api.server;

import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.deivce.AndroidDevice;
import one.rewind.android.automator.deivce.AndroidDeviceManager;
import one.rewind.db.RedissonAdapter;
import one.rewind.db.exception.DBInitException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.api.map.event.EntryCreatedListener;
import org.redisson.api.map.event.EntryUpdatedListener;

import java.sql.SQLException;

import static one.rewind.android.automator.api.server.Routes.*;
import static spark.Spark.path;
import static spark.Spark.post;


/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class AndroidServer {

    static RedissonClient client = RedissonAdapter.redisson;

    public static Logger logger = LogManager.getLogger(AndroidServer.class.getName());

    public static void initDevices() throws Exception {
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
    }


    public static void main(String[] args) throws Exception {

        initDevices();

        path("/android_automator", () -> {

            post("/", overview);

            // 提交任务
            post("/submit", submit);

            // 查看某一个设备的执行情况
            post("/operator", operator);

            // 获取指定Adapter登录的账号
            post("/getLoginAccountInfo", getLoginAccountInfo);

            // 切换账号
            post("/switchAccount", switchAccount);

            // 获取当前系统中有多少个设备在运行
            post("/getDevicesInfo", getDevicesInfo);
        });


        // 监控实时任务  MapCache#key:公众号昵称  getMapCache#value:公众号更新时间
        // 只要是实时任务，只采集历史页面第一页的数据  offset = 10
        new Thread(() -> {
            RMapCache<String, String> realTimeMsg = client.getMapCache("realTimeMessage");
            realTimeMsg.addListener((EntryCreatedListener<String, String>) e -> {


            });

        }).start();

        logger.info("API Server started!");
    }


}




























