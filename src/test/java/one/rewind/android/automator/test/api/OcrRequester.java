package one.rewind.android.automator.test.api;

import okhttp3.OkHttpClient;

/**
 * Ocr 客户端
 *
 * @author maxuefeng [m17793873123@163.com]
 */
public class OcrRequester {

    private final OkHttpClient client = new OkHttpClient();

    /**
     * 超时时间
     */
    private int connectTimeout;

    /**
     * 默认超时时间  单位:毫秒
     */
    private final static int DEFAULT_CONNECT_TIMEOUT = 8000;


    public OcrRequester() {
        this(DEFAULT_CONNECT_TIMEOUT);
    }

    public OcrRequester(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * 上传文件
     *
     * @param image
     */
    private void upload(byte[] image) {

    }

}
