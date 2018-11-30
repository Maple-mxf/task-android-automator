package one.rewind.android.automator.adapter;

import joptsimple.internal.Strings;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.exception.AndroidCollapseException;
import one.rewind.android.automator.exception.InvokingBaiduAPIException;
import one.rewind.android.automator.model.DBTab;
import one.rewind.android.automator.model.SubscribeMedia;
import one.rewind.android.automator.model.WordsPoint;
import one.rewind.android.automator.util.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

public abstract class AbstractWechatAdapter extends Adapter {

    ThreadLocal<Boolean> lastPage = new ThreadLocal<>();

    ThreadLocal<Boolean> firstPage = new ThreadLocal<>();

    /**
     * 上一次分析点击坐标记录的集合
     */
    ThreadLocal<Set<String>> previousEssayTitles = new ThreadLocal<>();


    private ThreadLocal<Integer> countVal = new ThreadLocal<>();

    void setCountVal() {
        if (countVal.get() != null) {
            int var = countVal.get();
            var += 1;
            countVal.set(var);
        } else {
            countVal.set(1);
        }
    }


    public static final int RETRY_COUNT = 5;


    AbstractWechatAdapter(AndroidDevice device) {
        super(device);
    }

    private WordsPoint accuracySubscribe(String mediaName) throws Exception {

        String fileName = UUID.randomUUID().toString() + ".png";
        String path = System.getProperty("user.dir") + "/screen/";

        AndroidUtil.screenshot(fileName, path, device.driver);

        JSONObject jsonObject = BaiduAPIUtil.imageOCR(path + fileName);

        FileUtil.deleteFile(path + fileName);

        JSONArray result = jsonObject.getJSONArray("words_result");
        result.remove(0);
        result.remove(0);
        result.remove(0);

        int top;
        int left;

        for (Object v : result) {
            JSONObject b = (JSONObject) v;
            String words = b.getString("words");
            if (words.startsWith("(")) words = words.replace("(", "");
            if (words.startsWith(")")) words = words.replace(")", "");
            words = words.replaceAll(" ", "");

            JSONObject location = b.getJSONObject("location");
            top = location.getInt("top");
            left = location.getInt("left");
            if (left <= 50 && words.endsWith(mediaName)) {
                return new WordsPoint(top + 30, left + 30, 0, 0, words);
            }
            if (words.equalsIgnoreCase(mediaName) || words.equalsIgnoreCase("<" + mediaName)) {
                return new WordsPoint(top + 30, left + 30, 0, 0, words);
            }
        }
        return null;
    }

    private List<WordsPoint> obtainClickPoints(String mediaName) throws Exception {
        String filePrefix = UUID.randomUUID().toString();
        String fileName = filePrefix + ".png";
        String path = System.getProperty("user.dir") + "/screen/";
        AndroidUtil.screenshot(fileName, path, driver);
        // 图像分析   截图完成之后需要去掉头部的截图信息  头部包括一些数据
        return analysisImage(mediaName, path + fileName);
    }

    /**
     * 分析图像
     *
     * @param mediaName
     * @param filePath
     * @return
     */
    @SuppressWarnings("JavaDoc")
    private List<WordsPoint> analysisImage(String mediaName, String filePath) throws Exception {
        JSONObject jsonObject = BaiduAPIUtil.imageOCR(filePath);
        FileUtil.deleteFile(filePath);
        //得到即将要点击的坐标位置
        return analysisWordsPoint(jsonObject.getJSONArray("words_result"), mediaName);

    }

