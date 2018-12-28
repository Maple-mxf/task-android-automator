package one.rewind.android.automator.test.util;

import org.junit.Test;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class StringTest {

	@Test
	public void testChaineseLength(){
		String tmp = "2018年12月21日";

		System.out.println(tmp.length());

		System.out.println(tmp.substring(0,11));
	}
}
