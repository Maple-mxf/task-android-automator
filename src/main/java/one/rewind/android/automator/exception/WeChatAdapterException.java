package one.rewind.android.automator.exception;

import one.rewind.android.automator.account.AppAccount;

/**
 * @author scisaga@gmail.com
 * @date 2019/1/16
 */
public class WeChatAdapterException extends Exception {

	public static class NoResponseException extends Exception {

	}

	public static class SearchPublicAccountFrozenException extends Exception {

		public AppAccount account;

		public SearchPublicAccountFrozenException(AppAccount account) {
			this.account = account;
		}
	}

	public static class GetPublicAccountEssayListFrozenException extends Exception {

		public AppAccount account;

		public GetPublicAccountEssayListFrozenException(AppAccount account) {
			this.account = account;
		}
	}

	public static class IllegalException extends Exception {


	}
}
