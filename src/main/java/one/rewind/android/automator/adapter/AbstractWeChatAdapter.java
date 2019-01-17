package one.rewind.android.automator.adapter;

import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.account.AppAccount;
import one.rewind.android.automator.exception.*;
import one.rewind.android.automator.model.*;
import one.rewind.android.automator.ocr.OCRParser;
import one.rewind.android.automator.ocr.TesseractOCRParser;
import one.rewind.android.automator.util.*;
import one.rewind.db.RedissonAdapter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.redisson.api.RPriorityQueue;
import org.redisson.api.RedissonClient;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * @author maxuefeng[m17793873123@163.com]
 * Adapter对应是设备上的APP  任务执行应该放在Adapter层面上
 */
public abstract class AbstractWeChatAdapter extends Adapter {

	// 点击无响应重试上限
	public static final int TOUCH_RETRY_COUNT = 5;

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
	AbstractWeChatAdapter(AndroidDevice device) {
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
				} else if (status == Status.PublicAccount_Essay_List_Top) {
					throw new WeChatAdapterException.GetPublicAccountEssayListFrozenException(account);
				}
			}
		}
		return textAreaList;
	}

	public void reset() {

	}

	/**
	 * 点击左上角的返回按钮
	 */
	public void returnPreiousPage() {

	}

	public void goToSubscribePublicAccountList() {

		// 从首页点 通讯录

		// 点 公众号

	}

	public void goToPublicAccountHome(String mediaName) {

		// 点搜索

		// 输入名称

		// 点确认

		// 点第一个结果

		// 点右上角的人头图标

	}

	public void goToPublicAccontMoreInfoPage() {

		// 点右上三个点图标

		// 点更多资料
	}

	/**
	 * 订阅公众号
	 */
	public void subscribePublicAccount() {

	}

	/**
	 * 取消订阅
	 */
	public void unsubscribePublicAccount() {

	}

	/**
	 *
	 */
	public void gotoPublicAccountEssayList() {

		// 向下滑动

		// 点击全部消息

	}



	/**
	 * @param mediaName 公众号名称
	 * @param retry     是否重试
	 * @throws Exception e
	 */
	private void delegateOpenEssay(String mediaName, boolean retry) throws Exception {

		if (retry) {
			if (!restore(mediaName)) {
				return;
			}
		} else {
			firstPage.set(Boolean.FALSE);
			// 采集近期更新的文章,出发lastPage的条件是接着上一次更新的时间
			SubscribeMedia subscribeRecord = Tab.subscribeDao.queryBuilder().where().eq("udid", device.udid).and().eq("media_name", mediaName).queryForFirst();
			// 搜索不到数据
			if (subscribeRecord == null) {
				lastPage.set(Boolean.TRUE);
				return;
			}
		}

		int count = 0;

		while (!lastPage.get()) {

			if (count >= 5) {
				throw new AndroidCollapseException("崩溃异常");
			}
			// 翻到下一页
			if (!firstPage.get()) {
				slideToPoint(606, 2387, 606, 300, device.driver, 2000);
			} else {
				// 第一次
				slideToPoint(431, 1250, 431, 455, device.driver, 1000);
				firstPage.set(Boolean.FALSE);
			}

			this.taskLog.step();

			List<WordsPoint> wordsPoints = obtainClickPoints();

			if (wordsPoints.size() == 0) {

				count++;
				if (!lastPage.get()) {
					// TODO   很大程度的造成微信无故被关闭  很可能是因为图像识别的缘故或者安卓惯性造成(效率优化的一块存在)
					logger.info("图像识别的结果为空!");
				} else {
					logger.info("公众号{}抓取到最后一页了", mediaName);
				}
			} else {

				count = 0;
				openEssays(wordsPoints);
			}

		}
	}


	/**
	 * 恢复到上次数据采集的位置
	 *
	 * @param mediaName 任务名称
	 * @return true or false
	 */
	private boolean restore(String mediaName) {

		try {

			String filePrefix = UUID.randomUUID().toString();

			String fileName = filePrefix + ".png";

			String path = System.getProperty("user.dir") + "/screen/";

			screenshot(device.driver);

			// 恢复之前截图分析是否被限流
//			final JSONObject jsonObject = TesseractOCRParser.imageOcr(path + fileName, false);

			final List<OCRParser.TouchableTextArea> touchableTextAreas = TesseractOCRParser.getInstance().imageOcr(path + fileName, false);
			final JSONObject jsonObject = null;// TODO==========================

			final JSONArray array = jsonObject.getJSONArray("words_result");

			array.forEach(t -> {
				JSONObject tmp = (JSONObject) t;

				final String words = tmp.getString("words");

				if (words.contains("操作频") || words.contains("请稍后再")) {
					throw new WeChatRateLimitException("微信被限流了");
				}
			});

			long count = Tab.essayDao.queryBuilder().where().eq("media_nick", mediaName).countOf();

			this.firstPage.set(count == 0);

			if (!this.firstPage.get()) {

				int var = (int) count % 6;

				int slideNumByPage;

				if (var == 0) {
					slideNumByPage = (int) ((count / 6) + 2);
				} else if (var <= 3) {
					slideNumByPage = (int) (count / 6) + 1;
				} else {
					slideNumByPage = (int) (count / 6) + 2;
				}
				for (int i = 0; i < slideNumByPage; i++) {
					slideToPoint(606, 2387, 606, 960, device.driver, 1500);
				}
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * @param wordsPoints 每一篇文章的坐标点集合
	 * @throws AndroidCollapseException 安卓崩溃
	 * @throws InterruptedException     线程中断
	 */
	private void openEssays(List<WordsPoint> wordsPoints) throws AndroidCollapseException, InterruptedException {
		int neverClickCount = 0;
		for (WordsPoint wordsPoint : wordsPoints) {

			this.taskLog.step();

			// 点击不动卡主抛出安卓设备异常
			if (neverClickCount > 3) {
				throw new AndroidCollapseException("安卓系统卡住点不动了！");
			}
			clickPoint(320, wordsPoint.top, 8000, device.driver);
			//所以去判断下是否点击成功    成功：返回上一页面   失败：不返回上一页面  continue
			if (this.device.isTouchResponse()) {

				System.out.println("文章点进去了....");
				for (int i = 0; i < 2; i++) {

					slideToPoint(1413, 2369, 1413, 277, device.driver, 500);
					this.taskLog.step();
				}

				Thread.sleep(1000);

				//关闭文章
				DeviceUtil.closeEssay(device.driver);
				this.taskLog.step();

				//设置为默认值
				this.device.setTouchResponse(false);
				this.taskLog.step();
			} else {
				++neverClickCount;
			}

		}
	}

	/**
	 * 打开文章
	 *
	 * @param textAreas
	 */
	public void openEssays(List<OCRParser.TouchableTextArea> textAreas) {
		for (OCRParser.TouchableTextArea area : textAreas) {

		}
	}

	/**
	 * 取消订阅公众号方法
	 *
	 * @param mediaName
	 */
	public void unsubscribeMedia(String mediaName) {

		try {

			device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'通讯录')]")).click();

			Thread.sleep(1000);

			device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'公众号')]")).click();

			Thread.sleep(1000);

			device.driver.findElement(By.xpath("//android.widget.ImageButton[contains(@content-desc,'搜索')]")).click();

			Thread.sleep(500);

			// 搜索
			device.driver.findElement(By.className("android.widget.EditText")).sendKeys(mediaName);

			device.touch(720, 150, 1000);
			device.touch(1350, 2250, 1000);

			// 进入公众号
			device.touch(720, 360, 1000);

			device.driver.findElement(By.xpath("//android.widget.ImageButton[contains(@content-desc,'聊天信息')]")).click();

			Thread.sleep(1000);

			device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'取消关注')]")).click();
			Thread.sleep(1000);

			device.driver.findElement(By.xpath("//android.widget.Button[contains(@text,'不再关注')]")).click();
			Thread.sleep(1000);
		} catch (Exception e) {
			e.printStackTrace();
			device.driver.navigate().back();
			device.driver.navigate().back();
		}
		device.driver.navigate().back();
	}


	/**
	 * 订阅公众号
	 * <p>
	 * 要订阅的公众号可能存在一个问题就是搜索不到微信账号或者最准确的结果并不是第一个
	 *
	 * @param mediaName media
	 * @throws AlreadySubscribeException,SearchMediaException,InterruptedException,SQLException e
	 */
	public void subscribeMedia(String mediaName) throws AlreadySubscribeException, SearchMediaException, InterruptedException, SQLException {

		// 获取topic
		String topic = Tab.topic(mediaName);

		mediaName = Tab.realMedia(mediaName);

		if (Tab.subscribeDao.queryBuilder().where().eq("media_name", mediaName).countOf() >= 1) return;

		//重启
		DeviceUtil.restartWechat(device);

		Thread.sleep(3000);

		// A 点搜索
		device.driver.findElement(By.xpath("//android.widget.TextView[contains(@content-desc,'搜索')]")).click();
		Thread.sleep(5000);

		// B 点公众号
		device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'公众号')]")).click();
		Thread.sleep(5000);

		// C1 输入框输入搜索信息
		device.driver.findElement(By.className("android.widget.EditText")).sendKeys("\"" + mediaName + "\"");
		Thread.sleep(3000);

		// C3 点击软键盘的搜索键
		device.touch(1350, 2250, 6000); //TODO

		// 点击第一个位置
		device.touch(320, 406, 2000);

		// 点击订阅
		try {

			device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'关注公众号')]")).click();
			Thread.sleep(4000);
			saveSubscribeRecord(mediaName, topic);

		} catch (NoSuchElementException e) {
			e.printStackTrace();
			logger.error("查找不到当前公众号!");
			throw new AlreadySubscribeException("可能是已经订阅了当前公众号");
		}

	}



	/**
	 * @param mediaName media
	 * @throws Exception e
	 */
	private static boolean enterEssay(String mediaName, AndroidDevice device) throws Exception {

		try {

			Thread.sleep(6000);

			// Stream
			device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'通讯录')]")).click();

		} catch (Exception e) {
			e.printStackTrace();
			DeviceUtil.closeApp(device);
			DeviceUtil.activeWechat(device);
			device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'通讯录')]")).click();
		}
		Thread.sleep(1000);

		device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'公众号')]")).click();

		Thread.sleep(1000);

		device.driver.findElement(By.xpath("//android.widget.ImageButton[contains(@content-desc,'搜索')]")).click();

		Thread.sleep(1000);

		// 搜索
		device.driver.findElement(By.className("android.widget.EditText")).sendKeys(mediaName);

		device.touch(720, 150, 1000);

		device.touch(1350, 2250, 1000);
		try {
			// 进入公众号
			device.touch(720, 360, 1000);

			device.driver.findElement(By.xpath("//android.widget.ImageButton[contains(@content-desc,'聊天信息')]")).click();

			Thread.sleep(1000);

			device.slideToPoint(720, 1196, 720, 170, 1000);

			device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'全部消息')]")).click();

			Thread.sleep(12000);
			return true;
		} catch (Exception e) {
			try {
				SubscribeMedia var = Tab.subscribeDao.queryBuilder().where().eq("udid", device.udid).and().eq("media_name", mediaName).queryForFirst();
				if (var != null) {
					var.update_time = new Date();
					var.status = SubscribeMedia.State.NOT_EXIST.status;
					var.update();
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
			return false;
		}
	}


	/**
	 * 重试
	 *
	 * @param mediaName 公众号名称
	 * @param udid      手机udid
	 * @return 为空
	 * @throws Exception ex
	 */
	private static SubscribeMedia retry(String mediaName, String udid) throws Exception {
		SubscribeMedia media = Tab.subscribeDao.queryBuilder().where().eq("media_name", mediaName).and().eq("udid", udid).queryForFirst();
		if (media == null) {
			media = new SubscribeMedia();
			media.update_time = new Date();
			media.insert_time = new Date();
			media.status = SubscribeMedia.State.NOT_EXIST.status;
			media.media_name = mediaName;
			media.udid = udid;
			media.number = 0;
			media.retry_count = 0;
			media.insert();
			return null;
		}

		if (media.status == SubscribeMedia.State.NOT_EXIST.status) {
			media.retry_count = 0;
			media.update_time = new Date();
			media.number = 0;
			media.update();
			return null;
		}
		long count = Tab.essayDao.queryBuilder().where().eq("media_nick", mediaName).countOf();
		if (media.retry_count >= RETRY_COUNT) {
			media.update_time = new Date();
			media.number = (int) count;
			media.status = SubscribeMedia.State.FINISH.status;
			media.update();
			return null;
		}
		if (count < media.number) {
			// 此处
			return media;
		} else {
			return null;
		}
	}

	/**
	 * 采集任务的入口？
	 *
	 * @param mediaName
	 * @param retry
	 */
	public void digestionCrawler(String mediaName, boolean retry) {
		try {
			if (!enterEssay(mediaName, device)) {
				lastPage.set(Boolean.TRUE);
				for (int i = 0; i < 3; i++) {
					device.driver.navigate().back();
				}
				return;
			}
			delegateOpenEssay(mediaName, retry);

		} catch (Exception e) {

			e.printStackTrace();

			logger.error(e);

			device.clearCacheLog();

			device.clearAllLog();

			if (e instanceof AndroidCollapseException) {
				logger.error("设备{}链路出问题了.", device.udid);
				retryRecord(mediaName);
			} else if (e instanceof InterruptedException) {
				e.printStackTrace();
				logger.error("InterruptedException 线程中断异常！");
				retryRecord(mediaName);
			} else if (e instanceof NoSuchElementException) {
				// 如果搜索不到公众号，则会在此处形成死循环  其他没有捕获到的异常
				// TODO 此处不记录  属于操作层次的异常
				e.printStackTrace();
			} else if (e instanceof WeChatRateLimitException) {
				try {
					// 需要计算啥时候到达明天   到达明天的时候需要重新分配任务
					Date nextDay = DateUtil.buildDate();

					Date thisDay = new Date();

					long waitMills = Math.abs(nextDay.getTime() - thisDay.getTime());

					Thread.sleep(waitMills + 1000 * 60 * 5);

				} catch (Exception e2) {
					logger.error(e2);
				}
			} else {
				lastPage.set(Boolean.TRUE);
			}
		}
	}

	/**
	 * 记录重试日志
	 *
	 * @param mediaName
	 */
	private void retryRecord(String mediaName) {
		try {
			DeviceUtil.closeApp(device);
			DeviceUtil.activeWechat(this.device);
			SubscribeMedia media = retry(mediaName, this.device.udid);
			if (media != null) {
				media.retry_count += 1;
				media.update_time = new Date();
				media.update();
				if (media.retry_count >= RETRY_COUNT) lastPage.set(Boolean.TRUE);
			}

		} catch (Exception e1) {
			logger.error(e1);
		}
	}

	/**
	 * 订阅任务第一个人入口
	 *
	 * @param mediaName 公众号名称  可能包含topic和udid
	 */
	public void digestionSubscribe(String mediaName) {
		try {
			subscribeMedia(mediaName);
		} catch (Exception e) {

			this.taskLog.error();

			logger.error("失败原因如下");

			e.printStackTrace();

			if (e instanceof AlreadySubscribeException) {

				logger.info("失败原因是已经订阅了当前公众号; 设备:{};公众号:{}", device.udid, Tab.realMedia(mediaName));

				String topic = Tab.topic(mediaName);

				// 已经关注了
				mediaName = Tab.realMedia(mediaName);

				saveSubscribeRecord(mediaName, topic);

				try {
					// 返回到主界面即可
					for (int i = 0; i < 3; i++) {
						device.driver.navigate().back();
						Thread.sleep(1000);
					}
				} catch (InterruptedException e1) {
					logger.error(e1);
				}

			} else if (e instanceof SearchMediaException) {
				logger.info("失败原因是搜索出现问题;  设备:{};公众号:{}", device.udid, Tab.realMedia(mediaName));

				device.clearCacheLog();

				device.clearAllLog();

				try {
					// 截图关闭
					DeviceUtil.closeApp(device);

					DeviceUtil.restartWechat(device);

					// 是否是接口任务
					if (mediaName.contains(Tab.REQUEST_ID_SUFFIX)) {
						// 将数据添加到redis中保证数据没有丢失  形成一个闭环
						RedissonClient client = RedissonAdapter.redisson;

						RPriorityQueue<Object> topicMedia = client.getPriorityQueue(Tab.TOPIC_MEDIA);
						if (!topicMedia.contains(mediaName)) {
							topicMedia.add(mediaName);
						}
					}
				} catch (Exception e1) {
					logger.error(e1);
				}
			} else if (e instanceof InterruptedException) {
				logger.error("线程中断错误!!当前线程名称:{}", Thread.currentThread().getName());
				e.printStackTrace();
			} else if (e instanceof SQLException) {
				logger.error("SQL数据库操作异常!");
				e.printStackTrace();

			} else if (e instanceof NoSuchElementException) {

				try {
					logger.info("失败原因是NoSuchElementException");

					DeviceUtil.closeApp(device);
					DeviceUtil.restartWechat(device);
				} catch (InterruptedException ignore) {
					logger.error(ignore);
				}
			}

		}
	}
}