    /**
     * {"words":"My Bastis三种批量插入方式的性能","location":{"top":1305,"left":42,"width":932,"height":78}}
     *
     * @param array
     * @param mediaName
     * @return
     */
    private List<WordsPoint> analysisWordsPoint(JSONArray array, String mediaName) throws AndroidCollapseException {

        array.remove(0);

        List<WordsPoint> wordsPoints = new ArrayList<>();

        //计算坐标  文章的标题最多有两行  标题过长微信会使用省略号代替掉
        for (int i = 0; i < array.length(); i++) {

            JSONObject outJSON = (JSONObject) array.get(i);

            JSONObject inJSON = outJSON.getJSONObject("location");

            String words = outJSON.getString("words");

            if (Strings.isNullOrEmpty(words)) continue;

            if (previousEssayTitles.get().size() > 0) {

                if (previousEssayTitles.get().contains(words)) {

                    boolean flag = true;

                    int k = i + 1;

                    while (flag) {
                        // 如果存在重复记录   删除下一条坐标信息
                        // JSONArray由于逻辑问题不能删除任何元素  将words可以替换


                        JSONObject tmpJSON = (JSONObject) array.get(k);

                        String tmpWords1 = tmpJSON.getString("words");

                        if (tmpWords1.contains("年") && tmpWords1.contains("月") && tmpWords1.contains("日")) {

                            flag = false;

                        }

                        // 将内容置换为空字符串  防止在统计坐标时出现重复
                        tmpJSON.put("words", "");
                        array.put(k, tmpJSON);
                        k++;
                        if (k == array.length()) {

                            break;
                        }
                    }
                    continue;
                }
            }

            if (words.contains("微信没有响应") || words.contains("全部消息")) throw new AndroidCollapseException("微信没有响应！");

            if (words.contains("已无更多")) {

                System.out.println("============================没有更多文章===================================");

                lastPage.set(Boolean.TRUE);

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

                if (wordsPoints.size() >= 6) return wordsPoints;
            }

        }
        preserveThePreviousSet(array);
        return wordsPoints;
    }


    private void preserveThePreviousSet(JSONArray array) {
        previousEssayTitles.get().clear();
        for (int i = 0; i < array.length(); i++) {
            JSONObject tmpJSON = (JSONObject) array.get(i);
            String words = tmpJSON.getString("words");
            if (!words.contains("年") && !words.contains("月") && !words.contains("日")) {
                previousEssayTitles.get().add(words);
            }
        }
    }

    private void delegateOpenEssay(String mediaName, boolean retry) throws Exception {
        if (retry)
            if (!restore(mediaName)) return;
        while (!lastPage.get()) {

            AndroidUtil.slideToPoint(606, 2387, 606, 960, driver, 5000);

            List<WordsPoint> wordsPoints = obtainClickPoints(mediaName);

            if (wordsPoints == null || wordsPoints.size() == 0) {

                if (!lastPage.get()) {
                    throw new AndroidCollapseException("不是最后一页,wordPoints is Null !");
                }

                logger.info("公众号{}抓取到最后一页了", mediaName);
            } else {
                openEssays(wordsPoints);
            }
        }

    }


    private boolean restore(String mediaName) {
        try {
            long count = DBTab.essayDao.queryBuilder().where().eq("media_nick", mediaName).countOf();
            this.firstPage.set(count == 0);
            if (!this.firstPage.get()) {

                AndroidUtil.slideToPoint(431, 1250, 431, 455, driver, 1000);

                int var = (int) count % 6;

                int slideNumByPage;

                if (var == 0) {
                    slideNumByPage = (int) ((count / 6) + 3);
                } else if (var <= 3) {
                    slideNumByPage = (int) (count / 6) + 2;
                } else {
                    slideNumByPage = (int) (count / 6) + 3;
                }
                for (int i = 0; i < slideNumByPage; i++) {
                    AndroidUtil.slideToPoint(606, 2387, 606, 960, driver, 1500);
                }
            } else {
                AndroidUtil.slideToPoint(431, 1250, 431, 455, driver, 0);
                firstPage.set(Boolean.FALSE);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void openEssays(List<WordsPoint> wordsPoints) throws AndroidCollapseException, InterruptedException {
        int neverClickCount = 0;
        for (WordsPoint wordsPoint : wordsPoints) {
            //睡眠策略
            setCountVal();
            sleepPolicy();
            if (neverClickCount > 3) {
                throw new AndroidCollapseException("安卓系统卡住点不动了！");
            }
            AndroidUtil.clickPoint(320, wordsPoint.top, 8000, driver);
            //所以去判断下是否点击成功    成功：返回上一页面   失败：不返回上一页面  continue
            if (this.device.isClickEffect()) {
                System.out.println("文章点进去了....");
                for (int i = 0; i < 2; i++) {
                    AndroidUtil.slideToPoint(1413, 2369, 1413, 277, driver, 500);
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

    @Deprecated
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

            AndroidUtil.clickPoint(720, 150, 1000, device.driver);
            AndroidUtil.clickPoint(1350, 2250, 1000, device.driver);

            // 进入公众号
            AndroidUtil.clickPoint(720, 360, 1000, device.driver);

            device.driver.findElement(By.xpath("//android.widget.ImageButton[contains(@content-desc,'聊天信息')]")).click();

            Thread.sleep(1000);

            device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'取消关注')]")).click();
            Thread.sleep(1000);

            device.driver.findElement(By.xpath("//android.widget.Button[contains(@text,'不再关注')]")).click();
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
            driver.navigate().back();
            driver.navigate().back();
        }
        driver.navigate().back();
    }

    /**
     * 订阅公众号
     * <p>
     * 要订阅的公众号可能存在一个问题就是搜索不到微信账号或者最准确的结果并不是第一个
     *
     * @param mediaName
     * @throws Exception
     */
    public void subscribeMedia(String mediaName) throws Exception {
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
        //公众号不存在的情况
        if (point == null) {
            SubscribeMedia tmp = new SubscribeMedia();
            tmp.media_name = mediaName;
            tmp.status = 2;
            tmp.update_time = new Date();
            tmp.number = 0;
            tmp.udid = udid;
            tmp.retry_count = 0;
            tmp.insert_time = new Date();
            tmp.insert();
            k--;
        } else {
            AndroidUtil.clickPoint(point.left, point.top, 2000, driver);

            try {
                // 点击订阅
                driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'关注公众号')]"))
                        .click();
                saveSubscribeRecord(mediaName);
                Thread.sleep(5000);
                driver.navigate().back();
            } catch (Exception e) {
                //已经订阅了
                e.printStackTrace();
                logger.info("Already add public account: {}", mediaName);
                driver.navigate().back();
                --k;
            }
        }
        Thread.sleep(1000);
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
            e.number = 100;
            e.retry_count = 0;
            e.status = SubscribeMedia.CrawlerState.NOFINISH.status;
            e.insert();
        }
    }

