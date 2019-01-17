package one.rewind.android.automator.adapter;

import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.account.AppAccount;
import one.rewind.android.automator.exception.WeChatAdapterException;
import one.rewind.android.automator.ocr.OCRParser;
import one.rewind.android.automator.ocr.TesseractOCRParser;
import one.rewind.android.automator.util.FileUtil;
import org.openqa.selenium.By;

import java.io.IOException;
import java.util.List;

/**
 * @author maxuefeng[m17793873123@163.com]
 * Adapter对应是设备上的APP  任务执行应该放在Adapter层面上
 */
public class WeChatAdapter extends Adapter {



	public static enum Status {
		Init,            // 初始化
		Home,            // 首页
		Search,            // 首页点进去的搜索
		PublicAccount_Search_Result,            // 公众号搜索结果
		PublicAccount_Home,                    // 公众号首页
		Address_List,                        // 通讯录
		Subscribe_PublicAccount_List,            // 我订阅的公众号列表
		Subscribe_PublicAccount_Search,        // 我订阅的公众号列表搜索
		Subscribe_PublicAccount_Search_Result, // 我订阅的公众号列表搜索结果
		PublicAccount_Conversation,            // 公众号回话列表
		PublicAccount_Essay_List,            // 公众号历史文章列表
		PublicAccountEssay,                    // 公众号文章
		Error                                // 出错
	}

	// 状态信息
	public Status status = Status.Init;

	// 当前使用的账号
	public AppAccount account;

	/**
	 * @param device
	 */
	WeChatAdapter(AndroidDevice device) {
		super(device);
	}

	/**
	 * 截图 并获取可点击的文本区域信息
	 *
	 * @return
	 * @throws IOException
	 */
	public List<OCRParser.TouchableTextArea> getPublicAccountEssayListTitles() throws IOException, InterruptedException, WeChatAdapterException.NoResponseException, WeChatAdapterException.SearchPublicAccountFrozenException, WeChatAdapterException.GetPublicAccountEssayListFrozenException {

		// A 获取截图
		String screenShotPath = this.device.screenShot();

		// B 获取可点击文本区域
		final List<OCRParser.TouchableTextArea> textAreaList = TesseractOCRParser.getInstance().getTextBlockArea(screenShotPath, true);

		// C 删除图片文件
		FileUtil.deleteFile(screenShotPath);

		// D 根据返回的文本信息 进行异常判断
		for (OCRParser.TouchableTextArea area : textAreaList) {

			if (area.content.contains("微信没有响应")) throw new WeChatAdapterException.NoResponseException();

			if (area.content.contains("操作频繁") || area.content.contains("请稍后再试")) {

				if (status == Status.PublicAccount_Search_Result) {
					throw new WeChatAdapterException.SearchPublicAccountFrozenException(account);
				} else if (status == Status.PublicAccount_Essay_List) {
					throw new WeChatAdapterException.GetPublicAccountEssayListFrozenException(account);
				}
			}
		}
		return textAreaList;
	}

	/**
	 * 返回微信首页
	 */
	public void reset() {

		// 关闭Wechat


		// 重新打开WeChat

		this.status = Status.Home;
	}

	/**
	 * 点击左上角的返回按钮
	 */
	public void returnPreiousPage() {

	}

	/**
	 * 点击左上角的叉号
	 */
	public void touchCloseButton() {

	}

	/**
	 * 进入已订阅公众号的列表页面  改变Adapter  status
	 */
	public void goToSubscribePublicAccountList() throws InterruptedException, WeChatAdapterException.IllegalException {

		if (this.status != Status.Home || this.status != Status.Address_List)
			throw new WeChatAdapterException.IllegalException();

		// 从首页点 通讯录
		device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'通讯录')]")).click();

		Thread.sleep(1000);
		// 点公众号
		device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'公众号')]")).click();

		this.status = Status.Subscribe_PublicAccount_List;
	}


	/**
	 * @param mediaName 根据media name搜索到相关的公众号（已订阅的公众号）
	 */
	public void goToPublicAccountHome(String mediaName) throws InterruptedException, WeChatAdapterException.IllegalException {

		if (this.status != Status.Home || this.status != Status.Address_List)
			throw new WeChatAdapterException.IllegalException();

		// 点搜索
		device.driver.findElement(By.xpath("//android.widget.ImageButton[contains(@content-desc,'搜索')]")).click();

		Thread.sleep(1000);

		// 输入名称
		device.driver.findElement(By.className("android.widget.EditText")).sendKeys(mediaName);

		// 点确认
		device.touch(720, 150, 1000);

		// 点第一个结果
		device.touch(1350, 2250, 1000);

		// 点右上角的人头图标
		device.touch(720, 360, 1000);

		device.driver.findElement(By.xpath("//android.widget.ImageButton[contains(@content-desc,'聊天信息')]")).click();

		Thread.sleep(1000);

		this.status = Status.PublicAccount_Home;
	}

	/**
	 * 查看公众号更多资料
	 */
	public void goToPublicAccontMoreInfoPage() {

		// 点右上三个点图标

		// 点更多资料
	}

	/**
	 * 订阅公众号
	 */
	public void subscribePublicAccount() throws InterruptedException, WeChatAdapterException.IllegalException {

		if (this.status != Status.PublicAccount_Home) throw new WeChatAdapterException.IllegalException();

		device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'关注公众号')]")).click();

		Thread.sleep(1000);

		this.status = Status.PublicAccount_Conversation;
	}

	/**
	 * 取消订阅
	 */
	public void unsubscribePublicAccount() throws InterruptedException, WeChatAdapterException.IllegalException {

		if (this.status != Status.PublicAccount_Home) throw new WeChatAdapterException.IllegalException();

		device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'取消关注')]")).click();

		Thread.sleep(1000);

		this.status = Status.Init;
	}

	/**
	 * 公众号历史消息页面
	 */
	public void gotoPublicAccountEssayList() throws InterruptedException, WeChatAdapterException.IllegalException {

		if (this.status != Status.PublicAccount_Home) throw new WeChatAdapterException.IllegalException();

		// 向下滑动
		device.slideToPoint(720, 1196, 720, 170, 1000);

		device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'全部消息')]")).click();

		Thread.sleep(12000); // TODO 此处时间需要调整

		this.status = Status.PublicAccount_Essay_List;
	}


	/**
	 * 进入文章详情页面
	 */
	public void goToEssayDetail(OCRParser.TouchableTextArea textArea) throws InterruptedException, WeChatAdapterException.IllegalException {

		if (this.status != Status.PublicAccount_Essay_List) throw new WeChatAdapterException.IllegalException();

		// A 点击文章
		device.touch(textArea.left, textArea.height, 6000);

		// B 向下滑拿到文章热度数据和评论数据
		for (int i = 0; i < 2; i++) {
			device.touch(1413, 2369, 500);
		}

		this.status = Status.PublicAccountEssay;
	}


	/**
	 * 从文章详情页返回到上一个页面   点击叉号
	 */
	public void goToEssayPreviousPage() throws WeChatAdapterException.IllegalException {

		if (this.status != Status.PublicAccountEssay) throw new WeChatAdapterException.IllegalException();

		// A 点击叉号  TODO

		this.status = Status.PublicAccount_Essay_List;
	}
}
