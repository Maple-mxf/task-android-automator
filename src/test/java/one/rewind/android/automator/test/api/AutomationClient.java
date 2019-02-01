package one.rewind.android.automator.test.api;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import okhttp3.*;
import one.rewind.json.JSON;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 服务客户端
 *
 * @author maxuefeng [m17793873123@163.com]
 */
public class AutomationClient {

    @Test
    public void requestAutomationServer() throws IOException {

        String url = "http://127.0.0.1:30002/feed";

        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(20000, TimeUnit.SECONDS).build();

        Map<String, Object> params = Maps.newHashMap();
        List<String> media = Lists.newArrayList();
        media.add("阿里巴巴");
        ImmutableBiMap<String, Object> map = ImmutableBiMap.of(
                "adapter_class_name", "one.rewind.android.automator.adapter.wechat.WeChatAdapter",
                "class_name", "one.rewind.android.automator.adapter.wechat.task.SubscribeMediaTask",
                "params", media
        );

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), JSON.toJson(map));

        Request request = new Request.Builder().url(url).post(body).build();

        Response response = client.newCall(request).execute();

        System.out.println(response.body().string());
    }
}
