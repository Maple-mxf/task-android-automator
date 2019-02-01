package one.rewind.android.automator.adapter.wechat.task;

import com.dw.ocr.parser.OCRParser;
import net.lightbody.bmp.filters.RequestFilter;
import net.lightbody.bmp.filters.ResponseFilter;
import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.adapter.wechat.exception.GetPublicAccountEssayListFrozenException;
import one.rewind.android.automator.adapter.wechat.exception.MediaException;
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
import one.rewind.io.requester.basic.BasicDistributor;
import one.rewind.txt.ContentCleaner;
import one.rewind.txt.DateFormatUtil;
import one.rewind.txt.NumberFormatUtil;
import one.rewind.txt.StringUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 获取微信公众号文章
 * 构建一个Task需要可用的Device，可用的Adapter，可用的微信账号
 *
 * @author scisaga@gmail.com
 * @date 2019/1/14
 */
public class GetMediaEssaysTask extends Task {

    public String media_nick;

    // 任务对应的Adapter
    public WeChatAdapter adapter;

    // 采集的公众号
    public Media media;

    // 公众号Essay源代码栈
    public Stack<String> content_stack = new Stack<>();

    // 公众号Essay统计信息源代码栈
    public Stack<String> stats_stack = new Stack<>();

    // 公众号Essay评论源代码栈
    public Stack<String> comments_stack = new Stack<>();

    // 已经访问过的微信公众号文章页面
    public List<String> visitedEssays = new ArrayList<>();

    // 已经保存过的微信公众号文章
    public List<String> collectedEssays = new ArrayList<>();

    public CountDownLatch countDown;

    public volatile boolean forward = false;

    /**
     * @param holder
     * @param params
     * @throws IllegalParamsException
     */
    public GetMediaEssaysTask(TaskHolder holder, String... params) throws IllegalParamsException {

        // 0
        super(holder, params);

        // A 参数判断 获取需要采集的公众号昵称
        if (params.length != 1)
            throw new IllegalParamsException(Arrays.stream(params).collect(Collectors.joining(", ")));

        media_nick = params[0];

        // B 初始化当前任务类型允许的账号状态
        accountPermitStatuses.add(Account.Status.Search_Public_Account_Frozen);

        // C 设定任务完成回调
        addDoneCallback((t) -> {
            // 移除过滤器
            ((GetMediaEssaysTask) t).removeFilters();
        });
    }

