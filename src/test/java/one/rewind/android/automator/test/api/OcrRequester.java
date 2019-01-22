package one.rewind.android.automator.test.api;

import one.rewind.android.automator.ocr.OCRParser;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Ocr 客户端
 *
 * @author maxuefeng [m17793873123@163.com]
 */
public class OcrRequester {

    public static Logger logger = LogManager.getLogger(OcrRequester.class);

    public static final String Ocr_Service_Url = "http://10.0.0.59:30001/ocr/service";

    /**
     * 超时时间 超时重试
     */
    private int connectTimeout;

    /**
     * 读取结果超时时间
     */
    private int readTimeout;

    /**
     * 失败重试次数
     */
    private int maxRetry;

    /**
     * 文件
     */
    private File file;

    /**
     * 默认超时时间  单位:毫秒 milliseconds
     */
    private final static int DEFAULT_CONNECT_TIMEOUT = 15000;

    /**
     * 默认读取response超时时间 单位:毫秒 milliseconds
     */
    private final static int DEFAULT_READ_TIMEOUT = 15000;

    /**
     * 默认重试次数
     */
    private final static int DEFAULT_MAX_RETRY = 5;


    public OcrRequester(File file) {
        this(DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT, file, DEFAULT_MAX_RETRY);
    }

    public OcrRequester(int connectTimeout, int readTimeout, File file, int maxRetry) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.file = file;
        this.maxRetry = maxRetry;
    }

    /**
     * 发送ocr 请求
     *
     * @return
     * @throws IOException
     */
    public Body execute() throws IOException {

        Body body = new Body();

        for (int i = 0; i < this.maxRetry; i++) {
            Response response = call(file);

            if (response.isSuccessful()) {
                String json = response.string();
                List<OCRParser.TouchableTextArea> data = (List<OCRParser.TouchableTextArea>) JSON.fromJson(json, Map.class).get("data");
                body.textAreas = data;
            } else {
                body.errors.push(response);
            }
        }
        return body;
    }


    private Response call(File file) throws IOException {

        // 检测File是否存在
        if (!file.exists()) {
            throw new IllegalStateException("file not exist！cause：[{" + file.getAbsolutePath() + "}]");
        }

        URL url = new URL(Ocr_Service_Url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setReadTimeout(readTimeout);
        connection.setConnectTimeout(connectTimeout);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");

        //try里面拿到输出流，输出端就是服务器端
        try (BufferedOutputStream bos = new BufferedOutputStream(connection.getOutputStream())) {

            InputStream is = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(is);

            byte[] buf = new byte[1024];
            int length;
            length = bis.read(buf);
            while (length != -1) {
                bos.write(buf, 0, length);
                length = bis.read(buf);
            }
            bis.close();
            bos.close();
            is.close();
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        // TODO  StringBuilder
        StringBuffer result = new StringBuffer();
        String getLine;

        while ((getLine = in.readLine()) != null) {
            result.append(getLine);
        }

        in.close();

        return new Response(result.toString(), connection.getResponseCode());
    }

    public static class Response implements JSONable<Body> {

        private String result;

        private int code;

        private String cause;

        public Response(String result, int code) {
            this.result = result;
            this.code = code;
            if (!isSuccessful()) {
                Document doc = Jsoup.parse(this.result);
                Elements els = doc.getElementsByTag("h2");
                cause = (els.get(0) != null) ? els.get(0).text() : "";
            }
        }

        private boolean isSuccessful() {
            return code == 200;
        }

        public String string() {
            return this.result;
        }

        public int code() {
            return this.code;
        }

        @Override
        public String toJSON() {
            return JSON.toJson(this);
        }
    }


    /**
     *
     */
    public static class Body implements JSONable<Body> {

        public List<OCRParser.TouchableTextArea> textAreas;

        public Stack<Response> errors = new Stack<>();

        @Override
        public String toJSON() {
            return JSON.toJson(this);
        }
    }

}
