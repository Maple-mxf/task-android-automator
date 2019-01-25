package one.rewind.android.automator.adapter.wechat.task;

import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.android.automator.task.Task;
import one.rewind.android.automator.task.TaskHolder;
import one.rewind.data.raw.model.Platform;
import one.rewind.txt.StringUtil;

import java.io.IOException;

/**
 * 订阅公众号
 *
 * @author scisaga@gmail.com
 */
public class SubscribeMediaTask extends Task {

    // 点击无响应重试上限
    public static final int MAX_ATTEMPTS = 5;

    public static Platform platform;

    public WeChatAdapter adapter;

    public String media_nick;


    static {
        try {
            platform = new Platform("微信公众号平台", "WX");
            platform.id = 1;
            platform.insert();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SubscribeMediaTask(TaskHolder holder, String... params) throws IllegalParamsException {
        super(holder, params);

        // 任务执行成功将账号与公众号的关系数据插入数据库
        this.doneCallbacks.add(new Thread(() -> {

        }));

        // 任务失败记录日志
        this.failureCallbacks.add(new Thread(() -> {

        }));
    }

    @Override
    public Boolean call() throws InterruptedException, IOException, AdapterException.OperationException {

        try {
            // A 启动微信
            adapter.start();

            // B 点击搜索
            adapter.goToSearchPage();

            // C 点击公众号
            adapter.goToSearchPublicAccountPage();

            // D 输入公众号进行搜索
            adapter.goToSearchNoSubscribePublicAccountResult(media_nick);

            // F 进入公众号首页
            adapter.goToNoSubscribePublicAccountHome();

            // G 点击订阅
            adapter.subscribePublicAccount();

            // 任务执行成功回调
            runCallbacks(doneCallbacks);

            // 无可用账号异常
        } catch (AccountException.NoAvailableAccount noAvailableAccount) {

            logger.error("Error no available account! cause[{}]", noAvailableAccount);

            // Adapter状态异常
        } catch (AdapterException.IllegalStateException e) {

            logger.error("AndroidDevice state error! cause[{}]", e);

        }
        return Boolean.TRUE;
    }

    public static String genId(String nick) {
        return StringUtil.MD5(platform.short_name + "-" + nick);
    }
}
