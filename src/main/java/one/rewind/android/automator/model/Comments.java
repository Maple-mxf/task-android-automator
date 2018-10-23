package one.rewind.android.automator.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.android.automator.util.MD5Util;
import one.rewind.data.raw.model.base.ModelD;
import one.rewind.db.DBName;
import one.rewind.txt.DateFormatUtil;
import one.rewind.txt.NumberFormatUtil;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@DBName(value = "raw")
@DatabaseTable(tableName = "comments")
public class Comments extends ModelD {

	@DatabaseField(dataType = DataType.STRING, width = 32, index = true)
	public String src_id;

	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String username;

	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String logo_url; //头像

	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String content;

	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String content_id;

	@DatabaseField(dataType = DataType.INTEGER)
	public int like_count;

	@DatabaseField(dataType = DataType.DATE)
	public Date pubdate = new Date();

	@DatabaseField(dataType = DataType.INTEGER)
	public int f_type;  //文章或评论

	@DatabaseField(dataType = DataType.STRING)
	public String uid;

	public Comments() {
	}

	public static List<Comments> parseComments(String mid, String source) throws ParseException {

		List<Comments> comments = new ArrayList<>();

		source = source.replaceAll("^.+?\"elected_comment\":", "");

		Pattern pattern = Pattern.compile("\\{.+?\"nick_name\":\"(?<nickname>.+?)\",\"logo_url\":\"(?<logourl>.+?)\",\"content\":\"(?<content>.+?)\",\"create_time\":(?<pubdate>.+?),\"content_id\":\"(?<contentid>.+?)\".+?\"like_num\":(?<likecount>.+?),.+?\\}");
		Matcher matcher = pattern.matcher(source);

		while (matcher.find()) {

			Comments comment = new Comments();

			comment.src_id = mid;
			comment.username = matcher.group("nickname");
			comment.logo_url = matcher.group("logourl").replace("\\", "");
			comment.content = matcher.group("content");
			comment.pubdate = DateFormatUtil.parseTime(matcher.group("pubdate"));
			comment.content_id = matcher.group("contentid");
			comment.like_count = NumberFormatUtil.parseInt(matcher.group("likecount"));
			comment.id = MD5Util.MD5Encode(
					comment.content + DateFormatUtils.format(new Date(), "yyyy-MM-dd hh:mm:ss"), "UTF-8");

			comments.add(comment);
		}

		return comments;
	}
}
