package one.rewind.android.automator.task;

import com.dw.ocr.parser.OCRParser;
import net.lightbody.bmp.filters.RequestFilter;
import net.lightbody.bmp.filters.ResponseFilter;
import one.rewind.android.automator.adapter.WeChatAdapter;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.android.automator.exception.WeChatAdapterException;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author scisaga@gmail.com
 * @date 2019/1/14
 */
public class WXGetMediaEssaysTask extends Task {

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


	/**
	 * 任务执行
	 *
	 * @return
	 * @throws Exception
	 */
	@Override
	public Boolean call() throws Exception {

		setupFilters();

		// 任务执行
		execute();

		removeFilters();

		return true;
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
		essay.platform_id = WXMediaSubscribeTask.platform.id;
		essay.platform = WXMediaSubscribeTask.platform.short_name;
		essay.title = title;
		essay.media_nick = media_nick;
		essay.src_id = src_id;
		essay.media_src_id = media_src_id;
		essay.f_id = f_id;
		essay.media_id = WXMediaSubscribeTask.genId(media_nick);
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
		return StringUtil.MD5(WXMediaSubscribeTask.platform.short_name + "-" + media_nick + "-" + title + "-" + src_id);
	}


	/**
	 * 具体任务执行
	 */
	public void execute() {

		try {

			// 0 重置微信进入首页
			this.adapter.start();

			// A 进入已订阅公众号的列表页面
			this.adapter.goToSubscribePublicAccountList();

			// B 根据media name搜索到相关的公众号（已订阅的公众号）
			this.adapter.goToPublicAccountHome(this.media.name);

			// C 进入历史文章数据列表页
			this.adapter.gotoPublicAccountEssayList();

			// D 截图分析数据点击文章
			while (true) {

				// D1 截图分析文章坐标
				List<OCRParser.TouchableTextArea> textAreas = this.adapter.getPublicAccountEssayListTitles();

				for (OCRParser.TouchableTextArea area : textAreas) {

					// D2 逐个文章去点击
					this.adapter.goToEssayDetail(area);

					// D3 如果点击无响应则不会返回

					// D4 如果当前页是最后一页  则需要返回标记任务完成 执行钩子函数即可

				}
			}
		} catch (WeChatAdapterException.IllegalStateException e) {

			logger.error("Error task execute failed [{}]", e);

		} catch (WeChatAdapterException.NoResponseException e) {

			logger.error("Error task execute failed [{}]", e);
		} catch (WeChatAdapterException.SearchPublicAccountFrozenException e) {

			logger.error("Error task execute failed [{}]", e);
		} catch (WeChatAdapterException.GetPublicAccountEssayListFrozenException e) {

			logger.error("Error task execute failed [{}]", e);
		} catch (IOException e) {

			logger.error("Error task execute failed [{}]", e);
		} catch (InterruptedException e6) {

			logger.error("Error task execute failed [{}]", e6);
		} catch (AdapterException.OperationException e) {
			e.printStackTrace();
		} catch (AccountException.NoAvailableAccount noAvailableAccount) {
			noAvailableAccount.printStackTrace();
		}
	}
}
