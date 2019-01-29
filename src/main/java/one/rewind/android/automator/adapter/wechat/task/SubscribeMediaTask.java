package one.rewind.android.automator.adapter.wechat.task;

import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.adapter.wechat.exception.MediaException;
import one.rewind.android.automator.adapter.wechat.exception.SearchPublicAccountFrozenException;
import one.rewind.android.automator.adapter.wechat.model.WechatAccountMediaSubscribe;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.android.automator.exception.AndroidException;
import one.rewind.android.automator.task.Task;
import one.rewind.android.automator.task.TaskHolder;
import one.rewind.data.raw.model.Media;
import one.rewind.data.raw.model.Platform;
import one.rewind.db.DaoManager;
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
        addSuccessCallback(t -> {

        });

        // 任务失败记录日志
        addFailureCallback(t -> logger.error("Error execute subscribe task failure, wechat media nick [{}] ", media_nick));
    }

    @Override
    public Boolean call() throws InterruptedException, AdapterException.OperationException {

        try {

            // A 启动微信
            adapter.restart();

            // B
            adapter.goToSearchPage();

            // C 点击公众号
            adapter.goToSearchPublicAccountPage();

            // D 输入公众号进行搜索
            adapter.searchPublicAccount(media_nick);

            // E 点击订阅  订阅完成之后返回到上一个页面
            WeChatAdapter.PublicAccountInfo pai = adapter.getPublicAccountInfo(media_nick, true);

            try {

                Media media = DaoManager.getDao(Media.class).queryBuilder().where().eq("nick", media_nick).queryForFirst();

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
        catch (SearchPublicAccountFrozenException e) {

        }
        // 无可用账号异常
        catch (AdapterException.IllegalStateException e) {

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
        // 订阅的媒体账号和指定的账号不相同
        catch (MediaException.NotEqual notEqual) {

            logger.error("Error account not equals, ", notEqual);

        }
        // 账号处于不可用状态
        catch (AccountException.Broken broken) {
            logger.error("Error account broken, ", broken);
        }
        return Boolean.TRUE;
    }

    public static String genId(String nick) {
        return StringUtil.MD5(platform.short_name + "-" + nick);
    }
}
