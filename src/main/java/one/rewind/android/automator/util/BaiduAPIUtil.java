package one.rewind.android.automator.util;

import com.j256.ormlite.dao.Dao;
import one.rewind.android.automator.exception.InvokingBaiduAPIException;
import one.rewind.android.automator.model.BaiduTokens;
import one.rewind.db.DaoManager;
import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Create By 2018/10/19
 * Description:
 */
public class BaiduAPIUtil {
    private static Dao<BaiduTokens, String> tokensDao;

    /**
     * 初始化百度API接口的token信息
     * k: appKey
     * s: appSecret
     */
    static {
        try {
            tokensDao = DaoManager.getDao(BaiduTokens.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            return jsonObject.getString("access_token");
        } catch (Exception e) {
            System.err.print("获取token失败！");
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
            BaiduTokens token = BaiduAPIUtil.obtainToken();
            String accessToken = BaiduAPIUtil.getAuth(token.app_k, token.app_s);
            String rs = HttpUtil.post(otherHost, accessToken, params);
            return new JSONObject(rs);
        } catch (Exception e) {
            e.printStackTrace();
            throw new InvokingBaiduAPIException("百度API调用失败！");
        }
    }

    public static BaiduTokens obtainToken() throws Exception {
        synchronized (BaiduAPIUtil.class) {
            BaiduTokens var;
            BaiduTokens result = tokensDao.
                    queryBuilder().
                    where().
                    ge("count", 0).
                    and().
                    le("count", 550).
                    queryForFirst();
            var = result;
            if (DateUtils.isSameDay(result.update_time, new Date())) {
                result.count += 1;
            } else {
                result.count = 0;
            }
            result.update_time = new Date();
            result.update();
            return var;
        }
    }
}

