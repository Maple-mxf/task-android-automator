package one.rewind.android.automator.adapter.wechat;

import com.dw.ocr.client.OCRClient;
import com.dw.ocr.parser.OCRParser;
import com.j256.ormlite.dao.Dao;
import io.appium.java_client.MobileElement;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.touch.offset.PointOption;
import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.adapter.Adapter;
import one.rewind.android.automator.adapter.wechat.exception.GetPublicAccountEssayListFrozenException;
import one.rewind.android.automator.adapter.wechat.exception.MediaException;
import one.rewind.android.automator.adapter.wechat.exception.SearchPublicAccountFrozenException;
import one.rewind.android.automator.adapter.wechat.model.WechatContact;
import one.rewind.android.automator.adapter.wechat.model.WechatMoment;
import one.rewind.android.automator.adapter.wechat.model.WechatMsg;
import one.rewind.android.automator.adapter.wechat.util.PublicAccountInfo;
import one.rewind.android.automator.adapter.wechat.util.UserInfo;
import one.rewind.android.automator.deivce.AndroidDevice;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.android.automator.exception.AndroidException;
import one.rewind.data.raw.model.Platform;
import one.rewind.db.Daos;
import one.rewind.db.exception.DBInitException;
import one.rewind.txt.NumberFormatUtil;
import one.rewind.util.FileUtil;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author maxuefeng[m17793873123@163.com]
 * Adapter对应是设备上的APP  任务执行应该放在Adapter层面上   Adapter层面的异常应当都应该放在Task层面去捕获处理
 */
public class WeChatAdapter extends Adapter {

    public static boolean NeedAccount = true;

    public static Platform platform;

    static {
        try {
            Dao<Platform, String> platformDao = Daos.get(one.rewind.data.raw.model.Platform.class);
            platform = new Platform("微信公众号平台", "WX");
            platform.id = 1;
            platformDao.createIfNotExists(platform);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public enum Status {
        Init,                                  // 初始化
        Home_Login,                            // 登陆
        Home,                                  // 首页
        Search,                                // 首页点进去的搜索
        SearchPublicAccount,                   // 首页点进去的搜索之后在点击公众号到达的页面
        PublicAccount_Search_Result,           // 公众号搜索结果
        PublicAccount_Home,                    // 公众号首页
        PublicAccount_MoreInfo,                // 公众号更多资料
        Address_List,                          // 通讯录
        Subscribe_PublicAccount_List,          // 我订阅的公众号列表
        Subscribe_PublicAccount_Search,        // 我订阅的公众号列表搜索
        Subscribe_PublicAccount_Search_Result, // 我订阅的公众号列表搜索结果
        PublicAccount_Conversation,            // 公众号回话列表
        PublicAccount_Essay_List,              // 公众号历史文章列表
        PublicAccountEssay,                    // 公众号文章
        Error                                  // 出错
    }

    // 状态信息
    public Status status = Status.Init;

    public WeChatAdapter() {
    }

    /**
     * 构造方法
     *
     * @param device  加载设备
     * @param account 加载账号
     */
    public WeChatAdapter(AndroidDevice device, Account account) throws AndroidException.IllegalStatusException {
        super(device);
        this.account = account;
        this.appInfo = new Adapter.AppInfo("com.tencent.mm", ".ui.LauncherUI");
    }

    /**
     * 启动Adapter 启动Adapter之后确认在APP首页
     *
     * @throws Exception
     */
    public void start() throws
            InterruptedException,
            AccountException.Broken,
            AdapterException.LoginScriptError {

        Thread.sleep(8000);

        // 判断是否有更新提示
        try {
            if (device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'更新')]")) != null) {
                device.driver.findElement(By.xpath("//android.widget.Button[contains(@text,'取消')]")).click();
            }
        } catch (Exception e) {
            logger.info("Home Page No update tip" + e);
        }

        // A 验证是否时登陆页面
        // 此时假设设备上的微信都登陆过账号
        // 如果是登陆页面，使用当前account进行登陆
        // 验证到首页 或者 首页登陆界面 并更改状态
        if (!atHome()) {

            logger.info("current page is not Home Page");
            login();

        } else {

            logger.info("current page is Home Page");
            status = Status.Home;

        }
    }

    /**
     * @throws InterruptedException
     * @throws AccountException.Broken
     * @throws AdapterException.LoginScriptError
     */
    public void checkAccount() throws
            InterruptedException,
            AccountException.Broken,
            AdapterException.LoginScriptError {

        // B11 验证当前微信用户 与 account 相对应
        UserInfo userInfo;
        try {

            userInfo = getLocalUserInfo();
        } catch (Exception e) {
            logger.error("[{}] login process unfinished, ", getInfo(), e);
            throw new AdapterException.LoginScriptError(this, account);
        }

        if (userInfo == null) throw new AdapterException.LoginScriptError(this, account);

        // 微信昵称 与 微信号 有一个不对应
        Account temp = this.account;

        if (!userInfo.name.equals(account.username) || !userInfo.id.equals(account.src_id)) {
            logout();
            this.account = temp;
            login();
        }
    }

    /**
     * 重启微信
     */
    public void restart() throws
            InterruptedException,
            AdapterException.LoginScriptError,
            AccountException.Broken {

        super.restart();
        start();
    }

    /**
     * 判断是否 在 微信首页
     *
     * @return
     */
    public boolean atHome() {

        logger.info("current Thread Name[{}]", Thread.currentThread().getName());

        try {

            Thread.sleep(5000);

            device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'微信')]")).click();

