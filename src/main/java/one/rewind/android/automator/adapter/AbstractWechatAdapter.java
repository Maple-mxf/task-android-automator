package one.rewind.android.automator.adapter;

import com.google.common.collect.Lists;
import com.j256.ormlite.dao.Dao;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.DBTab;
import one.rewind.android.automator.exception.AndroidCollapseException;
import one.rewind.android.automator.exception.InvokingBaiduAPIException;
import one.rewind.android.automator.model.FailRecord;
import one.rewind.android.automator.model.SubscribeMedia;
import one.rewind.android.automator.model.TaskType;
import one.rewind.android.automator.model.WordsPoint;
import one.rewind.android.automator.util.AndroidUtil;
import one.rewind.android.automator.util.BaiduAPIUtil;
import one.rewind.db.DaoManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public abstract class AbstractWechatAdapter extends Adapter {

    protected boolean lastPage = false;

    protected boolean firstPage = true;

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public static volatile ExecutorService executor;

    protected TaskType taskType = null;

    public static final int RETRY_COUNT = 5;


    public AbstractWechatAdapter(AndroidDevice device) {
        super(device);
    }

    public WordsPoint accuracySubscribe(String mediaName) throws InvokingBaiduAPIException {

        String fileName = UUID.randomUUID().toString() + ".png";
        String path = System.getProperty("user.dir") + "/screen/";

        AndroidUtil.screenshot(fileName, path, device.driver);

        JSONObject jsonObject = BaiduAPIUtil.imageOCR(path + fileName);

        JSONArray result = jsonObject.getJSONArray("words_result");

        int top;
        int left;

        for (Object v : result) {
            JSONObject b = (JSONObject) v;
            String words = b.getString("words");
            if (words.equals(mediaName)) {
                top = b.getInt("top");
                left = b.getInt("left");
                return new WordsPoint(top + 15, left, 0, 0, "");
            }
        }
        return null;
    }

    protected List<WordsPoint> obtainClickPoints(String mediaName) throws InterruptedException, InvokingBaiduAPIException {
        String filePrefix = UUID.randomUUID().toString();
        String fileName = filePrefix + ".png";
        String path = System.getProperty("user.dir") + "/screen/";
        AndroidUtil.screenshot(fileName, path, driver);
        //图像分析   截图完成之后需要去掉头部的截图信息  头部包括一些数据
        List<WordsPoint> wordsPoints = analysisImage(mediaName, path + fileName);
        if (wordsPoints != null && wordsPoints.size() > 0) {
            return wordsPoints;
        } else {
            //异常的具体原因是点击没反应，程序自动点击叉号进行关闭，已经返回到上一页面
            //当前公众号不能继续抓取了
            AndroidUtil.returnPrevious(driver);
            return null;
        }
    }

    /**
     * 分析图像
     *
     * @param mediaName
     * @param filePath
     * @return
     */
    @SuppressWarnings("JavaDoc")
    protected List<WordsPoint> analysisImage(String mediaName, String filePath) throws InvokingBaiduAPIException {
        JSONObject jsonObject = BaiduAPIUtil.imageOCR(filePath);
        //得到即将要点击的坐标位置
        return analysisWordsPoint(jsonObject.getJSONArray("words_result"), mediaName);

    }

    /**
     * {"words":"My Bastis三种批量插入方式的性能","location":{"top":1305,"left":42,"width":932,"height":78}}
     * {"words":"找工作交流群(北上广深杭成都重庆", "location":{"top":1676,"left":42,"width":972,"height":72}}
     * {"words":"南京武汉长沙西安)",            "location":{"top":1758,"left":55,"width":505,"height":72}}
     * {"words":"从初级程序员到编程大牛,只需要每","location":{"top":2040,"left":40,"width":978,"height":85}}
     * {"words":"天坚持做这件事情.",           "location":{"top":2130,"left":43,"width":493,"height":71}}
     *
     * @param array
     * @param mediaName
     * @return
     */
    private List<WordsPoint> analysisWordsPoint(JSONArray array, String mediaName) {

        array.remove(0);

        List<WordsPoint> wordsPoints = new ArrayList<>();

        //计算坐标  文章的标题最多有两行  标题过长微信会使用省略号代替掉
        for (Object o : array) {

            JSONObject outJSON = (JSONObject) o;

            JSONObject inJSON = outJSON.getJSONObject("location");

            String words = outJSON.getString("words");

            if (words.contains("已无更多")) {

                System.out.println("============================没有更多文章===================================");

                lastPage = true;

                try {
                    //计算当前公众号文章数量
                    long currentEssayNum = DBTab.essayDao.queryBuilder().where().eq("media_nick", mediaName).countOf();
                    SubscribeMedia var = DBTab.subscribeDao.queryBuilder().where().eq("udid", device.udid).and().eq("media_name", mediaName).queryForFirst();
                    var.number = (int) (currentEssayNum + wordsPoints.size());
                    var.update();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("是否到最后一页：" + lastPage);

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
     * @param mediaName
     * @throws InterruptedException
     */
    public void getIntoPublicAccountEssayList(String mediaName, boolean retry) throws AndroidCollapseException {
        try {
            if (retry) {
                FailRecord record = AndroidUtil.retry(mediaName, device.udid);
                if (record == null) {
                    //当前公众号抓取的文章已经达到100篇以上
                    return;
                } else {
                    firstPage = (record.finishNum == 0);
                    if (!firstPage) {
                        //下滑到第一页
                        AndroidUtil.slideToPoint(431, 1250, 431, 455, driver, 1000);
                        //向下划指定页数
                        for (int i = 0; i < record.slideNumByPage; i++) {
                            AndroidUtil.slideToPoint(606, 2387, 606, 960, driver, 1000);
                        }
                    }
                }
            }
            while (!lastPage) {
                List<WordsPoint> wordsPoints = obtainClickPoints(mediaName);
                //获取模拟点击的坐标位置
                //下滑到指定的位置
                if (firstPage) {
                    AndroidUtil.slideToPoint(431, 1250, 431, 455, driver, 0);
                    firstPage = false;
                } else {
                    AndroidUtil.slideToPoint(606, 2387, 606, 960, driver, 5000);
                }

                if (wordsPoints == null) {
                    logger.error("链路出现雪崩的情况了！one.rewind.android.automator.adapter.WechatAdapter.openEssay");
                    throw new AndroidCollapseException("可能是系统崩溃！请检查百度API调用和安卓系统是否崩溃 one.rewind.android.automator.adapter.WechatAdapter.openEssay");
                } else {
                    //点击计算出来的坐标
                    openEssays(wordsPoints);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("=========================当前设备{}已经崩溃了=============================", device.udid);
            throw new AndroidCollapseException("链路出现雪崩的情况了:one.rewind.android.automator.adapter.WechatAdapter.openEssay");
        }
    }


    private void openEssays(List<WordsPoint> wordsPoints) throws InterruptedException, AndroidCollapseException {
        int neverClickCount = 0;
        for (WordsPoint wordsPoint : wordsPoints) {
            if (neverClickCount > 3) {
                throw new AndroidCollapseException("安卓系统卡住点不动了！");
            }

            AndroidUtil.clickPoint(320, wordsPoint.top, 5000, driver);

            // 有很大的概率点击不进去
            //所以去判断下是否点击成功    成功：返回上一页面   失败：不返回上一页面  continue
            if (this.device.isClickEffect()) {
                System.out.println("文章点进去了....");
                for (int i = 0; i < 2; i++) {
                    AndroidUtil.slideToPoint(457, 2369, 457, 277, driver, 500);
                }
                Thread.sleep(1000);
                //关闭文章
                AndroidUtil.closeEssay(driver);
                //设置为默认值
                this.device.setClickEffect(false);
            } else {
                ++neverClickCount;
            }
        }
    }

    /**
     * 订阅公众号
     * <p>
     * 要订阅的公众号可能存在一个问题就是搜索不到微信账号或者最准确的结果并不是第一个
     *
     * @param mediaName
     * @throws Exception
     */
    public void subscribeWxAccount(String mediaName) throws Exception {
        if (DBTab.subscribeDao.queryBuilder().where().eq("media_name", mediaName).countOf() >= 1) return;
        int k = 3;
        // A 点搜索
        WebElement searchButton = driver.findElement(By.xpath("//android.widget.TextView[contains(@content-desc,'搜索')]"));
        searchButton.click();
        Thread.sleep(500);

        // B 点公众号
        WebElement publicAccountLink = driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'公众号')]"));
        publicAccountLink.click();
        Thread.sleep(2000);

        // C1 输入框输入搜索信息
        driver.findElement(By.className("android.widget.EditText")).sendKeys(mediaName);

        // C3 点击软键盘的搜索键
        AndroidUtil.clickPoint(1350, 2250, 6000, driver); //TODO 时间适当调整

        WordsPoint point = accuracySubscribe(mediaName);
        if (point == null) {
            SubscribeMedia tmp = new SubscribeMedia();
            tmp.media_name = mediaName;
            tmp.status = 2;
            tmp.update_time = new Date();
            tmp.number = 0;
            tmp.udid = udid;
            tmp.insert_time = new Date();
            tmp.insert();
            return;
        }
        AndroidUtil.clickPoint(point.left, point.top, 2000, driver);

        // 点击订阅
        try {
            driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'关注公众号')]"))
                    .click();
            saveSubscribeRecord(mediaName);
            Thread.sleep(3000);
            driver.navigate().back();
        } catch (Exception e) {
            //已经订阅了
            e.printStackTrace();
            logger.info("Already add public account: {}", mediaName);
            driver.navigate().back();
            --k;
        }
        Thread.sleep(1500);
        for (int i = 0; i < k; i++) {
            driver.navigate().back();
            Thread.sleep(500);
        }
    }

    private void saveSubscribeRecord(String mediaName) throws Exception {
        long tempCount = DBTab.subscribeDao.queryBuilder().where()
                .eq("media_name", mediaName)
                .countOf();
        if (tempCount == 0) {
            SubscribeMedia e = new SubscribeMedia();
            e.udid = device.udid;
            e.media_name = mediaName;
            e.insert();
        }
    }


    public static List<Future<?>> futures = Lists.newArrayList();

    /**
     * 针对于在抓取微信公众号文章时候的异常处理   失败无限重试  直到当前公众号的所有文章抓取完成
     *
     * @param mediaName
     */
    public void digestionCrawler(String mediaName, boolean retry) {
        try {
            //继续获取文章
            if (!AndroidUtil.enterEssaysPage(mediaName, device)) {
                return;
            }
            getIntoPublicAccountEssayList(mediaName, retry);
        } catch (AndroidCollapseException e) {
            e.printStackTrace();
            try {
                try {
                    SubscribeMedia subscribeMedia =
                            DBTab.subscribeDao.
                                    queryBuilder().
                                    where().
                                    eq("media_name", mediaName).
                                    and().
                                    eq("udid", this.device.udid).
                                    queryForFirst();

                    if (subscribeMedia == null) return;


                    if (subscribeMedia.retry_count >= RETRY_COUNT) return;

                    subscribeMedia.update_time = new Date();
                    subscribeMedia.retry_count += 1;

                    subscribeMedia.update();

                    AndroidUtil.closeApp(driver);

                    Thread.sleep(10000);

                    AndroidUtil.activeWechat(device);

                    digestionCrawler(mediaName, true);

                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 订阅公众号重试机制
     *
     * @param mediaName
     * @param retry
     */
    public void digestionSubscribe(String mediaName, boolean retry) throws Exception {
        try {
            if (retry) {
                AndroidUtil.closeApp(driver);
                AndroidUtil.activeWechat(device);
            }
            subscribeWxAccount(mediaName);
        } catch (Exception e) {
            e.printStackTrace();
            Dao<SubscribeMedia, String> dao = DaoManager.getDao(SubscribeMedia.class);
            SubscribeMedia forFirst = dao.queryBuilder().where().eq("media_name", mediaName).queryForFirst();
            if (forFirst == null) {
                digestionSubscribe(mediaName, true);
            }
        }
    }


}
