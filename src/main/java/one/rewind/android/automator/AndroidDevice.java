package one.rewind.android.automator;

import com.dw.ocr.util.ImageUtil;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.AutomationName;
import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import io.appium.java_client.touch.offset.PointOption;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.filters.RequestFilter;
import net.lightbody.bmp.filters.RequestFilterAdapter;
import net.lightbody.bmp.filters.ResponseFilter;
import net.lightbody.bmp.mitm.CertificateAndKeySource;
import net.lightbody.bmp.mitm.PemFileCertificateSource;
import net.lightbody.bmp.mitm.manager.ImpersonatingMitmManager;
import one.rewind.android.automator.adapter.Adapter;
import one.rewind.android.automator.callback.AndroidDeviceCallBack;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.android.automator.exception.AndroidException;
import one.rewind.android.automator.task.Task;
import one.rewind.android.automator.util.ShellUtil;
import one.rewind.android.automator.util.Tab;
import one.rewind.db.DBName;
import one.rewind.db.DaoManager;
import one.rewind.db.model.ModelL;
import one.rewind.util.EnvUtil;
import one.rewind.util.NetworkUtil;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.remote.DesiredCapabilities;
import se.vidstige.jadb.JadbConnection;
import se.vidstige.jadb.JadbDevice;
import se.vidstige.jadb.JadbException;
import se.vidstige.jadb.RemoteFile;
import se.vidstige.jadb.managers.PackageManager;

import javax.imageio.ImageIO;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

/**
 * Android 设备管理
 * <p>
 * 1. 通讯建立方式:
 * 第一步 本地启动 AppiumDriverLocalService 通过adb连接在目标Android设备上启动 AppiumServer
 * AppiumDriverLocalService --> (ADB) --> AppiumServer
 * 第二步 AppiumDriverLocalService 维护和目标Android设备的通讯, 并暴露本地服务地址
 * 第三部 通过AppiumDriverLocalService本地服务地址, 初始化AndroidDevice, 实现对设备的自动化操作
 * AndroidDriver --> AppiumDriverLocalService(HTTP) --> (ADB/HTTP Wired JSON) --> AppiumServer
 */
@DBName("android_automator")
@DatabaseTable(tableName = "androidDevices")
public class AndroidDevice extends ModelL {

    private static final Logger logger = LogManager.getLogger(AndroidDevice.class.getName());

    // 启动超时时间
    private static int INIT_TIMEOUT = 200000;

    // 关闭超时时间
    private static int CLOSE_TIMEOUT = 200000;

    // 截图保存的路径
    public static final String SCREEN_PATH = System.getProperty("user.dir") + "/screen/";

    public enum Status {
        New,  // 新创建
        Init, // 初始化中
        Idle, // 初始化完成，可执行任务
        Busy, // 任务执行中
        Failed, // 出错
        Terminating, // 终止过程中
        Terminated,  // 已终止
        Broken // 不可用
    }

    @DatabaseField(dataType = DataType.ENUM_STRING, width = 32)
    public Status status = Status.New;

    @DatabaseField(dataType = DataType.STRING, width = 32)
    private String local_ip; // 本地IP

    @DatabaseField(dataType = DataType.STRING, width = 32, canBeNull = false, unique = true)
    public String udid; // 设备 udid

    @DatabaseField(dataType = DataType.STRING, width = 32)
    public String name; // 名称

    @DatabaseField(dataType = DataType.BOOLEAN)
    public boolean online = true;

    @DatabaseField(dataType = DataType.BOOLEAN, width = 2)
    public boolean ca = false; // 是否已经安装CA证书

    // 本地代理服务器
    private transient BrowserMobProxyServer bmProxy;

    // Appium相关服务对象
    private transient AppiumDriverLocalService service;

    // 本地Driver
    public transient AndroidDriver driver;

    @DatabaseField(dataType = DataType.INTEGER, width = 5)
    private int proxyPort; // TODO 移动端代理端口

    @DatabaseField(dataType = DataType.INTEGER, width = 5)
    private int appiumPort; // 本地 appium 服务端口

    @DatabaseField(dataType = DataType.INTEGER, width = 5)
    public int localProxyPort; // TODO 代码运行端代理端口 区别？

    // Appium 服务URL 本地
    private transient URL serviceUrl;

