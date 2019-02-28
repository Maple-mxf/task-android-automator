package one.rewind.android.automator.adapter.wechat.task;

import one.rewind.android.automator.adapter.Adapter;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.android.automator.task.Task;
import one.rewind.android.automator.task.TaskHolder;
import one.rewind.db.RedissonAdapter;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;

import java.io.IOException;
import java.util.Map;

/**
 * 查看微信公众号的最新文章
 *
 * @author maxuefeng [m17793873123@163.com]
 */
public class MonitorRealTimeTask extends Task {

    public WeChatAdapter adapter;

    /**
     * 构造方法
     *
     * @param h
     * @param params
     * @throws IllegalParamsException
     */
    public MonitorRealTimeTask(TaskHolder h, String... params) throws IllegalParamsException {
        super(h, params);
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
    public Boolean call() throws InterruptedException, AccountException.Broken, AdapterException.LoginScriptError, AdapterException.IllegalStateException, IOException {

        RedissonClient client = RedissonAdapter.redisson;

        RC("启动微信");
        adapter.restart();

        RC("确认是否在首页");
        if (!adapter.atHome()) {
            return false;
        }

        RC("进入订阅号");
        adapter.goToSubscribeHomePage();

        RC("识别公众号最新发布的消息");
        Map<String, Integer> realTimeMessage = adapter.getRealTimeMessage();
        RSet<Object> var = client.getSet("realTimeMessage");

        RC("向redis发布任务");
        var.add(realTimeMessage);

        RC("任务执行完成！");
        success();

        return Boolean.TRUE;
    }
}
