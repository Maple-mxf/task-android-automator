package one.rewind.android.automator.adapter.wechat.task;

import com.dw.ocr.parser.OCRParser;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.android.automator.task.Task;
import one.rewind.android.automator.task.TaskHolder;

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
        }));
    }

    /**
     * @throws InterruptedException
     * @throws IOException
     * @throws AdapterException.OperationException
     */
    @Override
    public void execute() throws InterruptedException, IOException, AdapterException.OperationException {
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

            // 无可用账号异常
        } catch (AccountException.NoAvailableAccount noAvailableAccount) {
            logger.error("Error no available account! cause[{}]", noAvailableAccount);

            // Adapter状态异常
        } catch (AdapterException.IllegalStateException e) {
            logger.error("AndroidDevice state error! cause[{}]", e);
        }
    }
}
