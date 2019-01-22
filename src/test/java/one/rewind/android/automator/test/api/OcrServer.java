package one.rewind.android.automator.test.api;

import one.rewind.json.JSON;
import spark.Route;
import spark.Spark;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Paths;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class OcrServer {

//    public static


    public static void main(String[] args) {

        Spark.port(8080);

        Spark.threadPool(10);

        Spark.post("/ocr/service", ocrService, JSON::toJson);
    }


    public static Route ocrService = (request, response) -> {

        HttpServletRequest servletRequest = request.raw();

        // 获取part
        Part requestPart = servletRequest.getPart("file");
        String fileName = Paths.get(requestPart.getSubmittedFileName()).getFileName().toString();

        // 判断fileName
        int splitIndex = fileName.lastIndexOf(".");


        ServletInputStream is = (ServletInputStream) requestPart.getInputStream();
        OutputStream os = new FileOutputStream("");
        BufferedOutputStream bOutputStream = new BufferedOutputStream(os);

        byte[] buf = new byte[1024];

        int length = 0;

        length = is.readLine(buf, 0, buf.length);//使用sis的读取数据的方法

        while (length != -1) {
            bOutputStream.write(buf, 0, length);
            length = is.read(buf);
        }
        is.close();
        bOutputStream.close();
        os.close();
        return null;
    };

}
