package one.rewind.android.automator.api;

import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.deivce.AndroidDevice;
import one.rewind.android.automator.deivce.AndroidDeviceManager;
import one.rewind.db.exception.DBInitException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;

import static one.rewind.android.automator.api.Routes.*;
import static spark.Spark.*;


/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class APIServer {

    public static Logger logger = LogManager.getLogger(APIServer.class.getName());

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

            // 查看某一个设备的执行情况 operator
            post("/operator", null);


            // 获取指定Adapter登录的账号
            post("/getLoginAccountInfo", getLoginAccountInfo);

            // 切换账号
            post("/switchAccount", switchAccount);

        });

        logger.info("API Server started!");
    }


}




























