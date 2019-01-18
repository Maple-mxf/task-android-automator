package one.rewind.android.automator.exception;

import one.rewind.android.automator.account.Account;

/**
 * @author scisaga@gmail.com
 * @date 2019/1/18
 */
public class AccountException extends Exception {

	public static class NoAvailableAccount extends Exception {}

	public static class Broken extends Exception {

		public Account account;

		public Broken(Account account) {
			this.account = account;
		}

	}

	public static class SubscribeMediaExceedLimit extends Exception {

		public Account account;

		public SubscribeMediaExceedLimit(Account account) {
			this.account = account;
		}
	}
}
