package one.rewind.android.automator.exception;

import one.rewind.android.automator.account.Account;

/**
 * @author scisaga@gmail.com
 * @date 2019/1/16
 */
public class WeChatAdapterException extends Exception {

	public static class NoResponseException extends Exception {

	}

	public static class SearchPublicAccountFrozenException extends Exception {

		public Account account;

		public SearchPublicAccountFrozenException(Account account) {
			this.account = account;
		}
	}

	public static class GetPublicAccountEssayListFrozenException extends Exception {

		public Account account;

		public GetPublicAccountEssayListFrozenException(Account account) {
			this.account = account;
		}
	}

	public static class IllegalStateException extends Exception {


	}
}
