package one.rewind.android.automator.api.test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import okhttp3.*;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.adapter.wechat.task.GetMediaEssaysTask1;
import one.rewind.android.automator.adapter.wechat.task.SwitchAccountTask;
import one.rewind.json.JSON;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.io.IOException;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

/**
 * API接口测试
 *
 * @author maxuefeng [m17793873123@163.com]
 */
public class APIMainServerTest {

    public static Logger logger = LogManager.getLogger(APIMainServerTest.class.getName());

    private static OkHttpClient client = new OkHttpClient.Builder().connectTimeout(1000, TimeUnit.SECONDS).build();


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

        String udid = "ZX1G22PQLH";
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
        String mediaNick = "YangtzeBond";
        ImmutableMap<String, Object> params = ImmutableMap.of(
                "params", ImmutableList.of(mediaNick),
                "adapter_class_name", WeChatAdapter.class.getName(),
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
}

