    @Override
    public Boolean call() throws IOException, AccountException.NoAvailableAccount, AccountException.Broken, AdapterException.NoResponseException, AdapterException.LoginScriptError, DBInitException, SQLException {

        boolean retry = false;

        // 任务执行
        try {
            // A1 判定Adapter加载的Account的状态，并尝试切换账号
            checkAccountStatus(); // 有可能找不到符合条件的账号加载 并抛出NoAvailableAccount异常

            // A2 设定过滤器
            setupFilters();

            // B1 重置微信进入首页
            h.r("B1 重置微信进入首页");
            adapter.restart(); // 由于 checkAccountStatus步骤选择了有效账号，该步骤应该不会抛出Broken异常

            // B2 进入已订阅公众号的列表页面params
            h.r("B2 进入已订阅公众号的列表页面");
            adapter.goToSubscribePublicAccountList();

            // B3 根据 media_nick 搜索到相关的公众号（已订阅的公众号）
            h.r("B3 搜索到相关的公众号（已订阅的公众号）");
            adapter.goToSubscribedPublicAccountHome(media_nick);

            // B4 基于media_nick 查询Media
            try {

                Media media = Daos.get(Media.class).queryBuilder().where().eq("nick", media_nick).queryForFirst();

                // 如果对应的media不存在
                if (media == null) {

                    media = parseMedia(adapter.getPublicAccountInfo(media_nick, false));
                    media.insert();

                }
                // 加载media已经采集过的文章数据
                else {

					Daos.get(Essay.class).queryBuilder()
                            .where().eq("media_id", media.id)
                            .query()
                            .stream().forEach(essay -> {
                        collectedEssays.add(essay.title + " " + DateFormatUtil.dfd.print(essay.pubdate.getTime()));
                    });
                }

            } catch (Exception e) {
                logger.error("Error handling DB, ", e);
            }

            // C1 进入历史文章数据列表页
            h.r("C1 进入历史文章数据列表页");
            adapter.gotoPublicAccountEssayList();

            boolean atBottom = false;

            // D 截图分析数据点击文章
            while (!atBottom) {

                // D1 截图分析文章坐标  此处得到的图像识别结果是一个通用的东西  需要分解出日期的坐标
                List<OCRParser.TouchableTextArea> textAreas = this.adapter.getEssayListTextAreas();

                // D3 逐个文章去点击
                for (OCRParser.TouchableTextArea area : textAreas) {

                    // D2 通过 textAreas 分析是否是最后一页 一般来讲都是最后一个 是 已无更多
                    if (area.content.equals("已无更多") && textAreas.indexOf(area) == textAreas.size() - 1) {
                        atBottom = true;
                        break;
                    }

                    // D2 去重判断
                    String feature = area.content + " " + DateFormatUtil.dfd.print(area.date.getTime());
                    if (collectedEssays.contains(feature) || visitedEssays.contains(feature)) continue;

                    // D3 进入文章
                    countDown = new CountDownLatch(1);

                    h.r("D3 进入文章");
                    adapter.goToEssayDetail(area);

                    // D4 判断是否进入了文章页
                    h.r("D4 判断是否进入了文章页");
                    if (adapter.device.reliableTouch(area.left, area.top)) {

                        h.r("D41 向下滑动两次");
                        for (int i = 0; i < 2; i++) {
                            this.adapter.device.slideToPoint(1000, 800, 1000, 2000, 1000);
                        }

                        countDown.await(10, TimeUnit.SECONDS);

                        // D42 如果进入成功 需要记录已经点击的文章标题-时间
                        visitedEssays.add(feature);

                        // D43 在文章采集过程中 判断是否是转发文章
                        if (forward) {

                            countDown = new CountDownLatch(1);

                            // 点进去被转发的文章
                            h.r("D43 点进去被转发的文章");
                            adapter.device.touch(582, 557, 6000);

                            h.r("D44 向下滑动两次");
                            for (int i = 0; i < 2; i++) {
                                this.adapter.device.slideToPoint(1000, 800, 1000, 2000, 1000);
                            }
                        }

                        // D44 关闭文章
                        h.r("D45 关闭文章");
                        adapter.touchUpperLeftButton();
                        // adapter.device.touch(67, 165, 1000);

                        forward = false;
                    }
                }

                // D5 确定回到文章列表页
                if (adapter.status != WeChatAdapter.Status.PublicAccount_Essay_List)
                    throw new AdapterException.IllegalStateException(adapter);

                // D51 向下滑动
                h.r("D46 向下滑动一页");
                this.adapter.device.slideToPoint(1000, 800, 1000, 2000, 1000);

            }
        }
        // 微信查看全部消息被限流
        catch (GetPublicAccountEssayListFrozenException e) {

            logger.error("Error enter Media[{}] history essay list page, Account:[{}], ", media_nick, adapter.account.id, e);

            try {

                // 更新账号状态
                this.adapter.account.status = Account.Status.Get_Public_Account_Essay_List_Frozen;
                this.adapter.account.update();

            } catch (Exception e1) {
                logger.error("Error update account status failure, ", e);
            }

            // 将当前任务提交 下一次在执行任务的时候
            retry = true;

        }
        // 在指定账号的订阅列表中找不到指定的公众号的异常
        catch (MediaException.NotSubscribe e) {

            logger.error("Account:[{}] didn't subscribe public account:[{}], ", adapter.account.id, media_nick, e);

        }
        // 指定的媒体账号和订阅的账号不一致
        catch (MediaException.NotEqual e) {

            logger.error("Strange error for Account:[{}] public account:[{}], ", adapter.account.id, media_nick, e);

        }
        // Adapter状态异常
        catch (AdapterException.IllegalStateException e) {

            logger.error("Adapter State IllegalStateException, ", e);

            // 继续重试
            retry = true;
        }
        // 线程中断异常   此异常在外部捕获不到
        catch (InterruptedException e){

            logger.error("Thread InterruptedException, ", e);
        }

        return retry;
    }


