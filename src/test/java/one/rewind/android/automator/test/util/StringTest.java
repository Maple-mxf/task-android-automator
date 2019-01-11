package one.rewind.android.automator.test.util;

import one.rewind.android.automator.util.Tab;
import org.junit.Test;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class StringTest {

	@Test
	public void testChaineseLength() {
		String tmp = "2018年12月21日";


		System.out.println(tmp.substring(0, 4));

		System.out.println(tmp.substring(5, 7));

//		System.out.println(tmp.substring());
	}

	@Test
	public void testRealMedia() {
		final String media = Tab.realMedia("阿里巴巴Android-Automator-Topic-20190104152236676__udid_ZGHDGSHS");

		System.out.println(media);
	}
}
