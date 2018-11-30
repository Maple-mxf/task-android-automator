package one.rewind.android.automator;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AbstractService;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.AutomationName;
import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.filters.RequestFilter;
import net.lightbody.bmp.filters.RequestFilterAdapter;
import net.lightbody.bmp.filters.ResponseFilter;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import net.lightbody.bmp.mitm.PemFileCertificateSource;
import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;
import one.rewind.android.automator.model.Comments;
import one.rewind.android.automator.model.Essays;
import one.rewind.android.automator.model.TaskType;
import one.rewind.android.automator.util.AppInfo;
import one.rewind.android.automator.util.MD5Util;
import one.rewind.android.automator.util.ShellUtil;
import one.rewind.util.NetworkUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.openqa.selenium.remote.DesiredCapabilities;
import se.vidstige.jadb.JadbConnection;
import se.vidstige.jadb.JadbDevice;
import se.vidstige.jadb.JadbException;
import se.vidstige.jadb.managers.PackageManager;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 设备连接信息
 */
public class AndroidDevice extends AbstractService {


    private static final Logger logger = LogManager.getLogger(AndroidDevice.class.getName());

    /**
     * 任务队列
     */
    public Queue<String> queue = new ConcurrentLinkedQueue<>();

    private boolean clickEffect;

    public TaskType taskType = TaskType.SUBSCRIBE;

    public boolean isClickEffect() {
        return clickEffect;
    }

    public void setClickEffect(boolean clickEffect) {
        this.clickEffect = clickEffect;
    }

    private static String LOCAL_IP;

    // 配置设定
    static {
        LOCAL_IP = NetworkUtil.getLocalIp();
        logger.info("Local IP: {}", LOCAL_IP);
    }

    // 代理相关设定
    private BrowserMobProxy bmProxy;

    private int proxyPort;

    public String udid;

    private int appiumPort;

    // Appium相关服务对象
    AppiumDriverLocalService service;
    private URL serviceUrl;
    public AndroidDriver driver; // 本地Driver
    public int height;
    public int width;

    /**
     * @param udid       设备udid
     * @param appiumPort
     * @throws Exception
     */
    public AndroidDevice(String udid, int appiumPort) {
        this.udid = udid;
        this.appiumPort = appiumPort;
    }

    /**
     * 获得设备的宽度
     */
    public int getWidth() {
        return driver.manage().window().getSize().width;
    }

    /**
     * 获得设备的高度
     */
    public int getHeight() {
        return driver.manage().window().getSize().height;
    }

    /**
     * 启动MITM代理服务
     *
     * @param port
     */
    public void startProxy(int port) {

        CertificateAndKeySource source =
                new PemFileCertificateSource(new File("ca.crt"), new File("pk.crt"), "sdyk");

        // tell the MitmManager to use the root certificate we just generated
        ImpersonatingMitmManager mitmManager = ImpersonatingMitmManager.builder()
                .rootCertificateSource(source)
                .build();

        bmProxy = new BrowserMobProxyServer();
        bmProxy.setTrustAllServers(true);
        bmProxy.setMitmManager(mitmManager);
        bmProxy.start(port);
        proxyPort = bmProxy.getPort();

        logger.info("Proxy started @port {}", proxyPort);
    }

    /**
     * 设置代理请求过滤器
     *
     * @param filter
     */
    public void setProxyRequestFilter(RequestFilter filter) {
        if (bmProxy == null) return;
        bmProxy.addFirstHttpFilterFactory(new RequestFilterAdapter.FilterSource(filter, 16777216));
    }

    /**
     * 设置代理返回过滤器
     *
     * @param filter
     */
    public void setProxyResponseFilter(ResponseFilter filter) {
        if (bmProxy == null) return;
        bmProxy.addResponseFilter(filter);
    }

    /**
     * 停止代理
     */
    public void stopProxy() {
        if (bmProxy != null) bmProxy.stop();
    }

