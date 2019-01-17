package one.rewind.android.automator.task;

import one.rewind.android.automator.model.SubscribeMedia;
import one.rewind.android.automator.util.Tab;
import one.rewind.data.raw.model.Platform;
import one.rewind.txt.StringUtil;

/**
 * @author scisaga@gmail.com
 */
public class WXPublicAccountSubscribeTask extends Task {

	// 点击无响应重试上限
	public static final int TOUCH_RETRY_COUNT = 5;

	public static Platform platform;

	static {
		try {
			platform = new Platform("微信公众号平台", "WX");
			platform.id = 1;
			platform.insert();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public Boolean call() throws Exception {
		return null;
	}

	public static String genId(String nick) {
		return StringUtil.MD5(platform.short_name + "-" + nick);
	}

	/**
	 * save subscribe record
	 *
	 * @param mediaName media
	 * @param topic     redis topic
	 */
	private void saveSubscribeRecord(String mediaName, String topic) {
		try {
			long tempCount = Tab.subscribeDao.queryBuilder().where()
					.eq("media_name", mediaName)
					.countOf();
			if (tempCount == 0) {
				SubscribeMedia e = new SubscribeMedia();
				e.udid = adapter.device.udid;
				e.media_name = mediaName;
				e.number = 100;
				e.retry_count = 0;
				e.status = SubscribeMedia.State.NOT_FINISH.status;
				e.request_id = topic;
				e.relative = 1;
				e.insert();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