            return true;
        } catch (Exception e) {
            logger.warn("Can't find '微信' tab, ", e);
            return false;
        }
    }

    /**
     * 点击左上角的叉号 或者 返回按钮
     */
    public void touchUpperLeftButton() throws InterruptedException {

        // x=70 y=168
        device.touch(70, 168, 1000);
    }

    /**
     * 获取 微信公众号历史文章列表中的 文本区域
     *
     * @return
     * @throws IOException
     * @throws AdapterException.NoResponseException
     * @throws SearchPublicAccountFrozenException
     */
    public List<OCRParser.TouchableTextArea> getEssayListTextAreas() throws
            IOException,
            AdapterException.NoResponseException,
            AdapterException.IllegalStateException,
            GetPublicAccountEssayListFrozenException {

        if (status != Status.PublicAccount_Essay_List) throw new AdapterException.IllegalStateException(this);

        // A 获取可点击文本区域
        List<OCRParser.TouchableTextArea> textAreaList = OCRClient.getInstance().getTextBlockArea(device.screenshot(), 0, 0, 1056, 2550);

        if (device.checkContent(textAreaList, "微信没有响应")) throw new AdapterException.NoResponseException();

        if (device.checkContent(textAreaList, "操作频繁", "请稍后再试"))
            throw new GetPublicAccountEssayListFrozenException(account);

        textAreaList = mergeForTitle(textAreaList, 60);

        return textAreaList;
    }

    /**
     * 首页 --> 搜索页
     *
     * @throws AdapterException.IllegalStateException
     */
    public void goToSearchPage() throws AdapterException.IllegalStateException, InterruptedException {

        if (this.status != Status.Home)
            throw new AdapterException.IllegalStateException(this);

        try {
            FileUtil.writeBytesToFile(device.screenshot(), "tmp/" + device.udid + ".png");

        } catch (Exception e) {
            e.printStackTrace();
        }

        // 从首页点搜索按钮 TODO 多个Device可能在公用了一个Driver
        device.driver.findElement(By.xpath("//android.widget.TextView[contains(@content-desc,'搜索')]")).click();
        Thread.sleep(2000);
        this.status = Status.Search;
    }

    /**
     * 搜索页 --> 搜索公众号页
     *
     * @throws AdapterException.IllegalStateException
     */
    public void goToSearchPublicAccountPage() throws AdapterException.IllegalStateException, InterruptedException {

        if (this.status != Status.Search)
            throw new AdapterException.IllegalStateException(this);

        // 在搜索页点公众号
        device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'公众号')]")).click();

        Thread.sleep(8000);

        this.status = Status.SearchPublicAccount;
    }

    /**
     * 搜索公众号 --> 输入关键词搜索
     *
     * @throws AdapterException.IllegalStateException
     */
    public void searchPublicAccount(String media_nick) throws AdapterException.IllegalStateException, InterruptedException, SearchPublicAccountFrozenException, IOException, AdapterException.NoResponseException, MediaException.NotFound, MediaException.Illegal, MediaException.NotEqual {

        if (this.status != Status.SearchPublicAccount)
            throw new AdapterException.IllegalStateException(this);

        // 输入框输入公众号
        List<OCRParser.TouchableTextArea> areaList = device.searchAndGetResult(By.className("android.widget.EditText"), "\"" + media_nick + "\"", 250, 430, 1400, 2390);
        Thread.sleep(1000);

        this.status = Status.PublicAccount_Search_Result;

        if (areaList.size() == 0) {
            throw new MediaException.NotFound(media_nick);
        }

        logger.info("Search media ocr result, ", areaList.toString());

        // 识别是否被限流
        if (device.checkContent(areaList, "微信没有响应")) throw new AdapterException.NoResponseException();

        if (device.checkContent(areaList, "frequently", "操作频繁", "请稍后再试"))
            throw new SearchPublicAccountFrozenException(account);

        areaList = mergeForTitle(areaList, 60);

        // 进入微信公众号首页
        goToSubscribedPublicAccountHome(areaList.get(0).left + 10, areaList.get(0).top + 10);

        this.status = Status.PublicAccount_Home;

        PublicAccountInfo pai = getPublicAccountInfo(false, false);

        if (!pai.nick.equals(media_nick)) throw new MediaException.NotEqual(media_nick, pai.nick);
    }

    /**
     * 首页/通讯录 --> 已订阅公众号的列表页面
     *
     * @throws InterruptedException
     * @throws AdapterException.IllegalStateException
     */
    public void goToSubscribePublicAccountList() throws InterruptedException, AdapterException.IllegalStateException {

        if (this.status != Status.Home && this.status != Status.Address_List)
            throw new AdapterException.IllegalStateException(this);

        // 从首页点 通讯录
        device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'通讯录')]")).click();
        Thread.sleep(1000);

        // 点公众号
        device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'公众号')]")).click();
        Thread.sleep(1000);

        this.status = Status.Subscribe_PublicAccount_List;
    }

    /**
     * 已订阅公众号列表页面 --> 搜索到相关的公众号
     *
     * @param media_nick 媒体名称
     * @throws InterruptedException
     * @throws AdapterException.IllegalStateException
     * @throws IOException
     * @throws AdapterException.NoResponseException
     * @throws MediaException                         在订阅列表中找不到指定的公众号
     */
    public void goToSubscribedPublicAccountHome(String media_nick) throws
            InterruptedException,
            AdapterException.IllegalStateException,
            IOException,
            AdapterException.NoResponseException,
            MediaException.NotSubscribe,
            MediaException.NotEqual,
            MediaException.Illegal {

        if (this.status != Status.Subscribe_PublicAccount_List)
            throw new AdapterException.IllegalStateException(this);

        // 点搜索
        device.driver.findElement(By.xpath("//android.widget.ImageButton[contains(@content-desc,'搜索')]")).click();
        Thread.sleep(1000);

        List<AndroidElement> els = device.searchAndGetResult2(By.className("android.widget.EditText"), media_nick, 200, 255, 1400, 2380);

        if (device.checkContent2(els, "无结果") || els.size() == 0) {
            throw new MediaException.NotSubscribe(account, media_nick);
        }

        // 进入微信公众号首页
        goToSubscribedPublicAccountHome(els.get(0).getRect().x + 10, els.get(0).getRect().y + 10);

        this.status = Status.PublicAccount_Home;

        PublicAccountInfo pai = getPublicAccountInfo(false, false);

        if (!pai.nick.equals(media_nick)) throw new MediaException.NotEqual(media_nick, pai.nick);
    }

    /**
     * 根据坐标点入微信公众号页
     *
     * @throws InterruptedException
     * @throws AdapterException.IllegalStateException
     */
    public void goToSubscribedPublicAccountHome(int x, int y) throws
            InterruptedException,
            AdapterException.IllegalStateException {

        if (status != Status.Subscribe_PublicAccount_List && status != Status.PublicAccount_Search_Result)
            throw new AdapterException.IllegalStateException(this);

        this.device.touch(x, y, 1000);

        if (status == Status.Subscribe_PublicAccount_List)
            device.driver.findElement(By.xpath("//android.widget.ImageButton[contains(@content-desc,'聊天信息')]")).click();

        Thread.sleep(1000);

        this.status = Status.PublicAccount_Home;
    }

    /**
     * 公众号首页 --> 公众号更多资料页面
     * 查看公众号更多资料
     */
    public void goToPublicAccountMoreInfoPage() throws
            InterruptedException,
            AdapterException.IllegalStateException {

        if (this.status != Status.PublicAccount_Home) throw new AdapterException.IllegalStateException(this);

        // 点右上三个点图标
        // x=1342 y=168
        device.touch(1342, 168, 2000);
//        device.driver.findElementByClassName("android.widget.ImageButton").click();

        // 点更多资料
        device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'更多资料')]")).click();

        Thread.sleep(1000);

        this.status = Status.PublicAccount_MoreInfo;
    }

    /**
     * 公众号首页 :: 获取公众号信息
     * <p>
     * Adapter状态不发生变化
     *
     * @param subscribe
     * @return
     * @throws AdapterException.IllegalStateException
     * @throws InterruptedException
     * @throws MediaException.Illegal
     */
    public PublicAccountInfo getPublicAccountInfo(boolean subscribe, boolean getMoreInfo) throws AdapterException.IllegalStateException, InterruptedException, MediaException.Illegal {

        if (this.status != Status.PublicAccount_Home) throw new AdapterException.IllegalStateException(this);

        // 对公众号首页信息进行处理
        List<AndroidElement> els = device.driver.findElementsByClassName("android.widget.TextView");

        if (els.size() == 0) {
            logger.info("Not into public account page.");
            throw new AdapterException.IllegalStateException(this);
        }

        PublicAccountInfo wpa = new PublicAccountInfo();

        wpa.nick = els.get(0).getText();

        // 公众号首页异常信息
        for (WebElement we : els) {
            if (
                    we.getText().contains("该公众号已迁移至新帐号") ||
                            we.getText().contains("帐号已被系统回收") ||
                            we.getText().contains("该帐号已系统注销") ||
                            we.getText().contains("该帐号已自主注销")

            ) {
                throw new MediaException.Illegal(wpa.nick);
            }
        }

        // 该公众号已迁移至新账号，帐号已被系统回收
        if (wpa.nick.matches("transfer_.+?") || wpa.nick.matches("该帐号已注销|该帐号已冻结")) {
            throw new MediaException.Illegal(wpa.nick);
        }

        wpa.content = els.get(1).getText();
        wpa.essay_count = NumberFormatUtil.parseInt(
                els.get(2).getText().replaceAll("篇原创文章.*$", ""));

        // TODO 需要识别当日无法订阅更多公众号异常
        if (subscribe) subscribePublicAccount();

        if (getMoreInfo) {

            goToPublicAccountMoreInfoPage();

            // 对更多资料内容进行处理
            els = device.driver.findElementsByClassName("android.widget.TextView");

            els = els.stream().filter(el -> !el.getText().equals("更多资料") && el.getLocation().x != 0).collect(Collectors.toList());

            List<String> info = new ArrayList<>();

            for (int i = 0; i < els.size() - 1; i = i + 2) {
                info.add(els.get(i).getText() + "" + els.get(i + 1).getText());
            }

            for (String info_item : info) {
                if (info_item.contains("微信号")) {
                    wpa.name = info_item.replaceAll("微信号:? ?", "");
                }
                if (info_item.contains("帐号主体")) {
                    wpa.subject = info_item.replaceAll("帐号主体", "");
                }
                if (info_item.contains("商标保护")) {
                    wpa.trademark = info_item.replaceAll("商标保护", "");
                }
                if (info_item.contains("客服电话")) {
                    wpa.phone = info_item.replaceAll("客服电话", "");
                }
            }

            // 点击返回
            touchUpperLeftButton();

            this.status = Status.PublicAccount_Home;
        }

        return wpa;
    }

    /**
     * 公众号首页 订阅公众号
     *
     * @throws InterruptedException
     * @throws AdapterException.IllegalStateException
     */
    public void subscribePublicAccount() throws InterruptedException, AdapterException.IllegalStateException {

        if (this.status != Status.PublicAccount_Home) throw new AdapterException.IllegalStateException(this);

        try {
            // TODO  为何点击不了？
            device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'关注公众号')]")).click();

            Thread.sleep(5000);

