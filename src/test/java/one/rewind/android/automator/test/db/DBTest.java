package one.rewind.android.automator.test.db;

import com.google.common.collect.Sets;
import com.j256.ormlite.dao.Dao;
import one.rewind.android.automator.model.*;
import one.rewind.android.automator.util.MD5Util;
import one.rewind.db.DaoManager;
import one.rewind.db.Refacter;
import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONArray;
import org.junit.Test;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DBTest {

    @Test
    public void setupTables() throws Exception {
        String packageName = "one.rewind.android.automator.model";
        Refacter.dropTables(packageName);
        Refacter.createTables(packageName);
    }

    @Test
    public void setupTable() throws Exception {
        Refacter.dropTable(Comments.class);
        Refacter.createTable(Comments.class);
        Refacter.dropTable(Essays.class);
        Refacter.createTable(Essays.class);
    }


    @Test
    public void setupRawTable() throws Exception {
        Refacter.createTable(BaiduTokens.class);
    }


    @Test
    public void testSQLInject() throws Exception {
        Dao<Essays, String> dao = DaoManager.getDao(Essays.class);
        long countOf = dao.queryBuilder().where().eq("media_nick", "IPP评论").countOf();
        System.out.println(countOf);
    }

    @Test
    public void queryByCondition() throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date zero = calendar.getTime();
        Date current = DateUtils.addDays(zero, 1);
        Dao<SubscribeMedia, String> dao = DaoManager.getDao(SubscribeMedia.class);
        long count = dao.queryBuilder().where().between("insert_time", zero, current).countOf();
        System.out.println(count);
    }


    @Test
    public void updateData() throws Exception {
        Dao<SubscribeMedia, String> dao = DaoManager.getDao(SubscribeMedia.class);

        SubscribeMedia subscribeMedia = dao.queryBuilder().where().
                eq("udid", "ZX1G22PQLH").
                and().
                eq("media_name", "北京理工大学研究生教育").
                queryForFirst();

        System.out.println(subscribeMedia);

        subscribeMedia.status = SubscribeMedia.CrawlerState.FINISH.status;

        System.out.println(subscribeMedia);

        subscribeMedia.update();
    }


    @Test
    public void byTimeQuery() throws Exception {
        Dao<SubscribeMedia, String> dao = DaoManager.getDao(SubscribeMedia.class);

        Date date = new Date();

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        String result = df.format(date);

        Date parse = df.parse(result);

        List<SubscribeMedia> insert_time = dao.queryBuilder().where().eq("insert_time", parse).query();

        System.out.println(insert_time.size());
    }


    /**
     * tokens.put("rDztaDallgGp5GkiZ7mPBUwo", "em7eA1tsCXyqm0HdD83dMwsyG0gSU77n");
     * tokens.put("dZy8t8zi1j8G0j5yKlz5geQM", "1EKo8m3NheGQZM35gYEUNLjhnkQa9o9R");
     * tokens.put("y66vnud58pLDi0qi5NDVBnIg", "IEQpqda0jL4u2uFOgLL1TTo6fDSR42em");
     * tokens.put("e40LU71rtvxEsD6bVlb9U3wV", "zVK9qmK3B2hhWiw3WuYmGIHNHgXR6tgh");
     *
     * @throws Exception
     */
    @Test
    public void initBaiduTokens() throws Exception {
        BaiduTokens token1 = new BaiduTokens();
        token1.app_k = "rDztaDallgGp5GkiZ7mPBUwo";
        token1.app_s = "em7eA1tsCXyqm0HdD83dMwsyG0gSU77n";
        token1.count = 0;
        token1.insert();
    }

    @Test
    public void testQueryByQuery() throws SQLException {
        Calendar instance = Calendar.getInstance();
        instance.set(Calendar.HOUR_OF_DAY, 0);
        instance.set(Calendar.MINUTE, 0);
        instance.set(Calendar.SECOND, 0);
        Date time = instance.getTime();


        List<SubscribeMedia> query = DBTab.subscribeDao.queryBuilder().where().eq("udid", "ZX1G42BX4R").and().ge("insert_time", time).query();

        System.out.println(query);
    }

    @Test
    public void perfectEssays() throws Exception {
        Dao<Essays, String> dao = DBTab.essayDao;
        int page = 2975;
        boolean flag = true;
        while (flag) {
            List<Essays> result = dao.queryBuilder().limit(20).offset((page - 1) * 30).query();
            for (Essays var : result) {
                var.media_id = MD5Util.MD5Encode(var.platform + "-" + var.media_nick, "UTF-8");
                var.update();
                String content = var.content;
                Set<String> imgStr = getImgStr(content);
                JSONArray array = new JSONArray(imgStr);
                var.images = array.toString();
                var.update();
            }

            page++;
            if (page == 2976) {
                flag = false;
            }
        }
    }

    String splitCover(String fullContent) {
        int start = fullContent.indexOf("<img");
        String newSub = fullContent.substring(start);
        int end = newSub.indexOf(">");
        String tempStr = newSub.substring(0, end);
        int last = tempStr.indexOf("src=");
        String lastString = tempStr.substring(last);
        lastString = lastString.replaceAll("src=\"", "").replaceAll("\"", "");
        return lastString;
    }

    /**
     * 得到网页中图片的地址
     */
    public Set<String> getImgStr(String htmlStr) {
        Set<String> pics = new HashSet<String>();
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

    public static void main(String[] args) {
        Set set = Sets.newHashSet();
        set.add("ahjsadhasd");
        set.add("asdadsas");
        JSONArray array = new JSONArray(set);
        System.out.println(array);
    }
}
