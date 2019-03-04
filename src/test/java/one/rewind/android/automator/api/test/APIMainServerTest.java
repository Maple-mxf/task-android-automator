package one.rewind.android.automator.api.test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.j256.ormlite.dao.Dao;
import okhttp3.*;
import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.adapter.wechat.model.WechatAccountMediaSubscribe;
import one.rewind.android.automator.adapter.wechat.task.GetMediaEssaysTask1;
import one.rewind.android.automator.adapter.wechat.task.SwitchAccountTask;
import one.rewind.android.automator.deivce.AndroidDeviceManager;
import one.rewind.db.Daos;
import one.rewind.db.exception.DBInitException;
import one.rewind.json.JSON;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * API接口测试
 *
 * @author maxuefeng [m17793873123@163.com]
 */
public class APIMainServerTest {

    private static Logger logger = LogManager.getLogger(APIMainServerTest.class.getName());

    static OkHttpClient client = new OkHttpClient.Builder().connectTimeout(1000, TimeUnit.SECONDS).readTimeout(1000, TimeUnit.SECONDS).callTimeout(1000, TimeUnit.SECONDS).build();


    public static Request getRequest(String urlSuffix, String json) {
        String baseUrl = "http://127.0.0.1:4567/android_automator";
        StringJoiner joiner = new StringJoiner("/").add(baseUrl).add(urlSuffix);
        return new Request.Builder().url(joiner.toString()).post(RequestBody.create(MediaType.parse("application/json"), json)).build();
    }


    /**
     * 测试切换账号
     */
    @Test
    public void testSwitchAccountAPI() throws IOException {

        String udid = "ZX1G22MMSQ";

        String accountId = String.valueOf(9);

        String className = SwitchAccountTask.class.getName();

        String json = JSON.toJson(ImmutableMap.of("udid", udid, "accountId", accountId, "className", className));

        Response response = client.newCall(getRequest("switchAccount", json)).execute();

        if (response.isSuccessful()) {

            ResponseBody responseBody = response.body();

            Optional.ofNullable(responseBody).ifPresent(t -> {

                try {

                    logger.info("Success device[{}] Login Info [{}] ", udid, t.string());

                } catch (IOException e) {

                    e.printStackTrace();
                }
            });
        }
    }


    /**
     * 测试获取当前设备登录账户信息
     */
    @Test
    public void testGetLoginAccountInfoAPI() throws IOException {

        String udid = "ZX1G227PZ7";
        Response response = client.newCall(getRequest("getLoginAccountInfo", JSON.toJson(ImmutableMap.of("udid", udid)))).execute();
        if (response.isSuccessful()) {
            ResponseBody responseBody = response.body();
            Optional.ofNullable(responseBody).ifPresent(t -> {
                try {
                    logger.info("Success device[{}] Login Info [{}] ", udid, t.string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }


    /**
     * 测试订阅任务
     */
    @Test
    public void testSubscribeTaskAPI() {
    }


    /**
     * 测试采集任务
     */
    @Test
    public void testFetchEssaysTaskAPI() throws IOException {
        String mediaNick = "小青投资笔记";
        ImmutableMap<String, Object> params = ImmutableMap.of(
                "params", ImmutableList.of(mediaNick),
                "udid", "ZX1G227PZ7",
                "class_name", GetMediaEssaysTask1.class.getName()
        );

        Response response = client.newCall(getRequest("submit", JSON.toJson(params))).execute();

        if (response.isSuccessful()) {
            ResponseBody responseBody = response.body();
            Optional.ofNullable(responseBody).ifPresent(t -> {
                try {
                    logger.info("Success device[{}] Response Info [{}]", t.string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

    }


    public int getAccountId(String udid) throws IOException {
        Response response = client.newCall(getRequest("getLoginAccountInfo", JSON.toJson(ImmutableMap.of("udid", udid)))).execute();

        if (response.isSuccessful()) {
            ResponseBody resp = response.body();
            Map<String, Object> obj = JSON.fromJson(resp.string(), Map.class);

            Map<String, Object> acc = (Map<String, Object>) obj.get("data");
            Double id = (Double) acc.get("id");

            return id.intValue();

        }
        return 0;
    }

    @Test
    public void testGetAccountId() throws IOException {
        int id = getAccountId("ZX1G227PZ7");

        System.out.println(id);
    }

    /**
     * 测试采集任务   指定udid
     */
    @Test
    public void testMultiTaskAPI() throws IOException, DBInitException, SQLException, InterruptedException {

        String[] udids = AndroidDeviceManager.getAvailableDeviceUdids();

        Dao<WechatAccountMediaSubscribe, String> subscribeDao = Daos.get(WechatAccountMediaSubscribe.class);

        for (String udid : udids) {

            // 1获取当前设备登陆的账号
            int accountId = getAccountId(udid);

            if (accountId != 0) {

                List<String> media = subscribeDao.queryBuilder()
                        .where()
                        .eq("account_id", accountId)
                        .query().stream().map(m -> m.media_nick).limit(10).collect(Collectors.toList());

                for (String var : media) {

                    // build params body
                    ImmutableMap<String, Object> params = ImmutableMap.of(
                            "params", ImmutableList.of(var),
                            "udid", udid,                                // 指定设备
                            "class_name", GetMediaEssaysTask1.class.getName(),
                            "account_id", accountId                     // 指定账号
                    );

                    Response response = client.newCall(getRequest("submit", JSON.toJson(params))).execute();
                    logger.info("Success Response [{}]", response.body().string());

                    Thread.sleep(1000);
                }

                Thread.sleep(2000);
            }

        }
    }
}

























