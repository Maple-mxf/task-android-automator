package one.rewind.android.automator.test;

import com.google.common.collect.Maps;
import one.rewind.android.automator.model.Essays;
import one.rewind.android.automator.util.Tab;
import one.rewind.json.JSON;
import org.junit.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @author maxuefeng[m17793873123@163.com]
 */
public class CrawlerAPITest {

	@Test
	public void testPushMedias() throws SQLException {
		List<Essays> essays = Tab.essayDao.queryBuilder().offset(2).limit(1).query();
		Map<String, Object> json = Maps.newHashMap();
		json.put("essays", essays);
		String toJson = JSON.toJson(json);
		System.out.println(toJson);
	}

	public static void main(String[] args) {
		test1(0);
	}

	public static void test1(int arg) {
		int count = 0;
		if (arg == 0) {
			count += 1;
		}
		System.out.println(count);
	}

	@Test
	public void testEquals() {
		String s1 = "{\"words_result\":[{\"words\":\"人生的西东莫要问\",\"location\":{\"top\":276,\"left\":55,\"width\":523,\"height\":141}},{\"words\":\"2018年1月18曰\",\"location\":{\"top\":383,\"left\":55,\"width\":384,\"height\":238}},{\"words\":\"食指老了搞什么搞\",\"location\":{\"top\":647,\"left\":55,\"width\":525,\"height\":512}},{\"words\":\"2018年1月15曰\",\"location\":{\"top\":754,\"left\":55,\"width\":384,\"height\":609}},{\"words\":\"拦火车的网红老师\",\"location\":{\"top\":1018,\"left\":54,\"width\":525,\"height\":883}},{\"words\":\"2018年1月10曰\",\"location\":{\"top\":1125,\"left\":55,\"width\":384,\"height\":980}},{\"words\":\"天津终于有了头条\",\"location\":{\"top\":1389,\"left\":55,\"width\":526,\"height\":1254}},{\"words\":\"2018年1月8曰〈囝〉\",\"location\":{\"top\":1492,\"left\":55,\"width\":356,\"height\":1347}},{\"words\":\"野兽不过山海关\",\"location\":{\"top\":1760,\"left\":55,\"width\":466,\"height\":1625}},{\"words\":\"2018年1月5曰〈囝〉\",\"location\":{\"top\":1863,\"left\":55,\"width\":356,\"height\":1718}}]}\n";
		String s2 = "{\"words_result\":[{\"words\":\"人生的西东莫要问\",\"location\":{\"top\":276,\"left\":55,\"width\":523,\"height\":141}},{\"words\":\"2018年1月18曰\",\"location\":{\"top\":383,\"left\":55,\"width\":384,\"height\":238}},{\"words\":\"食指老了搞什么搞\",\"location\":{\"top\":647,\"left\":55,\"width\":525,\"height\":512}},{\"words\":\"2018年1月15曰\",\"location\":{\"top\":754,\"left\":55,\"width\":384,\"height\":609}},{\"words\":\"拦火车的网红老师\",\"location\":{\"top\":1018,\"left\":54,\"width\":525,\"height\":883}},{\"words\":\"2018年1月10曰\",\"location\":{\"top\":1125,\"left\":55,\"width\":384,\"height\":980}},{\"words\":\"天津终于有了头条\",\"location\":{\"top\":1389,\"left\":55,\"width\":526,\"height\":1254}},{\"words\":\"2018年1月8曰〈囝〉\",\"location\":{\"top\":1492,\"left\":55,\"width\":356,\"height\":1347}},{\"words\":\"野兽不过山海关\",\"location\":{\"top\":1760,\"left\":55,\"width\":466,\"height\":1625}},{\"words\":\"2018年1月5曰〈囝〉\",\"location\":{\"top\":1863,\"left\":55,\"width\":356,\"height\":1718}}]}\n";

		System.out.println(s1.equals(s2));
	}

}
