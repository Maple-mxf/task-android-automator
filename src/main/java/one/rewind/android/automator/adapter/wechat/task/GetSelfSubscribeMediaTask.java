package one.rewind.android.automator.adapter.wechat.task;

import com.dw.ocr.parser.OCRParser;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.adapter.wechat.exception.MediaException;
import one.rewind.android.automator.adapter.wechat.model.WechatAccountMediaSubscribe;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.android.automator.task.Task;
import one.rewind.android.automator.task.TaskHolder;
import one.rewind.txt.StringUtil;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 获取当前订阅的公众号列表
 *
 * @author scisaga@gmail.com
 * @date 2019/1/19
 */
public class GetSelfSubscribeMediaTask extends Task {

    // 任务对应的Adapter
    public WeChatAdapter adapter;

    // 当前账号对应的微信公众账号
    public Set<String> mediaSet = new HashSet<>();

    public Set<WechatAccountMediaSubscribe> accountMediaSubscribes = new HashSet<>();

    /**
     * @param holder
     * @param params
     */
    public GetSelfSubscribeMediaTask(TaskHolder holder, String... params) throws IllegalParamsException {
        super(holder, params);

        // 任务完成回调
        addDoneCallback((t) -> {

            // 更新数据库
            try {
                accountMediaSubscribes.forEach(v -> {
                    try {
                        v.insert();
                    } catch (Exception e) {
                        logger.error("Error insert accountMediaSubscribe failure, cause [{}]", e);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public Boolean call() throws InterruptedException {

        try {

            // 0 启动APP
            adapter.start();

            // A 进入已订阅公众号的列表页面params
            adapter.goToSubscribePublicAccountList();

            // 最后一页
            boolean atBottom = false;

            while (!atBottom) {
                // B 向下滑动一页
                this.adapter.device.slideToPoint(1000, 500, 1000, 2000, 1000);

                // C 获取当前页截图
                List<OCRParser.TouchableTextArea> accountList = this.adapter.getPublicAccountList();

                // D 添加到公众号集合中
                for (OCRParser.TouchableTextArea area : accountList) {

                    // 最后一页
                    if (area.content.matches("\\d[个公众号]")) {
                        atBottom = true;
                        break;
                    }

                    if (mediaSet.contains(area.content)) continue;
                    mediaSet.add(area.content);

                    // 进入公众号Home页
                    this.adapter.goToSubscribedPublicAccountHome(area.left, area.top);

                    // 查看公众号的更多资料
                    WeChatAdapter.PublicAccountInfo publicAccountInfo = this.adapter.getPublicAccountInfo(area.content, false);

                    // 缓存订阅关系的数据
                    String nick = publicAccountInfo.nick;
                    WechatAccountMediaSubscribe tmp = new WechatAccountMediaSubscribe(this.adapter.account.id, SubscribeMediaTask.genId(nick), publicAccountInfo.name, nick);
                    accountMediaSubscribes.add(tmp);

                    // 返回到原来的页面
                    this.adapter.goBackToPublicAccountListFromMoreInfo();
                }
            }


        }
        // 操作异常
        catch (AdapterException.OperationException e) {

            logger.error("Error update account status failure, ", e);

        }
        // Adapter状态异常
        catch (AdapterException.IllegalStateException e) {

            logger.error("Error update account status failure, ", e);

        }
        // 账号状态异常
        catch (AccountException.Broken broken) {

            logger.error("Error account is broken, ", broken);

        }
        // 当前登录的账号和指定的账号  这个异常
        catch (MediaException.NotEqual ignore) {

            logger.error(ignore);

        } catch (IOException e) {
            e.printStackTrace();
        }


        // 任务执行成功回调
        runCallbacks(doneCallbacks);

        return Boolean.TRUE;
    }

    /**
     * 媒体账号ID 生成
     *
     * @param media_nick
     * @param title
     * @param src_id
     * @return
     */
    public static String genId(String media_nick, String title, String src_id) {
        return StringUtil.MD5(SubscribeMediaTask.platform.short_name + "-" + media_nick + "-" + title + "-" + src_id);
    }
}
