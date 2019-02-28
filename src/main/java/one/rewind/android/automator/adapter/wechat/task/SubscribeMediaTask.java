package one.rewind.android.automator.adapter.wechat.task;

import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.adapter.Adapter;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.adapter.wechat.exception.MediaException;
import one.rewind.android.automator.adapter.wechat.exception.SearchPublicAccountFrozenException;
import one.rewind.android.automator.adapter.wechat.model.WechatAccountMediaSubscribe;
import one.rewind.android.automator.adapter.wechat.util.PublicAccountInfo;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.android.automator.task.Task;
import one.rewind.android.automator.task.TaskHolder;
import one.rewind.data.raw.model.Media;
import one.rewind.db.RedissonAdapter;
import one.rewind.db.exception.DBInitException;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 订阅公众号
 *
 * @author scisaga@gmail.com
 */
public class SubscribeMediaTask extends Task {

    public WeChatAdapter adapter;

    public String media_nick;

    public SubscribeMediaTask(TaskHolder holder, String... params) throws IllegalParamsException {

        super(holder, params);

        // A 参数判断 获取需要采集的公众号昵称
        if (params.length == 1) {
            media_nick = params[0];
        } else throw new IllegalParamsException(Arrays.stream(params).collect(Collectors.joining(", ")));

        accountPermitStatuses.add(Account.Status.Get_Public_Account_Essay_List_Frozen);
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

            RC("确认帐号状态");
            checkAccountStatus(adapter);

            RC("启动微信");
            adapter.restart();

            RC("进入搜索页");
            adapter.goToSearchPage();

            RC("点击公众号选项");
            adapter.goToSearchPublicAccountPage();

            RC("搜索 " + media_nick);
            adapter.searchPublicAccount(media_nick);

            RC("点击订阅");
            PublicAccountInfo pai = adapter.getPublicAccountInfo(true, true);

            try {

                Media media = GetMediaEssaysTask.parseMedia(pai);
                media.update();
                WechatAccountMediaSubscribe subscribeRecord = new WechatAccountMediaSubscribe();
                subscribeRecord.account_id = adapter.account.id;
                subscribeRecord.media_id = media.id;
                subscribeRecord.media_nick = media.nick;
                subscribeRecord.media_name = media.name;
                subscribeRecord.insert();

            } catch (SQLException ex) {
                logger.error("Error handling DB, ", ex);
            }

            RC("任务完成");
            success();

            RC("移除media");
            removeMedia();

            return true;
        }
        // 搜索公众号接口限流
        catch (SearchPublicAccountFrozenException e) {

            failure(e);

            // 更新账号状态
            this.adapter.account.status = Account.Status.Get_Public_Account_Essay_List_Frozen;
            this.adapter.account.update();

            // 需要重试
            return false;

        }
        // 搜索到的第一个公众号名称 与 搜索词不一致
        catch (MediaException.NotEqual e) {

            failure(e, "expect:" + e.media_nick_expected + " actual:" + e.media_nick);
            return false;
        }
        // 搜索到的第一个公众号状态异常
        catch (MediaException.Illegal e) {

            failure(e, e.media_nick + " illegal");
            return false;
        } catch (MediaException.NotFound e) {
            failure(e, e.media_nick + " not found");
            return false;
        } catch (Exception e) {
            failure(e, "Unknown exception");
            return false;
        }
    }

    public void removeMedia() {
        RedissonClient redissonClient = RedissonAdapter.redisson;

        RQueue<Object> queue = redissonClient.getQueue(this.adapter.device.udid + "-" + this.adapter.account.id);

        queue.remove(this.media_nick);
    }


}
