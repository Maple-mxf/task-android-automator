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
}
