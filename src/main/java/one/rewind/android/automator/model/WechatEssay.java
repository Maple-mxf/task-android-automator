package one.rewind.android.automator.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.data.raw.model.base.ModelL;
import one.rewind.db.DBName;
import one.rewind.txt.DateFormatUtil;
import one.rewind.txt.NumberFormatUtil;
import one.rewind.txt.StringUtil;

import java.text.ParseException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@DBName(value = "raw")
@DatabaseTable(tableName = "wechat_essays")
public class WechatEssay extends ModelL {

	@DatabaseField(dataType = DataType.STRING, width = 32, index = true, canBeNull = false)
	public String essay_id;

	@DatabaseField(dataType = DataType.STRING, width = 32, index = true, canBeNull = false)
	public String wechat_name;

	@DatabaseField(dataType = DataType.STRING, width = 32, index = true, canBeNull = false)
	public String mid;

	@DatabaseField(dataType = DataType.STRING, width = 256)
	public String title;

	@DatabaseField(dataType = DataType.DATE)
	public Date pubdate = new Date();

	@DatabaseField(dataType = DataType.STRING, columnDefinition = "MEDIUMTEXT")
	public String content;

	@DatabaseField(dataType = DataType.INTEGER)
	public int view_count;

	@DatabaseField(dataType = DataType.INTEGER)
	public int like_count;

	@DatabaseField(dataType = DataType.STRING)
	public String wechat_id;


	@DatabaseField(dataType = DataType.STRING, width = 32, index = true, canBeNull = false)
	public String uid;

	public WechatEssay() {
	}

	public WechatEssay parseContent(String source) throws ParseException {

		Pattern pattern = Pattern.compile("(?si)<h2.*?</h2>");
		Matcher matcher = pattern.matcher(source);
		if (matcher.find()) {
			title = matcher.group().replaceAll("<.+?>| +|\r\n|\n", "");
		}

		pattern = Pattern.compile("(?si)<span class=\"profile_meta_value\">.+?</span>");
		matcher = pattern.matcher(source);
		if (matcher.find()) {
			essay_id = matcher.group().replaceAll("<.+?>| +|\r\n|\n", "");
		}

		pattern = Pattern.compile("(?si)<strong class=\"profile_nickname\">.+?</strong>");
		matcher = pattern.matcher(source);
		if (matcher.find()) {
			wechat_name = matcher.group().replaceAll("<.+?>| +|\r\n|\n", "");
		}

		pattern = Pattern.compile("(?si)(?<=mid = \").+?(?=\")");
		matcher = pattern.matcher(source);
		if (matcher.find()) {
			mid = matcher.group();
		}

		pattern = Pattern.compile("(?si)(?<=publish_time = \").+?(?=\")");
		matcher = pattern.matcher(source);
		if (matcher.find()) {
			pubdate = DateFormatUtil.parseTime(matcher.group());
		}

		pattern = Pattern.compile("(?si)(?<=<div class=\"rich_media_content \" id=\"js_content\">).+?(?=</div>)");
		matcher = pattern.matcher(source);
		if (matcher.find()) {
			content = StringUtil.purgeHTML(matcher.group())
					.replaceAll("<section.*?/>", "\n")
					.replaceAll("<br.*?/>", "\n")
					.replaceAll("\n", "")
					.replaceAll("^ +", "")
					.replaceAll(" +$", "")
					.replaceAll(" >", ">");
		}

		return this;
	}

	public WechatEssay parseStat(String source) {

		Pattern pattern = Pattern.compile("(?si)(?<=\"read_num\":)\\d+");
		Matcher matcher = pattern.matcher(source);
		if (matcher.find()) {
			view_count = NumberFormatUtil.parseInt(matcher.group());
		}

		pattern = Pattern.compile("(?si)(?<=\"like_num\":)\\d+");
		matcher = pattern.matcher(source);
		if (matcher.find()) {
			like_count = NumberFormatUtil.parseInt(matcher.group());
		}

		return this;
	}
}
