package one.rewind.android.automator.api.client;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.StringJoiner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Client 根据数据为安卓自动化爬虫指定适当的任务
 * <p>
 * Client的第一种任务类型是接受采集文章的历史数据
 * Client的第二种任务类型是定时器任务，定时向爬虫系统提交任务
 *
 * @author maxuefeng [m17793873123@163.com]
 */
public class AndroidClient {

    private static Logger logger = LogManager.getLogger(AndroidClient.class.getName());

    private static OkHttpClient client = new OkHttpClient.Builder().connectTimeout(1000, TimeUnit.SECONDS).readTimeout(1000, TimeUnit.SECONDS).callTimeout(1000, TimeUnit.SECONDS).build();


    public static Request getRequest(String urlSuffix, String json) {
        String baseUrl = "http://127.0.0.1:4567/android_automator";
        StringJoiner joiner = new StringJoiner("/").add(baseUrl).add(urlSuffix);
        return new Request.Builder().url(joiner.toString()).post(RequestBody.create(MediaType.parse("application/json"), json)).build();
    }

    static {
        // 加载定时任务

        Timer timer = new Timer("real-time-task", true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {

            }
        },1);
    }



    public static void main(String[] args) {

    }


}






























