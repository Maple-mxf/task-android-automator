package one.rewind.android.automator.adapter.wechat.task;

import com.dw.ocr.parser.OCRParser;
import com.j256.ormlite.dao.Dao;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.adapter.wechat.model.AccountMediaSubscribe;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.android.automator.task.Task;
import one.rewind.android.automator.task.TaskHolder;
import one.rewind.db.DaoManager;
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

    /**
     * @param holder
     * @param params
     */
    public GetSelfSubscribeMediaTask(TaskHolder holder, String... params) throws IllegalParamsException {
        super(holder, params);

        // 任务完成回调
        this.doneCallbacks.add(new Thread(() -> {

            // 更新数据库
            try {
                Dao<AccountMediaSubscribe, String> dao = DaoManager.getDao(AccountMediaSubscribe.class);
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

    @Override
    public Boolean call() throws InterruptedException, IOException, AdapterException.OperationException {
        try {
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
                    mediaSet.add(area.content);
                }
            }

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
