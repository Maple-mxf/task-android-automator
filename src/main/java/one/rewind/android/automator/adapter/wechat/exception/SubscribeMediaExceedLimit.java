package one.rewind.android.automator.adapter.wechat.exception;

import one.rewind.android.automator.account.Account;

/**
 * @author scisaga@gmail.com
 * @date 2019/1/24
 *
 * 订阅媒体超限异常 针对于微信账号
 */
public class SubscribeMediaExceedLimit extends Exception {

	public Account account;

	public SubscribeMediaExceedLimit(Account account) {
		this.account = account;
	}
}
