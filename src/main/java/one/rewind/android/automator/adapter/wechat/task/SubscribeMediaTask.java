package one.rewind.android.automator.adapter.wechat.task;

import com.j256.ormlite.dao.Dao;
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
import one.rewind.data.raw.model.Platform;
import one.rewind.db.Daos;
import one.rewind.db.exception.DBInitException;
import one.rewind.txt.StringUtil;

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

    public static Platform platform;

    public WeChatAdapter adapter;

    public String media_nick;

    public boolean subscribe = true;

    static {
        try {
            Dao<Platform, String> platformDao = Daos.get(Platform.class);
            platform = new Platform("微信公众号平台", "WX");
            platform.id = 1;
            platformDao.createIfNotExists(platform);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SubscribeMediaTask(TaskHolder holder, String... params) throws IllegalParamsException {

        super(holder, params);

		// A 参数判断 获取需要采集的公众号昵称
		if(params.length == 1) {
			media_nick = params[0];
		} else if(params.length == 2) {
			media_nick = params[0];
			subscribe = false;
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
			DBInitException
    {
        try {

			RC("0A 确认帐号状态");
			checkAccountStatus(adapter);

			RC("0B 启动微信");
			adapter.restart();

			RC("1 进入搜索页");
            adapter.goToSearchPage();

            // C 点击公众号
			RC("2 点击公众号选项");
            adapter.goToSearchPublicAccountPage();

            // D 输入公众号进行搜索
			RC("3 搜索 " + media_nick);
            adapter.searchPublicAccount(media_nick);

            // E 点击订阅  订阅完成之后返回到上一个页面
			RC("4 " + (subscribe ? "点击订阅" : "取消订阅"));

			PublicAccountInfo pai = adapter.getPublicAccountInfo(subscribe);
			if (!pai.nick.equals(media_nick)) throw new MediaException.NotEqual(media_nick, pai.nick);

			if(!subscribe) {
				adapter.unsubscribePublicAccount();
			}

            try {

				Media media = GetMediaEssaysTask.parseMedia(pai);
				media.update();

            	if(subscribe) {

					WechatAccountMediaSubscribe subscribeRecord = new WechatAccountMediaSubscribe();
					subscribeRecord.account_id = adapter.account.id;
					subscribeRecord.media_id = media.id;
					subscribeRecord.media_nick = media.nick;
					subscribeRecord.media_name = media.name;
					subscribeRecord.insert();
				} else {

					WechatAccountMediaSubscribe.deleteByAccountIdMediaId(adapter.account.id, media.id);
				}

            } catch (SQLException ex) {
                logger.error("Error handling DB, ", ex);
            }

			RC("5 任务圆满完成");

			success();

			return false;
        }
        // 搜索公众号接口限流
        catch (SearchPublicAccountFrozenException e) {

        	logger.warn("[{}] [{}] search public account frozen, ", getAdapter().getInfo(), getInfo(), e);

			// 更新账号状态
			this.adapter.account.status = Account.Status.Get_Public_Account_Essay_List_Frozen;
			this.adapter.account.update();

			failure(e);

			// 需要重试
			return true;

        }
        // 搜索到的第一个公众号名称 与 搜索词不一致
        catch (MediaException.NotEqual e) {
			logger.warn("[{}] [{}] expect:{} actual:{} , ", getAdapter().getInfo(), getInfo(), e.media_nick_expected, e.media_nick, e);

			failure(e);

			return false;
		}
        // 搜索到的第一个公众号状态异常
        catch (MediaException.Illegal e) {

			logger.warn("[{}] [{}] public account:{} status illegal, ", getAdapter().getInfo(), getInfo(), e.media_nick, e);

			failure(e);

			return false;
		}
    }

    public static String genId(String nick) {
        return StringUtil.MD5(platform.short_name + "-" + nick);
    }
}
