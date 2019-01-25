package one.rewind.android.automator.adapter.wechat;

import one.rewind.android.automator.adapter.Action;

/**
 * @author scisaga@gmail.com
 * @date 2019/1/25
 */
public class Actions {

	// 重启App
	public static Action<WeChatAdapter, Boolean> restartAction = (adapter, params) -> {

		adapter.restart();

		// 无法正常进入主页
		if (!adapter.atHome()) {
			adapter.init();
		}

		return Boolean.TRUE;
	};

}
