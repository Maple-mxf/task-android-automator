package one.rewind.android.automator.deivce;

import com.dw.ocr.client.OCRClient;
import com.dw.ocr.parser.OCRParser;
import com.dw.ocr.util.ImageUtil;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.reflect.TypeToken;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.StringType;
import com.j256.ormlite.table.DatabaseTable;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidElement;
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
import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.adapter.Adapter;
import one.rewind.android.automator.callback.AndroidDeviceCallBack;
import one.rewind.android.automator.deivce.action.Clear;
import one.rewind.android.automator.deivce.action.Init;
import one.rewind.android.automator.deivce.action.Reboot;
import one.rewind.android.automator.deivce.action.Stop;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.android.automator.exception.AndroidException;
import one.rewind.android.automator.exception.TaskException;
import one.rewind.android.automator.task.Task;
import one.rewind.db.Daos;
import one.rewind.db.annotation.DBName;
import one.rewind.db.exception.DBInitException;
import one.rewind.db.model.ModelL;
import one.rewind.json.JSON;
import one.rewind.util.EnvUtil;
import one.rewind.util.NetworkUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.ErrorHandler;
import org.openqa.selenium.remote.UnreachableBrowserException;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.SocketException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

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
@DatabaseTable(tableName = "android_devices")
public class AndroidDevice extends ModelL {

    private static final Logger logger = LogManager.getLogger(AndroidDevice.class.getName());

    // 设备的pin密码
    public static final String PIN_PASSWORD = "1234";

    // 启动超时时间
    private static int INIT_TIMEOUT = 20000000;

    // 关闭超时时间
    private static int CLOSE_TIMEOUT = 20000000;

    // 任务重试次数
    private static int TASK_RETRY_LIMIT = 3;

    public enum Status {
        New,  // 新创建
        Init, // 初始化中
        Idle, // 初始化完成，可执行任务
        Busy, // 任务执行中
        Failed, // 出错 不可用
        Terminating, // 终止过程中
        Terminated,  // 已终止
        DeviceBooting // 重启过程中
    }

    public enum Flag {
        Cleaned, NewReboot, NewRestart
    }

    @DatabaseField(dataType = DataType.ENUM_STRING, width = 32)
    public volatile Status status = Status.New;

    @DatabaseField(persisterClass = JSONableFlagListPersister.class, width = 32)
    public List<Flag> flags = new ArrayList<>();

    @DatabaseField(dataType = DataType.STRING, width = 32)
    public String local_ip; // 本地IP

    @DatabaseField(dataType = DataType.STRING, width = 32, canBeNull = false, unique = true)
    public String udid; // 设备 udid

    @DatabaseField(dataType = DataType.STRING, width = 32)
    public String name; // 名称

    @DatabaseField(dataType = DataType.BOOLEAN)
    public boolean online = true;

    @DatabaseField(dataType = DataType.BOOLEAN, width = 2)
    public boolean ca = false; // 是否已经安装CA证书

    // 本地代理服务器
    public transient BrowserMobProxyServer bmProxy;

    // Appium相关服务对象
    public static transient AppiumDriverLocalService service;

    // 本地Driver
    public transient AndroidDriver<AndroidElement> driver;

    @DatabaseField(dataType = DataType.INTEGER, width = 5)
    public int proxyPort; // TODO 移动端代理端口

    @DatabaseField(dataType = DataType.INTEGER, width = 5)
    public int appiumPort; // 本地 appium 服务端口

    @DatabaseField(dataType = DataType.INTEGER, width = 5)
    public int localProxyPort; // TODO 代码运行端代理端口 区别？

    // Appium 服务URL 本地
    public static transient URL serviceUrl;

    @DatabaseField(dataType = DataType.INTEGER, width = 5)
    public int height; // 设备屏幕高度

    @DatabaseField(dataType = DataType.INTEGER, width = 5)
    public int width; // 设备屏幕宽度

    // 上次启动时间
    @DatabaseField(dataType = DataType.DATE)
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
    public transient List<AndroidDeviceCallBack.InitCallBack> initCallbacks = new ArrayList<>();

    //
    public transient List<AndroidDeviceCallBack> idleCallbacks = new ArrayList<>();

    //
    public transient List<AndroidDeviceCallBack> terminatedCallbacks = new ArrayList<>();

    // 当前正在执行的任务
    public transient Future<Boolean> taskFuture;

