package one.rewind.android.automator.adapter;

import com.dw.ocr.parser.OCRParser;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.android.automator.exception.AndroidException;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public abstract class Adapter {

    public static final Logger logger = LogManager.getLogger(Adapter.class.getName());

    public static boolean NeedAccount = false;

    public AndroidDevice device;

    public AppInfo appInfo;

    // 当前使用的账号
    public Account account;

    // 保存异常信息
    public List<Exception> exceptions = new ArrayList<>();

    public Adapter(AndroidDevice device) throws AndroidException.IllegalStatusException {
        this.device = device;
        device.addAdapter(this);
    }

    public Adapter(AndroidDevice device, Account account) throws AndroidException.IllegalStatusException {
        this.device = device;
        this.account = account;
        device.addAdapter(this);
    }

    public AndroidDevice getDevice() {
        return device;
    }

    public void setDevice(AndroidDevice device) {
        this.device = device;
    }

    /**
     * 启动应用
     */
    public void start() throws InterruptedException, AdapterException.OperationException, AccountException.Broken {
        device.startApp(appInfo);
    }

    /**
     *
     */
    public void restart() throws InterruptedException, AdapterException.OperationException, AccountException.Broken {
        device.stopApp(appInfo);
        device.startApp(appInfo);
    }

    public abstract void switchAccount(Account account) throws AdapterException.OperationException, AccountException.Broken;

    /**
     *
     */
    public static class AppInfo implements JSONable<AppInfo> {

        public String appPackage;
        public String appActivity;

        public AppInfo(String appPackage, String appActivity) {
            this.appPackage = appPackage;
            this.appActivity = appActivity;
        }

        @Override
        public String toJSON() {
            return JSON.toJson(this);
        }
    }
    /**
     * 由于默认的解析方法会把两行标题解析成两个文本框，此时需要根据顺序关系和坐标关系，对文本框进行合并
     *
     * @param originalTextAreas 初始解析的文本框列表
     * @param gap               文本框之间的最大距离
     * @return 合并后的文本框列表
     */
    public List<OCRParser.TouchableTextArea> mergeForTitle(List<OCRParser.TouchableTextArea> originalTextAreas, int gap) {

        List<OCRParser.TouchableTextArea> newTextAreas = new LinkedList<>();

        OCRParser.TouchableTextArea lastArea = null;

        // 遍历初始获得的TextArea
        for (OCRParser.TouchableTextArea area : originalTextAreas) {

            if (lastArea != null) {

                // 判断是否与之前的TextArea合并
                if (area.left == lastArea.left && (area.top - (lastArea.top + lastArea.height)) < gap) {
                    lastArea = lastArea.add(area);
                } else {
                    newTextAreas.add(area);
                    lastArea = area;
                }
            } else {
                newTextAreas.add(area);
                lastArea = area;
            }
        }
        return newTextAreas;
    }

}