    /**
     * 设置设备Wifi代理
     * <p>
     * 设备需要连接WIFI，设备与本机器在同一网段
     */
    public void setupWifiProxy() {

        try {

            JadbConnection jadb = new JadbConnection();

            // TODO
            // 需要调用process 启动adb daemon

            List<JadbDevice> devices = jadb.getDevices();

            for (JadbDevice d : devices) {

                if (d.getSerial().equals(udid)) {

                    execShell(d, "settings", "put", "global", "http_proxy", LOCAL_IP + ":" + proxyPort);
                    execShell(d, "settings", "put", "global", "https_proxy", LOCAL_IP + ":" + proxyPort);
//					d.push(new File("ca.crt"), new RemoteFile("/sdcard/_certs/ca.crt"));

                    Thread.sleep(2000);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 移除Wifi Proxy
     */
    public void removeWifiProxy() {

        try {

            JadbConnection jadb = new JadbConnection();

            List<JadbDevice> devices = jadb.getDevices();

            for (JadbDevice d : devices) {

                if (d.getSerial().equals(udid)) {

                    execShell(d, "settings", "delete", "global", "http_proxy");
                    execShell(d, "settings", "delete", "global", "https_proxy");
                    execShell(d, "settings", "delete", "global", "global_http_proxy_host");
                    execShell(d, "settings", "delete", "global", "global_http_proxy_port");

                    Thread.sleep(2000);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 安装APK包
     *
     * @param mobileSerial
     * @param fileName
     */
    public void installApk(String mobileSerial, String fileName) {

        try {

            JadbConnection jadb = new JadbConnection();

            List<JadbDevice> devices = jadb.getDevices();

            for (JadbDevice d : devices) {

                if (d.getSerial().equals(mobileSerial)) {

                    new PackageManager(d).install(new File(fileName));
                    Thread.sleep(2000);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 安装APK包
     *
     * @param apkPath
     */
    public void installApk(String apkPath) {
        String commandStr = "adb -s " + udid + " install " + apkPath;
        ShellUtil.exeCmd(commandStr);
    }

    /**
     * 执行远程设备shell命令
     *
     * @param d
     * @param command
     * @param args
     * @throws IOException
     * @throws JadbException
     */
    public static void execShell(JadbDevice d, String command, String... args) throws IOException, JadbException {

        InputStream is = d.executeShell(command, args);

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder builder = new StringBuilder();

        String line = null;
        try {

            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append("\n"); //appende a new line
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        logger.info(builder.toString());
    }

    /**
     * 初始化AppiumDriver
     *
     * @throws Exception
     */
    public void initAppiumServiceAndDriver(AppInfo info) throws MalformedURLException, InterruptedException {

        // 定义Service Capabilities
        DesiredCapabilities serviceCapabilities = new DesiredCapabilities();

        serviceCapabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android");
        serviceCapabilities.setCapability(MobileCapabilityType.UDID, udid); // udid是设备的唯一标识
        serviceCapabilities.setCapability(MobileCapabilityType.NEW_COMMAND_TIMEOUT, 3600);

        // 定义AppiumService
        service = new AppiumServiceBuilder()
                .withCapabilities(serviceCapabilities)
                .usingPort(appiumPort)
                .withArgument(GeneralServerFlag.LOG_LEVEL, "info")
                //.withArgument(GeneralServerFlag.SESSION_OVERRIDE, "true")
                .build();

        service.start();

        serviceUrl = service.getUrl();

        logger.info("Appium Service URL: {}", serviceUrl);

        // 定义Driver Capabilities
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability("app", "");
        capabilities.setCapability("appPackage", info.appPackage); // App包名
        capabilities.setCapability("appActivity", info.appActivity); // App启动Activity
        capabilities.setCapability("fastReset", false);
        capabilities.setCapability("fullReset", false);
        capabilities.setCapability("noReset", true);

        capabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, AutomationName.ANDROID_UIAUTOMATOR2);


        // TODO 下面两行代码如果不添加 是否不能进入小程序？
        String webViewAndroidProcessName = "com.tencent.mm:tools";
        // webViewAndroidProcessName = "com.tencent.mm:appbrand0"; App中的加载WebView的进程名
        capabilities.setCapability("chromeOptions", ImmutableMap.of("androidProcess", webViewAndroidProcessName));

        capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, udid);

        driver = new AndroidDriver(new URL("http://127.0.0.1:" + appiumPort + "/wd/hub"), capabilities);

        Thread.sleep(5000);

        // 设置宽高
        this.width = getWidth();
        this.height = getHeight();
    }

    /**
     * 返回已经安装的应用程序 TODO 应该返回列表
     */
    public void listApps() {
        String commandStr = "adb -s " + udid + " shell pm accounts packages -3";
        //-3为第三方应用 [-f] [-d] [-e] [-s] [-3] [-i] [-u]
        ShellUtil.exeCmd(commandStr);
    }

    /**
     * 以app的包名为参数，卸载选择的应用程序
     *
     * @param appPackage 卸载选择的app com.ss.android.ugc.aweme
     */
    public void uninstallApp(String appPackage) {
        String commandStr = "adb -s " + udid + " uninstall " + appPackage;
        ShellUtil.exeCmd(commandStr);
    }

    /**
     * 打开应用
     *
     * @param appPackage  包名
     * @param appActivity 主窗体名
     */
    public void startActivity(String appPackage, String appActivity) {
        String commandStr = "adb -s " + udid + " shell am start " + appPackage + "/" + appActivity;
        ShellUtil.exeCmd(commandStr);
    }

    /**
     * 打开应用
     *
     * @param appInfo 应用信息
     */
    public void startActivity(AppInfo appInfo) {
        String commandStr = "adb -s " + udid + " shell am start " + appInfo.appPackage + "/" + appInfo.appActivity;
        ShellUtil.exeCmd(commandStr);
    }

    /**
     * 初始化设备
     */
    public void initApp(int localProxyPort) {
        this.startProxy(localProxyPort);
        this.setupWifiProxy();
        System.out.println("Starting....Please wait!");
        try {
            RequestFilter requestFilter = (request, contents, messageInfo) -> {

                String url = messageInfo.getOriginalUrl();

                if (url.contains("https://mp.weixin.qq.com/s"))
                    System.out.println(" . " + url);
                return null;
            };
            Stack<String> content_stack = new Stack<>();
            Stack<String> stats_stack = new Stack<>();
            Stack<String> comments_stack = new Stack<>();
            ResponseFilter responseFilter = (response, contents, messageInfo) -> {

                String url = messageInfo.getOriginalUrl();

                if (contents != null && (contents.isText() || url.contains("https://mp.weixin.qq.com/s"))) {

                    // 正文
                    if (url.contains("https://mp.weixin.qq.com/s")) {
                        this.setClickEffect(true);
                        System.err.println(" : " + url);
                        content_stack.push(contents.getTextContents());
                    }
                    // 统计信息
                    else if (url.contains("getappmsgext")) {
                        this.setClickEffect(true);
                        System.err.println(" :: " + url);
                        stats_stack.push(contents.getTextContents());
                    }
                    // 评论信息
                    else if (url.contains("appmsg_comment?action=getcomment")) {
                        this.setClickEffect(true);
                        System.err.println(" ::: " + url);
                        comments_stack.push(contents.getTextContents());
                    }
                    if (content_stack.size() > 0) {
                        this.setClickEffect(true);
                        System.out.println("有内容了");
                        String content_src = content_stack.pop();
                        Essays we = null;
                        try {
                            if (stats_stack.size() > 0) {
                                String stats_src = stats_stack.pop();
                                we = new Essays().parseContent(content_src).parseStat(stats_src);
                            } else {
                                we = new Essays().parseContent(content_src);
                                we.view_count = 0;
                                we.like_count = 0;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        assert we != null;
                        we.insert_time = new Date();

                        we.update_time = new Date();
                        we.media_content = we.media_nick;
                        we.platform = "WX";
                        we.media_id = MD5Util.MD5Encode(we.platform + "-" + we.media_name, "UTF-8");
                        we.platform_id = 1;
                        we.fav_count = 0;
                        we.forward_count = 0;
                        we.images = new JSONArray(we.parseImages(we.content)).toString();
                        we.id = MD5Util.MD5Encode(we.platform + "-" + we.media_name + we.title, "UTF-8");
                        try {
                            we.insert();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (comments_stack.size() > 0) {
                            String comments_src = comments_stack.pop();
                            List<Comments> comments_ = null;
                            try {
                                comments_ = Comments.parseComments(we.src_id, comments_src);
                            } catch (ParseException e) {
//                                e.printStackTrace();
                            }
                            comments_.stream().forEach(c -> {
                                try {
                                    c.insert();
                                } catch (Exception e) {
//                                    e.printStackTrace();
                                }
                            });
                        }
                    }
                }
            };
            this.setProxyRequestFilter(requestFilter);
            this.setProxyResponseFilter(responseFilter);

            AppInfo appInfo = AppInfo.get(AppInfo.Defaults.WeChat);
            this.initAppiumServiceAndDriver(appInfo);
            Thread.sleep(3000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doStart() {
        Random random = new Random();
        int var = random.nextInt(50000) % (20000 + 1) + 20000;
        initApp(var);
    }

    public void start() {
        this.doStart();
    }

    @Override
    protected void doStop() {
        stopProxy();
        driver.close();
    }
}
