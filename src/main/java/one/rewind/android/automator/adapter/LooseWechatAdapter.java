package one.rewind.android.automator.adapter;

import com.j256.ormlite.dao.Dao;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.exception.AndroidCollapseException;
import one.rewind.android.automator.exception.InvokingBaiduAPIException;
import one.rewind.android.automator.model.*;
import one.rewind.android.automator.util.AndroidUtil;
import one.rewind.android.automator.util.BaiduAPIUtil;
import one.rewind.android.automator.util.DBUtil;
import one.rewind.db.DaoManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Create By 2018/10/10
 * Description: 微信的自动化操作
 */
@SuppressWarnings("JavaDoc")
public class LooseWechatAdapter extends Adapter {

    private boolean lastPage = false;

    private boolean firstPage = true;

    private ExecutorService executor;

    private void setExecutor() {
        this.executor =
                new ThreadPoolExecutor(0,
                        Integer.MAX_VALUE,
                        60,
                        TimeUnit.SECONDS,
                        new SynchronousQueue<>(),
                        threadFactory(UUID.randomUUID().toString(), false));
    }

    private static ThreadFactory threadFactory(final String name, final boolean daemon) {
        return runnable -> {
            Thread result = new Thread(runnable, name);
            result.setDaemon(daemon);
            return result;
        };
    }

    private TaskType taskType = null;

    public static final int ESSAY_NUM = 100;

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    private LooseWechatAdapter(AndroidDevice device) {
        super(device);
    }


    private static Dao<Essays, String> essayDao;

    private static Dao<SubscribeAccount, String> subscribeDao;

