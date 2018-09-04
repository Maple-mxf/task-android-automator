package com.sdyk.android.automator.adapter;

import com.sdyk.android.automator.AndroidDevice;
import com.sdyk.android.automator.model.WechatMsg;
import com.sdyk.android.automator.model.WechatFriend;
import com.sdyk.android.automator.model.WechatMoment;
import com.sdyk.android.automator.model.WechatPublicAccount;
import io.appium.java_client.MobileElement;
import io.appium.java_client.TouchAction;
import io.appium.java_client.touch.offset.PointOption;
import one.rewind.txt.NumberFormatUtil;
import one.rewind.util.FileUtil;
import org.openqa.selenium.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 微信的自动化操作
 */
public class WechatAdapter extends Adapter {

	String user_id = "";
	String user_name = "";

	public WechatAdapter(AndroidDevice androidDevice) {
		super(androidDevice);
	}

	/**
	 * 获取本台机器udid对应的微信号id和微信名
	 *
	 * @throws InterruptedException 中断异常
	 */
	public void getLocalUserInfo() throws InterruptedException {

		driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'我')]")).click(); //点击我
		Thread.sleep(500);

		Point qrCodeLoc = driver.findElementByAccessibilityId("查看二维码").getLocation(); // 找到二维码位置

		// 点二维码左边的用户名称，进入个人信息界面

		new TouchAction(driver).tap(PointOption.point(qrCodeLoc.x - 500, qrCodeLoc.y)).perform();

		Thread.sleep(1000);

		List<WebElement> lis = driver.findElementsById("android:id/summary");

		this.user_name = lis.get(0).getText();
		this.user_id = lis.get(1).getText();
		driver.navigate().back();

		logger.info("Current user_id:{} user_name:{}", user_id, user_name);

		Thread.sleep(5000);
	}

	/**
	 * 进入到本微信的微信朋友圈中
	 *
	 * @throws InterruptedException
	 */
	public void getIntoMoments() throws InterruptedException {

		WebElement discover_button = driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'发现')]"));//点击发现
		discover_button.click();

		Thread.sleep(1700);

		//点击朋友圈，朋友圈按钮为对应list中的第一个元素
		List<WebElement> lis = driver.findElementsByClassName("android.widget.LinearLayout");
		WebElement targetEle = lis.get(1);

		targetEle.click();

		Thread.sleep(2500);
	}

	/**
	 * 创建群组
	 *
	 * @param groupName
	 * @param ids
	 * @throws InterruptedException
	 */
	public void createGroupChat(String groupName, String... ids) throws InterruptedException {

		Thread.sleep(5000);

		// 点 +
		new TouchAction(driver).tap(PointOption.point(1202 + 100, 84 + 80)).perform();

		Thread.sleep(1000);

		// 创建群聊
		new TouchAction(driver).tap(PointOption.point(1050, 335)).perform();

		MobileElement id_input;

		for (String id : ids) {

			// 输入人名
			driver.findElement(By.className("android.widget.EditText")).sendKeys(id);

			Thread.sleep(1000);

			// 选择人
			new TouchAction(driver).tap(PointOption.point(1270, 641)).perform();

			Thread.sleep(1000);
		}

		// 创建群
		new TouchAction(driver).tap(PointOption.point(1320, 168)).perform();

		Thread.sleep(10000);

		// 点群设置
		new TouchAction(driver).tap(PointOption.point(1302, 168)).perform();

		Thread.sleep(1000);

		// 点群名称
		new TouchAction(driver).tap(PointOption.point(720, 784)).perform();

		Thread.sleep(1000);

		// 名称输入框
		driver.findElementByClassName("android.widget.EditText").sendKeys(groupName);

		Thread.sleep(1000);

		// 确定名称 点OK
		new TouchAction(driver).tap(PointOption.point(1331, 168)).perform();

		Thread.sleep(1000);

		// 返回群聊
		new TouchAction(driver).tap(PointOption.point(70, 168)).perform();

		Thread.sleep(1000);

		// 返回主界面
		new TouchAction(driver).tap(PointOption.point(70, 168)).perform();

		Thread.sleep(1000);
	}

	/**
	 * 获取群组中的成员信息
	 *
	 * @throws InterruptedException 中断异常
	 */
	public void getFriendFromGroupChat() throws InterruptedException {

		driver.findElementByAccessibilityId("聊天信息").click();
		Thread.sleep(1000);

		List<WebElement> ifHaveTitle = driver.findElementsById("android:id/title");
		while (ifHaveTitle.size() == 0) {
			new TouchAction(driver).press(PointOption.point(850, 1460)).moveTo(PointOption.point(840, 500)).release().perform();
		}

		driver.findElementById("android:id/title").click();
		Thread.sleep(1500);

		//根据群内的人数调整j的控制
		for (int j = 0; j < 10; j++) {
			List<WebElement> listOfFriends = driver.findElementsById("com.tencent.mm:id/ajj");
			for (int i = 0; i < listOfFriends.size(); i++) {
				listOfFriends.get(i).click();
				Thread.sleep(1000);
				WebElement add = null;

				try {
					add = driver.findElement(By.xpath("//android.widget.Button[contains(@text,'添加到通讯录')]"));

					if (add != null) {
						add.click();
						Thread.sleep(1000);
						driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'发送')]")).click();
						Thread.sleep(2000);
						driver.navigate().back();
					}
					// 如果没有添加到通讯录，说明已经是好友了
					else {
						driver.navigate().back();
						Thread.sleep(1000);
					}

				} catch (Exception e) {
					logger.error("Error add to wechat contact.", e);
				}
			}

			// TODO 返回主界面？
			new TouchAction(driver)
					.press(PointOption.point(1100, 2300))
					.moveTo(PointOption.point(1100, 200))
					.release().perform();
		}

	}

	/**
	 * 进入群组
	 * （这个方法同时可以用于进入与朋友的单人聊天）
	 * @param groupName 群聊名称
	 * @throws InterruptedException 中断异常
	 */
	public void getIntoGroup(String groupName) throws InterruptedException {

		new TouchAction(driver).tap(PointOption.point(1118, 168)).perform();

		Thread.sleep(1000);

		// 名称输入
		driver.findElementByClassName("android.widget.EditText").sendKeys(groupName);

		Thread.sleep(1000);

		// 确定名称 点OK
		new TouchAction(driver).tap(PointOption.point(1064, 532)).perform();

		Thread.sleep(1000);

	}

	/**
	 * 获取朋友圈并存入数据库
	 * 用try的方法试图获取该页面可能的第一个文字，图片，链接，和时间
	 * 然后进行判定，判断第一条发送的内容属于5种当中的哪一种（文字，图片，链接，文字图片，文字链接）
	 * 用对应的方法存入数据库：复制文字，图片截图保存二进制，链接点击后获取url）
	 * 然后将第一次的时间（作为分隔符）向上滑动直至屏幕内没有上一次的时间，此时屏幕内第一条为下一条朋友圈，重新进行判定并循环
	 *
	 * @throws Exception
	 */
	public void getMoments() throws Exception {

		int width = driver.manage().window().getSize().width;
		int height = driver.manage().window().getSize().height;


		for (int i = 0; i < 3; i++) {

			String copytext = "";
			String url = "";
			byte[] picByte = null;

			//此处获取到的是当前页面的每一个第一次出现的昵称，文字，图片，时间，或链接中的字
			//其中文字，图片，链接中间的字可能不存在
			WebElement name = driver.findElementById("com.tencent.mm:id/asc");//昵称
			WebElement pubtime = driver.findElementById("com.tencent.mm:id/dg7");//时间

			WebElement text = null;
			try {
				text = driver.findElementById("com.tencent.mm:id/ic");//文字
			} catch (Exception e) {
			}

			WebElement picture = null;
			try {
				picture = driver.findElementById("com.tencent.mm:id/dhi");//图片
			} catch (Exception e) {
			}

			WebElement textInUrl = null;
			try {
				textInUrl = driver.findElementById("com.tencent.mm:id/did");//链接中间的字
			} catch (Exception e) {
			}
			//取得不了发布时间

			System.out.println(picture.getLocation().getY());

			//长按文字进行复制
			if (text != null) {
				TouchAction copy = new TouchAction(driver).longPress(PointOption.point(text.getLocation().getX(), text.getLocation().getY())).release();
				copy.perform();
				Thread.sleep(500);
				driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'复制')]")).click();
				Thread.sleep(500);
				copytext = driver.getClipboardText();//将剪贴板中的内容保存到变量中
				Thread.sleep(500);
			}

			//点击进入图片然后截图
			if (picture != null) {
				picture.click();
				Thread.sleep(1000);
				picByte = driver.getScreenshotAs(OutputType.BYTES);
				Thread.sleep(500);
				driver.navigate().back();
				Thread.sleep(1000);
			}

			//点击链接并复制url
			if (textInUrl != null) {
				textInUrl.click();
				Thread.sleep(5000);
				driver.findElementByAccessibilityId("更多").click();
				Thread.sleep(1000);
				driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'复制链接')]")).click();
				Thread.sleep(500);
				driver.navigate().back();
				Thread.sleep(1000);
				url = driver.getClipboardText();
				Thread.sleep(500);
			}

			//判断文字是否是本次
			if (text != null) {
				//文字不是本次
				if (text.getLocation().getY() > pubtime.getLocation().getY()) {
					//判断图片是否为空
					if (picture != null) {
						//判断图片是否为本次
						//图片不为本次，说明本次只有链接
						if (picture.getLocation().getY() > pubtime.getLocation().getY()) {
							WechatMoment WM = new WechatMoment(udid, user_id, user_name);
							WM.friend_name = name.getText();
							WM.url = url;
							WM.insert_time = new Date();
							WM.insert();
						}
						//图片为本次，说明本次只有图片
						if (picture.getLocation().getY() < pubtime.getLocation().getY()) {
							WechatMoment WM = new WechatMoment(udid, user_id, user_name);
							WM.friend_name = name.getText();
							WM.content = picByte;
							WM.insert_time = new Date();
							WM.insert();
						}
					}
				}
				//如果文字是本次的
				if (text.getLocation().getY() < pubtime.getLocation().getY()) {
					//判断本次是纯文字还是文字图片还是文字链接
					//如果图片和链接都为空，则本次只有文字
					if (picture == null && textInUrl == null) {
						WechatMoment WM = new WechatMoment(udid, user_id, user_name);
						WM.friend_name = name.getText();
						WM.friend_text = copytext;
						WM.insert_time = new Date();
						WM.insert();
					}
					//如果图片为空链接不为空，再判断链接是否为本次
					if (picture == null && textInUrl != null) {
						//链接为本次
						if (textInUrl.getLocation().getY() > pubtime.getLocation().getY()) {
							WechatMoment WM = new WechatMoment(udid, user_id, user_name);
							WM.friend_name = name.getText();
							WM.url = url;

							WM.insert_time = new Date();
							WM.insert();
						}
						//链接不为本次，说明只有文字
						if (textInUrl.getLocation().getY() < pubtime.getLocation().getY()) {
							WechatMoment WM = new WechatMoment(udid, user_id, user_name);
							WM.friend_name = name.getText();
							WM.friend_text = copytext;
							WM.insert_time = new Date();
							WM.insert();
						}
					}
					//如果链接为空图片不为空，则判断图片是否为本次
					//图片是本次
					if (picture.getLocation().getY() > pubtime.getLocation().getY()) {
						WechatMoment WM = new WechatMoment(udid, user_id, user_name);
						WM.friend_name = name.getText();
						WM.content = picByte;
						WM.insert_time = new Date();
						WM.insert();
					}
					//图片不是本次，说明只有文字
					if (picture.getLocation().getY() < pubtime.getLocation().getY()) {
						WechatMoment WM = new WechatMoment(udid, user_id, user_name);
						WM.friend_name = name.getText();
						WM.friend_text = copytext;
						WM.insert_time = new Date();
						WM.insert();
					}
				}
			}
			//向下滑动，获取第一次发布时间的location，使其在屏幕中消失
			TouchAction action1 = new TouchAction(driver).press(PointOption.point(pubtime.getLocation().getX(), pubtime.getLocation().getY())).waitAction().moveTo(PointOption.point(250, 170)).release();
			action1.perform();
			Thread.sleep(1500);
		}

	}

	/**
	 * 通过输入的朋友姓名进入与朋友的聊天界面
	 * TODO 如果列表过长怎么办？
	 *
	 * @param friend
	 * @throws InterruptedException
	 */
	public void getSimpleFriend(String friend) throws InterruptedException {

		// 进入通讯录
		WebElement sells = driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'通讯录')]"));
		sells.click();

		Thread.sleep(1000);

		// 找到friend变量的好友并点击
		for (int i = 0; i < 25; i++) {
			List<WebElement> lis2 = (List<WebElement>) driver.findElementsByAccessibilityId(friend);
			if (lis2.size() != 0) {
				driver.findElementByAccessibilityId(friend).click();
				break;
			}
			TouchAction action1 = new TouchAction(driver).press(PointOption.point(200, 1024)).waitAction().moveTo(PointOption.point(200, 176)).release();
			action1.perform();
		}

		Thread.sleep(1000);

		driver.findElement(By.xpath("//android.widget.Button[contains(@text,'发消息')]")).click();//进入聊天界面

		Thread.sleep(1000);
	}

	/**
	 * 发送一条消息（在消息界面）
	 * @param msg
	 * @throws InterruptedException
	 */
	public void sendMsg(String msg) throws InterruptedException {
		driver.findElementByClassName("android.widget.EditText").sendKeys(msg);//sendKeys
		Thread.sleep(1500);
		driver.findElement(By.xpath("//android.widget.Button[contains(@text,'发送')]")).click();//发送
		Thread.sleep(1500);
	}

	/**
	 * 通过txt文件添加微信好友,txt中好友微信号，验证以制表符分隔
	 * 如果需要发送备注，则再使用制表符分隔后输入备注信息，并去掉代码中的注释
	 *
	 * @param filePath
	 * @throws Exception
	 */
	public void addFriendsByFile(String filePath) throws Exception {

		//从txt中按行导入
		String friends = FileUtil.readFileByLines(filePath);

		//设定验证信息和备注
		String yanzheng = "自动化测试账号";
		//String beizhu = "备注";

		Thread.sleep(1500);

		WebElement add = driver.findElementByAccessibilityId("更多功能按钮");//点击发现
		add.click();

		Thread.sleep(1500);

		WebElement newfriend = driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'添加朋友')]"));
		newfriend.click();

		Thread.sleep(1500);

		driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'微信号/QQ号/手机号')]")).click();

		Thread.sleep(1500);

		//filePath = "C:\\test\\newFriend.txt"
		outer:
		for (String friend : friends.split("\\n|\\r\\n")) {

			String[] token = friend.split("\\t");

			driver.findElement(By.xpath("//android.widget.EditText[contains(@text,'微信号/QQ号/手机号')]")).sendKeys(token[0]);//填入添加人的号码
			Thread.sleep(1500);

			driver.findElementByClassName("android.widget.TextView").click();//点击搜索
			Thread.sleep(2000);

			//如果用户不存在
			List<WebElement> lis2 = (List<WebElement>) driver.findElements(By.xpath("//android.widget.TextView[contains(@text,'该用户不存在')]"));
			if (lis2.size() != 0) {
				driver.navigate().back();//返回
				System.out.println(token[0]);
				Thread.sleep(1500);
				driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'微信号/QQ号/手机号')]")).click();
				Thread.sleep(1500);
				continue outer;
			}

			//操作过于频繁，直接结束
			List<WebElement> lis3 = (List<WebElement>) driver.findElements(By.xpath("//android.widget.TextView[contains(@text,'操作过于频繁，请稍后再试')]"));
			if (lis3.size() != 0) {
				Thread.sleep(500);
				driver.navigate().back();
				System.out.println("频繁报错，结束");
				System.out.println(token[0]);
			}

			driver.findElementByClassName("android.widget.Button").click();//添加到通讯录
			Thread.sleep(2000);

			List<WebElement> temp = (List<WebElement>) driver.findElementsByClassName("android.widget.EditText");
			temp.get(0).clear();
			temp.get(0).sendKeys(yanzheng);//重填验证
			Thread.sleep(1000);

			String friend_name = temp.get(1).getText();//在重填之前获取朋友的昵称保存在变量中
			//getAllDingdingContacts.get(1).clear();
			//getAllDingdingContacts.get(1).sendKeys(beizhu);//重填备注
			Thread.sleep(1000);
			driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'发送')]")).click();//发送
			Thread.sleep(5000);

			//存入数据库中
			WechatFriend a = new WechatFriend(udid, user_id, user_name, token[0], friend_name);
			a.insert_time = new Date();
			a.insert();

			driver.navigate().back();//返回
			Thread.sleep(1500);

			driver.findElementByClassName("android.widget.EditText").clear();//清空
			Thread.sleep(500);

		}
	}

	/**
	 * 取得聊天记录
	 * 首先提取页面中的全部元素，然后将他们按照Y值的顺序在list allObjects中进行排序 从下至上依次进行判断
	 * 由于最上方的文字没有对应的头像，因此不做考虑，判断文字，图片，url等的不同方法根据X值或是长按出现的选项数
	 * 判断标准可以根据微信的更新随时更改
	 * 在本页面所有元素全部判定完成之后，根据第二个，即判定完成的最后一个元素的位置向下滑动，直至该元素不再显示
	 *
	 * TODO 这个方法需要大量测试
	 *
	 * @throws Exception
	 */
	public void getChatRecord() throws Exception {

		for (int k = 0; k < 10; k++) {
			//将页面中的文字，时间，图片等所有元素放在allObjects组中
			List<WebElement> textlist = driver.findElementsById("com.tencent.mm:id/ki");//该界面中所有的文字
			List<WebElement> aevList = driver.findElementsById("com.tencent.mm:id/aev");//该界面中的aev为id的内容
			List<WebElement> timeList = driver.findElementsById("com.tencent.mm:id/a4");//该界面中所有的时间
			List<WebElement> allObjects = timeList;
			allObjects.addAll(aevList);
			allObjects.addAll(textlist);

			System.out.println(allObjects.size());

			for (int i = 1; i < allObjects.size(); i++) {
				WebElement nowObject = allObjects.get(i);
				//如果Y值大于231，则说明在本页面
				if (nowObject.getLocation().getY() > 280) {
					//如果在文字列表中即是文字，进行文字对应操作
					if (textlist.contains(nowObject)) {
						//首先判断是否为对方发送，对方发送的x值是固定的，为199
						//是对方发送的
						if (nowObject.getLocation().getX() == 199) {
							new TouchAction(driver).longPress(PointOption.point(nowObject.getLocation().getX(), nowObject.getLocation().getY())).perform();
							Thread.sleep(1000);
							driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'复制')]")).click();
							Thread.sleep(500);
							String copytext = driver.getClipboardText();
							//获取对方的微信
							new TouchAction(driver).tap(PointOption.point(nowObject.getLocation().getX() - 100, nowObject.getLocation().getY() + 10)).perform();
							Thread.sleep(1500);
							//微信号的id
							String friendname = driver.findElementById("com.tencent.mm:id/qk").getText();
							driver.navigate().back();
							Thread.sleep(500);
							//插入数据库
							WechatMsg WC = new WechatMsg(udid, user_id, user_name);
							WC.text = copytext;
							WC.friend_name = friendname;
							WC.text_type = WechatMsg.Type.Text;
							WC.insert_time = new Date();
							WC.insert();
						}

						//是自己发送的
						if (nowObject.getLocation().getX() != 199) {
							new TouchAction(driver).longPress(PointOption.point(nowObject.getLocation().getX(), nowObject.getLocation().getY())).perform();
							Thread.sleep(1000);
							driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'复制')]")).click();
							Thread.sleep(500);
							String copytext1 = driver.getClipboardText();
							//存入数据库
							WechatMsg WC1 = new WechatMsg(udid, user_id, user_name);
							WC1.text = copytext1;
							WC1.friend_name = "self";
							WC1.text_type = WechatMsg.Type.Text;
							WC1.insert_time = new Date();
							WC1.insert();
						}
					}

					//如果在时间列表内存在，即为时间
					if (timeList.contains(nowObject)) {
						//进行保存时间的操作
						String time = nowObject.getText();
						//保存到数据库中
						WechatMsg WC = new WechatMsg(udid, user_id, user_name);
						WC.text = time;
						WC.text_type = WechatMsg.Type.Text;
						WC.insert_time = new Date();
						WC.insert();
					}
					//如果在aev列表中存在，则判断是视频，图片，文件，url其中的一种，进行长按判定
					if (aevList.contains(nowObject)) {
						new TouchAction(driver).longPress(PointOption.point(nowObject.getLocation().getX(), nowObject.getLocation().getY())).perform();
						Thread.sleep(1000);
						List<WebElement> optionList = driver.findElementsByClassName("android.widget.TextView");
						//如果有4个选项，就是文件，对文件进行处理(过期文件3个选项)
						if (optionList.size() == 4) {
							String filename = driver.findElementById("com.tencent.mm:id/afc").getText();
							driver.navigate().back();
							Thread.sleep(500);
							//获取对方的微信
							new TouchAction(driver).tap(PointOption.point(nowObject.getLocation().getX() - 100, nowObject.getLocation().getY() + 10)).perform();
							Thread.sleep(1500);
							//微信号的id
							String friendname = driver.findElementById("com.tencent.mm:id/qk").getText();
							driver.navigate().back();
							Thread.sleep(500);
							//存入数据库
							WechatMsg WC = new WechatMsg(udid, user_id, user_name);
							WC.insert_time = new Date();
							WC.text = filename;
							WC.text_type = WechatMsg.Type.File;
							WC.insert();
						}
						//如果是5个选项，就是链接，点开并保存url
						if (optionList.size() == 5) {
							driver.navigate().back();
							Thread.sleep(500);
							nowObject.click();
							Thread.sleep(5000);
							driver.findElementByAccessibilityId("更多").click();
							Thread.sleep(1200);
							driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'复制链接')]")).click();
							Thread.sleep(500);
							String url = driver.getClipboardText();
							driver.navigate().back();
							Thread.sleep(500);
							//判断是对方还是自己发送
							//自己发送
							if (nowObject.getLocation().getY() == 231) {
								//存入数据库
								WechatMsg WC = new WechatMsg(udid, user_id, user_name);
								WC.insert_time = new Date();
								WC.text = url;
								WC.text_type = WechatMsg.Type.Url;
								WC.friend_name = "self";
								WC.insert();
							}
							//别人发送
							if (nowObject.getLocation().getY() != 231) {
								//获取对方的微信
								new TouchAction(driver).tap(PointOption.point(nowObject.getLocation().getX() - 100, nowObject.getLocation().getY() + 10)).perform();
								Thread.sleep(1500);
								//微信号的id
								String friendname = driver.findElementById("com.tencent.mm:id/qk").getText();
								driver.navigate().back();
								Thread.sleep(500);
								WechatMsg WC = new WechatMsg(udid, user_id, user_name);
								WC.insert_time = new Date();
								WC.text = url;
								WC.text_type = WechatMsg.Type.Url;
								WC.friend_name = friendname;
								WC.insert();
							}
						}
						//如果是6个选项，判定是图片还是视频
						if (optionList.size() == 6) {
							WebElement ifvideo = null;
							WebElement ifpicture = null;
							//先判断是否为图片,编辑是图片区别于视频的选项
							try {
								ifpicture = driver.findElement(By.xpath("android.widget.TextView[contains(@text,'编辑')]"));
							} catch (Exception e) {
							}
							//如果不是图片就是视频
							if (ifpicture == null) {
								driver.navigate().back();
								Thread.sleep(500);
								nowObject.click();
								Thread.sleep(3000);
								new TouchAction(driver).longPress(PointOption.point(1000, 1000)).perform();
								Thread.sleep(500);
								driver.findElement(By.xpath("android.widget.TextView[contains(@text,'保存视频')]"));
								//TODO 保存视频后如何做 可以通过adb文件导出
								Thread.sleep(500);
								driver.navigate().back();
								Thread.sleep(500);
							}
							//如果是图片
							if (ifpicture != null) {
								driver.navigate().back();
								Thread.sleep(500);
								nowObject.click();
								Thread.sleep(2000);
								byte[] picByte = driver.getScreenshotAs(OutputType.BYTES);
								driver.navigate().back();
								Thread.sleep(500);
								//判断是别人发送还是自己发送
								//别人发送
								if (nowObject.getLocation().getY() == 218) {
									//获取对方的微信
									new TouchAction(driver).tap(PointOption.point(nowObject.getLocation().getX() - 100, nowObject.getLocation().getY() + 10)).perform();
									Thread.sleep(1500);
									//微信号的id
									String friendname = driver.findElementById("com.tencent.mm:id/qk").getText();
									driver.navigate().back();
									Thread.sleep(500);
									//存入数据库
									WechatMsg WC = new WechatMsg(udid, user_id, user_name);
									WC.insert_time = new Date();
									WC.content = picByte;
									WC.friend_name = friendname;
									WC.insert();
								}
								//自己发送
								if (nowObject.getLocation().getY() != 218) {
									//存入数据库
									WechatMsg WC = new WechatMsg(udid, user_id, user_name);
									WC.insert_time = new Date();
									WC.content = picByte;
									WC.friend_name = "self";
									WC.insert();
								}
							}
						}
					}
				}
			}
			int estimate = allObjects.get(0).getLocation().getY();
			//如果第一个的Y等于231，说明第一个不在本页面中，取第二个的值
			if (estimate < 280) {
				//第二个的Y肯定大于231，将第二个向下滑动直至全部消失（大于2200）
				int anotherEstimate = allObjects.get(1).getLocation().getY();
				new TouchAction(driver).press(PointOption.point(allObjects.get(1).getLocation().getX(), allObjects.get(1).getLocation().getY())).moveTo(PointOption.point(allObjects.get(1).getLocation().getX(), 2350)).perform();
			}
		}
	}

	public void addPublicAccount(String name) throws Exception {
		// 点搜索
		WebElement searchButton = driver
				.findElement(By.xpath("//android.widget.TextView[contains(@content-desc,'搜索')]"));
		searchButton.click();

		Thread.sleep(1000);

		// 点 公众号
		WebElement publicAccountLink = driver
				.findElement(By.xpath("//android.widget.TextView[contains(@text,'公众号')]"));
		publicAccountLink.click();

		Thread.sleep(4000);

		// 查询 特定 公众号
		/*WebElement searchInput = device.driver
				.findElement(By.xpath("//android.widget.EditText[contains(@text,'搜索公众号')]"));

		searchInput.sendKeys("华尔街见闻", Keys.ENTER);*/

		driver.findElement(By.className("android.widget.EditText")).sendKeys(name);

		// 搜索
		new TouchAction(driver).tap(PointOption.point(720, 150)).perform();
		// Thread.sleep(100);
		new TouchAction(driver).tap(PointOption.point(1350, 2250)).perform();

		Thread.sleep(4000);

		// 选中第一个结果
		new TouchAction(driver).tap(PointOption.point(720, 600)).perform();

		Thread.sleep(2000);

		// 点击订阅
		try {
			driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'关注公众号')]"))
					.click();

			Thread.sleep(2000);
			driver.navigate().back();

		} catch (Exception e) {
			logger.info("Already add public account: {}", name);
		}

		WechatPublicAccount wpa = new WechatPublicAccount();

		// 点击左上角三个点
		driver.findElementByClassName("android.widget.ImageButton").click();
		Thread.sleep(1000);

		// 点更多资料
		try {
			driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'更多资料')]")).click();
		} catch (Exception e) {
			driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'更多资料')]")).click();
		}

		Thread.sleep(1000);

		// 对更多资料内容进行处理
		List<WebElement> els = driver.findElementsByClassName("android.widget.TextView");

		els = els.stream().filter(el -> {
			return !el.getText().equals("更多资料") && el.getLocation().x != 0;
		}).collect(Collectors.toList());

		List<String> info = new ArrayList<>();

		for(int i=0; i<els.size()-1; i=i+2) {
			info.add(els.get(i).getText() + "" + els.get(i+1).getText());
		}

		for(String info_item: info) {
			if(info_item.contains("微信号")) {
				wpa.wechat_id = info_item.replaceAll("微信号", "");
			}
			if(info_item.contains("帐号主体")) {
				wpa.subject = info_item.replaceAll("帐号主体", "");
			}
			if(info_item.contains("商标保护")) {
				wpa.trademark = info_item.replaceAll("商标保护", "");
			}
			if(info_item.contains("客服电话")) {
				wpa.phone = info_item.replaceAll("客服电话", "");
			}
		}

		// 点击返回
		driver.findElement(By.xpath("//android.widget.ImageView[contains(@content-desc,'返回')]")).click();

		Thread.sleep(1000);

		// 对公众号首页信息进行处理
		els = driver.findElementsByClassName("android.widget.TextView");

		for(WebElement we : els) {
			System.err.println(els.indexOf(we) + " --> " + we.getText());
		}

		wpa.name = els.get(0).getText();
		wpa.content = els.get(1).getText();
		System.err.println();
		wpa.essay_count = NumberFormatUtil.parseInt(
				els.get(2).getText().replaceAll("篇原创文章.*?", ""));

		try {
			wpa.insert();
		} catch (Exception e) {
			logger.error("Error insert record.", e);
		}

		Thread.sleep(1000);

		driver.navigate().back();
		Thread.sleep(500);
		driver.navigate().back();
		Thread.sleep(500);
		driver.navigate().back();
		Thread.sleep(500);
	}

	public void getIntoPublicAccountEssayList(String name) throws InterruptedException {

		driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'通讯录')]")).click();

		Thread.sleep(1000);

		driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'公众号')]")).click();

		Thread.sleep(1000);

		driver.findElement(By.xpath("//android.widget.ImageButton[contains(@content-desc,'搜索')]")).click();

		Thread.sleep(1000);

		// 搜索
		driver.findElement(By.className("android.widget.EditText")).sendKeys(name);

		new TouchAction(driver).tap(PointOption.point(720, 150)).perform();
		new TouchAction(driver).tap(PointOption.point(1350, 2250)).perform();

		Thread.sleep(2000);

		// 进入公众号
		new TouchAction(driver).tap(PointOption.point(720, 360)).perform();

		Thread.sleep(1000);

		driver.findElement(By.xpath("//android.widget.ImageButton[contains(@content-desc,'聊天信息')]")).click();

		Thread.sleep(1000);

		new TouchAction(driver).press(PointOption.point(720, 1196))
				.waitAction()
				.moveTo(PointOption.point(720, 170))
				.release()
				.perform();

		Thread.sleep(1000);

		driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'全部消息')]")).click();

		Thread.sleep(20000); // TODO 此处时间需要调整

		System.err.println(driver.getContextHandles());

		// TODO 不能正常switch context
		/*driver.context("WEBVIEW_com.tencent.mm:tools");
		Thread.sleep(4000);
		System.err.println(driver.getPageSource());*/

		new TouchAction(driver).tap(PointOption.point(720, 1550)).perform();
		Thread.sleep(6000);
		// 点击返回
		driver.findElement(By.xpath("//android.widget.ImageView[contains(@content-desc,'返回')]")).click();
		Thread.sleep(1000);

		new TouchAction(driver).tap(PointOption.point(720, 1920)).perform();
		Thread.sleep(6000);
		// 点击返回
		driver.findElement(By.xpath("//android.widget.ImageView[contains(@content-desc,'返回')]")).click();
		Thread.sleep(1000);

		new TouchAction(driver).tap(PointOption.point(720, 2330)).perform();
		Thread.sleep(6000);
		// 点击返回
		driver.findElement(By.xpath("//android.widget.ImageView[contains(@content-desc,'返回')]")).click();
		Thread.sleep(1000);

		driver.navigate().back();
		Thread.sleep(500);
		driver.navigate().back();
		Thread.sleep(500);
		driver.navigate().back();
		Thread.sleep(500);
		driver.navigate().back();
		Thread.sleep(500);

		driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'微信')]")).click();
		Thread.sleep(500);

	}
}
