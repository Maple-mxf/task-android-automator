package one.rewind.android.automator.util;

import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.touch.offset.PointOption;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.adapter.AbstractWechatAdapter;
import one.rewind.android.automator.model.Tab;
import one.rewind.android.automator.model.SubscribeMedia;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.UUID;

/**
 * @author maxuefeng[m17793873123@163.com]
 */
@SuppressWarnings("JavaDoc")
public class AndroidUtil {


    // 重启手机APP

    public static void restartWechatAPP(AndroidDevice device) throws InterruptedException {
        AndroidUtil.clearMemory(device.udid);
        AndroidUtil.activeWechat(device);
    }

    /**
     * @param mediaName
     * @throws InterruptedException
     */
    public static boolean enterEssay(String mediaName, AndroidDevice device) throws Exception {

        try {
            device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'通讯录')]")).click();
        } catch (Exception e) {
            closeApp(device);
            clearMemory(device.udid);
            AndroidUtil.activeWechat(device);
            device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'通讯录')]")).click();
        }
        Thread.sleep(1000);

        device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'公众号')]")).click();

        Thread.sleep(1000);

        device.driver.findElement(By.xpath("//android.widget.ImageButton[contains(@content-desc,'搜索')]")).click();

        Thread.sleep(1000);

        // 搜索
        device.driver.findElement(By.className("android.widget.EditText")).sendKeys(mediaName);

        AndroidUtil.clickPoint(720, 150, 1000, device.driver);

        AndroidUtil.clickPoint(1350, 2250, 1000, device.driver);
        try {
            // 进入公众号
            AndroidUtil.clickPoint(720, 360, 1000, device.driver);

            device.driver.findElement(By.xpath("//android.widget.ImageButton[contains(@content-desc,'聊天信息')]")).click();

            Thread.sleep(1000);

            AndroidUtil.slideToPoint(720, 1196, 720, 170, device.driver, 1000);

            device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'全部消息')]")).click();

            Thread.sleep(10000); // TODO 此处时间需要调整
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

    //检测是否订阅mediaName

    @Deprecated
    private static boolean hasSubscribe(String mediaName, AndroidDevice device) throws Exception {
        String fileName = UUID.randomUUID().toString() + ".png";
        String path = System.getProperty("user.dir") + "/screen/";
        AndroidUtil.screenshot(fileName, path, device.driver);
        JSONObject imageOCR = BaiduAPIUtil.imageOCR(path + fileName);
        JSONArray var0 = imageOCR.getJSONArray("words_result");
        for (Object v : var0) {
            JSONObject var1 = (JSONObject) v;
            if (var1.getString("words").contains("无结果")) {
                try {
                    //标记当前公众号
                    SubscribeMedia media =
                            Tab.subscribeDao.
                                    queryBuilder().
                                    where().
                                    eq("media_name", mediaName).
                                    and().
                                    eq("udid", device.udid).
                                    queryForFirst();
                    if (media != null) {
                        media.status = SubscribeMedia.State.NOT_EXIST.status;
                        media.update_time = new Date();
                        media.update();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        }
        return true;
    }

    /**
     * 截图
     *
     * @param fileName
     * @param path
     */
    public static void screenshot(String fileName, String path, AndroidDriver driver) {
        try {
            File screenFile = ((TakesScreenshot) driver)
                    .getScreenshotAs(OutputType.FILE);

            FileUtils.copyFile(screenFile, new File(path + fileName));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 点击固定的位置
     *
     * @param xOffset
     * @param yOffset
     * @param sleepTime 睡眠时间
     * @throws InterruptedException
     */
    public static void clickPoint(int xOffset, int yOffset, int sleepTime, AndroidDriver driver) throws InterruptedException {
        new TouchAction(driver).tap(PointOption.point(xOffset, yOffset)).perform();
        if (sleepTime > 0) {
            Thread.sleep(sleepTime);
        }
    }

    /**
     * 下滑到指定位置
     *
     * @param startX
     * @param startY
     * @param endX
     * @param endY
     */
    public static void slideToPoint(int startX, int startY, int endX, int endY, AndroidDriver driver, int sleepTime) throws InterruptedException {
        new TouchAction(driver).press(PointOption.point(startX, startY))
                .waitAction()
                .moveTo(PointOption.point(endX, endY))
                .release()
                .perform();
        if (sleepTime > 0) {
            Thread.sleep(sleepTime);
        }
    }

    // 设备处于offline移除当前的设备

    public static String[] obtainDevices() {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        try {
            Process p = Runtime.getRuntime().exec("adb devices");
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        String r = sb.toString().replace("List of devices attached", "").replace("\t", "");
        return r.split("device");
    }

    public static void closeEssay(AndroidDriver driver) throws InterruptedException {
        driver.navigate().back();
        Thread.sleep(1000);
    }

    /**
     * 关闭App可能会存在一些无法预料的问题   比如手机出来一层透明层  此时closeApp方法调用可能会不起作用
     *
     * @param device
     * @throws Exception
     */
    public static void closeApp(AndroidDevice device) {
        try {
            //截图
            String filePrefix = UUID.randomUUID().toString();
            String fileName = filePrefix + ".png";
            String path = System.getProperty("user.dir") + "/screen/";
            AndroidUtil.screenshot(fileName, path, device.driver);
            JSONObject jsonObject = BaiduAPIUtil.imageOCR(path + fileName);
            JSONArray array = (JSONArray) jsonObject.get("words_result");
            for (Object o : array) {

                JSONObject v = (JSONObject) o;

                String words = v.getString("words");
                if (words.contains("微信没有响应") || words.contains("关闭应用")) {
                    AndroidUtil.clickPoint(517, 1258, 1000, device.driver);
                    break;
                }
                if (words.contains("要将其关闭吗") && words.contains("微信无响应")) {
                    //点击确定  这个截图和上面的截图是有点不太一样的
                    AndroidUtil.clickPoint(1196, 1324, 1000, device.driver);
                    break;
                }
                if (words.contains("系统繁忙") && words.contains("请稍后再试")) {
                    AndroidUtil.clickPoint(1110, 1342, 5000, device.driver);
                    break;
                }
            }
            clearMemory(device.udid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static SubscribeMedia retry(String mediaName, String udid) throws Exception {
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
        if (media.retry_count >= AbstractWechatAdapter.RETRY_COUNT) {
            media.update_time = new Date();
            media.number = (int) count;
            media.status = SubscribeMedia.State.FINISH.status;
            media.update();
            return null;
        }
        if (count < media.number) {
            return media;
        } else {
            return null;
        }
    }

    /**
     * 激活应用
     *
     * @param device
     */
    public static void activeWechat(AndroidDevice device) throws InterruptedException {
        device.startActivity("com.tencent.mm", ".ui.LauncherUI");
        Thread.sleep(8000);
    }

    /**
     * 更新为完成的公众号数据
     */
    public static void updateProcess(String mediaName, String udid) throws Exception {

        SubscribeMedia account = Tab.subscribeDao.
                queryBuilder().
                where().
                eq("media_name", mediaName).
                and().
                eq("udid", udid).
                queryForFirst();

        if (account != null) {
            long countOf = Tab.essayDao.
                    queryBuilder().
                    where().
                    eq("media_nick", mediaName).
                    countOf();
            account.number = (int) countOf;
            account.status = (countOf == 0 ? SubscribeMedia.State.NOT_EXIST.status : SubscribeMedia.State.FINISH.status);
            account.status = 1;
            account.update_time = new Date();
            account.retry_count = 5;
            account.update();
        }
    }


    public static void clearMemory(String udid) {
        try {
            ShellUtil.shutdownProcess(udid, "com.tencent.mm");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