    /**
     *
     */
    public void setupFilters() {

        logger.info("Add Request/Response filters...");

        RequestFilter requestFilter = (request, contents, messageInfo) -> null;

        ResponseFilter responseFilter = (response, contents, messageInfo) -> {

            String url = messageInfo.getOriginalUrl();

            if (contents != null && (contents.isText() || url.contains("https://mp.weixin.qq.com/s"))) {

                // 正文
                if (url.contains("https://mp.weixin.qq.com/s")) {
                    logger.info(" : " + url);
                    content_stack.push(contents.getTextContents());
                }
                // 统计信息
                else if (url.contains("getappmsgext")) {
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
                    logger.info(content_src);

                    // 获取转发的Essay Id
                    String f_id = parseForwardId(content_src);

                    // 判定是否是转发文章
                    if (f_id != null) {
                        forward = true;
                    }

                    Essay essay = null;

                    essay = parseContent(content_src, f_id);

                    try {
                        Source source = new Source(essay.id, url_permanent, null, essay.id + ".html", "text/xml", content_src.getBytes());
                        source.insert();
                    } catch (Exception e) {
                        logger.error("Error insert source:{}, ", essay.id, e);
                    }

                    essay.source_id = essay.id;
                    essay.origin_url = url_permanent;

                    try {
                        if (stats_stack.size() > 0) {
                            String stats_src = stats_stack.pop();
                            essay = parseStat(essay, stats_src);
                        }
                    } catch (Exception e) {
                        logger.error("Error parse essay:{},", e);
                    }

                    try {
                        essay.insert();
                    } catch (Exception e) {
                        logger.info("Error insert essay:{},", e);
                    }

                    // 对评论的处理
                    if (comments_stack.size() > 0) {

                        String comments_src = comments_stack.pop();

                        List<Comment> comments = null;

                        try {
                            comments = parseComments(Comment.FType.Essay, essay.id, essay.src_id, comments_src);
                        } catch (ParseException e) {
                            logger.error("Error parse comments:{},", e);
                        }

                        comments.stream().forEach(c -> {
                            try {
                                c.insert();
                            } catch (Exception e) {
                                logger.error("Error insert comments:{}", e);
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

        adapter.device.setProxyRequestFilter((request, contents, messageInfo) -> null);

        adapter.device.setProxyResponseFilter((response, contents, messageInfo) -> {
        });
    }

    /**
     * 解析文章主题内容
     *
     * @param source
     * @return
     * @throws ParseException
     */
    public Essay parseContent(String source, String f_id) {

        String title = null;
        String media_nick = null;
        String src_id = null;
        String media_src_id = null;

        // 标题 必须
        Pattern pattern = Pattern.compile("(?si)<h2.*?</h2>|<title>.*?</title>");
        Matcher matcher = pattern.matcher(source);
        if (matcher.find()) {
            title = matcher.group().replaceAll("<.+?>| +|\r\n|\n", "");
        }

        // 公号昵称 必须
        pattern = Pattern.compile("(?si)<strong class=\"profile_nickname\">.+?</strong>|<div class=\"account_nickname\">.+?</div>");
        matcher = pattern.matcher(source);
        if (matcher.find()) {
            media_nick = matcher.group().replaceAll("<.+?>| +|\r\n|\n", "");
        }

        // 原始文章id 必须
        pattern = Pattern.compile("(?si)(?<=mid = \").+?(?=\";)");
        matcher = pattern.matcher(source);
        if (matcher.find()) {
            src_id = matcher.group().replaceAll("\"| |\\|", "");
        }

        // 原始媒体 id
        pattern = Pattern.compile("(?si)(?<=user_name = \").+?(?=\";)");
        matcher = pattern.matcher(source);
        if (matcher.find()) {
            media_src_id = matcher.group().replaceAll("\"| |\\|", "");
        }

        if (title == null || media_nick == null) {
            return null;
        }

        // 生成文章
        Essay essay = new Essay();
        essay.platform_id = SubscribeMediaTask.platform.id;
        essay.platform = SubscribeMediaTask.platform.short_name;
        essay.title = title;
        essay.media_nick = media_nick;
        essay.src_id = src_id;
        essay.media_src_id = media_src_id;
        essay.f_id = f_id;
        essay.media_id = SubscribeMediaTask.genId(media_nick);
        essay.id = genId(media_nick, title, src_id);

        // 元信息
        pattern = Pattern.compile("(?si)<div id=\"meta_content\" class=\"rich_media_meta_list\">.*?<div id=\"js_profile_qrcode\" class=\"profile_container\" style=\"display:none;\">");
        matcher = pattern.matcher(source);
        if (matcher.find()) {
            essay.meta_content = matcher.group()
                    .replaceAll("<.+?>| +|\r\n|\n", " ")
                    .replaceAll(" +", " ")
                    .replaceAll("： ", "：")
                    .replaceAll("^ ", "")
                    .replaceAll(" $", "");
        }

        // 原始公号name
        // 如果是转发文章，或随手贴个图，就没有这个name
        pattern = Pattern.compile("(?si)<span class=\"profile_meta_value\">.*?</span>");
        matcher = pattern.matcher(source);
        if (matcher.find()) {
            essay.media_name = matcher.group().replaceAll("<.+?>| +|\r\n|\n", "");
        }

		/* pattern = Pattern.compile("(?si)(?<=biz = \").+?(?=\";)");
		matcher = pattern.matcher(source);
		if(matcher.find()) {
			essay.src_media_id = matcher.group().replaceAll("\"| |\\|", "");
		}*/

        // 日期
        pattern = Pattern.compile("(?si)(?<=createDate=new Date\\(\").+?(?=\")");
        matcher = pattern.matcher(source);
        if (matcher.find()) {
            essay.pubdate = DateFormatUtil.parseTime(matcher.group());
        }

		/*long ts = Long.valueOf(this.getStringFromVars("ts")) * 1000L;
		essay.pubdate = new Date(ts);*/

        // 内容
        pattern = Pattern.compile("(?si)(?<=<div class=\"rich_media_content \" id=\"js_content\">).+?(?=</div>)");
        matcher = pattern.matcher(source);
        if (matcher.find()) {

            String content = StringUtil.purgeHTML(matcher.group());

            // 获取 公众号 文章底部的转载信息
            Pattern pattern_copyright = Pattern.compile("(?si)(?<=<a class=\"original_tool_area\" id=\"copyright_info\").+?(?=</a>)");
            Matcher matcher_copyright = pattern_copyright.matcher(source);
            if (matcher_copyright.find()) {

                String copyright = "<p>" + matcher_copyright.group().replaceAll("<.+?>", "") + "</p>";
                content += copyright;
            }

            List<String> imgs = new ArrayList<>();
            essay.content = ContentCleaner.clean(content, imgs);

            // TODO 抓不到头图
			/*if(this.getStringFromVars("cover") != null && this.getStringFromVars("cover").length() > 0) {
				imgs.add(0, this.getStringFromVars("cover"));
			}*/

            // 去重
            imgs = imgs.stream().distinct().collect(Collectors.toList());

            // 下载图片 保存图片
            List<String> imgs_ = new ArrayList<>();

            for (String img_url : imgs) {

                String img_source_id = StringUtil.MD5(img_url);

                try {
                    // 生成下载图片的任务
                    BasicDistributor.getInstance("download").submit(Source.getDTH(img_url));
                } catch (Exception e) {
                    logger.error("Error download {}", img_url, e);
                }

                imgs_.add(img_source_id);
                //essay.content = essay.content.replace(img_url, img_source_id); // 不替换url
            }

            // 此时记录的都是id
            //essay.images = imgs_; // 保存原有url
        }

        return essay;
    }

    /**
     * 解析阅读数和点赞量
     *
     * @param essay
     * @param source
     * @return
     */
    public Essay parseStat(Essay essay, String source) {

        Pattern pattern = Pattern.compile("(?si)(?<=\"read_num\":)\\d+");
        Matcher matcher = pattern.matcher(source);
        if (matcher.find()) {
            essay.view_count = NumberFormatUtil.parseInt(matcher.group());
        }

        pattern = Pattern.compile("(?si)(?<=\"like_num\":)\\d+");
        matcher = pattern.matcher(source);
        if (matcher.find()) {
            essay.like_count = NumberFormatUtil.parseInt(matcher.group());
        }

        return essay;
    }

    /**
     * 解析转发文章的id
     *
     * @param content_src
     * @return
     */
    public String parseForwardId(String content_src) {

        String f_id = null;

        // 找转发标题
        String title = null, url_f = null, media_nick = null, src_id = null;

        Pattern pattern = Pattern.compile("(?si)<title>.*?</title>");
        Matcher matcher = pattern.matcher(content_src);

        if (matcher.find()) {
            title = matcher.group().replaceAll("<.+?>| +|\r\n|\n", "");
        }

        // 找转发公众号名称 和 原始文章链接
        pattern = Pattern.compile("(?si)<div class=\"share_media\" id=\"js_share_content\">.*?<img class=\"account_avatar\" .*? alt=\"(?<a>.+?)\">.*?<a id=\"js_share_source\" href=\"(?<s>.+?)\">阅读全文");

        matcher = pattern.matcher(content_src);

        if (matcher.find()) {

            url_f = matcher.group("s")
                    .replaceAll("https?://mp.weixin.qq.com/", "")
                    .replaceAll("&amp;(amp;)?", "&");

            media_nick = matcher.group("a");
        }

        // 原始文章mid
        pattern = Pattern.compile("(?si)(?<=source_mid = \").+?(?=\";)");
        matcher = pattern.matcher(content_src);

        if (matcher.find()) {
            src_id = matcher.group().replaceAll("\"| |\\|", "");
        }

        if (title != null || url_f != null || media_nick != null) {
            f_id = genId(media_nick, title, src_id);
        }

        return f_id;
    }

    /**
     * @param mid
     * @param source
     * @return
     * @throws ParseException
     */
    public static List<Comment> parseComments(Comment.FType f_type, String f_id, String mid, String source) throws ParseException {

        List<Comment> comments = new ArrayList<>();

        source = source.replaceAll("^.+?\"elected_comment\":", "");

        Pattern pattern = Pattern.compile("\\{.+?\"nick_name\":\"(?<nickname>.+?)\",\"logo_url\":\"(?<logourl>.+?)\",\"content\":\"(?<content>.+?)\",\"create_time\":(?<pubdate>.+?),\"content_id\":\"(?<contentid>.+?)\".+?\"like_num\":(?<likecount>.+?),.+?\\}");

        Matcher matcher = pattern.matcher(source);

        while (matcher.find()) {

            Comment comment = new Comment();

            comment.f_type = f_type;
            comment.f_id = f_id;
            comment.src_id = matcher.group("contentid");
            comment.username = matcher.group("nickname");
            // comment.logo_url = matcher.group("logourl").replace("\\", "");
            comment.content = matcher.group("content");
            comment.pubdate = DateFormatUtil.parseTime(matcher.group("pubdate"));
            comment.like_count = NumberFormatUtil.parseInt(matcher.group("likecount"));
            comment.id = StringUtil.MD5(f_type + "::" + f_id + "::" + comment.src_id);

            comments.add(comment);
        }

        return comments;
    }

    /**
     * ID 生成
     *
     * @param media_nick
     * @param title
     * @param src_id
     * @return
     */
    public static String genId(String media_nick, String title, String src_id) {
        return StringUtil.MD5(SubscribeMediaTask.platform.short_name + "-" + media_nick + "-" + title + "-" + src_id);
    }

    /**
     * @param pai
     * @return
     */
    public static Media parseMedia(WeChatAdapter.PublicAccountInfo pai) {

        Media media = new Media();
        media.name = pai.name;
        media.nick = pai.nick;
        media.content = pai.content;
        media.essay_count = pai.essay_count;
        media.subject = pai.subject;
        media.trademark = pai.trademark;
        media.phone = pai.phone;

        media.id = SubscribeMediaTask.genId(media.nick);

        return media;
    }

}