    static {
        try {
            essayDao = DaoManager.getDao(Essays.class);
            subscribeDao = DaoManager.getDao(SubscribeAccount.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private List<WordsPoint> obtainPoints(String mediaName) throws InterruptedException, InvokingBaiduAPIException {
        String filePrefix = UUID.randomUUID().toString();
        String fileName = filePrefix + ".png";
        String path = System.getProperty("user.dir") + "/screen/";
        AndroidUtil.screenshot(fileName, path, driver);
        //图像分析   截图完成之后需要去掉头部的截图信息  头部包括一些数据
        List<WordsPoint> wordsPoints = analysisImage(mediaName, path + fileName);
        if (wordsPoints != null && wordsPoints.size() > 0) {
            return wordsPoints;
        } else {
            AndroidUtil.returnPrevious(driver);
            return null;
        }
    }

    /**
     * @param mediaName
     * @param filePath
     * @return
     */
    @SuppressWarnings("JavaDoc")
    private List<WordsPoint> analysisImage(String mediaName, String filePath) throws InvokingBaiduAPIException {
        JSONObject jsonObject = BaiduAPIUtil.imageOCR(filePath);
        //得到即将要点击的坐标位置
        return analysisPoint(jsonObject.getJSONArray("words_result"), mediaName);

    }


    /**
     * @param array
     * @param mediaName
     * @return
     */
    private List<WordsPoint> analysisPoint(JSONArray array, String mediaName) {

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
                    long currentEssayNum = essayDao.queryBuilder().where().eq("media_nick", mediaName).countOf();
                    SubscribeAccount var = subscribeDao.queryBuilder().where().eq("udid", device.udid).and().eq("media_name", mediaName).queryForFirst();
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
    private void openEssay(String mediaName, boolean retry) throws AndroidCollapseException {
        try {
            if (retry) {
                TaskFailRecord record = AndroidUtil.retry(mediaName, essayDao, device.udid);
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
                List<WordsPoint> wordsPoints = obtainPoints(mediaName);
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

    public void start() throws Exception {
        this.setExecutor();
        Task task = new Task();
        this.taskType = calculateTaskType(this.device.udid);
        task.setRetry(TaskType.CRAWLER.equals(this.taskType));
        this.executor.execute(task);
    }


    class Task implements Runnable {

        private boolean retry;

        boolean getRetry() {
            return retry;
        }

        void setRetry(boolean retry) {
            this.retry = retry;
        }

        @Override
        public void run() {
            assert taskType != null;
            if (TaskType.SUBSCRIBE.equals(taskType)) {
                try {
                    for (String var : device.queue) {
                        digestionSubscribe(var, false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (TaskType.CRAWLER.equals(taskType)) {
                try {
                    for (String var : device.queue) {
                        lastPage = false;
                        digestionCrawler(var, getRetry());
                        AndroidUtil.updateProcess(var, device.udid);
                        //返回到主界面
                        for (int i = 0; i < 5; i++) {
                            driver.navigate().back();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void execute() {

        }
    }

    /**
     * 订阅公众号
     * <p>
     * 要订阅的公众号可能存在一个问题就是搜索不到微信账号或者最准确的结果并不是第一个
     *
     * @param mediaName
     * @throws Exception
     * @see DBUtil#reset()
     */
    private void subscribeWxAccount(String mediaName) throws Exception {
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

        //根据
        WordsPoint point = AndroidUtil.accuracySubscribe(mediaName);
        if (point == null) {
            SubscribeAccount tmp = new SubscribeAccount();
            tmp.media_name = mediaName;
            tmp.status = SubscribeAccount.CrawlerState.NOMEDIANAME.status;
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
        long tempCount = subscribeDao.queryBuilder().where()
                .eq("media_name", mediaName)
                .countOf();
        if (tempCount == 0) {
            //订阅完成之后再数据库存储记录
            SubscribeAccount e = new SubscribeAccount();
            e.udid = device.udid;
            e.media_name = mediaName;
            e.insert();
        }
    }


    /**
     * 针对于在抓取微信公众号文章时候的异常处理   失败无限重试  直到当前公众号的所有文章抓取完成
     *
     * @param mediaName
     */
    private void digestionCrawler(String mediaName, boolean retry) {
        try {
            if (!AndroidUtil.enterEssaysPage(mediaName, device)) {
                return;
            }
            openEssay(mediaName, retry);
        } catch (AndroidCollapseException e) {
            e.printStackTrace();
            try {
                //进入重试
                AndroidUtil.closeApp(driver);
                Thread.sleep(10000);
                AndroidUtil.activeWechat(device);
                long number = essayDao.queryBuilder().where().eq("media_nick", mediaName).countOf();
                SubscribeAccount tmp = subscribeDao.queryBuilder().where().eq("media_name", mediaName).
                        and().
                        eq("udid", device.udid).
                        queryForFirst();
                if (tmp.number - number > 5) digestionCrawler(mediaName, true);
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
    private void digestionSubscribe(String mediaName, boolean retry) throws Exception {
        try {
            if (retry) {
                AndroidUtil.closeApp(driver);
                AndroidUtil.activeWechat(device);
            }
            subscribeWxAccount(mediaName);
        } catch (Exception e) {
            e.printStackTrace();
            Dao<SubscribeAccount, String> dao = DaoManager.getDao(SubscribeAccount.class);
            SubscribeAccount forFirst = dao.queryBuilder().where().eq("media_name", mediaName).queryForFirst();
            if (forFirst == null) {
                digestionSubscribe(mediaName, true);
            }
        }
    }

    public static class Builder {

        private AndroidDevice device;

        public Builder device(AndroidDevice device) {
            this.device = device;
            return this;
        }

        public LooseWechatAdapter build() {
            return new LooseWechatAdapter(this.device);
        }
    }


    /**
     * 主要计算设备当前应该处于的一个状态
     *
     * @return
     * @throws Exception
     */
    private static TaskType calculateTaskType(String udid) throws Exception {

        long allSubscribe = subscribeDao.queryBuilder().where().eq("udid", udid).countOf();

        List<SubscribeAccount> notFinishR = subscribeDao.queryBuilder().where().
                eq("udid", udid).and().
                eq("status", SubscribeAccount.CrawlerState.NOFINISH.status).
                query();

        int todaySubscribe = DBUtil.obtainSubscribeNumToday(udid);

        if (allSubscribe > 993 && todaySubscribe >= 40) {
            return TaskType.CRAWLER;
        } else if (allSubscribe < 993 && todaySubscribe >= 40) {
            return TaskType.CRAWLER;
        } else if (allSubscribe < 993) {
            if (notFinishR.size() == 0) {
                return TaskType.SUBSCRIBE;
            } else {
                return TaskType.CRAWLER;
            }
        }
        return null;
    }

}
