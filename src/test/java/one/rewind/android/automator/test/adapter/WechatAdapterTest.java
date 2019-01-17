package one.rewind.android.automator.test.adapter;

import com.google.common.collect.Queues;
import io.netty.handler.codec.http.HttpHeaders;
import net.lightbody.bmp.filters.RequestFilter;
import net.lightbody.bmp.filters.ResponseFilter;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.model.Comments;
import one.rewind.android.automator.util.DeviceUtil;
import one.rewind.android.automator.util.Tab;
import one.rewind.android.automator.model.Essays;
import one.rewind.android.automator.model.SubscribeMedia;
import one.rewind.android.automator.util.AppInfo;
import one.rewind.android.automator.util.MD5Util;
import one.rewind.android.automator.util.ShellUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * @author maxuefeng[m17793873123@163.com]
 */
public class WechatAdapterTest {

    String udid = "ZX1G323GNB";
    int appiumPort = 47356;
    int localProxyPort = 48356;
    AndroidDevice device;
    WeChatAdapter adapter;

    /**
     * 初始化设备
     *
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {

        device = new AndroidDevice(udid);

        //device.removeRemoteWifiProxy();
        device.startProxy(localProxyPort);
        device.setupRemoteWifiProxy();

        /**
         * TODO 请求过滤器
         */
        RequestFilter requestFilter = (request, contents, messageInfo) -> {

            String url = messageInfo.getOriginalUrl();

            HttpHeaders headers = request.headers();

            List<Map.Entry<String, String>> entryList = headers.entries();

            System.out.println("Headers;Please Wait...");
            entryList.forEach(entry -> {
                System.out.println("key:" + entry.getKey());
                System.out.println("value:" + entry.getValue());
            });


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
                        device.setTouchResponse(true);
                        System.err.println(" : " + url);
                        content_stack.push(contents.getTextContents());
                    }
                    // 统计信息
                    else if (url.contains("getappmsgext")) {
                        device.setTouchResponse(true);
                        System.err.println(" :: " + url);
                        stats_stack.push(contents.getTextContents());
                    }
                    // 评论信息
                    else if (url.contains("appmsg_comment?action=getcomment")) {
                        device.setTouchResponse(true);
                        System.err.println(" ::: " + url);
                        comments_stack.push(contents.getTextContents());
                    }

                    if (content_stack.size() > 0) {
                        device.setTouchResponse(true);
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
        AppInfo appInfo = AppInfo.get(AppInfo.Defaults.WeChat);

        device.initAppiumServiceAndDriver(appInfo);

        adapter = new WeChatAdapter(device);

        Thread.sleep(3000);
    }

    //先将公众号关注  再点击进去抓取文章

    @Test
    public void testGetOnePublicAccountsEssays() {
        adapter.digestionCrawler("阿里巴巴", true);
    }

    @Test
    public void testGetOnePublicAccountsEssaysByHandlerException() {
        adapter.digestionCrawler("成安邮政", true);
    }

    @Test
    public void subscribe() {
        Queue<String> collections = Queues.newConcurrentLinkedQueue();

        collections.add("阿里巴巴");
        collections.add("今日头条");
        collections.add("蚂蚁金服");
        collections.add("抖音");
        collections.add("菜鸟网络");

        collections.forEach(v -> {
            try {
                adapter.subscribeMedia(v);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    public void testActiveApp() throws InterruptedException {
        device.driver.closeApp();
        DeviceUtil.activeWechat(device);
        //48356
//        DeviceUtil.enterEssay("Java技术栈", device.driver);
    }


    @Test
    public void testSubscribeAccount() throws Exception {
        adapter.digestionSubscribe("芋道源码");
//        adapter.digestionCrawler("阿里巴巴", true);
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
//        DefaultDeviceManager.originalAccounts.add("菜鸟教程");
//        DefaultDeviceManager.originalAccounts.add("Java技术栈");
//        DefaultDeviceManager manager = DefaultDeviceManager.getInstance();
//        manager.allotTask(DefaultDeviceManager.TaskType.SUBSCRIBE);

    }

    @Test
    public void testRemoveWifiProxy() {
        device.removeRemoteWifiProxy();
    }

    @Test
    public void testDeviceSleepAndNotify() throws IOException, InterruptedException {
        ShellUtil.clickPower(udid);
        ShellUtil.notifyDevice(udid, device.driver);
    }

    @Test
    public void testSendFile() {
        device.setupRemoteWifiProxy();
    }

    @Test
    public void testUnsubscribeMedia() throws SQLException {
        Calendar instance = Calendar.getInstance();
        instance.set(Calendar.HOUR_OF_DAY, 0);
        instance.set(Calendar.MINUTE, 0);
        instance.set(Calendar.SECOND, 0);
        Date time = instance.getTime();
        List<SubscribeMedia> query = Tab.subscribeDao.queryBuilder().where().eq("udid", udid).and().ge("insert_time", time).query();
        for (SubscribeMedia media : query) {
            adapter.unsubscribeMedia(media.media_name);
        }
    }


    @Test
    public void testRealMediaName() {
//        String var = WeChatAdapter.realMedia("芋道源码$req_HKDFNMADSFQWTEHFBVM7");
//
//        System.out.println(var);
    }

    @Test
    public void testRequestID() {
        // result： $req_HKDFNMADSFQWTEHFBVM7
//        String var = WeChatAdapter.requestID("芋道源码$req_HKDFNMADSFQWTEHFBVM7");

//        System.out.println(var);
    }

    @Test
    public void testGetEssays() {
        adapter.digestionCrawler("阿里巴巴", true);
    }

}