//            device.touch(702, 1277, 3000);

            this.status = Status.PublicAccount_Conversation;
        }
        // TODO 可能已经关注了
        catch (NoSuchElementException e) {

        }

        // 返回到公众号主页
        device.driver.findElement(By.xpath("//android.widget.ImageButton[contains(@content-desc,'聊天信息')]")).click();

        Thread.sleep(2000);

        this.status = Status.PublicAccount_Home;
    }


    /**
     * 公众号首页 取消订阅
     *
     * @throws InterruptedException
     * @throws AdapterException.IllegalStateException
     */
    public void unsubscribePublicAccount(String media_nick) throws InterruptedException, AdapterException.IllegalStateException, MediaException.NotSubscribe {

        if (this.status != Status.PublicAccount_Home) throw new AdapterException.IllegalStateException(this);

        try {
            device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'取消关注')]")).click();
        } catch (Exception e) {
            throw new MediaException.NotSubscribe(account, media_nick);
        }

        Thread.sleep(1000);

        WebElement ae = device.driver.findElement(By.xpath("//android.widget.Button[contains(@text,'不再关注')]"));
        if (ae != null) {
            ae.click();
            Thread.sleep(1000);
        }

        for (AndroidElement el : device.driver.findElementsByClassName("android.widget.TextView")) {
            if (el.getText().matches("微信(\\(\\d+\\))?")) {
                this.status = Status.Home;
            } else if (el.getText().equals("公众号")) {
                this.status = Status.Subscribe_PublicAccount_List;
            }
            break;
        }
    }

    /**
     * 公众号首页 取消订阅
     *
     * @throws InterruptedException
     * @throws AdapterException.IllegalStateException
     */
    public void unsubscribePublicAccount() throws InterruptedException, AdapterException.IllegalStateException, MediaException.NotSubscribe {

        if (this.status != Status.Subscribe_PublicAccount_List) throw new AdapterException.IllegalStateException(this);

        // A 长按取消关注
        new TouchAction<>(device.driver).longPress(PointOption.point(297, 430)).perform();
        Thread.sleep(1000);

        boolean flag = true;

        try {

            // B 查找
            device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'取消关注')]")).click();
            Thread.sleep(1000);

        } catch (Exception e) {
            flag = false;
        }

        if (flag) {

            // C 点击不再关注
            device.driver.findElement(By.xpath("//android.widget.Button[contains(@text,'不再关注')]")).click();
            Thread.sleep(2000);

            // D 递归调用
            unsubscribePublicAccount();
        }
    }


    /**
     * 公众号首页 --> 公众号历史文章列表
     */
    public void goToPublicAccountEssayList() throws InterruptedException, AdapterException.IllegalStateException {

        if (this.status != Status.PublicAccount_Home) throw new AdapterException.IllegalStateException(this);

        // 向下滑动
        device.slideToPoint(720, 1196, 720, 170, 1000);

        device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'全部消息')]")).click();

        Thread.sleep(12000); // TODO 此处时间需要调整

        this.status = Status.PublicAccount_Essay_List;
    }

    /**
     * 公众号历史文章列表 --> 文章详情页
     *
     * @param textArea
     * @throws InterruptedException
     * @throws AdapterException.IllegalStateException
     */
    public void goToEssayDetail(OCRParser.TouchableTextArea textArea) throws InterruptedException, AdapterException.IllegalStateException, IOException, AdapterException.NoResponseException {

        if (this.status != Status.PublicAccount_Essay_List) throw new AdapterException.IllegalStateException(this);

        // A 点击文章
        device.reliableTouch(textArea.left + 10, textArea.top + 10);

        // B 向下滑拿到文章热度数据和评论数据
        for (int i = 0; i < 2; i++) {
            device.slideToPoint(1000, 800, 1000, 2000, 1000);
        }

        this.status = Status.PublicAccountEssay;
    }

    /**
     * 从文章详情页返回到上一个页面 点击叉号
     *
     * @throws AdapterException.IllegalStateException
     */
    public void goToEssayPreviousPage() throws AdapterException.IllegalStateException, InterruptedException {

        if (this.status != Status.PublicAccountEssay) throw new AdapterException.IllegalStateException(this);

        touchUpperLeftButton();

        this.status = Status.PublicAccount_Essay_List;
    }

    /**
     * 退出登录
     *
     * @throws InterruptedException
     */
    public void logout() throws AdapterException.LoginScriptError, InterruptedException {

        try {
            // A 点击我
            device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'我')]")).click();
            Thread.sleep(500);

            // B 点击设置
            device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'设置')]")).click();
            Thread.sleep(1000);

            // C 向下滑
            device.slideToPoint(500, 1800, 600, 1000, 500);

            // D 点击退出
            device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'退出')]")).click();
            Thread.sleep(1000);

            // E 点击退出登录
            device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'退出登录')]")).click();
            Thread.sleep(3000);

            // F 点击弹窗中的确认退出
            device.driver.findElement(By.xpath("//android.widget.Button[contains(@text,'退出')]")).click();
            Thread.sleep(8000);

            // 不能正常执行上述操作
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Adapter:[{}] Account:[{}] logout process unfinished, ", this.getClass().getSimpleName(), account.id, e);
            throw new AdapterException.LoginScriptError(this, account);
        } finally {
            account = null;
            status = Status.Home_Login;
        }
    }

    /**
     * 登录
     */
    public void login() throws AccountException.Broken, InterruptedException, AdapterException.LoginScriptError {

        try {

            device.driver.findElement(By.xpath("//android.widget.Button[contains(@text,'更多')]")).click();
            Thread.sleep(2000);

            device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'登录其他帐号')]")).click();
            Thread.sleep(2000);

            device.driver.findElement(By.xpath("//android.widget.Button[contains(@text,'用微信号/QQ号/邮箱登录')]")).click();
            Thread.sleep(2000);

            // A 输入账号密码  appAccount
            device.driver.findElement(By.xpath("//android.widget.EditText[contains(@text,'请填写微信号/QQ号/邮箱')]")).sendKeys(account.src_id);
            Thread.sleep(1000);

            device.driver.findElement(By.xpath("//android.widget.EditText[contains(@text,'请填写密码')]")).sendKeys(account.password);
            Thread.sleep(1000);

            // B 点击登录  Xpath   //android.widget.Button[@text,'登录']