    @DatabaseField(dataType = DataType.INTEGER, width = 5)
    public int height; // 设备屏幕高度

    @DatabaseField(dataType = DataType.INTEGER, width = 5)
    public int width; // 设备屏幕宽度

    // 上次启动时间
    @DatabaseField(dataType = DataType.DATE_TIME)
    public Date init_time;

    // Executor Queue
    private transient LinkedBlockingQueue queue = new LinkedBlockingQueue<Runnable>();

    // Executor
    private transient ThreadPoolExecutor executor;

    /**
     * 单个设备对应多个Adapter,Adapter是指手机硬件上安装的可正常运行的APP,Device是指具体物理机,单个Device可对应多个Adapter
     * 单个Device可随时切换Adapter,Device只是核心控制控制程序之间的一个桥接,相当于一个连接对象,Device不应该负责去执行任务,
     * 任务启动执行在Adapter层,控制Device执行.当前Device在切换Adapter的时候,任务随着Adapter的切换而切换.单个Adapter对应一个
     * 账号列表,每个账号都存在一个可用或者不可用的状态,
     * Account -> Adapter -> Device
     */
    public transient Map<String, Adapter> adapters = new HashMap<>();

    //
    public transient List<AndroidDeviceCallBack> initCallbacks = new ArrayList<>();

    //
    public transient List<AndroidDeviceCallBack> idleCallbacks = new ArrayList<>();

    //
    public transient List<AndroidDeviceCallBack> failedCallbacks = new ArrayList<>();

    //
    public transient List<AndroidDeviceCallBack> terminatedCallbacks = new ArrayList<>();

    // 当前正在执行的任务
    public transient Future<Boolean> taskFuture;

