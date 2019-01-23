package one.rewind.android.automator.test.api;

import com.dw.ocr.client.OCRClient;
import com.dw.ocr.parser.OCRParser;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import okhttp3.*;
import one.rewind.json.JSON;
import one.rewind.util.FileUtil;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author maxuefeng [m17793873123@163.com]   安卓自动化爬虫接口测试用例   避免进入看图模式：多点击一次页面
 */
public class APIClient {

    private final String fetchUrl = "http://192.168.8.104:56789/fetch";
    private final String subscribeUrl = "http://192.168.8.104:56789/subscribe";

    private Request buildRequest(String json) {
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
        return new Request.Builder().url(fetchUrl).post(body).build();
    }

    private Logger logger = LoggerFactory.getLogger(APIClient.class);

    private OkHttpClient client = new OkHttpClient.Builder().connectTimeout(1000, TimeUnit.SECONDS).callTimeout(1000, TimeUnit.SECONDS).readTimeout(1000, TimeUnit.SECONDS).build();


    /**
     * 未指定设备,测试非历史任务 切未完成
     * TODO test pass
     * ---------------------- 测试结果------------------------------
     * {
     * "code": 1,
     * "msg": "SUCCESS",
     * "data": {
     * "media_result": [{
     * "is_follow": false,
     * "last": "",
     * "is_finish": false,
     * "is_finish_history": false,
     * "media": "阿里巴巴",
     * "in_queue": false
     * }],
     * "topic": "Android-Automator-Topic-20190104152236676"
     * }
     * }
     */
    @Test
    public void testPushNoFinishTask() throws IOException {

        // 测试 "facebook" 公众号
        // 在微信公众号中确定真是存在名称为facebook的公众号
        Request request = buildRequest("{\"media\":[\"定投十年赚十倍\"]}");

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

    /**
     * 未指定设备,测试历史任务,切已经完成  TODO TEST pass
     * <p>
     * {
     * "code": 1,
     * "msg": "SUCCESS",
     * "data": {
     * "media_result": [{
     * "is_follow": true,
     * "last": "2018-11-14 11:50:01",
     * "is_finish": false,
     * "is_finish_history": true,
     * "media": "北京理工大学研究生教育",
     * "in_queue": false
     * }],
     * "topic": "Android-Automator-Topic-20190104152555867"* 	}
     * }
     *
     * @throws IOException read ex
     */
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

    /**
     * TODO TEST pass
     * 未指定设备,测试历史任务(已订阅但未完成),切未完成
     * {
     * "code": 1,
     * "msg": "SUCCESS",
     * "data": {
     * "media_result": [{
     * "is_follow": false,
     * "last": "",
     * "is_finish": false,
     * "is_finish_history": false,
     * "media": "鹿鸣财经",
     * "in_queue": false
     * }],
     * "topic": "Android-Automator-Topic-20190104152747076"* 	}
     * }
     *
     * @throws IOException read ex
     */
    @Test
    public void testPushNoFinishTaskOfHistory() throws IOException {
        // 测试 "承德中公教育" 公众号
//		Request request = buildRequest("{\"media\":[\"海尔家电产业\"]}");
        Request request = buildRequest("{\"media\":[\"鹿鸣财经\"]}");
        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            if (response.body() != null) {
                System.out.println(response.body().string());
            }
        } else {
            logger.error("操作失败！");
        }
    }