    static {
        // A 定义Service Capabilities
        DesiredCapabilities serviceCapabilities = new DesiredCapabilities();

        serviceCapabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android");
        // serviceCapabilities.setCapability(MobileCapabilityType.UDID, udid); // udid是设备的唯一标识
        serviceCapabilities.setCapability(MobileCapabilityType.NEW_COMMAND_TIMEOUT, 0); // 永不超时

        // B 定义AppiumService
        if (EnvUtil.isHostLinux()) {
            service = new AppiumServiceBuilder()
                    .withCapabilities(serviceCapabilities)
                    .usingPort(42755)
                    .withArgument(GeneralServerFlag.LOG_LEVEL, "error")
                    .withAppiumJS(new File("/usr/local/lib/node_modules/appium/build/lib/main.js")) // TODO Ubuntu系统下的固定文件路径，必须添加
                    /*.withArgument(GeneralServerFlag.SESSION_OVERRIDE, "true")*/ // TODO  session覆盖问题解决
                    .build();
        } else {
            service = new AppiumServiceBuilder()
                    .withCapabilities(serviceCapabilities)
                    .usingPort(42755)
                    .withArgument(GeneralServerFlag.LOG_LEVEL, "error")
                    /*.withArgument(GeneralServerFlag.SESSION_OVERRIDE, "true")*/ // TODO  session覆盖问题解决
                    .build();
        }

        service.start();

		/*File logFile = new File("log/appium.log");
		logFile.createNewFile(); // if file already exists will do nothing
		FileOutputStream oFile = new FileOutputStream(logFile, false);
		service.sendOutputTo(oFile);*/

        serviceUrl = service.getUrl();

        logger.info("Appium Service URL: {}", serviceUrl);
    }

    public AndroidDevice() {
    }

    /**
     * 构造方法
     *
     * @param udid 设备udid
     * @throws Exception
     */
    public AndroidDevice(String udid) {
        this.udid = udid;
        setupExecutor();
    }

