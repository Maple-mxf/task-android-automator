package one.rewind.android.automator.adapter.wechat.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpMethod;
import one.rewind.android.automator.adapter.wechat.util.EssayProcessor;
import one.rewind.android.automator.adapter.wechat.util.ReqObj;
import one.rewind.io.requester.basic.BasicDistributor;
import one.rewind.io.requester.basic.BasicRequester;
import one.rewind.io.requester.proxy.ProxyImpl;
import one.rewind.io.requester.task.Task;
import one.rewind.io.requester.task.TaskHolder;
import one.rewind.json.JSON;
import one.rewind.util.FileUtil;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author scisaga@gmail.com
 * @date 2019/2/11
 */
public class GenerateRequestTest {

    @Test
    public void testReplayRequest() throws MalformedURLException, URISyntaxException, UnsupportedEncodingException {

        String src = FileUtil.readFileByLines("tmp/wx/res/EssayStat-0.html");
        ReqObj reqObj = JSON.fromJson(src, ReqObj.class);

        Task t = new Task(reqObj.url/*.replace("offset=10", "offset=20")*/);
        t.setPost_data(reqObj.req);
        t.setPost();
        t.setHeaders(reqObj.headers);

        //t.setProxy(new ProxyImpl("reid.red", 60103, null, null));

        BasicRequester.getInstance().submit(t);
        FileUtil.writeBytesToFile(t.getResponse().getSrc(), "tmp/test1.html");
        System.err.println(t.getResponse().getText());
    }

    @Test
    public void testGenerateEssayRequest() throws MalformedURLException, URISyntaxException, UnsupportedEncodingException {

        String src = FileUtil.readFileByLines("tmp/wx/res/EssayList-18.html");
        ReqObj reqObj = JSON.fromJson(src, ReqObj.class);

        Task t = new Task(reqObj.url/*.replace("offset=10", "offset=20")*/);
        if (reqObj.method.equals(HttpMethod.POST)) {
            t.setPost();
            t.setPost_data(reqObj.req);
        }
        t.setHeaders(reqObj.headers);
        t.setProxy(new ProxyImpl("reid.red", 60103, null, null));

        BasicRequester.getInstance().submit(t);
        System.err.println(t.getResponse().getText());
        /*System.err.println(parseJson(t.getResponse().getText()));*/
    }

    @Test
    public void uriTest() throws URISyntaxException {

        String url = "https://mp.weixin.qq.com/s?__biz=MjM5NDM1Mzc4MQ==&mid=2651794199&idx=1&sn=35240dbc3ad45c70deec48d21780dde5&chksm=bd728c0d8a05051b9fc7c02a580e1e1ca41f5d9dac3828ebd9c838da6a530d4d18e6e4e7d50b&scene=4&subscene=126&ascene=0&devicetype=android-25&version=2607033d&nettype=WIFI&abtest_cookie=BQABAAoACwASABMAFAAFACOXHgBamR4Am5keAJ2ZHgDRmR4AAAA%3D&lang=zh_CN&pass_ticket=jCjaRMi4QjnTLCurp8Oy%2BFa8%2FWw7Pd6VHCM%2BcSgeQh%2B8VIJg%2Bv4zhV9xML7%2BXqRH&wx_header=1";

        URI uri = new URI(url);

        System.out.println(uri.getHost());
        System.out.println(uri.getPath());

    }

    @Test
    public void testEncode() throws UnsupportedEncodingException {

        String s = "SqivLFxBvIbBoK7hL3RWJjbMxrnIZG%252By4u6XPN%252BSTNQ0WJxXI64s98DA8SOBs6cM";
        System.err.println(URLEncoder.encode(s, "UTF-8"));

        System.err.println(URLDecoder.decode("SqivLFxBvIbBoK7hL3RWJjbMxrnIZG%252By4u6XPN%252BSTNQ0WJxXI64s98DA8SOBs6cM", "UTF-8"));

    }

    @Test
    public void testParseListContent() throws Exception {

        Class.forName(EssayProcessor.class.getName());

        ReqObj reqObj0 = JSON.fromJson(FileUtil.readFileByLines("tmp/wx/res/EssayList-0.html"), ReqObj.class);
        ReqObj reqObj1 = JSON.fromJson(FileUtil.readFileByLines("tmp/wx/res/EssayList-1.html"), ReqObj.class);
        ReqObj reqObj2 = JSON.fromJson(FileUtil.readFileByLines("tmp/wx/res/EssayContent-0.html"), ReqObj.class);

        EssayProcessor ep = new EssayProcessor("", "拍拍贷", reqObj0, reqObj1, reqObj2);

        List<TaskHolder> nths = new ArrayList<>();
        ep.getEssayTH(reqObj0.res, "拍拍贷", nths);

        /*System.out.println(JSON.toPrettyJson(nths.get(9)));*/

        nths.forEach(th -> {
            try {
                BasicDistributor.getInstance().submit(th);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

		/*Task<Task> t = nths.get(9).build();

		t.setHeaders(reqObj1.headers);
		t.setCookies(reqObj1.getCookies().getCookies(t.url));

		BasicRequester.getInstance().submit(t);
		for(one.rewind.io.requester.callback.NextTaskGenerator callback : t.nextTaskGenerators) {
			callback.run(t, new ArrayList<>());
		}*/

        Thread.sleep(1000000);
    }

    @Test
    public void testGenerateNextPage() throws Exception {

        Class.forName(EssayProcessor.class.getName());

        ReqObj reqObj0 = JSON.fromJson(FileUtil.readFileByLines("tmp/wx/res/EssayList-0.html"), ReqObj.class);
        ReqObj reqObj1 = JSON.fromJson(FileUtil.readFileByLines("tmp/wx/res/EssayList-1.html"), ReqObj.class);
        ReqObj reqObj2 = JSON.fromJson(FileUtil.readFileByLines("tmp/wx/res/EssayContent-0.html"), ReqObj.class);

        EssayProcessor ep = new EssayProcessor("", "火山财富", reqObj0, reqObj1, reqObj2);

        ep.call();

        Thread.sleep(100000000);
    }

    @Test
    public void testDecode() {

        String src = FileUtil.readFileByLines("tmp/test1.html");
        src = StringEscapeUtils.unescapeHtml4(StringEscapeUtils.unescapeHtml4(src)).replaceAll("\\\\", "").replaceAll("(?si)^.+?\"general_msg_list\":\"", "").replaceAll("(?si)\",\"next_offset\":20.+?$", "");

        System.out.println(src);

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode jsonNode = mapper.readTree(src);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
