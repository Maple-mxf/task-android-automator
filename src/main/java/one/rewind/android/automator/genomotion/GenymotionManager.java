package one.rewind.android.automator.genomotion;

import one.rewind.android.automator.util.ShellUtil;

public class GenymotionManager {

	public GenymotionManager(){

	}

	/**
	 * 启动genymotion.exe
	 */
	public static void startGenyMotion(){

		ShellUtil.exeCall("C:\\Program Files\\Genymobile\\Genymotion\\genymotion.exe");
	}
}
