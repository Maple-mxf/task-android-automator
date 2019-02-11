package one.rewind.android.automator.adapter.wechat.test;

import io.netty.handler.codec.http.HttpMethod;
import one.rewind.android.automator.adapter.wechat.util.ReqObj;
import one.rewind.io.requester.basic.BasicRequester;
import one.rewind.io.requester.task.Task;
import one.rewind.json.JSON;
import one.rewind.util.FileUtil;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author scisaga@gmail.com
 * @date 2019/2/11
 */
public class GenerateRequestTest {

	public static String parseHtml(String src) {

		Pattern pattern = Pattern.compile("(?<=var msgList = ').+?(?=';)");
		Matcher matcher = pattern.matcher(src);

		if(matcher.find()) {
			src = matcher.group();
		}

		return StringEscapeUtils.unescapeHtml4(src).replaceAll("\\\\", "");
	}

	public static String parseJson(String src) {

		Pattern pattern = Pattern.compile("^\\{.+?\\}$");
		Matcher matcher = pattern.matcher(src);

		if(matcher.find()) {
			src = matcher.group();
		}

		return StringEscapeUtils.unescapeHtml4(src).replaceAll("\\\\", "");
	}

	@Test
	public void testParseHtml() {

		String src = FileUtil.readFileByLines("tmp/wx/拍拍贷历史文章列表.html");
		System.out.println(parseHtml(src));
	}

	@Test
	public void testParseJson() {

		String src = FileUtil.readFileByLines("tmp/wx/拍拍贷历史文章列表.page2.js");
		System.out.println(parseJson(src));
	}

	@Test
	public void testReplayRequest() throws MalformedURLException, URISyntaxException, UnsupportedEncodingException {

		String src = FileUtil.readFileByLines("tmp/wx/res/EssayList-1.html");
		ReqObj reqObj = JSON.fromJson(src, ReqObj.class);

		Task t = new Task(reqObj.url/*.replace("offset=10", "offset=20")*/);
		if(reqObj.method.equals(HttpMethod.POST)) t.setPost();
		t.setHeaders(reqObj.headers);
		//t.setProxy(new ProxyImpl("reid.red", 60103, null, null));

		BasicRequester.getInstance().submit(t);
		System.err.println(t.getResponse().getText());
		/*System.err.println(parseJson(t.getResponse().getText()));*/
	}

	@Test
	public void testGenerateEssayRequest() throws MalformedURLException, URISyntaxException, UnsupportedEncodingException {

		String src = FileUtil.readFileByLines("tmp/wx/res/EssayList-1.html");
		ReqObj reqObj = JSON.fromJson(src, ReqObj.class);

		Task t = new Task(reqObj.url/*.replace("offset=10", "offset=20")*/);
		if(reqObj.method.equals(HttpMethod.POST)) t.setPost();
		t.setHeaders(reqObj.headers);
		//t.setProxy(new ProxyImpl("reid.red", 60103, null, null));

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
}
