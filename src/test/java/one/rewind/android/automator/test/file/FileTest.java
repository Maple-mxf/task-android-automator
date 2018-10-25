package one.rewind.android.automator.test.file;

import org.junit.Test;

import java.io.File;

/**
 * 描述：
 * 作者：MaXFeng
 * 时间：2018/10/16
 */
public class FileTest {


	@Test
	public void testWorkspacePath() {
		String property = System.getProperty("user.dir");
		System.out.println(property);
		File file = new File(property);
		boolean b = file.canRead();
		System.out.println(b);
	}
}
