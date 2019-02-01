package one.rewind.android.automator.adapter.wechat.task;

import com.dw.ocr.client.OCRClient;
import com.dw.ocr.parser.OCRParser;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.adapter.wechat.exception.MediaException;
import one.rewind.android.automator.adapter.wechat.model.WechatAccountMediaSubscribe;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.android.automator.task.Task;
import one.rewind.android.automator.task.TaskHolder;
import one.rewind.data.raw.model.Media;
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
    }

    @Override
    public Boolean call() throws
            InterruptedException, // 任务中断
            IOException, //
            AccountException.NoAvailableAccount, // 没有可用账号
            AccountException.Broken, // 账号不可用
            AdapterException.LoginScriptError, // Adapter 逻辑出错
            AdapterException.IllegalStateException, // Adapter 状态有问题 多数情况下是 逻辑出错
            AdapterException.NoResponseException // App 没有响应
    {


        boolean retry = false;

        try {

            // 0 启动APP
            h.r("0 启动APP");
            adapter.restart();

            // A 进入已订阅公众号的列表页面params
            h.r("A 进入已订阅公众号的列表页面params");
            adapter.goToSubscribePublicAccountList();

            // 最后一页
            boolean atBottom = false;

            while (!atBottom) {


                // B 获取当前页截图
                h.r("B 获取当前页截图");
                List<OCRParser.TouchableTextArea> accountList = OCRClient.getInstance().getTextBlockArea(adapter.device.screenshot());

                // C 获取公众号信息
                for (OCRParser.TouchableTextArea area : accountList) {

                    // C1 最后一页判别
                    if (area.content.matches("\\d[个公众号]")) {
                        atBottom = true;
                        break;
                    }

                    // C2 获取订阅的公众号名称
                    String media_nick = area.content;

                    // C3 去重判别
                    if (mediaSet.contains(media_nick)) continue;
                    mediaSet.add(media_nick);

                    Media media = null;

                    try {
                        // C4 查找对应的Media
                        media = (Media) Media.getById(Media.class, SubscribeMediaTask.genId(media_nick));

                    } catch (Exception e) {
                        logger.error("Error get media, ", e);
                    }
                        //
                    if (media == null) {

                        // D1 进入公众号Home页
                        h.r("D1 进入公众号Home页");
                        this.adapter.goToSubscribedPublicAccountHome(area.left, area.top);

                        // D2 查看公众号的更多资料

                        h.r("D2 查看公众号的更多资料");
                        WeChatAdapter.PublicAccountInfo pai = this.adapter.getPublicAccountInfo(area.content, false);

                        media = GetMediaEssaysTask.parseMedia(pai);

                        try {
                            media.insert();
                        } catch (Exception e) {
                            logger.error("Error get media, ", e);
                        }

                        // D3 返回到原来的页面
                        h.r("D3 返回到原来的页面");
                        adapter.device.goBack();
                        adapter.device.goBack();

                        adapter.status = WeChatAdapter.Status.Subscribe_PublicAccount_List;

                    }

                    // 记录Account订阅的Media信息
                    h.r("D3 记录Account订阅的Media信息");
                    WechatAccountMediaSubscribe wams = new WechatAccountMediaSubscribe(this.adapter.account.id, media.id, media.name, media.nick);

                    try {
                        wams.insert();
                    } catch (Exception e) {
                        logger.error("Error get media, ", e);
                    }
                }

                // E 向下滑动一页
                this.adapter.device.slideToPoint(1000, 500, 1000, 2000, 1000);
            }
        }
        // 当前登录的账号和指定的账号  这个异常
        catch (MediaException.NotEqual ignore) {

            logger.error("Error media not equals, ", ignore);

        }

        return retry;
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