    /**
     * 构造方法
     *
     * @param udid 设备udid
     * @throws Exception
     */
    public AndroidDevice(String udid) {

        this.udid = udid;

        this.local_ip = NetworkUtil.getLocalIp();
        logger.info("Local IP: {}", local_ip);

        name = "AD[" + udid + "]";

        // 初始化单线程执行器
        executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, queue);
        executor.setThreadFactory(new ThreadFactoryBuilder()
                .setNameFormat(name + "-%d").build());
    }

    public AndroidDevice() {

    }

    /**
     * @param udid
     * @return
     * @throws Exception
     */
    public static AndroidDevice getAndroidDeviceByUdid(String udid) throws Exception {

        Dao<AndroidDevice, String> dao = DaoManager.getDao(AndroidDevice.class);
        AndroidDevice ad = dao.queryBuilder().where().eq("udid", udid).queryForFirst();

        if (ad == null) ad = new AndroidDevice(udid);

        return ad;
    }

    /**
     * 添加Adapter
     * 只有在 New 状态才能添加
     *
     * @param adapter
     * @return
     */
    public AndroidDevice addAdapter(Adapter adapter) throws AndroidException.IllegalStatusException {

        if (status != Status.New) {
            throw new AndroidException.IllegalStatusException();
        }

        this.adapters.put(adapter.getClass().getName(), adapter);

        this.initCallbacks.add((d) -> {

            try {
                adapter.start();
            } catch (InterruptedException e) {

                logger.error("Interrupted, ", e);
            }
            // 对应的Adapter操作定义的有问题，已经与app对应不上
            catch (AdapterException.OperationException e) {

                logger.error("Adapter operation exception, ", e);

                this.adapters.remove(adapter.getClass().getName());
            }
            // 没有可用账号了
            catch (AccountException.Broken e) {

                logger.error("{} Account[{}] broken, ", adapter.getClass().getSimpleName(), e.account.id, e);

                this.adapters.remove(adapter.getClass().getName());
            }
        });

        return this;
    }

    /**
     *
     */
    public class Init implements Callable<Boolean> {

        public Boolean call() throws Exception {

            logger.info("Init...");

            AndroidDevice.this.appiumPort = Tab.appiumPort.getAndIncrement();
            AndroidDevice.this.proxyPort = Tab.proxyPort.getAndIncrement();
            AndroidDevice.this.localProxyPort = Tab.localProxyPort.getAndIncrement();

            // 安装CA
            // installCA();

            // 启动代理
            startProxy();

            // 设置设备Wifi代理
            setupRemoteWifiProxy();

            // 启动相关服务
            initAppiumServiceAndDriver(new Adapter.AppInfo("com.tencent.mm", ".ui.LauncherUI"));

            runCallbacks(initCallbacks).get();

            init_time = new Date();

            return true;
        }
    }

    /**
     *
     */
    public class Stop implements Callable<Boolean> {

        public Boolean call() throws IOException {

            logger.info("Stopping [{}] ...", name);

            // 停止 driver
            driver.close();

            // 停止 Appium service运行
            if (service.isRunning()) service.stop();

            // 停止设备端的 appium
            stopRemoteAppiumServer();

            // 停止代理服务器
            stopProxy();

            logger.info("[{}] stopped.", name);

            return true;
        }
    }

    /**
     * 同步方法
     *
     * @return
     * @throws AndroidException.IllegalStatusException
     */
    public synchronized AndroidDevice start() throws AndroidException.IllegalStatusException {

        if (!(status == Status.New || status == Status.Terminated)) {
            throw new AndroidException.IllegalStatusException();
        }

        status = Status.Init;

        //
        Future<Boolean> initFuture = executor.submit(new Init());

        try {

            boolean initSuccess = initFuture.get(INIT_TIMEOUT, TimeUnit.MILLISECONDS);

            status = Status.Idle;

            if (initSuccess) {
                logger.info("[{}] INIT done.", name);
            }

            // 执行状态回调函数
            runCallbacks(idleCallbacks);

        }
        // 当前进程被终止
        catch (InterruptedException e) {

            status = Status.Failed;
            logger.error("[{}] INIT interrupted. ", name, e);
            stop();

        } catch (ExecutionException e) {

            status = Status.Failed;
            logger.error("[{}] INIT failed. ", name, e.getCause());
            stop();

        } catch (TimeoutException e) {

            initFuture.cancel(true);

            status = Status.Failed;
            logger.error("[{}] INIT failed. ", name, e);
            stop();
        }

        return this;
    }

    /**
     * @return
     * @throws AndroidException.IllegalStatusException
     */
    public synchronized AndroidDevice stop() throws AndroidException.IllegalStatusException {
        return stop(true);
    }

    /**
     * @return
     * @throws AndroidException.IllegalStatusException
     */
    public synchronized AndroidDevice stop(boolean runCallbacks) throws AndroidException.IllegalStatusException {

        if (!(status == Status.Init || status == Status.Idle || status == Status.Busy || status == Status.Failed)) {
            // TODO New
            // Terminating, // 终止过程中
            // Terminated,  // 已终止
            // Broken // 不可用
            throw new AndroidException.IllegalStatusException();
        }

        this.status = Status.Terminating;

        Future<Boolean> closeFuture = executor.submit(new Stop());

        try {

            closeFuture.get(CLOSE_TIMEOUT, TimeUnit.MILLISECONDS);
            status = Status.Terminated;
            logger.info("[{}] Stop done.", name);

            if (runCallbacks) runCallbacks(terminatedCallbacks);

        } catch (InterruptedException e) {

            status = Status.Failed;
            logger.error("[{}] Stop interrupted. ", name, e);

        } catch (ExecutionException e) {

            status = Status.Failed;
            logger.error("[{}] Stop failed. ", name, e.getCause());

        } catch (TimeoutException e) {

            closeFuture.cancel(true);
            status = Status.Failed;
            logger.error("[{}] Stop failed. ", name, e);
        } finally {
            try {
                this.update();
            } catch (Exception e) {
                logger.error("Can't update device, ", e);
            }
        }

        return this;
    }

    /**
     * 重启
     * TODO 是否可以通过添加Terminated回调来实现？
     * 场景: 设备运行时间过程, 没有响应
     */
    public void restart() throws IOException, InterruptedException, AndroidException.IllegalStatusException {
        stop();
        clear();
        clearCacheLog();
        clearAllLog();
        start();
    }

    /**
     * @param task
     * @throws AndroidException.IllegalStatusException
     */
    public synchronized void submit(Task task) throws AndroidException.IllegalStatusException {

        if (!(status == Status.Idle || status == Status.Busy)) {
            throw new AndroidException.IllegalStatusException();
        }

        this.status = Status.Busy;

        task.adapter = adapters.get(task.holder.adapter_class_name);

        taskFuture = executor.submit(task);

        try {

            taskFuture.get(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
            status = Status.Idle;
            logger.info("[{}] Stop done.", name);

            runCallbacks(idleCallbacks);
        }
        //
        catch (InterruptedException e) {

            status = Status.Failed;
            logger.error("[{}] Stop interrupted. ", name, e);

        }
        // TODO 对执行时异常的精细处理
        catch (ExecutionException e) {

            status = Status.Failed;
            logger.error("[{}] Stop failed. ", name, e.getCause());
            try {
                throw e.getCause();
            } catch (Throwable ex) {

            }

        }
        //
        catch (TimeoutException e) {

            taskFuture.cancel(true);
            status = Status.Failed;
            logger.error("[{}] Stop failed. ", name, e);
        }

        taskFuture = null;
    }

    /**
     * 获得设备的宽度
     * TODO 改用 Optional nullable
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
     * <p>
     * https://github.com/lightbody/browsermob-proxy
     */
    public void startProxy() {

        // A 加载证书
        // 证书生成参考 openssl相关命令
        /*CertificateAndKeySource source = new PemFileCertificateSource(
                new File("ca.crt"), new File("pk.crt"), "sdyk");*/

        CertificateAndKeySource source = new PemFileCertificateSource(new File("ca.crt"), new File("pk.crt"), "sdyk");

        // B 让 MitmManager 使用刚生成的 root certificate
        ImpersonatingMitmManager mitmManager = ImpersonatingMitmManager.builder()
                .rootCertificateSource(source)
                .build();

        // C 初始化 bmProxy
        bmProxy = new BrowserMobProxyServer();
        bmProxy.setTrustAllServers(true);
        bmProxy.setMitmManager(mitmManager);
        bmProxy.start(proxyPort);
        proxyPort = bmProxy.getPort(); // TODO 是否有必要?

        // 设定初始 RequestFilter
        bmProxy.addRequestFilter((request, contents, messageInfo) -> null);

        // 设定初始 ResponseFilter
        bmProxy.addResponseFilter((response, contents, messageInfo) -> {
        });

        logger.info("Proxy started @proxyPort {}", proxyPort);
    }

    /**
     * 设置代理请求过滤器
     *
     * @param filter
     */
    public void setProxyRequestFilter(RequestFilter filter) {
        if (bmProxy == null) return;
        //bmProxy.addFirstHttpFilterFactory(new RequestFilterAdapter.FilterSource(filter, 16777216));
        bmProxy.replaceFirstHttpFilterFactory(new RequestFilterAdapter.FilterSource(filter, 16777216));
    }

    /**
     * 设置代理返回过滤器
     *
     * @param filter
     */
    public void setProxyResponseFilter(ResponseFilter filter) {
        if (bmProxy == null) return;
        // bmProxy.addResponseFilter(filter);
        bmProxy.replaceLastHttpFilter(filter);
    }

    /**
     * 停止代理
     */
    public void stopProxy() {
        Optional.ofNullable(bmProxy).ifPresent(BrowserMobProxy::stop);
    }

    /**
     * 安装CA证书，否则无法解析https数据
     *
     * @throws IOException
     * @throws JadbException
     * @throws InterruptedException
     */
    public void installCA() throws IOException, JadbException, InterruptedException {

        JadbConnection jadb = new JadbConnection();

        // TODO
        // 需要调用process 启动adb daemon, 否则第一次执行会出错

        List<JadbDevice> devices = jadb.getDevices();

        for (JadbDevice d : devices) {

            if (d.getSerial().equals(udid)) {

                // TODO 使用 adb 判断远端文件是否存在
                // ls /path/to/your/files* 1> /dev/null 2>&1;
                d.push(new File("ca.crt"), new RemoteFile("/sdcard/_certs/ca.crt"));
                Thread.sleep(2000);
            }
        }
    }

    /**
     * 设置设备Wifi代理
     * <p>
     * 设备需要连接WIFI，设备与本机器在同一网段
     */
    public void setupRemoteWifiProxy() throws IOException, JadbException, InterruptedException {

        JadbConnection jadb = new JadbConnection();

        // TODO
        // 需要调用process 启动adb daemon, 否则第一次执行会出错

        List<JadbDevice> devices = jadb.getDevices();

        for (JadbDevice d : devices) {

            if (d.getSerial().equals(udid)) {

                execShell(d, "settings", "put", "global", "http_proxy", local_ip + ":" + proxyPort);
                execShell(d, "settings", "put", "global", "https_proxy", local_ip + ":" + proxyPort);
                // d.push(new File("ca.crt"), new RemoteFile("/sdcard/_certs/ca.crt"));
                Thread.sleep(2000);
            }
        }
    }

    /**
     * 移除Wifi Proxy
     */
    public void removeRemoteWifiProxy() throws IOException, JadbException, InterruptedException {

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
    }

    /**
     * 初始化AppiumDriver
     * TODO 是否可以不调用此方法，启动其他App
     *
     * @throws Exception
     */
    public void initAppiumServiceAndDriver(Adapter.AppInfo appInfo) throws MalformedURLException, InterruptedException {

        // A 定义Service Capabilities
        DesiredCapabilities serviceCapabilities = new DesiredCapabilities();

        serviceCapabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android");
        serviceCapabilities.setCapability(MobileCapabilityType.UDID, udid); // udid是设备的唯一标识
        serviceCapabilities.setCapability(MobileCapabilityType.NEW_COMMAND_TIMEOUT, 3600);

        // B 定义AppiumService
        if (EnvUtil.isHostLinux()) {
            service = new AppiumServiceBuilder()
                    .withCapabilities(serviceCapabilities)
                    .usingPort(appiumPort)
                    .withArgument(GeneralServerFlag.LOG_LEVEL, "info")
                    .withAppiumJS(new File("/usr/local/lib/node_modules/appium/build/lib/main.js")) // TODO Ubuntu系统下的固定文件路径，必须添加
                    /*.withArgument(GeneralServerFlag.SESSION_OVERRIDE, "true")*/ // TODO  session覆盖问题解决
                    .build();
        } else {
            service = new AppiumServiceBuilder()
                    .withCapabilities(serviceCapabilities)
                    .usingPort(appiumPort)
                    .withArgument(GeneralServerFlag.LOG_LEVEL, "info")
                    /*.withArgument(GeneralServerFlag.SESSION_OVERRIDE, "true")*/ // TODO  session覆盖问题解决
                    .build();
        }

        service.start();

        Thread.sleep(5000);

        serviceUrl = service.getUrl();

        logger.info("Appium Service URL: {}", serviceUrl);

        // C 定义Driver Capabilities
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability("app", "");
        capabilities.setCapability("appPackage", appInfo.appPackage);   // App包名
        capabilities.setCapability("appActivity", appInfo.appActivity); // App启动Activity
        capabilities.setCapability("fastReset", false);
        capabilities.setCapability("fullReset", false);
        capabilities.setCapability("noReset", true);

        capabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, AutomationName.ANDROID_UIAUTOMATOR2);

        // TODO 下面两行代码如果不添加 是否不能进入小程序？
        /*String webViewAndroidProcessName = "com.tencent.mm:tools";
        webViewAndroidProcessName = "com.tencent.mm:appbrand0"; // App中的加载WebView的进程名
        capabilities.setCapability("chromeOptions", ImmutableMap.of("androidProcess", webViewAndroidProcessName));*/

        capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, udid);

        // TODO 是否可以自动获取url?
        driver = new AndroidDriver(new URL("http://127.0.0.1:" + appiumPort + "/wd/hub"), capabilities);
        Thread.sleep(15000);

        // 设置宽高
        this.width = getWidth();
        this.height = getHeight();
    }

    /**
     * 安装APK包
     *
     * @param apkPath 本地apk路径
     */
    public void installApk(String apkPath) {

        try {

            JadbConnection jadb = new JadbConnection();

            List<JadbDevice> devices = jadb.getDevices();

            for (JadbDevice d : devices) {

                if (d.getSerial().equals(udid)) {

                    new PackageManager(d).install(new File(apkPath));
                    Thread.sleep(2000);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 远程安装APK包
     *
     * @param apkPath 设备apk路径
     */
    public void installApkRemote(String apkPath) {
        String commandStr = "adb -s " + udid + " install " + apkPath;
        ShellUtil.exeCmd(commandStr);
    }

    /**
     * @param command
     * @param args
     * @throws IOException
     * @throws JadbException
     */
    public void execShell(String command, String... args) throws IOException, JadbException {

        JadbConnection jadb = new JadbConnection();

        List<JadbDevice> devices = jadb.getDevices();

        for (JadbDevice d : devices) {

            if (d.getSerial().equals(udid)) {

                execShell(d, command, args);

                return;
            }
        }
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

        String line;
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
     * 返回已经安装的应用程序
     * TODO 应该返回列表
     */
    public void listApps() {

        String commandStr = "adb -s " + udid + " shell pm accounts packages -3";
        //-3为第三方应用 [-f] [-d] [-e] [-s] [-3] [-i] [-u]
        ShellUtil.exeCmd(commandStr);
    }

    /**
     * 进入相应设备的shell
     *
     * @param udid
     * @throws IOException
     */
    private static void enterADBShell(String udid) throws IOException {
        String command = "adb -s " + udid + " shell";
        ShellUtil.exeCmd(command);
    }

    /**
     * 摁电源键
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void clickPower() throws IOException, InterruptedException {
        String powerCommand = "adb -s " + udid + " shell input keyevent 26";
        Runtime runtime = Runtime.getRuntime();
        runtime.exec(powerCommand);
        Thread.sleep(2000);
    }

    /**
     * 重启设备
     *
     * @param udid
     * @throws IOException
     * @throws InterruptedException
     */
    public static void reboot(String udid) throws IOException, InterruptedException {

        enterADBShell(udid);

        ShellUtil.exeCmd("reboot");

        Thread.sleep(120000);

        enterADBShell(udid);

        // TODO 执行 adb usb

        // 滑动解锁 TODO 如果不设定密码 就可以不用解锁了 默认到Home界面
        ShellUtil.exeCmd("adb shell input swipe 300 1000 300 500");
    }


    /**
     * 唤醒设备
     *
     * @param driver
     * @throws IOException
     * @throws InterruptedException
     */
    public void notifyDevice(AndroidDriver driver) throws IOException, InterruptedException {
        Runtime runtime = Runtime.getRuntime();

        clickPower();
        Thread.sleep(2000);

        // 滑动解锁  728 2356  728 228  adb shell -s  input swipe  300 1500 300 200
        String unlockCommand = "adb -s " + udid + " shell input swipe  300 1500 300 200";
        runtime.exec(unlockCommand);
        Thread.sleep(2000);

        // 输入密码adb -s ZX1G42BX4R shell input text szqj  adb -s ZX1G42BX4R shell input swipe 300 1000 300 500
        String loginCommand = "adb -s " + udid + " shell input text szqj";
        runtime.exec(loginCommand);
        Thread.sleep(4000);

        // 点击确认
        touch(1350, 2250, 6000); //TODO 时间适当调整
        Thread.sleep(2000);
    }

    /**
     * 杀死进程
     *
     * @param packageName
     * @throws IOException
     * @throws InterruptedException
     */
    public void shutdownProcess(String packageName) {
        String command = "adb -s " + udid + " shell am force-stop " + packageName;
        ShellUtil.exeCmd(command);
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
    public void startApp(String appPackage, String appActivity) {
        String commandStr = "adb -s " + udid + " shell am start " + appPackage + "/" + appActivity;
        ShellUtil.exeCmd(commandStr);
    }

    /**
     * 打开应用
     *
     * @param appInfo 应用信息
     */
    public void startApp(Adapter.AppInfo appInfo) {
        String commandStr = "adb -s " + udid + " shell am start " + appInfo.appPackage + "/" + appInfo.appActivity;
        ShellUtil.exeCmd(commandStr);
    }

    /**
     * @param appInfo
     * @throws IOException
     * @throws InterruptedException
     */
    public void stopApp(Adapter.AppInfo appInfo) {
        shutdownProcess(appInfo.appPackage);
    }

    /**
     * 清空缓存日志
     */
    public void clearCacheLog() {
        try {
            String command = "adb -s " + this.udid + " logcat -c -b events";
            Runtime.getRuntime().exec(command);
            logger.info("清空设备 {} 的缓存日志");
        } catch (Exception ignore) {
            logger.error(ignore);
        }
    }

    /**
     * 清空所有日志
     */
    public void clearAllLog() {
        try {
            String command = "adb -s " + this.udid + " logcat -c -b main -b events -b radio -b system";
            Runtime.getRuntime().exec(command);
            logger.info("清空设备 {} 的全部系统级别日志");
        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     * TODO 返回桌面 清理所有后台app进程
     * 返回桌面 am start -a android.intent.action.MAIN -c android.intent.category.HOME
     * 清理app进程 am kill <package_name>
     */
    public void clear() throws IOException, InterruptedException {

    }

    /**
     * 重启移动端appium
     *
     * @throws IOException
     */
    public void stopRemoteAppiumServer() throws IOException {
        String command1 = "adb -s " + this.udid + " shell am force-stop io.appium.settings";
        Runtime.getRuntime().exec(command1);

        String command2 = "adb -s " + this.udid + " shell am force-stop io.appium.uiautomator2.server";
        Runtime.getRuntime().exec(command2);
        logger.info("Restart {} Appium Server", udid);
    }

    /**
     * 截图
     *
     * @return 获取保存的文件路径
     */
    public String screenShot() throws IOException {

        File screenFile = ((TakesScreenshot) this.driver).getScreenshotAs(OutputType.FILE);

        String imageFullName = SCREEN_PATH + UUID.randomUUID().toString() + ".png";

        FileUtils.copyFile(screenFile, new File(imageFullName));

        return imageFullName;
    }

    /**
     * 点击固定的位置
     *
     * @param x         x
     * @param y         y
     * @param sleepTime 睡眠时间
     * @throws InterruptedException e
     */
    public void touch(int x, int y, long sleepTime) throws InterruptedException {

        new TouchAction(driver).tap(PointOption.point(x, y)).perform();

        if (sleepTime > 0) {
            Thread.sleep(sleepTime);
        }
    }

    /**
     * @param x
     * @param y
     * @return
     * @throws InterruptedException
     * @throws IOException
     * @throws AdapterException.NoResponseException
     */
    public boolean reliableTouch(int x, int y) throws InterruptedException, IOException, AdapterException.NoResponseException {
        return reliableTouch(x, y, 5000, 0);
    }

    /**
     *
     */
    public boolean reliableTouch(int x, int y, long sleep, int retry) throws IOException, InterruptedException, AdapterException.NoResponseException {

        // 0 判断retry是否超限
        if (retry < 3) {

            // A 截图1
            String path1 = screenShot();

            // B 进行touch 操作
            touch(x, y, sleep);

            // C 截图2
            String path2 = screenShot();

            // D 比较 两个截图相似性
            boolean similar = ImageUtil.isSame(ImageIO.read(new File(path1)), ImageIO.read(new File(path2)));

            // E 如果相似 递归调用
            if (similar) {
                return reliableTouch(x, y, sleep, retry++);
            } else {
                return true;
            }

        } else {
            throw new AdapterException.NoResponseException();
        }
    }

    /**
     * 下滑到指定位置
     *
     * @param startX    start x point
     * @param startY    start y point
     * @param endX      end x point
     * @param endY      end y point
     * @param sleepTime thread sleep time by mill
     * @throws InterruptedException e
     */
    public void slideToPoint(int startX, int startY, int endX, int endY, int sleepTime) throws InterruptedException {
        new TouchAction(driver).press(PointOption.point(startX, startY))
                .waitAction()
                .moveTo(PointOption.point(endX, endY))
                .release()
                .perform();
        Thread.sleep(sleepTime);
    }

    /**
     * 点击安卓原生的导航栏返回按钮
     */
    public void goBack() {
        driver.navigate().back();
    }


    public AndroidDevice addInitCallback(AndroidDeviceCallBack callBack) {
        this.initCallbacks.add(callBack);
        return this;
    }

    public AndroidDevice addIdleCallback(AndroidDeviceCallBack callBack) {
        this.idleCallbacks.add(callBack);
        return this;
    }

    public AndroidDevice addFailedCallback(AndroidDeviceCallBack callBack) {
        this.failedCallbacks.add(callBack);
        return this;
    }

    public AndroidDevice addTerminatedCallback(AndroidDeviceCallBack callBack) {
        this.terminatedCallbacks.add(callBack);
        return this;
    }

    /**
     * @param callbacks
     * @return
     */
    private Future runCallbacks(List<AndroidDeviceCallBack> callbacks) {

        if (callbacks == null) return null;

        return AndroidDeviceManager.getInstance().executor.submit(() -> {

            try {
                for (AndroidDeviceCallBack callback : callbacks) {
                    callback.call(this);
                }
            } catch (Exception e) {
                logger.error("Error execute callback, ", e);
            }
        });
    }
}
