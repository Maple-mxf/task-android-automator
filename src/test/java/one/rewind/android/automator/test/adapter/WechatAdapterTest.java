package one.rewind.android.automator.test.adapter;

import net.lightbody.bmp.filters.RequestFilter;
import net.lightbody.bmp.filters.ResponseFilter;
import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.AndroidDeviceManager;
import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.adapter.Adapter;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import org.junit.Before;
import org.junit.Test;
import se.vidstige.jadb.JadbException;

import java.io.IOException;
import java.util.Stack;

/**
 * @author maxuefeng[m17793873123@163.com]
 */
public class WechatAdapterTest {

    //    String udid = "ZX1G323GNB";
    String udid = AndroidDeviceManager.getAvailableDeviceUdids()[0];
    AndroidDevice device;
    WeChatAdapter adapter;

    @Before
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

    //先将公众号关注  再点击进去抓取文章

   /* @Test
    public void testGetOnePublicAccountsEssays() {
        adapter.digestionCrawler("阿里巴巴", true);
    }

    @Test
    public void testGetOnePublicAccountsEssaysByHandlerException() {
        adapter.digestionCrawler("成安邮政", true);
    }*/

   /* @Test
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
    }*/

   /* @Test
    public void testActiveApp() throws InterruptedException {
        device.driver.closeApp();
        DeviceUtil.activeWechat(device);
    }
*/

   /* @Test
    public void testSubscribeAccount() throws Exception {
        adapter.digestionSubscribe("芋道源码");
//        adapter.digestionCrawler("阿里巴巴", true);
    }*/


    @Test
    public void testCloseAPP() {

//		device.stop();
//		device.driver.close();

//		device.driver.manage().ime().deactivate();

//		device.driver.terminateApp(udid);

//		device.driver.stopApp();

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
    public void testRemoveWifiProxy() throws InterruptedException, IOException, JadbException {
        device.removeRemoteWifiProxy();
    }

    /*@Test
    public void testDeviceSleepAndNotify() throws IOException, InterruptedException {
        ShellUtil.clickPower(udid);
        ShellUtil.notifyDevice(udid, device.driver);
    }*/

    @Test
    public void testSendFile() throws InterruptedException, IOException, JadbException {
        device.setupRemoteWifiProxy();
    }

   /* @Test
    public void testUnsubscribeMedia() throws SQLException {
        Calendar instance = Calendar.getInstance();
        instance.set(Calendar.HOUR_OF_DAY, 0);
        instance.set(Calendar.MINUTE, 0);
        instance.set(Calendar.SECOND, 0);
        Date time = instance.getTime();
        List<WechatAccountMediaSubscribe> query = Tab.subscribeDao.queryBuilder().where().eq("udid", udid).and().ge("insert_time", time).query();
        for (WechatAccountMediaSubscribe media : query) {
            adapter.unsubscribeMedia(media.media_name);
        }
    }
*/

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

}
