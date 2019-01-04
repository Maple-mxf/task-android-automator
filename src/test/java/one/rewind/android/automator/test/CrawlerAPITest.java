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
}
