package one.rewind.android.automator.model;

import com.google.common.collect.Sets;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.data.raw.model.base.ModelD;
import one.rewind.db.DBName;
import one.rewind.txt.DateFormatUtil;
import one.rewind.txt.NumberFormatUtil;
import one.rewind.txt.StringUtil;

import java.text.ParseException;
import java.util.Date;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Create By 2018/10/23
 * Description      id // md5(平台简称+媒体名称+title)
 *
 * @author maxuefeng [m17793873123@163.com]
 */
@DBName(value = "raw")
@DatabaseTable(tableName = "essays")
public class Essays extends ModelD {

    @DatabaseField(dataType = DataType.STRING, width = 32, index = true, canBeNull = false)
    public String media_name;  // 微信号的ID

    @DatabaseField(dataType = DataType.STRING, width = 32, index = true, canBeNull = false)
    public String media_nick; // 微信号的名称

    @DatabaseField(dataType = DataType.STRING)
    public String media_content;  //微信号名称


    @DatabaseField(dataType = DataType.STRING, width = 32, index = true, canBeNull = false)
    public String src_id;

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
    public String platform; // default = WX

    @DatabaseField(dataType = DataType.STRING)
    public String images;

    @DatabaseField(dataType = DataType.INTEGER)
    public int platform_id;   // default = 1

    @DatabaseField(dataType = DataType.INTEGER)
    public int fav_count;  // 喜欢数量

    @DatabaseField(dataType = DataType.INTEGER)
    public int comment_count;  //评论量

    @DatabaseField(dataType = DataType.INTEGER)
    public int forward_count; //转发数量

    @DatabaseField(dataType = DataType.STRING)
    public String f_id; //未知

    @DatabaseField(dataType = DataType.STRING)
    public String media_id;


    @DatabaseField(dataType = DataType.STRING, width = 32, index = true, canBeNull = false)
    public String uid;

    public Essays() {
    }

    public Essays parseContent(String source) throws ParseException {

        Pattern pattern = Pattern.compile("(?si)<h2.*?</h2>");
        Matcher matcher = pattern.matcher(source);
        if (matcher.find()) {
            title = matcher.group().replaceAll("<.+?>| +|\r\n|\n", "");
        }

        pattern = Pattern.compile("(?si)<span class=\"profile_meta_value\">.+?</span>");
        matcher = pattern.matcher(source);
        if (matcher.find()) {
            media_name = matcher.group().replaceAll("<.+?>| +|\r\n|\n", "");
        }

        pattern = Pattern.compile("(?si)<strong class=\"profile_nickname\">.+?</strong>");
        matcher = pattern.matcher(source);
        if (matcher.find()) {
            media_nick = matcher.group().replaceAll("<.+?>| +|\r\n|\n", "");
        }

        pattern = Pattern.compile("(?si)(?<=mid = \").+?(?=\")");
        matcher = pattern.matcher(source);
        if (matcher.find()) {
            src_id = matcher.group();
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

    public Essays parseStat(String source) {

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

    /**
     * 得到网页中图片的地址
     */
    public Set<String> parseImages(String htmlStr) {
        Set<String> pics = Sets.newHashSet();
        String img = "";
        Pattern p_image;
        Matcher m_image;
        // String regEx_img = "<img.*src=(.*?)[^>]*?>"; //图片链接地址
        String regEx_img = "<img.*src\\s*=\\s*(.*?)[^>]*?>";
        p_image = Pattern.compile
                (regEx_img, Pattern.CASE_INSENSITIVE);
        m_image = p_image.matcher(htmlStr);
        while (m_image.find()) {
            // 得到<img />数据
            img = m_image.group();
            // 匹配<img>中的src数据
            Matcher m = Pattern.compile("src\\s*=\\s*\"?(.*?)(\"|>|\\s+)").matcher(img);
            while (m.find()) {
                pics.add(m.group(1));
            }
        }
        return pics;
    }
}
