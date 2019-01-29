package one.rewind.android.automator.adapter.wechat.task;

import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.adapter.wechat.model.WechatAccountMediaSubscribe;
import one.rewind.android.automator.callback.TaskCallback;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.android.automator.exception.AndroidException;
import one.rewind.android.automator.task.Task;
import one.rewind.android.automator.task.TaskHolder;
import one.rewind.data.raw.model.Platform;
import one.rewind.txt.StringUtil;

/**
 * 订阅公众号
 *
 * @author scisaga@gmail.com
 */
public class SubscribeMediaTask extends Task {

    public static Platform platform;

    public WeChatAdapter adapter;

    public String media_nick;

    public WeChatAdapter.PublicAccountInfo accountInfo;

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
        this.doneCallbacks.add((TaskCallback) t -> {

            try {
                WechatAccountMediaSubscribe var = new WechatAccountMediaSubscribe();

                var.account_id = adapter.account.id;
                var.media_nick = media_nick;
                var.media_id = WechatAccountMediaSubscribe.genMediaId(accountInfo.name, media_nick);
                var.media_name = accountInfo.name;

                var.insert();
            } catch (Exception e) {
                logger.error("Error save WechatAccountMediaSubscribe failure! cause [{}], media [{}],account [{}] ", e, media_nick, adapter.account.id);
            }
        });

        // 任务失败记录日志
        this.failureCallbacks.add((TaskCallback) t -> logger.error("Error execute subscribe task failure, wechat media nick [{}] ", media_nick));
    }

    @Override
    public Boolean call() throws InterruptedException, AdapterException.OperationException {

        try {
            // A 启动微信
            adapter.restart();

            // B 点击搜索
            adapter.goToSearchPage();

            // C 点击公众号
            adapter.goToSearchPublicAccountPage();

            // D 输入公众号进行搜索
            adapter.goToSearchNoSubscribePublicAccountResult(media_nick);

            // F 进入公众号首页
            adapter.goToNoSubscribePublicAccountHome();

            // G 点击订阅  订阅完成之后返回到上一个页面
            adapter.subscribePublicAccount();

            // H 获取当前公众号的详细信息
            accountInfo = adapter.getPublicAccountInfo(media_nick, false);

            // 任务执行成功回调
            runCallbacks(doneCallbacks);

            // 无可用账号异常
        } catch (AdapterException.IllegalStateException e) {

            logger.error("AndroidDevice state error! cause[{}]", e);

            // 当前账号不可用
        } catch (AccountException.Broken broken) {

            logger.error("AndroidDevice state error! cause[{}]", broken);

            try {
                // 更新账号状态
                this.adapter.account.status = Account.Status.Get_Public_Account_Essay_List_Frozen;
                this.adapter.account.update();

            } catch (Exception e1) {

                logger.error("Error update account status failure, cause [{}] ", e1);

            }

            // 将当前任务提交  下一次在执行任务的时候
            try {
                this.adapter.device.submit(this);
            } catch (AndroidException.IllegalStatusException ill) {
                logger.error("Error submit task failure, cause [{}]", ill);
            }
        }
        return Boolean.TRUE;
    }

    public static String genId(String nick) {
        return StringUtil.MD5(platform.short_name + "-" + nick);
    }
}
