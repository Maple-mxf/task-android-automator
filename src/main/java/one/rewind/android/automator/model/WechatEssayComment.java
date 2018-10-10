package one.rewind.android.automator.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.DBName;
import one.rewind.txt.DateFormatUtil;
import one.rewind.txt.NumberFormatUtil;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@DBName(value = "wechat")
@DatabaseTable(tableName = "wechat_essay_comments")
public class WechatEssayComment extends Model {

	@DatabaseField(dataType = DataType.STRING, width = 32, index = true)
	public String mid;

	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String nick_name;

	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String logo_url;

	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String content;

	@DatabaseField(dataType = DataType.STRING, width = 32, unique = true)
	public String content_id;

	@DatabaseField(dataType = DataType.INTEGER)
	public int like_count;

	@DatabaseField(dataType = DataType.DATE)
	public Date pubdate = new Date();

	public WechatEssayComment() {}

	public static List<WechatEssayComment> parseComments(String mid, String source) throws ParseException {

		List<WechatEssayComment> comments = new ArrayList<>();

		source = source.replaceAll("^.+?\"elected_comment\":", "");

		Pattern pattern = Pattern.compile("\\{.+?\"nick_name\":\"(?<nickname>.+?)\",\"logo_url\":\"(?<logourl>.+?)\",\"content\":\"(?<content>.+?)\",\"create_time\":(?<pubdate>.+?),\"content_id\":\"(?<contentid>.+?)\".+?\"like_num\":(?<likecount>.+?),.+?\\}");
		Matcher matcher = pattern.matcher(source);
		while(matcher.find()) {

			WechatEssayComment comment = new WechatEssayComment();
			comment.mid = mid;
			comment.nick_name = matcher.group("nickname");
			comment.logo_url = matcher.group("logourl").replace("\\","");
			comment.content = matcher.group("content");
			comment.pubdate = DateFormatUtil.parseTime(matcher.group("pubdate"));
			comment.content_id = matcher.group("contentid");
			comment.like_count = NumberFormatUtil.parseInt(matcher.group("likecount"));

			comments.add(comment);
		}

		return comments;
	}
}
