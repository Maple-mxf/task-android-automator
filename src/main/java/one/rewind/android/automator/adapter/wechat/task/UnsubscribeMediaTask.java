package one.rewind.android.automator.adapter.wechat.task;

import one.rewind.android.automator.adapter.Adapter;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.adapter.wechat.exception.MediaException;
import one.rewind.android.automator.adapter.wechat.model.WechatAccountMediaSubscribe;
import one.rewind.android.automator.adapter.wechat.util.Generator;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.android.automator.task.Task;
import one.rewind.android.automator.task.TaskHolder;
import one.rewind.db.exception.DBInitException;
import one.rewind.txt.StringUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author scisaga@gmail.com
 * @date 2019/2/10
 */
public class UnsubscribeMediaTask extends Task {

    public WeChatAdapter adapter;

    public String media_nick;

    public UnsubscribeMediaTask(TaskHolder holder, String... params) throws IllegalParamsException {

        super(holder, params);

        // A 参数判断 获取需要采集的公众号昵称
        if (params.length == 1) {
            media_nick = params[0];
        } else throw new IllegalParamsException(Arrays.stream(params).collect(Collectors.joining(", ")));
    }

    @Override
    public Task setAdapter(Adapter adapter) {
        this.adapter = (WeChatAdapter) adapter;
        return this;
    }

    @Override
    public Adapter getAdapter() {
        return this.adapter;
    }

    @Override
    public Boolean call() throws
            InterruptedException, // 任务中断
            IOException, //
            AccountException.NoAvailableAccount, // 没有可用账号
            AccountException.Broken, // 账号不可用
            AdapterException.LoginScriptError, // Adapter 逻辑出错
            AdapterException.IllegalStateException, // Adapter 状态有问题 多数情况下是 逻辑出错
            AdapterException.NoResponseException, // App 没有响应
            SQLException,
            DBInitException {
        try {

           /* RC("确认帐号状态");
            checkAccountStatus(adapter);*/

            RC("启动微信");
            adapter.restart();

            RC("进入已订阅公众号的列表页面");
            adapter.goToSubscribePublicAccountList();

            /*RC("进入公众号Home页");
            adapter.goToSubscribedPublicAccountHome(media_nick);

			RC("尝试取消订阅");
			adapter.unsubscribePublicAccount(media_nick);

			RC("删除Account订阅记录");
			WechatAccountMediaSubscribe.deleteByAccountIdMediaId(adapter.account.id, Generator.genMediaId(media_nick));*/

            RC("尝试取消订阅记录");
            adapter.unsubscribePublicAccount();

            RC("任务完成");

            success();

            return false;

        }
        // 搜索到的第一个公众号名称 与 搜索词不一致
        /*catch (MediaException.NotEqual e) {

            failure(e, "expect:" + e.media_nick_expected + " actual:" + e.media_nick);
            return false;
        }
        // 搜索到的第一个公众号状态异常
        catch (MediaException.Illegal e) {

            failure(e, e.media_nick + " illegal");
            return false;
        } */catch (MediaException.NotSubscribe e) {

            failure(e, media_nick + " not subscribe");
            return false;

        }
    }

    public static String genId(String nick) {
        return StringUtil.MD5(WeChatAdapter.platform.short_name + "-" + nick);
    }
}
