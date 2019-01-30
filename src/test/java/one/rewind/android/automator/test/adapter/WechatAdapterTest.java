package one.rewind.android.automator.test.adapter;

import net.lightbody.bmp.filters.RequestFilter;
import net.lightbody.bmp.filters.ResponseFilter;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.AndroidDeviceManager;
import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.adapter.Adapter;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import org.junit.Test;

import java.io.IOException;
import java.util.Stack;
import java.util.concurrent.*;

/**
 * @author maxuefeng[m17793873123@163.com]
 */
public class WechatAdapterTest {

    // Executor Queue
    private transient LinkedBlockingQueue queue = new LinkedBlockingQueue<Runnable>();

    private transient ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS, queue);


    //    String udid = "ZX1G323GNB";
    String udid = AndroidDeviceManager.getAvailableDeviceUdids()[0];
    AndroidDevice device;
    WeChatAdapter adapter;
/*
    @Before*/
    public void setup() throws Exception {

        device = new AndroidDevice(udid);

        //device.removeRemoteWifiProxy();
        device.startProxy();
        device.setupRemoteWifiProxy();

        RequestFilter requestFilter = (request, contents, messageInfo) -> null;
        Stack<String> content_stack = new Stack<>();
        Stack<String> stats_stack = new Stack<>();
        Stack<String> comments_stack = new Stack<>();

        ResponseFilter responseFilter = (response, contents, messageInfo) -> {

            String url = messageInfo.getOriginalUrl();

            if (contents != null && (contents.isText() || url.contains("https://mp.weixin.qq.com/s"))) {

                try {
                    // 正文
                    if (url.contains("https://mp.weixin.qq.com/s")) {
//                        device.setTouchResponse(true);
                        System.err.println(" : " + url);
                        content_stack.push(contents.getTextContents());
                    }
                    // 统计信息
                    else if (url.contains("getappmsgext")) {
//                        device.setTouchResponse(true);
                        System.err.println(" :: " + url);
                        stats_stack.push(contents.getTextContents());
                    }
                    // 评论信息
                    else if (url.contains("appmsg_comment?action=getcomment")) {
//                        device.setTouchResponse(true);
                        System.err.println(" ::: " + url);
                        comments_stack.push(contents.getTextContents());
                    }

                    if (content_stack.size() > 0) {
//                        device.setTouchResponse(true);
                        System.out.println("有内容了");
                        String content_src = content_stack.pop();
                     /*   Essays we;
                        if (stats_stack.size() > 0) {
                            String stats_src = stats_stack.pop();
                            we = new Essays().parseContent(content_src).parseStat(stats_src);
                        } else {}*/
                      /*  we.id = MD5Util.MD5Encode("WX" + we.media_name + we.title, "UTF-8");
                        we.insert_time = new Date();

                        we.update_time = new Date();

                        we.media_content = we.media_nick;
                        we.platform = "WX";
                        we.platform_id = 1;
                        we.fav_count = 0;
                        we.forward_count = 0;
                        we.insert();*/
                      /*  if (comments_stack.size() > 0) {
                            String comments_src = comments_stack.pop();
                            List<Comments> comments_ = Comments.parseComments(we.src_id, comments_src);
                            comments_.stream().forEach(c -> {
                                try {
                                    c.insert();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                        }*/
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };

        device.setProxyRequestFilter(requestFilter);
        device.setProxyResponseFilter(responseFilter);

        // 从AppInfo中选择需要启动的程序
        Adapter.AppInfo appInfo = new Adapter.AppInfo("com.tencent.mm", ".ui.LauncherUI");

        device.initAppiumServiceAndDriver(appInfo);

        Account account = new Account();
        account.src_id = "SZQJ_001";
        account.username = "李楠";

        // TODO   给Account进行赋值
        adapter = new WeChatAdapter(device, account);

        Thread.sleep(3000);
    }

    /**
     * 测试安卓自动化操作
     *
     * @throws InterruptedException
     */
    @Test
    public void testAppium() throws InterruptedException {
        WeChatAdapter.UserInfo localUserInfo = adapter.getLocalUserInfo();

        System.out.println(localUserInfo.id);
        System.out.println(localUserInfo.name);

    }

    class InnerTask implements Callable<Void> {

        @Override
        public Void call() throws Exception {
            while (true) {
                System.out.println("hhhh");
                Thread.sleep(500);
            }
        }
    }

    class Task implements Callable<Void> {

        Future<Void> future = null;

        @Override
        public Void call() throws Exception {
            try {
                future = executor.submit(new InnerTask());
                System.err.println("AAA");
                Object o = future.get();
                System.err.println("BBB");
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @Test
    public void test1() throws IOException, InterruptedException {

        // Thread.sleep(10000);

        try {
            Future<Void> future = null;
            Task task = new Task();
            try {

                future = executor.submit(task);
                Thread.sleep(5000);
                task.future.cancel(true);
                // future.get(5000, TimeUnit.MILLISECONDS);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        Thread.sleep(10000);

    }
}
