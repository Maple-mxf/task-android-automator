package one.rewind.android.automator.adapter.wechat.task;

import com.dw.ocr.parser.OCRParser;
import net.lightbody.bmp.filters.RequestFilter;
import net.lightbody.bmp.filters.ResponseFilter;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.adapter.wechat.exception.GetPublicAccountEssayListFrozenException;
import one.rewind.android.automator.adapter.wechat.exception.NoSubscribeMediaException;
import one.rewind.android.automator.adapter.wechat.exception.SearchPublicAccountFrozenException;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.android.automator.task.Task;
import one.rewind.android.automator.task.TaskHolder;
import one.rewind.data.raw.model.Comment;
import one.rewind.data.raw.model.Essay;
import one.rewind.data.raw.model.Media;
import one.rewind.data.raw.model.Source;
import one.rewind.io.requester.basic.BasicDistributor;
import one.rewind.txt.ContentCleaner;
import one.rewind.txt.DateFormatUtil;
import one.rewind.txt.NumberFormatUtil;
import one.rewind.txt.StringUtil;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
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
    public List<EssayTitle> visitedEssays = new ArrayList<>();

    // 已经保存过的微信公众号文章
    public List<EssayTitle> collectedEssays = new ArrayList<>();

    // 最大尝试次数
    public static final int MAX_ATTEMPTS = 5;

    /**
     * @param holder
     * @param params
     * @throws IllegalParamsException
     */
    public GetMediaEssaysTask(TaskHolder holder, String... params) throws IllegalParamsException {

        super(holder, params);
        if (params.length != 1)
            throw new IllegalParamsException(Arrays.stream(params).collect(Collectors.joining(", ")));

        media_nick = params[0];
    }

    /**
     * 文章标题-发布时间
     */
    class EssayTitle {

        public String title;
        public Date pubdate;

        EssayTitle(String title, Date pubdate) {
            this.title = title;
            this.pubdate = pubdate;
        }
    }


    @Override
    public void execute() throws InterruptedException, IOException, AdapterException.OperationException {

        setupFilters();

        // 任务执行成功回调
        this.doneCallbacks.add((Runnable) () -> {

            // TODO 通知redis队列

            // 移除过滤器
            removeFilters();

        });

        // 任务执行
        try {
            // 0 重置微信进入首页
            adapter.start();

            // A 进入已订阅公众号的列表页面params
            adapter.goToSubscribePublicAccountList();

            // B 根据media name搜索到相关的公众号（已订阅的公众号）
            adapter.goToSubscribedPublicAccountHome(media_nick);

            // 同时从数据库查找已经采集的文章列表

            // C 进入历史文章数据列表页
            adapter.gotoPublicAccountEssayList();

            boolean atBottom = false;

            // D 截图分析数据点击文章
            while (!atBottom) {

                // D1 截图分析文章坐标  此处得到的图像识别结果是一个通用的东西  需要分解出日期的坐标
                List<OCRParser.TouchableTextArea> textAreas = this.adapter.getPublicAccountEssayListTitles();

                // TODO 需要把 最后一页 对应的 textArea 删掉，一般来讲都是最后一个
                // D3 逐个文章去点击
                for (OCRParser.TouchableTextArea area : textAreas) {

                    // D2 通过 textAreas 分析是否是最后一页
                    if (area.content.equals("已无更多") && textAreas.indexOf(area) == textAreas.size()) {
                        atBottom = true;
                    }

                    // D2 去重判断  TODO  发布日期处理
                    EssayTitle et = new EssayTitle(area.content, area.date);
                    if (collectedEssays.contains(et) || visitedEssays.contains(et)) continue;

                    adapter.goToEssayDetail(area);

                    // D3 判断是否进入了文章页
                    if (adapter.device.reliableTouch(area.left, area.top)) {
                        // D3-1 如果进入成功 需要记录已经点击的文章标题-时间
                        visitedEssays.add(et);

                        // D3-2 如果点击无响应则不会返回 TODO 是否需要点击左上角叉号？（文章转载）
                        adapter.device.goBack();
                    }

                    // D4 向下滑动两次
                    for (int i = 0; i < 2; i++) {
                        this.adapter.device.slideToPoint(1000, 800, 1000, 2000, 1000);
                    }
                }
            }
            // 获取公众号文章列表没反应
        } catch (GetPublicAccountEssayListFrozenException e) {

            logger.error("Error enter essay list page error,touch not response! cause[{}]", e);
            e.printStackTrace();

            // 搜索公众号没响应
        } catch (SearchPublicAccountFrozenException e) {

            logger.error("Error search WeChat media not response! cause[{}]", e);
            e.printStackTrace();

            // Adapter状态异常
        } catch (AdapterException.IllegalStateException e) {

            logger.error("AndroidDevice state error! cause[{}]", e);
            e.printStackTrace();

            // 点击没反应
        } catch (AdapterException.NoResponseException e) {

            logger.error("Error enter essay detail touch not response! cause[{}]", e);
            e.printStackTrace();

            // 在指定账号的订阅列表中找不到指定的公众号的异常
        } catch (NoSubscribeMediaException e) {
            e.printStackTrace();

            // 无可用账户异常
        } catch (AccountException.NoAvailableAccount noAvailableAccount) {

            logger.error("Error no available account! cause[{}]", noAvailableAccount);
            noAvailableAccount.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
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


                    String f_id = null;

                    // TODO 此处解析f_id
                    // 如果是引用文章，模拟touch，采集原始文章

                    Essay essay = null;

                    essay = parseContent(content_src, f_id);

                    try {
                        // TODO source 内容无法更新
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

}
