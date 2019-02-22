package one.rewind.android.automator.adapter.wechat.task;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import net.lightbody.bmp.filters.RequestFilter;
import net.lightbody.bmp.filters.ResponseFilter;
import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.adapter.Adapter;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.adapter.wechat.exception.MediaException;
import one.rewind.android.automator.adapter.wechat.util.EssayProcessor;
import one.rewind.android.automator.adapter.wechat.util.Generator;
import one.rewind.android.automator.adapter.wechat.util.PublicAccountInfo;
import one.rewind.android.automator.adapter.wechat.util.ReqObj;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.android.automator.task.Task;
import one.rewind.android.automator.task.TaskHolder;
import one.rewind.data.raw.model.Comment;
import one.rewind.data.raw.model.Essay;
import one.rewind.data.raw.model.Media;
import one.rewind.data.raw.model.Source;
import one.rewind.db.Daos;
import one.rewind.db.exception.DBInitException;
import one.rewind.db.model.Model;
import one.rewind.json.JSON;
import one.rewind.txt.DateFormatUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * 获取微信公众号文章
 * 构建一个Task需要可用的Device，可用的Adapter，可用的微信账号
 *
 * @author scisaga@gmail.com
 * @date 2019/1/14
 */
public class GetMediaEssaysTask1 extends Task {

    public String media_nick;

    // 任务对应的Adapter
    public WeChatAdapter adapter;

    public int page = Integer.MAX_VALUE;

    // 采集的公众号
    public Media media;

    // 公众号Essay源代码栈
    public Stack<String> content_stack = new Stack<>();

    // 公众号Essay统计信息源代码栈
    public Stack<String> stats_stack = new Stack<>();

    // 公众号Essay评论源代码栈
    public Stack<String> comments_stack = new Stack<>();

    // 已经保存过的微信公众号文章
    public List<String> collectedEssays = new ArrayList<>();

    public CountDownLatch countDown;

    public volatile boolean forward = false;

    private Map<String, ReqObj> reqs = new HashMap<>();

    private Map<String, Integer> responseCount = new HashMap<>();

    /**
     * 存储响应返回的Headers
     */
    private Map<String, ReqObj> responseInfo = new HashMap<>();

