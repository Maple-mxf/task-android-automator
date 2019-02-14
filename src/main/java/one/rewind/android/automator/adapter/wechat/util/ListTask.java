package one.rewind.android.automator.adapter.wechat.util;

import com.google.common.collect.ImmutableMap;
import one.rewind.io.requester.basic.BasicRequester;
import one.rewind.io.requester.chrome.ChromeTask;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

/**
 * @author scisaga@gmail.com
 * @date 2019/2/14
 */
public class ListTask extends ChromeTask {
	static {

		registerBuilder(
				ChromeTask.class,
				"{{url}}",
				ImmutableMap.of("url", "String"),
				ImmutableMap.of("url", ""),
				false,
				Priority.HIGH,
				60 * 60 * 1000L
		);
	}

	int offset = 10;

	public ListTask(String url) throws MalformedURLException, URISyntaxException {

		super(url);

		// 设定 Header

		// 设定 cookie

		offset = Integer.parseInt(url.replaceAll("^.+?offset=", "").replaceAll("&count.+?$", ""));

		addNextTaskGenerator((t, nts) -> {

			// 解析内容

			// 生成下一级别

			// 更新Cookie

		});

	}

	public static void main(String[] args) throws Exception {

		String baseUrl = "https://mp.weixin.qq.com/mp/profile_ext?action=getmsg&__biz=MjM5NDM1Mzc4MQ==&f=json&offset=10&count=10&is_ok=1&scene=126&uin=777&key=777&pass_ticket=SqivLFxBvIbBoK7hL3RWJjbMxrnIZG%2By4u6XPN%2BSTNQ0WJxXI64s98DA8SOBs6cM&wxtoken=&appmsg_token=996_jsxgGc4TCc1lFissbhxZ6Qm6VjTU1SkJgsiEbQ~~&x5=1&f=json";

		BasicRequester.getInstance().submit(ChromeTask.at(ListTask.class, ImmutableMap.of("url", baseUrl)).build());

	}
}
