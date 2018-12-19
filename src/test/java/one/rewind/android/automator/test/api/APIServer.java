package one.rewind.android.automator.test.api;

import one.rewind.io.server.Msg;
import one.rewind.json.JSON;
import spark.Spark;

import java.util.Map;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class APIServer {

    public static void main(String[] args) {

        Spark.port(8080);
        Spark.post("/push", (req, resp) -> {
            String body = req.body();

            Map map = JSON.fromJson(body, Map.class);

            System.out.println(map);

            return new Msg<>(1, "Hello world");
        });
    }
}