//            device.driver.findElement(By.xpath("//android.widget.Button[contains(@text,'登录')]")).click();
//            device.driver.findElement(By.xpath("//android.widget.Button[@text,'登录']")).click();
            device.touch(722, 1316, 8000);

        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Adapter:[{}] Account:[{}] login process unfinished, ", this.getClass().getSimpleName(), account.id, e);
            throw new AdapterException.LoginScriptError(this, account);
        }

        // C 验证是否存在拖拽操作等安全验证操作  TODO

        // D 人工拖拽

        // E 进入首页  完成登录操作
        if (atHome()) {
            return;
        }
        // F 无法进入首页，应该是账号问题
        else {
            account.status = Account.Status.Broken;
            throw new AccountException.Broken(account);
        }
    }

    /**
     * 获取本台机器udid对应的微信号id和微信名
     *
     * @throws InterruptedException 中断异常
     */
    public UserInfo getLocalUserInfo() throws InterruptedException {

        device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'我')]")).click(); //点击我

        Thread.sleep(500);

        Point qrCodeLoc = device.driver.findElementByAccessibilityId("查看二维码").getLocation(); // 找到二维码位置

        // 点二维码左边的用户名称，进入个人信息界面
        device.touch(qrCodeLoc.x - 500, qrCodeLoc.y, 1000);

        Thread.sleep(1000);

        List<AndroidElement> lis = device.driver.findElementsById("android:id/summary");

        UserInfo ui = new UserInfo(lis.get(1).getText(), lis.get(0).getText());

        device.goBack();

        logger.info("user_id:{} user_name:{}", ui.id, ui.name);

        Thread.sleep(5000);

        return ui;
    }

    /**
     * 进入到本微信的微信朋友圈中
     *
     * @throws InterruptedException
     */
    public void getIntoMoments() throws InterruptedException {

        WebElement discover_button = device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'发现')]"));//点击发现
        discover_button.click();

        Thread.sleep(1700);

        //点击朋友圈，朋友圈按钮为对应list中的第一个元素
        List<AndroidElement> lis = device.driver.findElementsByClassName("android.widget.LinearLayout");
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
        device.touch(1202 + 100, 84 + 80, 1000);

        // 创建群聊
        device.touch(1050, 335, 500);

        MobileElement id_input;

        for (String id : ids) {

            // 输入人名
            device.driver.findElement(By.className("android.widget.EditText")).sendKeys(id);

            Thread.sleep(1000);

            // 选择人
            device.touch(1270, 641, 1000);
        }

        // 创建群
        device.touch(1320, 168, 10000);

        // 点群设置
        device.touch(1320, 168, 1000);

        // 点群名称
        device.touch(720, 784, 1000);

        // 名称输入框
        device.driver.findElementByClassName("android.widget.EditText").sendKeys(groupName);

        Thread.sleep(1000);

        // 确定名称 点OK
        device.touch(1331, 168, 1000);

        // 返回群聊
        touchUpperLeftButton();

        // 返回主界面
        touchUpperLeftButton();
    }

    /**
     * 获取群组中的成员信息
     * <p>
     * TODO 向下滑动的方法处理不完善
     *
     * @throws InterruptedException 中断异常
     */
    public void getFriendFromGroupChat() throws InterruptedException {

        device.driver.findElementByAccessibilityId("聊天信息").click();
        Thread.sleep(1000);

        List<AndroidElement> ifHaveTitle = device.driver.findElementsById("android:id/title");
        while (ifHaveTitle.size() == 0) {
            new TouchAction(device.driver).press(PointOption.point(850, 1460)).moveTo(PointOption.point(840, 500)).release().perform();
        }

        device.driver.findElementById("android:id/title").click();
        Thread.sleep(1500);

        //根据群内的人数调整j的控制
        for (int j = 0; j < 10; j++) {

            List<AndroidElement> listOfFriends = device.driver.findElementsById("com.tencent.mm:id/ajj");

            for (int i = 0; i < listOfFriends.size(); i++) {

                listOfFriends.get(i).click();
                Thread.sleep(1000);

                WebElement add = null;

                try {
                    add = device.driver.findElement(By.xpath("//android.widget.Button[contains(@text,'添加到通讯录')]"));

                    if (add != null) {
                        add.click();
                        Thread.sleep(1000);

                        device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'发送')]")).click();
                        Thread.sleep(2000);

                        device.driver.navigate().back();
                    }
                    // 如果没有添加到通讯录，说明已经是好友了
                    else {
                        device.driver.navigate().back();
                        Thread.sleep(1000);
                    }

                } catch (Exception e) {
                    logger.error("Error add to task contact.", e);
                }
            }

            // TODO 向下滑动获取更多用户
            new TouchAction(device.driver)
                    .press(PointOption.point(1100, 2300))
                    .moveTo(PointOption.point(1100, 200))
                    .release().perform();
        }
    }

    /**
     * 进入聊天回话界面 群组 单个人
     * （这个方法同时可以用于进入与朋友的单人聊天）
     *
     * @param name 群聊名称
     * @throws InterruptedException 中断异常
     */
    public void getIntoConversation(String name) throws InterruptedException {

        device.touch(1118, 168, 1000);

        // 名称输入
        device.driver.findElementByClassName("android.widget.EditText").sendKeys(name);

        Thread.sleep(1000);

        // 确定名称 点OK
        device.touch(1064, 532, 1000);

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

        for (int i = 0; i < 3; i++) {

            String copytext = "";
            String url = "";
            byte[] picByte = null;

            // 此处获取到的是当前页面的每一个第一次出现的昵称，文字，图片，时间，或链接中的字
            // 其中文字，图片，链接中间的字可能不存在
            WebElement name = device.driver.findElementById("com.tencent.mm:id/asc"); // 昵称
            WebElement pubtime = device.driver.findElementById("com.tencent.mm:id/dg7"); // 时间

            WebElement text = null;
            try {
                text = device.driver.findElementById("com.tencent.mm:id/ic"); // 文字
            } catch (Exception e) {
            }

            WebElement picture = null;
            try {
                picture = device.driver.findElementById("com.tencent.mm:id/dhi"); // 图片
            } catch (Exception e) {
            }

            WebElement textInUrl = null;
            try {
                textInUrl = device.driver.findElementById("com.tencent.mm:id/did"); // 链接中间的字
            } catch (Exception e) {

            }

            // 取得不了发布时间
            logger.info(picture.getLocation().getY());

            // 长按文字进行复制
            if (text != null) {

                TouchAction copy = new TouchAction(device.driver).longPress(PointOption.point(text.getLocation().getX(), text.getLocation().getY())).release();
                copy.perform();
                Thread.sleep(500);

                device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'复制')]")).click();
                Thread.sleep(500);
                copytext = device.driver.getClipboardText(); // 将剪贴板中的内容保存到变量中
                Thread.sleep(500);
            }

            //点击进入图片然后截图
            if (picture != null) {
                picture.click();
                Thread.sleep(1000);
                picByte = device.driver.getScreenshotAs(OutputType.BYTES);
                Thread.sleep(500);
                device.driver.navigate().back();
                Thread.sleep(1000);
            }

            //点击链接并复制url
            if (textInUrl != null) {

                textInUrl.click();
                Thread.sleep(5000);

                device.driver.findElementByAccessibilityId("更多").click();
                Thread.sleep(1000);

                device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'复制链接')]")).click();
                Thread.sleep(500);

                device.driver.navigate().back();
                Thread.sleep(1000);

                url = device.driver.getClipboardText();
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
                            WechatMoment WM = new WechatMoment(device.udid, account.src_id, account.username);
                            WM.friend_name = name.getText();
                            WM.url = url;
                            WM.insert_time = new Date();
                            WM.insert();
                        }
                        // 图片为本次，说明本次只有图片
                        if (picture.getLocation().getY() < pubtime.getLocation().getY()) {
                            WechatMoment WM = new WechatMoment(device.udid, account.src_id, account.username);
                            WM.friend_name = name.getText();
                            WM.content = picByte;
                            WM.insert_time = new Date();
                            WM.insert();
                        }
                    }
                }

                // 如果文字是本次的
                if (text.getLocation().getY() < pubtime.getLocation().getY()) {

                    // 判断本次是纯文字还是文字图片还是文字链接
                    // 如果图片和链接都为空，则本次只有文字
                    if (picture == null && textInUrl == null) {
                        WechatMoment WM = new WechatMoment(device.udid, account.src_id, account.username);
                        WM.friend_name = name.getText();
                        WM.friend_text = copytext;
                        WM.insert_time = new Date();
                        WM.insert();
                    }

                    // 如果图片为空链接不为空，再判断链接是否为本次
                    if (picture == null && textInUrl != null) {
                        // 链接为本次
                        if (textInUrl.getLocation().getY() > pubtime.getLocation().getY()) {
                            WechatMoment WM = new WechatMoment(device.udid, account.src_id, account.username);
                            WM.friend_name = name.getText();
                            WM.url = url;

                            WM.insert_time = new Date();
                            WM.insert();
                        }
                        //链接不为本次，说明只有文字
                        if (textInUrl.getLocation().getY() < pubtime.getLocation().getY()) {
                            WechatMoment WM = new WechatMoment(device.udid, account.src_id, account.username);
                            WM.friend_name = name.getText();
                            WM.friend_text = copytext;
                            WM.insert_time = new Date();
                            WM.insert();
                        }
                    }

                    //如果链接为空图片不为空，则判断图片是否为本次
                    //图片是本次
                    if (picture.getLocation().getY() > pubtime.getLocation().getY()) {
                        WechatMoment WM = new WechatMoment(device.udid, account.src_id, account.username);
                        WM.friend_name = name.getText();
                        WM.content = picByte;
                        WM.insert_time = new Date();
                        WM.insert();
                    }

                    //图片不是本次，说明只有文字
                    if (picture.getLocation().getY() < pubtime.getLocation().getY()) {
                        WechatMoment WM = new WechatMoment(device.udid, account.src_id, account.username);
                        WM.friend_name = name.getText();
                        WM.friend_text = copytext;
                        WM.insert_time = new Date();
                        WM.insert();
                    }
                }
            }

            //向下滑动，获取第一次发布时间的location，使其在屏幕中消失
            TouchAction action1 = new TouchAction(device.driver).press(PointOption.point(pubtime.getLocation().getX(), pubtime.getLocation().getY())).waitAction().moveTo(PointOption.point(250, 170)).release();
            action1.perform();
            Thread.sleep(1500);
        }

    }

    /**
     * 发送一条消息（在聊天回话界面）
     *
     * @param msg
     * @throws InterruptedException
     */
    public void sendMsg(String msg) throws InterruptedException {
        // TODO 状态判定

        device.driver.findElementByClassName("android.widget.EditText").sendKeys(msg); //sendKeys
        Thread.sleep(1500);

        device.driver.findElement(By.xpath("//android.widget.Button[contains(@text,'发送')]")).click(); //发送
        Thread.sleep(1500);
    }

    /**
     * 如果需要发送备注，则再使用制表符分隔后输入备注信息，并去掉代码中的注释
     *
     * @param contacts
     * @param verification
     * @throws Exception
     */
    public void addContacts(List<String> contacts, String verification) throws Exception {

        //设定验证信息和备注
        if (verification == null) verification = "自动化测试账号";

        Thread.sleep(1500);

        WebElement add = device.driver.findElementByAccessibilityId("更多功能按钮"); //点击发现
        add.click();

        Thread.sleep(1500);

        WebElement newfriend = device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'添加朋友')]"));
        newfriend.click();

        Thread.sleep(1500);

        device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'微信号/QQ号/手机号')]")).click();

        Thread.sleep(1500);

        outer:
        for (String contact : contacts) {

            String[] token = contact.split("\\t");

            device.driver.findElement(By.xpath("//android.widget.EditText[contains(@text,'微信号/QQ号/手机号')]")).sendKeys(token[0]);//填入添加人的号码
            Thread.sleep(1500);

            device.driver.findElementByClassName("android.widget.TextView").click();//点击搜索
            Thread.sleep(2000);

            //如果用户不存在
            List<AndroidElement> lis2 = device.driver.findElements(By.xpath("//android.widget.TextView[contains(@text,'该用户不存在')]"));

            if (lis2.size() != 0) {

                device.driver.navigate().back();//返回
                logger.info("{} not exist.", token[0]);
                Thread.sleep(1500);

                device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'微信号/QQ号/手机号')]")).click();
                Thread.sleep(1500);
                continue outer;
            }

            //操作过于频繁，直接结束
            List<AndroidElement> lis3 = device.driver.findElements(By.xpath("//android.widget.TextView[contains(@text,'操作过于频繁，请稍后再试')]"));

            if (lis3.size() != 0) {

                Thread.sleep(500);
                device.driver.navigate().back();

                logger.info("{} add failed, {}", token[0], "频繁报错，结束");
                return;
            }

            device.driver.findElementByClassName("android.widget.Button").click(); //添加到通讯录
            Thread.sleep(2000);

            List<AndroidElement> temp = device.driver.findElementsByClassName("android.widget.EditText");
            temp.get(0).clear();
            temp.get(0).sendKeys(verification); //重填验证信息
            Thread.sleep(1000);

            String friend_name = temp.get(1).getText(); // 在重填之前获取朋友的昵称保存在变量中

            device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'发送')]")).click(); // 发送
            Thread.sleep(5000);

            // 存入数据库中
            WechatContact wc = new WechatContact(device.udid, account.src_id, account.username, token[0], friend_name);
            wc.insert_time = new Date();
            wc.insert();

            device.driver.navigate().back(); // 返回
            Thread.sleep(1500);

            device.driver.findElementByClassName("android.widget.EditText").clear(); // 清空
            Thread.sleep(500);
        }
    }

    /**
     * 取得聊天记录
     * 首先提取页面中的全部元素，然后将他们按照Y值的顺序在list allObjects中进行排序 从下至上依次进行判断
     * 由于最上方的文字没有对应的头像，因此不做考虑，判断文字，图片，url等的不同方法根据X值或是长按出现的选项数
     * 判断标准可以根据微信的更新随时更改
     * 在本页面所有元素全部判定完成之后，根据第二个，即判定完成的最后一个元素的位置向下滑动，直至该元素不再显示
     * <p>
     * TODO 这个方法需要大量测试
     *
     * @throws Exception
     */
    public void getChatRecords() throws Exception {

        for (int k = 0; k < 10; k++) {

            //将页面中的文字，时间，图片等所有元素放在allObjects组中
            List<AndroidElement> textlist = device.driver.findElementsById("com.tencent.mm:id/ki");//该界面中所有的文字
            List<AndroidElement> aevList = device.driver.findElementsById("com.tencent.mm:id/aev");//该界面中的aev为id的内容
            List<AndroidElement> timeList = device.driver.findElementsById("com.tencent.mm:id/a4");//该界面中所有的时间
            List<AndroidElement> allObjects = timeList;
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
                            new TouchAction(device.driver).longPress(PointOption.point(nowObject.getLocation().getX(), nowObject.getLocation().getY())).perform();
                            Thread.sleep(1000);

                            device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'复制')]")).click();
                            Thread.sleep(500);

                            String copytext = device.driver.getClipboardText();
                            //获取对方的微信

                            new TouchAction(device.driver).tap(PointOption.point(nowObject.getLocation().getX() - 100, nowObject.getLocation().getY() + 10)).perform();
                            Thread.sleep(1500);
                            //微信号的id
                            String friendname = device.driver.findElementById("com.tencent.mm:id/qk").getText();
                            device.driver.navigate().back();
                            Thread.sleep(500);

                            //插入数据库
                            WechatMsg WC = new WechatMsg(device.udid, account.src_id, account.username);
                            WC.text = copytext;
                            WC.friend_name = friendname;
                            WC.text_type = WechatMsg.Type.Text;
                            WC.insert_time = new Date();
                            WC.insert();
                        }

                        //是自己发送的
                        if (nowObject.getLocation().getX() != 199) {
                            new TouchAction(device.driver).longPress(PointOption.point(nowObject.getLocation().getX(), nowObject.getLocation().getY())).perform();
                            Thread.sleep(1000);
                            device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'复制')]")).click();
                            Thread.sleep(500);
                            String copytext1 = device.driver.getClipboardText();
                            //存入数据库
                            WechatMsg WC1 = new WechatMsg(device.udid, account.src_id, account.username);
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
                        WechatMsg WC = new WechatMsg(device.udid, account.src_id, account.username);
                        WC.text = time;
                        WC.text_type = WechatMsg.Type.Text;
                        WC.insert_time = new Date();
                        WC.insert();
                    }

                    //如果在aev列表中存在，则判断是视频，图片，文件，url其中的一种，进行长按判定
                    if (aevList.contains(nowObject)) {
                        new TouchAction(device.driver).longPress(PointOption.point(nowObject.getLocation().getX(), nowObject.getLocation().getY())).perform();
                        Thread.sleep(1000);

                        List<AndroidElement> optionList = device.driver.findElementsByClassName("android.widget.TextView");
                        //如果有4个选项，就是文件，对文件进行处理(过期文件3个选项)
                        if (optionList.size() == 4) {
                            String filename = device.driver.findElementById("com.tencent.mm:id/afc").getText();
                            device.driver.navigate().back();
                            Thread.sleep(500);
                            //获取对方的微信
                            new TouchAction(device.driver).tap(PointOption.point(nowObject.getLocation().getX() - 100, nowObject.getLocation().getY() + 10)).perform();
                            Thread.sleep(1500);

                            //微信号的id
                            String friendname = device.driver.findElementById("com.tencent.mm:id/qk").getText();
                            device.driver.navigate().back();
                            Thread.sleep(500);

                            //存入数据库
                            WechatMsg WC = new WechatMsg(device.udid, account.src_id, account.username);
                            WC.insert_time = new Date();
                            WC.text = filename;
                            WC.text_type = WechatMsg.Type.File;
                            WC.insert();
                        }

                        //如果是5个选项，就是链接，点开并保存url
                        if (optionList.size() == 5) {

                            device.driver.navigate().back();
                            Thread.sleep(500);

                            nowObject.click();
                            Thread.sleep(5000);

                            device.driver.findElementByAccessibilityId("更多").click();
                            Thread.sleep(1200);

                            device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'复制链接')]")).click();
                            Thread.sleep(500);

                            String url = device.driver.getClipboardText();
                            device.driver.navigate().back();
                            Thread.sleep(500);

                            //判断是对方还是自己发送
                            //自己发送
                            if (nowObject.getLocation().getY() == 231) {
                                //存入数据库
                                WechatMsg WC = new WechatMsg(device.udid, account.src_id, account.username);
                                WC.insert_time = new Date();
                                WC.text = url;
                                WC.text_type = WechatMsg.Type.Url;
                                WC.friend_name = "self";
                                WC.insert();
                            }
                            //别人发送
                            if (nowObject.getLocation().getY() != 231) {
                                //获取对方的微信
                                new TouchAction(device.driver).tap(PointOption.point(nowObject.getLocation().getX() - 100, nowObject.getLocation().getY() + 10)).perform();
                                Thread.sleep(1500);
                                //微信号的id
                                String friendname = device.driver.findElementById("com.tencent.mm:id/qk").getText();
                                device.driver.navigate().back();
                                Thread.sleep(500);
                                WechatMsg WC = new WechatMsg(device.udid, account.src_id, account.username);
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
                                ifpicture = device.driver.findElement(By.xpath("android.widget.TextView[contains(@text,'编辑')]"));
                            } catch (Exception e) {
                            }
                            //如果不是图片就是视频
                            if (ifpicture == null) {
                                device.driver.navigate().back();
                                Thread.sleep(500);
                                nowObject.click();
                                Thread.sleep(3000);
                                new TouchAction(device.driver).longPress(PointOption.point(1000, 1000)).perform();
                                Thread.sleep(500);
                                device.driver.findElement(By.xpath("android.widget.TextView[contains(@text,'保存视频')]"));
                                //TODO 保存视频后如何做 可以通过adb文件导出
                                Thread.sleep(500);
                                device.driver.navigate().back();
                                Thread.sleep(500);
                            }
                            //如果是图片
                            if (ifpicture != null) {
                                device.driver.navigate().back();
                                Thread.sleep(500);
                                nowObject.click();
                                Thread.sleep(2000);
                                byte[] picByte = device.driver.getScreenshotAs(OutputType.BYTES);
                                device.driver.navigate().back();
                                Thread.sleep(500);
                                //判断是别人发送还是自己发送
                                //别人发送
                                if (nowObject.getLocation().getY() == 218) {
                                    //获取对方的微信
                                    new TouchAction(device.driver).tap(PointOption.point(nowObject.getLocation().getX() - 100, nowObject.getLocation().getY() + 10)).perform();
                                    Thread.sleep(1500);
                                    //微信号的id
                                    String friendname = device.driver.findElementById("com.tencent.mm:id/qk").getText();
                                    device.driver.navigate().back();
                                    Thread.sleep(500);
                                    //存入数据库
                                    WechatMsg WC = new WechatMsg(device.udid, account.src_id, account.username);
                                    WC.insert_time = new Date();
                                    WC.content = picByte;
                                    WC.friend_name = friendname;
                                    WC.insert();
                                }
                                //自己发送
                                if (nowObject.getLocation().getY() != 218) {
                                    //存入数据库
                                    WechatMsg WC = new WechatMsg(device.udid, account.src_id, account.username);
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
                new TouchAction(device.driver).press(PointOption.point(allObjects.get(1).getLocation().getX(), allObjects.get(1).getLocation().getY())).moveTo(PointOption.point(allObjects.get(1).getLocation().getX(), 2350)).perform();
            }
        }
    }

    /**
     * @throws InterruptedException
     * @throws AccountException.NoAvailableAccount
     * @throws AdapterException.OperationException
     * @throws DBInitException
     * @throws SQLException
     */
    public void switchAccount(Account.Status... statuses) throws
            InterruptedException,
            AccountException.NoAvailableAccount,
            AdapterException.LoginScriptError,
            SQLException,
            DBInitException {

        Account account;

        if (statuses.length == 0) {
            account = Account.getAccount(device.udid, WeChatAdapter.class.getName());
        } else {
            account = Account.getAccount(device.udid, getClass().getName(), Arrays.asList(statuses));
        }

        if (account != null) {

            try {

                this.account = account;

                // A 退出登录
                logout();

                // B 获取账号  TODO 当前Account为空？
                this.account = Account.getAccount(device.udid, getClass().getName(), Arrays.asList(statuses));

                // C 登录账号  TODO 登录账号需要改变当前类的Account
                login();

            } catch (AccountException.Broken broken) {

                logger.warn("[{}] account broken, ", getInfo(), account.id, broken);
                account.status = Account.Status.Broken;
                account.update();
                switchAccount();
            }
        } else {
            throw new AccountException.NoAvailableAccount();
        }
    }

    @Override
    public void switchAccount(Account account) throws InterruptedException, AdapterException.LoginScriptError, AccountException.NoAvailableAccount, SQLException, DBInitException {
        if (account != null) {
            try {
                // A 退出登录
                logout();

                // B 获取账号  TODO 当前Account为空？
                this.account = account;

                // C 登录账号  TODO 登录账号需要改变当前类的Account
                login();

            } catch (AccountException.Broken broken) {

                logger.warn("[{}] account broken, ", getInfo(), account.id, broken);
                account.status = Account.Status.Broken;
                account.update();
                switchAccount();
            }
        } else {
            throw new AccountException.NoAvailableAccount();
        }
    }
}
