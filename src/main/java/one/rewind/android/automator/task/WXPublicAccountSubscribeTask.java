package one.rewind.android.automator.task;

import one.rewind.data.raw.model.Platform;
import one.rewind.txt.StringUtil;

/**
 * @author scisaga@gmail.com
 * @date 2019/1/14
 */
public class WXPublicAccountSubscribeTask extends Task {

	public static Platform platform;

	// TODO 此处处理有问题
	static {
		try {
			platform = new Platform("微信公众号平台", "WX");
			platform.id = 1;
			platform.insert();
		}
		catch (Exception e) {

		}
	}

	@Override
	public Boolean call() throws Exception {
		return null;
	}

	public static String genId(String nick) {
		return StringUtil.MD5(platform.short_name + "-" + nick);
	}
}
