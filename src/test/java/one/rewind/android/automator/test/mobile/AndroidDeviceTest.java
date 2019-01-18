package one.rewind.android.automator.test.mobile;

import one.rewind.android.automator.AndroidDevice;
import org.junit.Before;
import org.junit.Test;

public class AndroidDeviceTest {

//	String udid = "ZX1G323GNB";
	String udid = "9YJ7N17429007528";
	int appiumPort = 47454;
	int localProxyPort = 48454;
	AndroidDevice device;

	/**
	 * 初始化设备
	 * @throws Exception
	 */
	@Before
	public void setup() throws Exception {

		device = new AndroidDevice(udid);
		device.removeRemoteWifiProxy();

		// 从AppInfo中选择需要启动的程序
		/*AppInfo appInfo = AppInfo.get(AppInfo.Defaults.Contacts);

		device.initAppiumServiceAndDriver(appInfo);

		Thread.sleep(3000);*/
	}

	@Test
	public void voidTest() {

	}

	/**
	 * 测试安装APK
	 */
	@Test
	public void testInstallApk() {
		System.out.println(device);

		System.out.println(device.getHeight());
		System.out.println(device.getWidth());

		String apkPath = "com.facebook.katana_180.0.0.35.82_free-www.apkhere.com.apk";
		device.installApk(apkPath);
		device.listApps();

		// TODO 测试删除APK
	}

	/**
	 * 测试通讯录功能
	 * @throws InterruptedException
	 */
	/*@Test
	public void testAddContact() throws InterruptedException {

		String name = "name";
		String number = "123456";
		String filePath = "newFriend.txt";

		AppInfo info = AppInfo.get(AppInfo.Defaults.Contacts);
		device.startApp(info);

		ContactsAdapter ca = new ContactsAdapter(device);
		ca.clearContacts();
		ca.addContact(name, number);
		ca.addContactsFromFile(filePath);
		ca.deleteOneContact(name);
	}

	@Test
	public void testProxyFilters() throws InterruptedException {

		device.startProxy(localProxyPort);
		device.setupRemoteWifiProxy();

		*//**
		 * TODO 请求过滤器
		 *//*
		RequestFilter requestFilter = (request, contents, messageInfo) -> {

			String url = messageInfo.getOriginalUrl();

			if(url.contains("https://mp.weixin.qq.com/s"))
				System.out.println(" . " + url);

			return null;
		};

		Stack<String> content_stack = new Stack<>();
		Stack<String> stats_stack = new Stack<>();
		Stack<String> comments_stack = new Stack<>();

		*//**
		 * TODO 返回过滤器
		 *//*
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

						Essays we = new Essays().parseContent(content_src).parseStat(stats_src);

						System.err.println(JSON.toPrettyJson(we));

						we.insert();

						List<Comments> comments_ = Comments.parseComments(we.src_id, comments_src);

						System.err.println(JSON.toPrettyJson(comments_));

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

		Thread.sleep(10000000);

	}
*/
}