    public void digestionCrawler(String mediaName, boolean retry) {
        try {
            if (!AndroidUtil.enterEssay(mediaName, device)) {
                lastPage.set(Boolean.TRUE);
                //搜索不到公众号
                for (int i = 0; i < 3; i++) {
                    driver.navigate().back();
                }
                return;
            }
            delegateOpenEssay(mediaName, retry);
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof AndroidCollapseException) {
                logger.error("设备{}链路出问题了.", device.udid);
                try {
                    //手机睡眠
                    AndroidUtil.closeApp(device);
                    Thread.sleep(1000 * 60 * 3);
                    AndroidUtil.activeWechat(this.device);
                    SubscribeMedia media = AndroidUtil.retry(mediaName, this.device.udid);
                    if (media != null) {
                        media.retry_count += 1;
                        media.update_time = new Date();
                        media.update();
                        if (media.retry_count >= 5) lastPage.set(Boolean.TRUE);
                    }

                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            } else if (e instanceof InvokingBaiduAPIException) {
                logger.error("百度API调用失败！");
                try {
                    //睡眠到明天
                    //线程睡眠
                    //需要计算啥时候到达明天   到达明天的时候需要重新分配任务
                    Date nextDay = DateUtil.buildDate();
                    Date thisDay = new Date();
                    long waitMills = Math.abs(nextDay.getTime() - thisDay.getTime());
                    Thread.sleep(waitMills + 1000 * 60 * 5);
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            } else if (e instanceof InterruptedException) {
                logger.error("InterruptedException 线程中断异常！");
            } else {

                // 如果搜索不到公众号，则会在此处形成死循环
                lastPage.set(Boolean.TRUE);
                e.printStackTrace();
            }
        }

    }

    /**
     * @param mediaName
     */
    void digestionSubscribe(String mediaName) {
        try {
            subscribeMedia(mediaName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 睡眠策略
     */
    private void sleepPolicy() {
        try {
            int hour = LocalDateTime.now().getHour();
            if (hour >= 0 && hour <= 5) {
                //睡眠到凌晨5点
                long after = DateUtil.addHour(new Date()).getTime();
                long now = new Date().getTime();
                Thread.sleep(after - now);
            }


            if (this.countVal.get() != null) {
                //抓取50篇文章休息5分钟
                Integer var = countVal.get();
                if (var % 50 == 0) {
                    Thread.sleep(1000 * 60 * 3);
                    sleep(1000 * 60 * 3);

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Deprecated
    private void sleep(long millis) throws IOException, InterruptedException {
        //手机睡眠
        ShellUtil.clickPower(device.udid);
        //线程睡眠
        Thread.sleep(millis);
        //手机唤醒
        ShellUtil.notifyDevice(device.udid, device.driver);
    }

    abstract void start();


    private void check(String words, String mediaName) {
//        DBTab.essayDao.queryBuilder().where().eq("media_name",mediaName).and().
    }
}
