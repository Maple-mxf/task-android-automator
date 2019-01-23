package one.rewind.android.automator.exception;

import one.rewind.android.automator.account.Account;

/**
 * @author scisaga@gmail.com
 * @date 2019/1/16
 */
public class WeChatAdapterException extends Exception {


	/**
	 * 搜索公众号没反应
	 */
	public static class SearchPublicAccountFrozenException extends Exception {

		public Account account;

		public SearchPublicAccountFrozenException(Account account) {
			this.account = account;
		}
	}

	/**
	 * 获取公众号文章列表没反应
	 */
	public static class GetPublicAccountEssayListFrozenException extends Exception {

		public Account account;

		public GetPublicAccountEssayListFrozenException(Account account) {
			this.account = account;
		}
	}

	/**
	 * 订阅媒体超限异常 针对于微信账号
	 */
	public static class SubscribeMediaExceedLimit extends Exception {

		public Account account;

		public SubscribeMediaExceedLimit(Account account) {
			this.account = account;
		}
	}
}
