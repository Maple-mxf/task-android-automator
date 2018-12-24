package one.rewind.android.automator.test.api;


import okhttp3.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

/**
 * @author maxuefeng [m17793873123@163.com]   安卓自动化爬虫接口测试用例
 */
public class APIClient {

    private Request buildRequest(String json) {
        String url = "http://10.0.0.157:8080/push";
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        return new Request.Builder().url(url).post(body).build();
    }

    private Logger logger = LoggerFactory.getLogger(APIClient.class);

    private OkHttpClient client = new OkHttpClient();


    // 测试非历史任务 切未完成
    // TODO test pass
    //---------------------- 测试结果------------------------------
    // {"code":1,"msg":"SUCCESS","data":{"topic":"req_id20181224052529635","alreadyCompleted":[]}}
    //---------------------- 测试结果------------------------------
    @Test
    public void testPushNoFinishTask() throws IOException {

        // 测试 "facebook" 公众号
        // 在微信公众号中确定真是存在名称为facebook的公众号
        Request request = buildRequest("{\"media\":[\"facebook\"]}");

        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            Optional.ofNullable(response.body()).ifPresent(t -> {
                try {
                    System.out.println(t.string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } else {
            logger.error("操作失败！");
        }
    }

    // 测试历史任务 切已经完成
    // TODO TEST pass
    //---------------------- 测试结果------------------------------
    // {"code":1,"msg":"SUCCESS","data":{"topic":"req_id20181224052549993","alreadyCompleted":["北京理工大学研究生教育"]}}
    //---------------------- 测试结果------------------------------
    @Test
    public void testPushFinishTaskOfHistory() throws IOException {
        // 测试 "北京理工大学研究生教育" 公众号
        // 在数据库中存在记录 切已经完成 "北京理工大学研究生教育" 的文章采集任务
        Request request = buildRequest("{\"media\":[\"北京理工大学研究生教育\"]}");

        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            Optional.ofNullable(response.body()).ifPresent(t -> {
                try {
                    System.out.println(t.string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } else {
            logger.error("操作失败！");
        }
    }

    // 测试历史人物 切未完成
    // TODO TEST pass
    //---------------------- 测试结果------------------------------
    // {"code":1,"msg":"SUCCESS","data":{"topic":"req_id20181224052705979","alreadyCompleted":[]}}
    //---------------------- 测试结果------------------------------
    @Test
    public void testPushNoFinishTaskOfHistory() throws IOException {
        // 测试 "承德中公教育" 公众号
        Request request = buildRequest("{\"media\":[\"海尔家电产业\"]}");
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            Optional.ofNullable(response.body()).ifPresent(t -> {
                try {
                    System.out.println(t.string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } else {
            logger.error("操作失败！");
        }
    }

}