    private ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));

    /**
     * @param holder
     * @param params
     * @throws IllegalParamsException
     */
    public GetMediaEssaysTask1(TaskHolder holder, String... params) throws IllegalParamsException {

        super(holder, params);

        // A 参数判断 获取需要采集的公众号昵称
        if (params.length == 1) {
            media_nick = params[0];
        } else if (params.length == 2) {
            media_nick = params[0];
            try {
                page = Integer.parseInt(params[1]);
            } catch (Exception e) {
                throw new IllegalParamsException(Arrays.stream(params).collect(Collectors.joining(", ")));
            }
        } else throw new IllegalParamsException(Arrays.stream(params).collect(Collectors.joining(", ")));

        // B 初始化当前任务类型允许的账号状态
        accountPermitStatuses.add(Account.Status.Search_Public_Account_Frozen);

        // C 设定任务完成回调
        addDoneCallback((t) -> {
            // 移除过滤器
            ((GetMediaEssaysTask1) t).removeFilters();
        });
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
    public Boolean call() throws
            InterruptedException,
            IOException,
            AccountException.NoAvailableAccount,
            AccountException.Broken,
            AdapterException.IllegalStateException,
            AdapterException.NoResponseException,
            AdapterException.LoginScriptError,
            DBInitException,
            SQLException {

        // 任务执行
        try {

            RC("判断帐号状态");
            checkAccountStatus(adapter); // 有可能找不到符合条件的账号加载 并抛出NoAvailableAccount异常

            RC("设置过滤器");
            setupFilters();

            RC("重置微信进入首页");
            adapter.restart(); // 由于 checkAccountStatus步骤选择了有效账号，该步骤应该不会抛出Broken异常

            logger.info("Adapter[{}] ", this.adapter);

            RC("进入已订阅公众号的列表页面");
            adapter.goToSubscribePublicAccountList();

            RC("搜索公众号 " + media_nick);
            adapter.goToSubscribedPublicAccountHome(media_nick);

            RC("查询Media并加载已采集文章 " + media_nick);

            try {

                Media media = Daos.get(Media.class).queryBuilder().where().eq("nick", media_nick).queryForFirst();

                // 如果对应的media不存在
                if (media == null) {

                    media = parseMedia(adapter.getPublicAccountInfo(false, true));
                    media.insert();

                }
                // 加载media已经采集过的文章数据
                else {

                    Daos.get(Essay.class).queryBuilder()
                            .where().eq("media_id", media.id)
                            .query().forEach(essay -> collectedEssays.add(essay.title + " " + DateFormatUtil.dfd.print(essay.pubdate.getTime())));
                }

            } catch (SQLException e) {
                logger.error("Error handling DB, ", e);
            } catch (MediaException.Illegal e) {
                failure(e);
                return false;
            }

            RC("进入历史文章数据列表页");
            adapter.goToPublicAccountEssayList();

            RC("点击第一篇文章");
            this.adapter.device.touch(209, 1611, 8000);
            Thread.sleep(2000);

            RC("返回历史页面");
            this.adapter.device.goBack();
            Thread.sleep(2000);


            RC("向下拖动10次");
            for (int i = 0; i < 10; i++) {
                this.adapter.device.slideToPoint(1181, 2176, 1181, 865, 2000);
            }

            try {

                logger.info(JSON.toJson(responseInfo));

                ReqObj reqObj0 = responseInfo.get("EssayList-0");
                ReqObj reqObj1 = responseInfo.get("EssayList-1");
                ReqObj reqObj2 = responseInfo.get("EssayContent-0");

                if (reqObj0 == null || reqObj1 == null || reqObj2 == null) {
                    RC("任务失败");
                    return false;
                }

                EssayProcessor ep = new EssayProcessor("火山财富", reqObj0, reqObj1, reqObj2);

                RC("提交数据采集任务");
                ListenableFuture<?> future = service.submit(ep);

                // 阻塞等待任务执行
                future.get();

            } catch (Exception e) {
                e.printStackTrace();
            }


            RC("任务完成");
            success();
            return false;
        }
        // 在指定账号的订阅列表中找不到指定的公众号的异常
        catch (MediaException.NotSubscribe e) {

            failure(e, e.media_nick + " not subscribe");
            return false;

        }
        // 指定的媒体账号和订阅的账号不一致
        catch (MediaException.NotEqual e) {

            failure(e, "expect:" + e.media_nick_expected + " actual:" + e.media_nick);
            return false;

        } catch (MediaException.Illegal e) {
            failure(e, e.media_nick + " illegal");
            return false;
        }
    }

    public static String getFeature(String url) {

        Map<String, String> features = ImmutableMap.of(
                "https://mp.weixin.qq.com/mp/profile_ext?action=home", "EssayList",
                "https://mp.weixin.qq.com/mp/profile_ext?action=getmsg", "EssayList",
                "https://mp.weixin.qq.com/s", "EssayContent",
                "https://mp.weixin.qq.com/mp/getappmsgext", "EssayStat",
                "https://mp.weixin.qq.com/mp/appmsg_comment?action=getcomment", "EssayComments"
        );

        for (String feature : features.keySet()) {
            if (url.contains(feature)) return features.get(feature);
        }

        return null;
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return object -> seen.putIfAbsent(keyExtractor.apply(object), Boolean.TRUE) == null;
    }

    /**
     * 设置响应过滤器
     */
    public void setupFilters() {

        logger.info("[{}] [{}] Add Request/Response filters...", adapter.getInfo(), getInfo());

        RequestFilter requestFilter = (request, contents, messageInfo) -> {

            // 请求记录
            String url = messageInfo.getOriginalUrl();

            reqs.put(url, new ReqObj(url, request.method(), ImmutableMap.copyOf(request.headers().entries()), contents.getTextContents()));

            return null;
        };

        ResponseFilter responseFilter = (response, contents, messageInfo) -> {

            String url = messageInfo.getOriginalUrl();

            String feature = getFeature(url);

            // 返回内容记录
            if (contents != null && contents.isText() && feature != null) {

                if (reqs.get(url) != null) {

                    int count = responseCount.get(feature) == null ? 0 : responseCount.get(feature);

                    ReqObj resObj = reqs.get(url).setRes(
                            response.headers().entries().stream()
                                    .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, toList())))
                            , contents.getTextContents());

                    String prefix = feature + "-" + (count++);

//                    FileUtil.writeBytesToFile(resObj.toJSON().getBytes(), "tmp/wx/res/" + prefix + ".html");

                    if (prefix.equals("EssayList-0") || prefix.equals("EssayList-1") || prefix.equals("EssayContent-0")) {

                        /*if (prefix.equals("EssayList-1")) {
                            resObj.url = resObj.url.replace("count=10", "count=73").replace("offset=10", "offset=73");
                        }*/

                        responseInfo.put(prefix, resObj);
                    }

                    responseCount.put(feature, count);
                }
            }

            if (contents != null && (contents.isText() || url.contains("https://mp.weixin.qq.com/s"))) {

                // 正文
                if (url.contains("https://mp.weixin.qq.com/s")) {
                    logger.info(" : " + url);
                    content_stack.push(contents.getTextContents());
                }
                // 统计信息
                else if (url.contains("mp/getappmsgext")) {
                    logger.info(" :: " + url);
                    stats_stack.push(contents.getTextContents());
                }
                // 评论信息
                else if (url.contains("appmsg_comment?action=getcomment")) {
                    logger.info(" ::: " + url);
                    comments_stack.push(contents.getTextContents());
                }

                if (content_stack.size() > 0) {

                    String content_src = content_stack.pop();

                    String url_permanent = null;

                    // TODO 此处模拟共享，复制链接，保存文章持久连接
                    // adapter.device.driver.getClipboardText();

                    // 获取转发的Essay Id
                    String f_id = null/* = parseForwardId(content_src)*/;

                    // 判定是否是转发文章
                    if (f_id != null) {
                        forward = true;
                    }

                    Essay essay;

                    essay = EssayProcessor.parseContent(content_src, f_id, null);

                    try {
                        Source source = new Source(essay.id, url_permanent, null, essay.id + ".html", "text/xml", content_src.getBytes());
                        /*source.insert();*/
                    } catch (Exception e) {
                        logger.error("Error insert source:{}, ", essay.id, e);
                    }

                    essay.origin_url = url_permanent;

                    try {
                        if (stats_stack.size() > 0) {
                            String stats_src = stats_stack.pop();
                            essay = EssayProcessor.parseStat(essay, stats_src);
                        }
                    } catch (Exception e) {
                        logger.error("Error parse essay:{},", e);
                    }

                    try {
//                        essay.insert();
                    } catch (Exception e) {
                        logger.info("Error insert essay:{},", e);
                    }

                    // 对评论的处理
                    if (comments_stack.size() > 0) {

                        String comments_src = comments_stack.pop();

                        List<Comment> comments = null;

                        comments = EssayProcessor.parseComments(Comment.FType.Essay, essay.id, comments_src);

                        comments.stream().forEach(c -> {
                            try {
                                c.insert();
                            } catch (Exception e) {
                                logger.error("Error insert comments, {}", e.getMessage());
                            }
                        });
                    }


                    if (countDown != null) countDown.countDown();
                }
            }

        };

        adapter.device.setProxyRequestFilter(requestFilter);
        adapter.device.setProxyResponseFilter(responseFilter);
    }

    /**
     *
     */
    public void removeFilters() {

        logger.info("[{}] [{}] Remove Request/Response filters...", adapter.getInfo(), getInfo());

        adapter.device.setProxyRequestFilter((request, contents, messageInfo) -> null);

        adapter.device.setProxyResponseFilter((response, contents, messageInfo) -> {
        });
    }

    /**
     * @param pai
     * @return
     * @throws DBInitException
     * @throws SQLException
     */
    public static Media parseMedia(PublicAccountInfo pai) throws DBInitException, SQLException {

        Media media = Model.getById(Media.class, Generator.genMediaId(pai.nick));

        if (media == null) {
            media = new Media();
            media.id = Generator.genMediaId(pai.nick);
            media.insert();
        }

        media.name = pai.name;
        media.nick = pai.nick;
        media.content = pai.content;
        media.essay_count = pai.essay_count;
        media.subject = pai.subject;
        media.trademark = pai.trademark;
        media.phone = pai.phone;

        return media;
    }

}
