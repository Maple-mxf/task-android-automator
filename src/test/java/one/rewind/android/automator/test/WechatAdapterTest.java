package one.rewind.android.automator.test;

import com.google.common.collect.Sets;
import net.lightbody.bmp.filters.RequestFilter;
import net.lightbody.bmp.filters.ResponseFilter;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.adapter.DefaultWechatAdapter;
import one.rewind.android.automator.manager.DefaultDeviceManager;
import one.rewind.android.automator.model.Comments;
import one.rewind.android.automator.model.Essays;
import one.rewind.android.automator.util.*;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class WechatAdapterTest {

    String udid = "192.168.55.101:5555";
    int appiumPort = 47356;
    int localProxyPort = 48356;
    AndroidDevice device;
    DefaultWechatAdapter adapter;

    /**
     * 初始化设备
     *
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {

        device = new AndroidDevice(udid, appiumPort);

        //device.removeWifiProxy();
        device.startProxy(localProxyPort);
        device.setupWifiProxy();

        /**
         * TODO 请求过滤器
         */
        RequestFilter requestFilter = (request, contents, messageInfo) -> {

            String url = messageInfo.getOriginalUrl();

            if (url.contains("https://mp.weixin.qq.com/s"))
                System.out.println(" . " + url);

            return null;
        };

        Stack<String> content_stack = new Stack<>();
        Stack<String> stats_stack = new Stack<>();
        Stack<String> comments_stack = new Stack<>();

        /**
         * TODO 返回过滤器
         */
        ResponseFilter responseFilter = (response, contents, messageInfo) -> {

            String url = messageInfo.getOriginalUrl();

            if (contents != null && (contents.isText() || url.contains("https://mp.weixin.qq.com/s"))) {

                try {
                    // 正文
                    if (url.contains("https://mp.weixin.qq.com/s")) {
                        device.setClickEffect(true);
                        System.err.println(" : " + url);
                        content_stack.push(contents.getTextContents());
                    }
                    // 统计信息
                    else if (url.contains("getappmsgext")) {
                        device.setClickEffect(true);
                        System.err.println(" :: " + url);
                        stats_stack.push(contents.getTextContents());
                    }
                    // 评论信息
                    else if (url.contains("appmsg_comment?action=getcomment")) {
                        device.setClickEffect(true);
                        System.err.println(" ::: " + url);
                        comments_stack.push(contents.getTextContents());
                    }

                    if (content_stack.size() > 0) {
                        device.setClickEffect(true);
                        System.out.println("有内容了");
                        String content_src = content_stack.pop();
                        Essays we;
                        if (stats_stack.size() > 0) {
                            String stats_src = stats_stack.pop();
                            we = new Essays().parseContent(content_src).parseStat(stats_src);
                        } else {
                            we = new Essays().parseContent(content_src);
                            we.view_count = 0;
                            we.like_count = 0;
                        }
                        we.id = MD5Util.MD5Encode("WX" + we.media_name + we.title, "UTF-8");
                        we.insert_time = new Date();

                        we.update_time = new Date();

                        we.media_content = we.media_nick;
                        we.platform = "WX";
                        we.platform_id = 1;
                        we.fav_count = 0;
                        we.forward_count = 0;
                        we.insert();
                        if (comments_stack.size() > 0) {
                            String comments_src = comments_stack.pop();
                            List<Comments> comments_ = Comments.parseComments(we.src_id, comments_src);
                            comments_.stream().forEach(c -> {
                                try {
                                    c.insert();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };

        device.setProxyRequestFilter(requestFilter);
        device.setProxyResponseFilter(responseFilter);

        // 从AppInfo中选择需要启动的程序
//        AppInfo appInfo = AppInfo.get(AppInfo.Defaults.WeChat);

//        device.initAppiumServiceAndDriver(appInfo);

        adapter = new DefaultWechatAdapter(device);

        Thread.sleep(3000);
    }

    //先将公众号关注  再点击进去抓取文章

    @Test
    public void testGetOnePublicAccountsEssays() throws IOException, InterruptedException {
        adapter.digestionCrawler("安农大就创会", true);
    }

    @Test
    public void testGetOnePublicAccountsEssaysByHandlerException() throws IOException, InterruptedException {
        adapter.digestionCrawler("万金解盘", true);
    }

    @Test
    public void subscribe() throws Exception {
        adapter.subscribeMedia("时代光华");
    }

    @Test
    public void testActiveApp() throws InterruptedException {
        device.driver.closeApp();
        AndroidUtil.activeWechat(device);
//        AndroidUtil.enterEssay("Java技术栈", device.driver);
    }


    @Test
    public void testSubscribeAccount() throws Exception {
        Set<String> strings = Sets.newHashSet();
        DBUtil.obtainFullData(strings, 11, 20);
        for (String var : strings) {
            adapter.digestionSubscribe(var, true);
        }
    }


    @Test
    public void testCloseAPP() {

//		device.stop();
//		device.driver.close();

//		device.driver.manage().ime().deactivate();

//		device.driver.terminateApp(udid);

//		device.driver.closeApp();

//		Route postAccounts = PublicAccountsHandler.postAccounts;

    }


    @Test
    public void testAllotTask() throws InterruptedException {
        DefaultDeviceManager.originalAccounts.add("菜鸟教程");
        DefaultDeviceManager.originalAccounts.add("Java技术栈");
        DefaultDeviceManager manager = DefaultDeviceManager.getInstance();
//        manager.allotTask(DefaultDeviceManager.TaskType.SUBSCRIBE);

    }

    @Test
    public void testRemoveWifiProxy() {
        device.removeWifiProxy();
    }

    @Test
    public void testDeviceSleepAndNotify() throws IOException, InterruptedException {
        ShellUtil.clickPower(udid);
        ShellUtil.notifyDevice(udid, device.driver);
    }

    @Test
    public void testSendFile(){
        device.setupWifiProxy();
    }
}