    private void setupExecutor() {
        this.local_ip = NetworkUtil.getLocalIp();
        logger.info("Local IP: {}", local_ip);

        name = "" + udid + "";

        // 初始化单线程执行器
        executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, queue);
        executor.setThreadFactory(new ThreadFactoryBuilder()
                .setNameFormat(name + "-%d").build());
    }

    /**
     * @param udid
     * @return
     * @throws Exception
     */
    public static AndroidDevice getAndroidDeviceByUdid(String udid) throws DBInitException, SQLException {

        Dao<AndroidDevice, String> dao = Daos.get(AndroidDevice.class);

        AndroidDevice ad = dao.queryBuilder().where().eq("udid", udid).queryForFirst();

        // 同步数据库记录
        if (ad == null) {
            ad = new AndroidDevice(udid);
            ad.insert();
        } else {
            ad.setupExecutor();
            ad.update();
        }

        return ad;
    }

    /**
     * 添加Adapter
     * 只有在 New 状态才能添加 ?
     *
     * @param adapter
     * @return
     */
    public AndroidDevice addAdapter(Adapter adapter) throws AndroidException.IllegalStatusException {

        if (status != Status.New) {
            throw new AndroidException.IllegalStatusException();
        }

        this.adapters.put(adapter.getClass().getName(), adapter);
        logger.info("[{}] added ", adapter.getInfo());

        // 初始化回调函数
        this.initCallbacks.add((d) -> {

            try {

                d.startAdapter(adapter);

            } catch (AdapterException.LoginScriptError e) {

                logger.error("[{}] login script error, ", adapter.getInfo(), e);
                removeAdapter(adapter);

            } catch (AccountException.NoAvailableAccount noAvailableAccount) {

                logger.error("[{}] no available account, ", adapter.getInfo(), noAvailableAccount);
                removeAdapter(adapter);
            }

        });

        return this;
    }

    /**
     * @param adapter
     */
    public void removeAdapter(Adapter adapter) {
        this.adapters.remove(adapter.getClass().getName());
        logger.info("[{}] removed, ", adapter.getInfo());
    }

    /**
     * @param adapter
     * @return
     */
    public AndroidDevice startAdapter(Adapter adapter) throws DBInitException, SQLException, InterruptedException, AdapterException.LoginScriptError, AccountException.NoAvailableAccount {

        try {
            adapter.start();
//			adapter.checkAccount();  TODO   是否有必要检查账号？
            logger.info("[{}] started", adapter.getInfo());

        } catch (AccountException.Broken broken) {

            logger.error("[{}] broken, ", adapter.getInfo(), broken);
            broken.account.status = Account.Status.Broken;
            broken.account.update();

            adapter.switchAccount();
        }

        return this;
    }

    /**
     * 同步方法
     *
     * @return
     * @throws AndroidException.IllegalStatusException
     */
    public boolean start() throws AndroidException.IllegalStatusException, DBInitException, SQLException {

        if (!(status == Status.New || status == Status.Terminated)) {
            throw new AndroidException.IllegalStatusException();
        }

        status = Status.Init;

        //
        Future<Boolean> initFuture = executor.submit(new Init(this));

        try {

            boolean initSuccess = initFuture.get(INIT_TIMEOUT, TimeUnit.MILLISECONDS);

            if (initSuccess) {
                status = Status.Idle;
                logger.info("[{}] INIT done", udid);
                // 执行状态回调函数
                runCallbacks(idleCallbacks);
                logger.info("idleCallbacks size[{}] ", idleCallbacks.size());
                return initSuccess;
            } else {
                status = Status.Failed;
                logger.info("[{}] INIT failed", udid);
                stop();
            }

        }
        // 当前进程被终止
        catch (InterruptedException e) {

            status = Status.Failed;
            logger.error("[{}] INIT interrupted, ", udid, e);
            stop();

        } catch (ExecutionException e) {

            status = Status.Failed;
            logger.error("[{}] INIT failed, ", udid, e.getCause());
            stop();

        } catch (TimeoutException e) {

            initFuture.cancel(true);

            status = Status.Failed;
            logger.error("[{}] INIT timeout, ", udid, e);
            stop();
        } finally {
            update();
        }

        return false;
    }

    /**
     * @return
     * @throws AndroidException.IllegalStatusException
     */
    public boolean stop() throws AndroidException.IllegalStatusException, DBInitException, SQLException {
        return stop(true);
    }

    /**
     * @return
     * @throws AndroidException.IllegalStatusException
     */
    public boolean stop(boolean runCallbacks) throws AndroidException.IllegalStatusException, DBInitException, SQLException {

        if (!(status == Status.Init || status == Status.Idle || status == Status.Busy || status == Status.Failed)) {
            // TODO New
            // Terminating, // 终止过程中
            // Terminated,  // 已终止
            // DeviceBroken // 不可用
            throw new AndroidException.IllegalStatusException();
        }

        this.status = Status.Terminating;

        boolean stopSuccess = false;

        Future<Boolean> closeFuture = executor.submit(new Stop(this));

        try {

            stopSuccess = closeFuture.get(CLOSE_TIMEOUT, TimeUnit.MILLISECONDS);

            if (stopSuccess) {
                status = Status.Terminated;
                logger.info("[{}] stop done", udid);

                if (runCallbacks) runCallbacks(terminatedCallbacks);
            } else {
                logger.info("[{}] stop failed", udid);
                status = Status.Failed;
            }
        } catch (InterruptedException e) {

            status = Status.Failed;
            logger.error("[{}] stop interrupted, ", udid, e);

        } catch (ExecutionException e) {

            status = Status.Failed;
            logger.error("[{}] stop failed, ", udid, e.getCause());

        } catch (TimeoutException e) {

            closeFuture.cancel(true);
            status = Status.Failed;
            logger.error("[{}] stop timeout, ", udid, e);

        } finally {

            this.update();
        }

        return stopSuccess;
    }

    /**
     * @throws DBInitException
     * @throws SQLException
     */
    public boolean clear() throws DBInitException, SQLException, AndroidException.IllegalStatusException {

        boolean clearSuccess = false;

        Future<Boolean> feature = executor.submit(new Clear(this));

        try {

            clearSuccess = feature.get(CLOSE_TIMEOUT, TimeUnit.MILLISECONDS);
            if (clearSuccess) {
                logger.info("[{}] clear done", udid);
            } else {
                logger.info("[{}] clear failed", udid);
            }


        } catch (InterruptedException e) {

            status = Status.Failed;
            logger.error("[{}] clear interrupted, ", udid, e);
            stop();

        } catch (ExecutionException e) {

            status = Status.Failed;
            logger.error("[{}] clear failed, ", udid, e.getCause());
            stop();

        } catch (TimeoutException e) {

            feature.cancel(true);
            status = Status.Failed;
            logger.error("[{}] clear timeout, ", udid, e);
            stop();

        } finally {
            this.update();
        }

        return clearSuccess;
    }

    /**
     * 设备重启
     *
     * @throws DBInitException
     * @throws SQLException
     * @throws AndroidException.IllegalStatusException
     */
    public boolean reboot() throws DBInitException, SQLException, AndroidException.IllegalStatusException {

        // 停止本地服务
        if (!stop(false)) return false;

        boolean rebootSuccess = false;

        Future<Boolean> feature = executor.submit(new Reboot(this));

        try {

            rebootSuccess = feature.get(CLOSE_TIMEOUT, TimeUnit.MILLISECONDS);
            if (rebootSuccess) {
                logger.info("[{}] reboot done", udid);
                status = Status.New;
            } else {
                logger.info("[{}] reboot failed", udid);
                status = Status.Failed;
            }

        } catch (InterruptedException e) {

            status = Status.Failed;
            logger.error("[{}] reboot interrupted, ", udid, e);

        } catch (ExecutionException e) {

            status = Status.Failed;
            logger.error("[{}] reboot failed, ", udid, e.getCause());

        } catch (TimeoutException e) {

            feature.cancel(true);
            status = Status.Failed;
            logger.error("[{}] reboot failed, ", udid, e);

        } finally {
            this.update();
        }

        // 重启本地服务
        if (rebootSuccess) return start();

        return rebootSuccess;
    }

    /**
     * 重启
     * TODO 是否可以通过添加Terminated回调来实现？
     * 场景: 设备运行时间过程, 没有响应
     */
    public void restart() throws AndroidException.IllegalStatusException, DBInitException, SQLException {
        stop();
        clear();
        start();
        this.flags.add(Flag.NewRestart);
    }

    /**
     * @param task
     * @throws AndroidException.IllegalStatusException
     */
    public void submit(Task task) throws
            AndroidException.IllegalStatusException,
            DBInitException,
            SQLException,
            AndroidException.NoSuitableAdapter,
            InterruptedException,
            AndroidException.NoAvailableDevice,
            TaskException.IllegalParameters,
            AccountException.AccountNotLoad {

        if (!(status == Status.Idle)) {
            throw new AndroidException.IllegalStatusException();
        }

        this.status = Status.Busy;

        Adapter adapter = adapters.get(task.h.adapter_class_name);

        if (adapter == null) {
            throw new AndroidException.NoSuitableAdapter();
        }

        task.setAdapter(adapter); // 由于异常处理 task.adapter 有可能被 device 移除

        boolean retry = false;

        try {
            //  任务提交线程池
            taskFuture = executor.submit(task);

            retry = taskFuture.get(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);

            // 正常执行任务后，任务对应的Adapter的异常记录重置
            task.getAdapter().exceptions.clear();

            // 正常执行任务后，设备的异常标签重置
            flags.clear();

            status = Status.Idle;
        }
        // E1 线程中断异常 TODO 基本上是 executor 被异常终止才会报该异常
        catch (InterruptedException e) {

            task.failure(e.getCause());
            status = Status.Failed;
            retry = false;
        }
        // E2 任务被外部终止 taskFuture.cancel() 产生 状态设为Idle
        catch (CancellationException e) {

            task.failure(e.getCause());
            status = Status.Idle;
            retry = false;
        }
        // E3 基本不会捕获
        catch (TimeoutException e) {

            task.failure(e.getCause());
            taskFuture.cancel(true);
            status = Status.Idle;
            retry = false;
        }
        // E4 执行异常
        catch (ExecutionException e) {

            // 统一预设置重试
            retry = true;

            try {

                // E5 账号Broken 尝试切换帐号
                if (e.getCause() instanceof AccountException.Broken) {

                    task.failure(e.getCause());

                    task.getAdapter().switchAccount();

                    status = Status.Idle;
                }
                // E6 Adapter 操作定义异常
                else if (e.getCause() instanceof AdapterException.OperationException || e.getCause() instanceof AdapterException.IllegalStateException) {

                    task.failure(e.getCause());

                    // 上一次执行 也抛出相同异常
                    if (task.getAdapter().exceptions.containsKey(e.getCause().getClass().getName())) {
                        removeAdapter(task.getAdapter());
                    } else {
                        task.getAdapter().exceptions.put(e.getCause().getClass().getName(), 1);
                        // 重置Adapter状态
                        task.getAdapter().restart();
                    }

                    status = Status.Idle;
                }
                // E7 App没有响应异常
                else if (e.getCause() instanceof AdapterException.NoResponseException) {

                    task.failure(e.getCause());

                    // 上一次执行 也遇到了相同异常
                    if (task.getAdapter().exceptions.containsKey(e.getCause().getClass().getName())) {

                        // 第4次 设备不可用
                        if (flags.contains(Flag.NewReboot)) {

                            status = Status.Failed;
                        }
                        // 第3次 重启设备
                        else if (flags.contains(Flag.Cleaned)) {

                            this.reboot();
                        }
                        // 第2次清空缓存 重启app
                        else {

                            this.clear();
                            task.getAdapter().restart();
                            status = Status.Idle;
                        }

                    }
                    // 第一次 app重启
                    else {

                        task.getAdapter().exceptions.put(e.getCause().getClass().getName(), 1);

                        // 重置Adapter状态
                        task.getAdapter().restart();
                        status = Status.Idle;
                    }

                } else {
                    throw e.getCause();
                }
            }
            // 代理问题 TODO 出现场景需确认
            catch (SocketException ex) {
                task.failure(ex);
                status = Status.Failed;
            }
            // 脚本异常 / 操作异常
            // 异常参考 http://www.softwaretestingstudio.com/common-exceptions-selenium-webdriver/
            catch (ElementNotVisibleException |
                    NoAlertPresentException |
                    StaleElementReferenceException |
                    ElementNotSelectableException |
                    NoSuchFrameException |
                    org.openqa.selenium.NoSuchElementException |
                    ElementClickInterceptedException ex) {

                task.failure(ex);

                // 当前任务失败 连续出现三次
                // 上一次执行 也遇到了相同异常
                if (task.getAdapter().exceptions.containsKey(ex.getClass().getName())) {

                    int count = task.getAdapter().exceptions.get(ex.getClass().getName());
                    if (count < 3) {
                        task.getAdapter().exceptions.put(ex.getClass().getName(), count + 1);
                    }

                    removeAdapter(task.getAdapter());
                } else {
                    task.getAdapter().exceptions.put(ex.getClass().getName(), 1);
                }

                status = Status.Idle;

            }
            // WebDriver 命令超时问题 网络连接问题
            catch (org.openqa.selenium.TimeoutException | // 操作超时异常
                    NoSuchWindowException |               // 找不到特定页面异常
                    UnreachableBrowserException |         //
                    SessionNotCreatedException |          // 无法创建Session异常
                    NoSuchSessionException |              // 无法识别的Session
                    ErrorHandler.UnknownServerException ex) {  // 未知服务端异常

                task.failure(ex);

                if (flags.contains(Flag.NewRestart)) {
                    status = Status.Failed;
                } else {
                    restart();
                }
            }

            // 其他 WebDriver 异常 无法正常调用WebDriver --> 关闭
            catch (WebDriverException ex) {

                task.failure(ex);

                if (flags.contains(Flag.NewRestart)) {
                    status = Status.Failed;
                } else {
                    restart();
                }
            }
            // 无可用账号异常 --> 对应的Adapter不可用
            catch (AccountException.NoAvailableAccount ex) {

                task.failure(ex);
                removeAdapter(task.getAdapter());
                status = Status.Idle;
            }
            // 当前使用的Adapter对应的Account失效 --> 对应的Adapter需要切换账号
            catch (AdapterException.LoginScriptError ex) {

                task.failure(ex);
                retry = false;
                removeAdapter(task.getAdapter());
                status = Status.Idle;

            }
            // 其他未知异常
            catch (Throwable throwable) {
                task.failure(throwable);
                removeAdapter(task.getAdapter());
                status = Status.Failed;
            }

        } finally {

            this.update();

            // 任务执行成功回调
            if (task.h.success) {
                task.successCallbacks.forEach(c -> c.call(task));
                task.doneCallbacks.forEach(c -> c.call(task));
            }
            // 任务执行失败
            else {

                // 需要重试 且重试次数小于重试上线
                if (retry && task.getRetryCount() < TASK_RETRY_LIMIT) {
                    task.addRetryCount();
                    AndroidDeviceManager.getInstance().submit(task);
                }
                // 任务执行失败回调
                else {
                    task.failureCallbacks.forEach(c -> c.call(task));
                    task.doneCallbacks.forEach(c -> c.call(task));
                }
            }

            // 执行空闲回调
            if (status == Status.Idle) {
                runCallbacks(idleCallbacks);
            }

            if (status == Status.Failed) {
                stop();
            }

            taskFuture = null;
        }
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
        /*bmProxy.addFirstHttpFilterFactory(new RequestFilterAdapter.FilterSource(filter, 16777216));*/
        bmProxy.replaceFirstHttpFilterFactory(new RequestFilterAdapter.FilterSource(filter, 16777216));
    }

    /**
     * 设置代理返回过滤器
     *
     * @param filter
     */
    public void setProxyResponseFilter(ResponseFilter filter) {
        if (bmProxy == null) return;
        /*bmProxy.addResponseFilter(filter);*/
        bmProxy.replaceLastHttpFilter(filter);
    }

    /**
     * 停止代理
     */
    public void stopProxy() {
        Optional.ofNullable(bmProxy).ifPresent(BrowserMobProxy::stop);
    }

    /**
     * 初始化AppiumDriver
     * TODO 是否可以不调用此方法，启动其他App
     *
     * @throws Exception
     */
    public void initAppiumServiceAndDriver(Adapter.AppInfo appInfo) throws IOException, InterruptedException {

        // C 定义Driver Capabilities
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability("app", ""); // 必须设定 否则无法启动Driver
        capabilities.setCapability("appPackage", appInfo.appPackage);   // App包名 必须设定 否则无法启动Driver
        capabilities.setCapability("appActivity", appInfo.appActivity); // App启动Activity 必须设定 否则无法启动Driver
        //capabilities.setCapability("fastReset", false);
        capabilities.setCapability("fullReset", false);
        capabilities.setCapability("noReset", true);

        capabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, AutomationName.ANDROID_UIAUTOMATOR2);

        // TODO 下面两行代码如果不添加 是否不能进入小程序？
		/*String webViewAndroidProcessName = "com.tencent.mm:tools";
		webViewAndroidProcessName = "com.tencent.mm:appbrand0"; // App中的加载WebView的进程名
		capabilities.setCapability("chromeOptions", ImmutableMap.of("androidProcess", webViewAndroidProcessName));*/

        capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, udid);
        capabilities.setCapability(MobileCapabilityType.UDID, udid);

        // TODO 是否可以自动获取url?
        driver = new AndroidDriver(serviceUrl, capabilities);
        // 关闭日志打印
        driver.setLogLevel(Level.OFF);
        Thread.sleep(1000);

        // driver.startRecordingScreen();

        // 设置宽高
        this.width = getWidth();
        this.height = getHeight();
    }

    /**
     * 重启移动端appium
     *
     * @throws IOException
     */
    public void stopRemoteAppiumServer() throws IOException {

        logger.info("[{}] stop appium Server", udid);

        String command1 = "adb -s " + this.udid + " shell am force-stop io.appium.settings";
        Runtime.getRuntime().exec(command1);

        String command2 = "adb -s " + this.udid + " shell am force-stop io.appium.uiautomator2.server";
        Runtime.getRuntime().exec(command2);
    }

    /**
     * 唤醒设备
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void awake() throws IOException, InterruptedException {

        Runtime runtime = Runtime.getRuntime();

        AndroidUtil.clickPower(udid);
        Thread.sleep(2000);

        // 滑动解锁  728 2356  728 228  adb shell -s  input swipe  300 1500 300 200
        String unlockCommand = "adb -s " + udid + " shell input swipe  300 1500 300 200";
        runtime.exec(unlockCommand);
        Thread.sleep(2000);

        // 输入密码adb -s ZX1G42BX4R shell input text szqj  adb -s ZX1G42BX4R shell input swipe 300 1000 300 500
        String loginCommand = "adb -s " + udid + " shell input text " + PIN_PASSWORD;
        runtime.exec(loginCommand);
        Thread.sleep(4000);

        // 点击确认
        touch(1350, 2250, 6000);  //TODO 时间适当调整
        Thread.sleep(2000);
    }


    /**
     * @return
     * @throws IOException
     */
    public byte[] screenshot() {
        logger.info("Device[{}], Driver HashCode[{}], Driver[{}] ", this, this.driver.hashCode(), this.driver.toString());
        return driver.getScreenshotAs(OutputType.BYTES);
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
     * @param by
     * @param input
     */
    public void search(By by, String input) throws InterruptedException, IOException, AdapterException.NoResponseException {

        // 输入框输入关键词
        driver.findElement(by).sendKeys(input);

        Thread.sleep(2000);

        // 点击软键盘搜索
//		reliableTouch(1350, 2250, 4000L, 0);
        touch(1350, 2250, 4000);
    }

    /**
     * @param by
     * @param input
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    public List<OCRParser.TouchableTextArea> searchAndGetResult(By by, String input, int x1, int y1, int x2, int y2) throws InterruptedException, IOException, AdapterException.NoResponseException {

        search(by, input);

        List<OCRParser.TouchableTextArea> textAreas = OCRClient.getInstance().getTextBlockArea(screenshot(), x1, y1, x2, y2);

        return textAreas;
    }

    public List<AndroidElement> searchAndGetResult2(By by, String input, int x1, int y1, int x2, int y2) throws InterruptedException, IOException, AdapterException.NoResponseException {

        search(by, input);

        List<AndroidElement> els = new ArrayList<>();

        for (AndroidElement ae : driver.findElements(By.className("android.widget.TextView"))) {
            if (ae.getRect().x < x1 || ae.getRect().y < y1 || ae.getRect().x + ae.getRect().width > x2 || ae.getRect().y + ae.getRect().height > y2) {

            } else {
                els.add(ae);
            }
        }

        return els;
    }

    /**
     * @param areas
     * @param text
     * @return
     */
    public boolean checkContent(List<OCRParser.TouchableTextArea> areas, String... text) {
        for (OCRParser.TouchableTextArea area : areas) {
            for (String t : text) {
                if (area.content.contains(t)) return true;
            }
        }
        return false;
    }

    public boolean checkContent2(List<AndroidElement> els, String... text) {
        for (AndroidElement el : els) {
            for (String t : text) {
                if (el.getText().contains(t)) return true;
            }
        }
        return false;
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
            byte[] imageData1 = screenshot();

            // B 进行touch 操作
            touch(x, y, sleep);

            // C 截图2
            byte[] imageData2 = screenshot();

            // D 比较 两个截图相似性
            boolean similar = ImageUtil.isSame(ImageIO.read(new ByteArrayInputStream(imageData1)), ImageIO.read(new ByteArrayInputStream(imageData2)));

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


    public AndroidDevice addInitCallback(AndroidDeviceCallBack.InitCallBack callBack) {
        this.initCallbacks.add(callBack);
        return this;
    }

    public AndroidDevice addIdleCallback(AndroidDeviceCallBack callBack) {
        this.idleCallbacks.add(callBack);
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
    private void runCallbacks(List<AndroidDeviceCallBack> callbacks) {

        if (callbacks == null) return;

        ListenableFuture<Boolean> future = AndroidDeviceManager.getInstance().executorService.submit(() -> {

            for (AndroidDeviceCallBack callback : callbacks) {
                callback.call(this);
            }

            return true;
        });

        /*Callbacks run failed, one.rewind.android.automator.exception.AndroidException$IllegalStatusException: null*/
        Futures.addCallback(future, new FutureCallback<Boolean>() {

            public void onSuccess(Boolean result) {
                logger.info("Callbacks run success[{}]", result);
            }

            public void onFailure(Throwable thrown) {
                logger.error("Callbacks run failed, ", thrown);
            }

        }, AndroidDeviceManager.getInstance().executorService);

    }

    /**
     *
     */
    public static class JSONableFlagListPersister extends StringType {

        private static final JSONableFlagListPersister INSTANCE = new JSONableFlagListPersister();

        private JSONableFlagListPersister() {
            super(SqlType.STRING, new Class[]{List.class});
        }

        public static JSONableFlagListPersister getSingleton() {
            return INSTANCE;
        }

        public Object javaToSqlArg(FieldType fieldType, Object javaObject) {
            List list = (List) javaObject;
            return list != null ? JSON.toJson(list) : null;
        }

        public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {
            Type type = (new TypeToken<List<Flag>>() {
            }).getType();
            List<Flag> list = (List) JSON.fromJson((String) sqlArg, type);
            return sqlArg != null ? list : null;
        }
    }
}
