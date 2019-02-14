package one.rewind.android.automator.adapter.wechat.util;

import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.txt.StringUtil;

/**
 * @author scisaga@gmail.com
 * @date 2019/2/10
 */
public class Generator {

	public static String genMediaId(String nick) {
		return StringUtil.MD5(WeChatAdapter.platform.short_name + "-" + nick);
	}

	public static String genEssayId(String media_nick, String title, String src_id) {
		return StringUtil.MD5(WeChatAdapter.platform.short_name + "-" + media_nick + "-" + title + "-" + src_id);
	}

	public static String genCommentId(String f_id, String src_id, String comment) {
		return StringUtil.MD5(WeChatAdapter.platform.short_name + "-" + f_id + "-" + src_id + "-" + comment);
	}
}
