package one.rewind.android.automator.test;

import com.google.common.collect.Lists;
import one.rewind.io.requester.task.Task;
import org.json.JSONObject;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Create By 2018/12/6
 * Description:
 */
public class CrawlerAPITest {

    @Test
    public void testPushMedias() throws MalformedURLException, URISyntaxException {

        List<String> medias = Lists.newArrayList();

        medias.add("阿里巴巴");

        String jsonStr = JSONObject.valueToString(medias);


        Task task = new Task("http://127.0.0.1:4567/push", jsonStr);

        Task.Response response = task.getResponse();

        String text = response.getText();

        JSONObject o = new JSONObject(text);

        System.out.println(o);

    }
}
