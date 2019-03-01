package one.rewind.android.automator.adapter.wechat.task;

import one.rewind.android.automator.adapter.Adapter;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.android.automator.task.Task;
import one.rewind.android.automator.task.TaskHolder;
import one.rewind.db.RedissonAdapter;
import one.rewind.json.JSON;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.io.IOException;
import java.util.List;
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

        RC("检查加载的当前账号是否与登录的一直");
        adapter.checkAccount();

        RedissonClient client = RedissonAdapter.redisson;

        RC("启动微信");
        adapter.restart();

        /*RC("检查加载的当前账号是否与登录的一直");
        adapter.checkAccount();

        RC("启动微信");
        adapter.restart();*/

        RC("进入订阅号");
        adapter.goToSubscribeHomePage();

        RC("识别公众号最新发布的消息");
        List<Map<String, String>> realTimeMessage = adapter.getRealTimeMessage();
        RMap<String, String> var = client.getMap("realTimeMessage");
        
        RC("向redis发布任务");
        realTimeMessage.forEach(m -> {
            var.put("mediaNick", m.get("mediaNick"));
            var.put("pubStr", m.get("pubStr"));
        });

        System.out.println(JSON.toJson(var));

        RC("任务执行完成！");
        success();

        return Boolean.TRUE;
    }
}
