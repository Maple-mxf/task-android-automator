package one.rewind.android.automator.adapter;

import com.google.common.collect.Lists;
import io.appium.java_client.TouchAction;
import io.appium.java_client.touch.offset.PointOption;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.AndroidDeviceManager;
import one.rewind.android.automator.exception.AndroidCollapseException;
import one.rewind.android.automator.exception.AndroidException;
import one.rewind.android.automator.exception.InvokingBaiduAPIException;
import one.rewind.android.automator.model.SubscribeAccount;
import one.rewind.android.automator.model.WordsPoint;
import one.rewind.android.automator.util.AndroidUtil;
import one.rewind.android.automator.util.BaiduAPIUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * 微信的自动化操作
 */
@SuppressWarnings("JavaDoc")
public class WechatAdapter extends Adapter {

	private boolean isLastPage = false;

	private boolean isFirstPage = true;

	public static ExecutorService executor = Executors.newFixedThreadPool(10);

	public static AndroidDeviceManager.TaskType taskType = null;

	public WechatAdapter(AndroidDevice device) {
		super(device);
	}


	/**
	 * @param name
	 * @throws InterruptedException
	 */
	private void enterEssaysPage(String name) throws InterruptedException {
		driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'通讯录')]")).click();

		Thread.sleep(1000);

		driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'公众号')]")).click();

		Thread.sleep(1000);

		driver.findElement(By.xpath("//android.widget.ImageButton[contains(@content-desc,'搜索')]")).click();

		Thread.sleep(1000);

		// 搜索
		driver.findElement(By.className("android.widget.EditText")).sendKeys(name);

		AndroidUtil.clickPoint(720, 150, 0, driver);

		AndroidUtil.clickPoint(1350, 2250, 2000, driver);

		// 进入公众号
		AndroidUtil.clickPoint(720, 360, 1000, driver);

		driver.findElement(By.xpath("//android.widget.ImageButton[contains(@content-desc,'聊天信息')]")).click();

		Thread.sleep(1000);

		AndroidUtil.slideToPoint(720, 1196, 720, 170, driver);

		Thread.sleep(1000);

		driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'全部消息')]")).click();

		Thread.sleep(7000); // TODO 此处时间需要调整

	}


	private List<WordsPoint> obtainClickPoints() throws InterruptedException, InvokingBaiduAPIException {
		//截图
		String filePrefix = UUID.randomUUID().toString();
		String fileName = filePrefix + ".png";
		String path = System.getProperty("user.dir") + "/screen/";
		AndroidUtil.screenshot(fileName, path, driver);
		//图像分析   截图完成之后需要去掉头部的截图信息  头部包括一些数据
		List<WordsPoint> wordsPoints = analysisImage(path + fileName);
		if (wordsPoints != null && wordsPoints.size() > 0) {
			return wordsPoints;
		} else {
			/**
			 * 此处已经出现异常：====>>>> 异常的具体原因是点击没反应，程序自动点击叉号进行关闭，已经返回到上一页面
			 *
			 * 当前公众号不能继续抓取了
			 */
			AndroidUtil.returnPrevious(driver);
			return null;
		}
	}

	/**
	 * 分析图像
	 *
	 * @param filePath
	 * @return
	 */
	@SuppressWarnings("JavaDoc")
	private List<WordsPoint> analysisImage(String filePath) throws InvokingBaiduAPIException {
		JSONObject jsonObject = BaiduAPIUtil.executeImageRecognitionRequest(filePath);
		/**
		 * 得到即将要点击的坐标位置
		 */
		return analysisWordsPoint(jsonObject.getJSONArray("words_result"));

	}


	/**
	 * {"words":"My Bastis三种批量插入方式的性能","location":{"top":1305,"left":42,"width":932,"height":78}}
	 * {"words":"找工作交流群(北上广深杭成都重庆", "location":{"top":1676,"left":42,"width":972,"height":72}}
	 * {"words":"南京武汉长沙西安)",            "location":{"top":1758,"left":55,"width":505,"height":72}}
	 * {"words":"从初级程序员到编程大牛,只需要每","location":{"top":2040,"left":40,"width":978,"height":85}}
	 * {"words":"天坚持做这件事情.",           "location":{"top":2130,"left":43,"width":493,"height":71}}
	 *
	 * @param array
	 * @return
	 */
	public List<WordsPoint> analysisWordsPoint(JSONArray array) {

		array.remove(0);

		List<WordsPoint> wordsPoints = new ArrayList<>();

		//计算坐标  文章的标题最多有两行  标题过长微信会使用省略号代替掉
		for (Object o : array) {

			JSONObject outJSON = (JSONObject) o;

			JSONObject inJSON = outJSON.getJSONObject("location");

			String words = outJSON.getString("words");

			if (words.contains("已无更多")) {

				isLastPage = true;

			}

			int top = inJSON.getInt("top");

			int left = inJSON.getInt("left");

			int width = inJSON.getInt("width");

			int height = inJSON.getInt("height");

			//确保时间标签的位置   有可能有年月日字符串的在文章标题中   为了防止这种情况   left<=80

			if (words.contains("年") && words.contains("月") && words.contains("日") && left <= 80) {

				wordsPoints.add(new WordsPoint((top), left, width, height, words));
			}
		}
		return wordsPoints;
	}


	/**
	 * 获取公众号的文章列表
	 *
	 * @param wxPublicName
	 * @throws InterruptedException
	 */
	public void getIntoPublicAccountEssayList(String wxPublicName) throws InterruptedException, AndroidCollapseException, InvokingBaiduAPIException {
		try {
			enterEssaysPage(wxPublicName);

			while (!isLastPage) {
				/**
				 * 下滑到指定的位置
				 */
				if (isFirstPage) {

					AndroidUtil.slideToPoint(431, 1250, 431, 455, driver);

					isFirstPage = false;

				} else {

					AndroidUtil.slideToPoint(606, 2387, 606, 960, driver);

					Thread.sleep(10000);
				}
				//获取模拟点击的坐标位置
				List<WordsPoint> wordsPoints = obtainClickPoints();

				if (wordsPoints == null) {

					logger.error("链路出现雪崩的情况了！one.rewind.android.automator.adapter.WechatAdapter.getIntoPublicAccountEssayList");

					throw new AndroidCollapseException("可能是系统崩溃！请检查百度API调用和安卓系统是否崩溃 one.rewind.android.automator.adapter.WechatAdapter.getIntoPublicAccountEssayList");
				} else {
					//点击计算出来的坐标
					openEssays(wordsPoints);
				}
			}

		} catch (WebDriverException e) {
			e.printStackTrace();
			/**
			 * 如果此处捕获到异常  则说明移动端的微信处于挂掉状态
			 * 解决方法：截图  分析是否存在等待或者关闭等
			 * 如果存在，关闭微信  进入微信一路返回进入主界面
			 * 进入主界面之后再次搜索公众号名称
			 */
			e.printStackTrace();

			logger.error("系统崩溃！");

			throw new AndroidCollapseException("链路出现雪崩的情况了:one.rewind.android.automator.adapter.WechatAdapter.getIntoPublicAccountEssayList");
		} catch (InvokingBaiduAPIException e) {

			e.printStackTrace();

			throw new InvokingBaiduAPIException("百度API调用失败");
		}
	}


	private void openEssays(List<WordsPoint> wordsPoints) throws InterruptedException, AndroidCollapseException {

		int neverClickCount = 0;
		for (WordsPoint wordsPoint : wordsPoints) {

			if (neverClickCount > 3) {
				throw new AndroidCollapseException("安卓系统卡住点不动了！");
			}

			AndroidUtil.clickPoint(320, wordsPoint.top, 8000, driver);
			/**
			 * 有很大的概率点击不进去
			 * 所以去判断下是否点击成功    成功：返回上一页面   失败：不返回上一页面  continue
			 */

			if (this.device.isClickEffect()) {
				for (int i = 0; i < 10; i++) {
					AndroidUtil.slideToPoint(457, 2369, 457, 277, driver);
				}
				//返回到上一页面
				AndroidUtil.returnPrevious(driver);
				/**
				 *  设置为默认值
				 */
				this.device.setClickEffect(false);
			} else {
				++neverClickCount;
			}
		}
	}

	/**
	 *
	 */
	public class Start implements Callable<Boolean> {

		@Override
		public Boolean call() throws Exception {

			Random random = new Random();

//			init(random.nextInt(50000));

			assert taskType != null;
			if (taskType.equals(AndroidDeviceManager.TaskType.SUBSCRIBE)) {
				for (String var : device.queue) {

					searchPublicAccount(var, true);

					//订阅完成之后再数据库存储记录
					SubscribeAccount e = new SubscribeAccount();

					e.udid = device.udid;
					e.media_name = var;
					e.insert();
				}
			} else if (taskType.equals(AndroidDeviceManager.TaskType.CRAWLER)) {
				for (String var : device.queue) {
					digestion(var);
				}
			}
			return true;
		}
	}

	public class Close implements Callable<Boolean> {
		@Override
		public Boolean call() {
			logger.info("Stopping....Please Wait!");
			if (device == null) return false;
			device.driver.closeApp();
			device.state = AndroidDevice.State.CLOSE;
			return true;
		}
	}



	/**
	 * @throws ExecutionException
	 * @throws InterruptedException
	 * @throws AndroidException
	 * @throws InvokingBaiduAPIException
	 */
	public void start() {
		Future<Boolean> future = executor.submit(new Start());
		submit(future);
		logger.info("任务提交完毕！");
	}


	/**
	 * 将执行结果添加进来
	 *
	 * @param future
	 */
	public void submit(Future<?> future) {
		futures.add(future);
	}

	/**
	 * 关闭APP   系统崩溃时调用
	 *
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public void close() throws ExecutionException, InterruptedException {
		Future<Boolean> submit = executor.submit(new Close());
		if (submit.get()) {
			executor.submit(new Close());
			device.state = AndroidDevice.State.CLOSE;
		}

	}

	/**
	 * 订阅公众号
	 *
	 * @param name
	 * @throws Exception
	 */
	public void searchPublicAccount(String name, boolean subscribe) throws Exception {
		// A 点搜索
		WebElement searchButton = driver
				.findElement(By.xpath("//android.widget.TextView[contains(@content-desc,'搜索')]"));
		searchButton.click();

		Thread.sleep(1000);

		// B 点公众号
		WebElement publicAccountLink = driver
				.findElement(By.xpath("//android.widget.TextView[contains(@text,'公众号')]"));
		publicAccountLink.click();

		Thread.sleep(4000);

		// C1 输入框输入搜索信息
		driver.findElement(By.className("android.widget.EditText")).sendKeys(name);
		// C2 点击搜索输入框
		new TouchAction(driver).tap(PointOption.point(720, 150)).perform();
		/*Thread.sleep(100);*/
		// C3 点击软键盘的搜索键
		new TouchAction(driver).tap(PointOption.point(1350, 2250)).perform();

		Thread.sleep(4000);

		// D 点击第一个结果
		new TouchAction(driver).tap(PointOption.point(720, 600)).perform();

		Thread.sleep(2000);

		// 是否直接进行订阅
		if (subscribe) {
			// 点击订阅
			try {

				driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'关注公众号')]"))
						.click();

				Thread.sleep(2000);

				driver.navigate().back();

			} catch (Exception e) {
				logger.info("Already add public account: {}", name);
			}
		}

		Thread.sleep(1000);
		driver.navigate().back();
		Thread.sleep(500);
		driver.navigate().back();
		Thread.sleep(500);
		driver.navigate().back();
		Thread.sleep(500);
	}


	public static List<Future<?>> futures = Lists.newArrayList();

	/**
	 * 针对于在抓取微信公众号文章时候的异常处理
	 *
	 * @param wxAccountName
	 */
	private void digestion(String wxAccountName) {
		try {
			getIntoPublicAccountEssayList(wxAccountName);
		} catch (InterruptedException e) {
			e.printStackTrace();

			logger.error("====线程中断异常====");

		} catch (AndroidCollapseException e) {

			e.printStackTrace();

			logger.error("=====安卓系统出现崩溃情况！=====");

			try {
				/**
				 * 当前设备系统卡死    关闭app之后再次进入微信进行操作
				 */
				close();

				Thread.sleep(5000);

				/**
				 *  重新启动APP
				 */
				start();
			} catch (ExecutionException | InterruptedException e1) {
				e1.printStackTrace();
			}

		} catch (InvokingBaiduAPIException e) {

			e.printStackTrace();

			logger.error("=====百度API调用失败！=====");
		}
	}

}
