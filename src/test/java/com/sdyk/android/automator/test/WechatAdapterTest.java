package com.sdyk.android.automator.test;

import com.sdyk.android.automator.AndroidDevice;
import com.sdyk.android.automator.adapter.WechatAdapter;
import com.sdyk.android.automator.model.WechatEssay;
import com.sdyk.android.automator.model.WechatEssayComment;
import com.sdyk.android.automator.model.WechatPublicAccount;
import com.sdyk.android.automator.util.AppInfo;
import net.lightbody.bmp.filters.RequestFilter;
import net.lightbody.bmp.filters.ResponseFilter;
import one.rewind.json.JSON;
import one.rewind.util.FileUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class WechatAdapterTest {

	String udid = "ZX1G323GNB";
	int appiumPort = 47454;
	int localProxyPort = 48454;
	AndroidDevice device;
	WechatAdapter adapter;

	List<String> publicAccounts;

	/**
	 * 初始化设备
	 * @throws Exception
	 */
	@Before
	public void setup() throws Exception {

		device = new AndroidDevice(udid, appiumPort);

		//device.removeWifiProxy();
		device.startProxy(localProxyPort);
		device.setupWifiProxy();

		/**
		 * TODO 请求过滤器
		 */
		RequestFilter requestFilter = (request, contents, messageInfo) -> {

			String url = messageInfo.getOriginalUrl();

			if(url.contains("https://mp.weixin.qq.com/s"))
				System.out.println(" . " + url);

			return null;
		};

		Stack<String> content_stack = new Stack<>();
		Stack<String> stats_stack = new Stack<>();
		Stack<String> comments_stack = new Stack<>();

		/**
		 * TODO 返回过滤器
		 */
		ResponseFilter responseFilter = (response, contents, messageInfo) -> {

			String url = messageInfo.getOriginalUrl();

			if(contents != null && (contents.isText() || url.contains("https://mp.weixin.qq.com/s"))) {

				try {

					// 正文
					if (url.contains("https://mp.weixin.qq.com/s")) {
						System.err.println(" : " + url);
						content_stack.push(contents.getTextContents());
					}
					// 统计信息
					else if (url.contains("getappmsgext")) {
						System.err.println(" :: " + url);
						stats_stack.push(contents.getTextContents());
					}
					// 评论信息
					else if (url.contains("appmsg_comment?action=getcomment")) {
						System.err.println(" ::: " + url);
						comments_stack.push(contents.getTextContents());
					}

					if (content_stack.size() >= 1 && stats_stack.size() >= 1 && comments_stack.size() >= 1) {

						System.err.println("Fully received.");

						String content_src = content_stack.pop();

						String stats_src = stats_stack.pop();

						String comments_src = comments_stack.pop();

						WechatEssay we = new WechatEssay().parseContent(content_src).parseStat(stats_src);

						System.err.println(we.title);

						we.insert();

						List<WechatEssayComment> comments_ = WechatEssayComment.parseComments(we.mid, comments_src);

						System.err.println(comments_.size());

						comments_.stream().forEach(c -> {
							try {
								c.insert();
							} catch (Exception e) {
								e.printStackTrace();
							}
						});
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		};

		device.setProxyRequestFilter(requestFilter);
		device.setProxyResponseFilter(responseFilter);

		// 从AppInfo中选择需要启动的程序
		AppInfo appInfo = AppInfo.get(AppInfo.Defaults.WeChat);

		device.initAppiumServiceAndDriver(appInfo);

		adapter = new WechatAdapter(device);

		Thread.sleep(3000);
	}

	@Test
	public void loadPublicAccounts() {
		publicAccounts = Arrays.asList(FileUtil.readFileByLines("data/wechat_public_accounts.txt")
				.split("\r\n|\n"));

		System.out.println("Total " + publicAccounts.size() + " public accounts.");
	}

	@Test
	public void testAddPublicAccounts() throws Exception {

		loadPublicAccounts();

		for(String name : publicAccounts) {
			//if(name.equals("IPP评论"))

			WechatPublicAccount wpa = WechatPublicAccount.getByName(name);
			if(wpa == null)
				adapter.addPublicAccount(name);
		}
	}

	@Test
	public void testGetIntoPublicAccount() throws InterruptedException {

		loadPublicAccounts();

		for(String name : publicAccounts) {
			adapter.getIntoPublicAccountEssayList(name);
		}

		Thread.sleep(100000000);
	}
}
