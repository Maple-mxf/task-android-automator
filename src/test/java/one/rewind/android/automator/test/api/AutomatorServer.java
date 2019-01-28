package one.rewind.android.automator.test.api;

import one.rewind.json.JSON;
import spark.Route;

import static spark.Spark.port;
import static spark.Spark.post;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class AutomatorServer {


    public static void main(String[] args) {

        port(30006);

        post("/", feed, JSON::toJson);
    }


    // 将参数生成TaskHolder
    public static Route feed = (request, response) -> {

        String paramBody = request.body();
        return null;
    };
}
