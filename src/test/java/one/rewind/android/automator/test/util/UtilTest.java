package one.rewind.android.automator.test.util;

import one.rewind.android.automator.util.AndroidUtil;
import org.junit.Test;

/**
 * Create By 2018/10/18
 */
public class UtilTest {


	/**
	 * List of devicesInfo attached
	 * 9YJ7N17429007528	device
	 * ZX1G323GNB	device
	 */
	@Test
	public void testShellExecute() {
		String[] devicesInfoByShell = AndroidUtil.obtainDevices();
	}



}
