package one.rewind.android.automator.util;

import com.google.common.collect.Maps;
import one.rewind.android.automator.exception.InvokingBaiduAPIException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * Create By 2018/10/19
 * Description:
 */
public class BaiduAPIUtil {

    static {
        initTokens();
    }

    private static ConcurrentMap<String, String> tokens = Maps.newConcurrentMap();

    private static ConcurrentMap<String, String> currentToken = Maps.newConcurrentMap();


    /**
     * 初始化百度API接口的token信息
     * k: appKey
     * s: appSecret
     */
    static void initTokens() {
        tokens.put("rDztaDallgGp5GkiZ7mPBUwo", "em7eA1tsCXyqm0HdD83dMwsyG0gSU77n");
        currentToken.clear();
        tokens.forEach((k, v) -> {
            currentToken.put(k, v);
            return;
        });
    }

    /**
     * 获取接口的信任信息
     *
     * @param ak
     * @param sk
     * @return
     */
    private static String getAuth(String ak, String sk) {
        // 获取token地址
        String authHost = "https://aip.baidubce.com/oauth/2.0/token?";
        String getAccessTokenUrl = authHost
                // 1. grant_type为固定参数
                + "grant_type=client_credentials"
                // 2. 官网获取的 API Key
                + "&client_id=" + ak
                // 3. 官网获取的 Secret Key
                + "&client_secret=" + sk;
        try {
            URL realUrl = new URL(getAccessTokenUrl);
            // 打开和URL之间的连接
            HttpURLConnection connection = (HttpURLConnection) realUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            // 获取所有响应头字段
            Map<String, List<String>> map = connection.getHeaderFields();
            // 遍历所有的响应头字段
            for (String key : map.keySet()) {
                System.err.println(key + "--->" + map.get(key));
            }
            // 定义 BufferedReader输入流来读取URL的响应
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String result = "";
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
            /**
             * 返回结果示例
             */
            System.err.println("result:" + result);
            JSONObject jsonObject = new JSONObject(result);
            String access_token = jsonObject.getString("access_token");
            return access_token;
        } catch (Exception e) {
            System.err.printf("获取token失败！");
            e.printStackTrace(System.err);
        }
        return null;
    }

    /**
     * 发起请求到百度服务器进行图像识别
     *
     * @param filePath
     * @return
     */
    public static JSONObject executeImageRecognitionRequest(String filePath) throws InvokingBaiduAPIException {
        try {
            String otherHost = "https://aip.baidubce.com/rest/2.0/ocr/v1/general";
            byte[] imgData = FileUtil.readFileByBytes(filePath);
            String imgStr = Base64Util.encode(imgData);
            String params = URLEncoder.encode("image", "UTF-8") + "=" + URLEncoder.encode(imgStr, "UTF-8");
            /**
             * 线上环境access_token有过期时间， 客户端可自行缓存，过期后重新获取。
             */
            if (currentToken.size() > 1) {
                throw new RuntimeException("当前token设置的太多了...");
            }
            Set<String> keySet = currentToken.keySet();
            StringBuilder key = new StringBuilder();
            StringBuilder secret = new StringBuilder();
            for (String var : keySet) {
                key.append(var);
                secret.append(currentToken.get(var));
            }
            String accessToken = BaiduAPIUtil.getAuth(key.toString(), secret.toString());
            return new JSONObject(HttpUtil.post(otherHost, accessToken, params));
        } catch (Exception e) {
            e.printStackTrace();
            throw new InvokingBaiduAPIException("百度API调用失败！");
        }
    }

    /**
     * 检查百度API是否可用
     * {"err_msg":}
     *
     * @param jsonObject
     * @return
     */
    public static boolean insepectRateLimit(JSONObject jsonObject) {
        try {
            JSONArray array = (JSONArray) jsonObject.get("words_result");
            return array != null;
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * 切换当前token
     */
    public static JSONObject switchToken(String filePath) {

        currentToken.clear();

        for (Map.Entry<String, String> entry : tokens.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            try {
                JSONObject jsonObject = BaiduAPIUtil.executeImageRecognitionRequest(filePath);

                if (jsonObject.get("err_msg") == null) {
                    currentToken.put(k, v);
                    return jsonObject;
                }
            } catch (InvokingBaiduAPIException e) {
                e.printStackTrace();

            }
        }
        return null;
    }
}
