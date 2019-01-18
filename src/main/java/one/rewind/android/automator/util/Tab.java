package one.rewind.android.automator.util;

import com.j256.ormlite.dao.Dao;
import one.rewind.android.automator.model.BaiduToken;
import one.rewind.android.automator.model.SubscribeMedia;
import one.rewind.db.DaoManager;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Const method
 *
 * @author maxuefeng [m17793873123@163.com]
 */
public class Tab {


	public static Dao<SubscribeMedia, String> subscribeDao;

	public static Dao<BaiduToken, String> tokenDao;


	public static final String REQUEST_ID_SUFFIX = "Android-Automator-Topic-";

	public static final String TOPIC_MEDIA = "topic_media";

	public static final String TOPICS = "topics";

	// 辅助标记指定设备去做
	public static final String UDID_SUFFIX = "_udid_";

	// 本地appium端口  为了不使得端口冲突   port自增
	public static AtomicInteger proxyPort = new AtomicInteger(41000);

	public static AtomicInteger appiumPort = new AtomicInteger(42756);

	public static AtomicInteger localProxyPort = new AtomicInteger(42000);

	static {
		try {
			subscribeDao = DaoManager.getDao(SubscribeMedia.class);
			tokenDao = DaoManager.getDao(BaiduToken.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 转换成公众号名称
	public static String realMedia(String media) {
		// ZY的投资自习室req_id20181225101231956_udid_ZYDGDBNFKS   字符串形式
		if (media.contains(Tab.REQUEST_ID_SUFFIX)) {
			int index = media.indexOf(Tab.REQUEST_ID_SUFFIX);
			return media.substring(0, index);
		}
		return media;
	}


	// 转换成requestID
	public static String topic(String media) {

		if (media.contains(Tab.REQUEST_ID_SUFFIX)) {
			// 指定设备采集
			if (media.contains(Tab.UDID_SUFFIX)) {
				int startIndex = media.indexOf(Tab.REQUEST_ID_SUFFIX);
				int endIndex = media.indexOf(Tab.UDID_SUFFIX);
				return media.substring(startIndex, endIndex);
			} else {
				// 没有指定设备采集
				int index = media.indexOf(Tab.REQUEST_ID_SUFFIX);
				return media.substring(index);
			}
		}
		return null;
	}


	// ZY的投资自习室req_id20181225101231956_udid_ZYDGDBNFKS
	public static String udid(String media) {
		if (media.contains(Tab.UDID_SUFFIX)) {
			int index = media.indexOf(Tab.UDID_SUFFIX);
			return media.substring(index).replace(Tab.UDID_SUFFIX, "");
		} else {
			return null;
		}
	}

}
