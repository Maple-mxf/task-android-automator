package one.rewind.android.automator.test.api;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class OCRAPIClient {

    public static void main(String[] args) throws IOException {
        String url = "http://10.0.0.59:30001/ocrService";
        String result = doPost(url, "D:\\java-workplace\\wechat-android-automator\\tmp\\weixin.jpg");
        System.out.println(result);

    }

    public static String doPost(String apiUrl, String filePath)
            throws IOException {
        URL url = new URL(apiUrl);
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);

        //try里面拿到输出流，输出端就是服务器端
        try (BufferedOutputStream bos = new BufferedOutputStream(connection.getOutputStream())) {

            //我java代码是在Windows上运行的，图片路径就是下面这个
            InputStream is = new FileInputStream(filePath);
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
        String result = "";
        String getLine;

        while ((getLine = in.readLine()) != null) {
            result += getLine;
        }

        in.close();
        return result;
    }
}
