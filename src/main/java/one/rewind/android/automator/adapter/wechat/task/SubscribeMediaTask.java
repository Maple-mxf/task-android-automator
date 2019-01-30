package one.rewind.android.automator.adapter.wechat.task;

import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.adapter.wechat.exception.MediaException;
import one.rewind.android.automator.adapter.wechat.exception.SearchPublicAccountFrozenException;
import one.rewind.android.automator.adapter.wechat.model.WechatAccountMediaSubscribe;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.android.automator.task.Task;
import one.rewind.android.automator.task.TaskHolder;
import one.rewind.data.raw.model.Media;
import one.rewind.data.raw.model.Platform;
import one.rewind.db.Daos;
import one.rewind.txt.StringUtil;

import java.io.IOException;

/**
 * 订阅公众号
 *
 * @author scisaga@gmail.com
 */
public class SubscribeMediaTask extends Task {

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
    }

    @Override
    public Boolean call() throws InterruptedException, AdapterException.OperationException {

        boolean retry = false;

        try {

            // A 启动微信
            adapter.restart();

            // B 进入搜索页
            adapter.goToSearchPage();

            // C 点击公众号
            adapter.goToSearchPublicAccountPage();

            // D 输入公众号进行搜索
            adapter.searchPublicAccount(media_nick);

            // E 截图识别是否被限流了
            adapter.getPublicAccountList();

            // E 点击订阅  订阅完成之后返回到上一个页面
            WeChatAdapter.PublicAccountInfo pai = adapter.getPublicAccountInfo(media_nick, true);

            try {

                Media media = Daos.get(Media.class).queryBuilder().where().eq("nick", media_nick).queryForFirst();

                // 如果对应的media不存在
                if (media == null) {

                    media = GetMediaEssaysTask.parseMedia(pai);
                    media.insert();

                }
                // 加载media已经采集过的文章数据
                else {

                    media = GetMediaEssaysTask.parseMedia(pai);
                    media.update();
                }

                WechatAccountMediaSubscribe var = new WechatAccountMediaSubscribe();

                var.account_id = adapter.account.id;
                var.media_id = genId(media.nick);
                var.media_nick = media.nick;
                var.media_name = media.name;

                try {
                    var.insert();
                } catch (Exception exx) {
                }

            } catch (Exception ex) {
                logger.error("Error handling DB, ", ex);
            }
        }


        // 搜索公众号接口限流
        catch (SearchPublicAccountFrozenException e) {

            logger.error("Error enter Media[{}] history essay list page, Account:[{}], ", media_nick, adapter.account.id, e);

            try {
                // 更新账号状态
                this.adapter.account.status = Account.Status.Get_Public_Account_Essay_List_Frozen;
                this.adapter.account.update();

            } catch (Exception e1) {
                logger.error("Error update account status failure, ", e);
            }

            // 将当前任务提交 下一次在执行任务的时候
            retry = true;

        }
        // Adapter 状态异常
        catch (AdapterException.IllegalStateException e) {

            logger.error("AndroidDevice state error, ", e);
            retry = true;

            // 当前账号不可用(切换也出现问题)
        } catch (AccountException.Broken broken) {

            // 不需要进行重试  任务失败即可
            logger.error("AndroidDevice state error, ", broken);
        }
        // 订阅的媒体账号和指定的账号不相同
        catch (MediaException.NotEqual notEqual) {

            logger.error("Error account not equals, ", notEqual);

        } catch (IOException e) {

            logger.error("Error screen shot failure, ", e);
        }


        return retry;
    }

    public static String genId(String nick) {
        return StringUtil.MD5(platform.short_name + "-" + nick);
    }
}