    /**
     * 通过读取文件批量输入到API接口中
     */
    @Test
    public void testGoodMedia() throws IOException {

        Request request = buildRequest(JSON.toJson(ImmutableMap.of(
                "media", media()
        )));

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
            logger.error("Response code: {}", response.code());
        }

    }


    public List<String> media() throws IOException {
        // 读取文件
        File file = new File("/usr/local/media.txt");
        return Files.readLines(file, Charset.forName("GBK"));
    }


    /**
     * 指定设备采集数据  采集任务当前数据库中和redis队列中不存在的公众号
     * TODO TEST pass
     * <p>
     * 备注:谷歌开发者公众号在任务队列,历史任务中都不存在
     * 测试成功结果
     * {
     * "code": 1,
     * "msg": "SUCCESS",
     * "data": {
     * "media_result": [{
     * "is_follow": false,
     * "last": "",
     * "is_finish": false,
     * "is_finish_history": false,
     * "media": "谷歌开发者",
     * "udid": "ZX1G22PQLH",
     * "in_queue": false
     * }],
     * "topic": "Android-Automator-Topic-20190104083039214"* 	}
     * }
     * 测试失败结果
     * {
     * "code": 0,
     * "msg": "FAILURE",
     * "data": "设备不存在或者！"
     * }
     */
    @Test
    public void specifyUdidAndNoRedisTask() throws IOException {

        String body = "{\"udid\":\"ZX1G22PQLH\",\"media\":[\"谷歌开发者\"]}";

        Request request = buildRequest(body);
        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            if (response.body() != null) {
                System.out.println(response.body().string());
            }
        } else {
            logger.error("操作失败！");
        }
    }

    /**
     * 指定设备采集数据 采集任务在redis队列中存在,但是没有任何设备订阅的公众号
     * <p>
     * 备注:  "中诚宝捷思货币经纪有限公司" 公众号是任何一个设备没有订阅采集的,并且在redis任务队列中不存在
     * <p>
     * {
     * "code": 1,
     * "msg": "SUCCESS",
     * "data": {
     * "media_result": [{
     * "is_follow": true,
     * "last": "2019-01-04 08:28:45",
     * "is_finish": false,
     * "is_finish_history": false,
     * "media": "中诚宝捷思货币经纪有限公司",
     * "udid": "ZX1G22PQLH",
     * "in_queue": false
     * }],
     * "topic": "Android-Automator-Topic-20190104083001661"* 	}
     * }
     */
    @Test
    public void specifyUdidAndRedisTask() throws IOException {

        String body = "{\"udid\":\"ZX1G22PQLH\",\"media\":[\"中诚宝捷思货币经纪有限公司\"]}";
        Request request = buildRequest(body);
        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            if (response.body() != null) {
                System.out.println(response.body().string());
            }
        } else {
            logger.error("操作失败！");
        }
    }

    /**
     * 指定设备采集数据 采集任务是当前设备的历史任务 并且之前就已经完成了
     * <p>
     * 备注:  "第一财经商业数据中心" 公众号是当前让设备之前完成的任务
     * 测试结果:
     * {
     * "code": 1,
     * "msg": "SUCCESS",
     * "data": {
     * "media_result": [{
     * "is_follow": true,
     * "last": "2019-01-04 08:28:45",
     * "is_finish": false,
     * "is_finish_history": false,
     * "media": "中诚宝捷思货币经纪有限公司",
     * "udid": "ZX1G22PQLH",
     * "in_queue": false
     * }],
     * "topic": "Android-Automator-Topic-20190104082919968"* 	}
     * }
     */
    @Test
    public void specifyUdidAndHistoryTask() throws IOException {

        String body = "{\"udid\":\"ZX1G22PQLH\",\"media\":[\"中诚宝捷思货币经纪有限公司\"]}";
        Request request = buildRequest(body);
        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            if (response.body() != null) {
                System.out.println(response.body().string());
            }
        } else {
            logger.error("操作失败！");
        }
    }

    /**
     * 指定设备采集数据 采集任务其他设备订阅的公众号 并且当前指定的公众号任务已经完成
     * <p>
     * 备注:  "高校筹资联盟" 是另外一个设备订阅的公众号
     * 测试结果:
     * {
     * "code": 1,
     * "msg": "SUCCESS",
     * "data": {
     * "media_result": [{
     * "is_follow": true,
     * "last": "2019-01-03 11:50:01",
     * "is_finish": false,
     * "is_finish_history": false,
     * "media": "高校筹资联盟",
     * "udid": "ZX1G22PQLH",
     * "in_queue": false
     * }],
     * "topic": "Android-Automator-Topic-20190104082800580"* 	}
     * }
     */
    @Test
    public void specifyUdidAndNoCurrentDeviceAndFinish() throws IOException {

        String url = "http://192.168.8.100:56789/specific";
        String body = "{\"udid\":\"ZX1G22PQLH\",\"media\":[\"高校筹资联盟\"]}";
        Request request = buildRequest(body);
        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            if (response.body() != null) {
                System.out.println(response.body().string());
            }
        } else {
            logger.error("操作失败！");
        }
    }

    /**
     * 指定设备采集数据 采集任务其他设备订阅的公众号 并且当前指定的公众号任务没有完成完成
     * <p>
     * 测试结果:
     * {
     * "code": 1,
     * "msg": "SUCCESS",
     * "data": {
     * "media_result": [{
     * "is_follow": true,
     * "last": "2019-01-03 06:57:55",
     * "is_finish": false,
     * "is_finish_history": false,
     * "media": "下注的快感",
     * "udid": "ZX1G22PQLH",
     * "in_queue": false
     * }],
     * "topic": "Android-Automator-Topic-20190104082701805"* 	}
     * }
     *
     * @throws IOException read ioex
     */
    @Test
    public void specifyUdidAndNoCurrentDeviceAndNoFinish() throws IOException {

        String body = "{\"udid\":\"ZX1G22PQLH\",\"media\":[\"青鸟报修\"]}";
        Request request = buildRequest(body);
        
        Response response = client.newCall(request).execute();

        if (response.isSuccessful()) {
            if (response.body() != null) {
                System.out.println(response.body().string());
            }
        } else {
            logger.error("操作失败！");
        }
    }

    /**
     * @throws IOException
     */
    @Test
    public void testOCRClient() throws IOException {


        List<OCRParser.TouchableTextArea> textArea = OCRClient.getInstance().getTextBlockArea(FileUtil.readBytesFromFile("D:\\java-workplace\\wechat-android-automator\\tmp\\微信图片_20190122111641.jpg"));

        System.out.println(JSON.toPrettyJson(textArea));
    }
}
