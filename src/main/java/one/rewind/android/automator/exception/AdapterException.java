package one.rewind.android.automator.exception;

import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.adapter.Adapter;

/**
 * @author scisaga@gmail.com
 * @date 2019/1/18
 */
public class AdapterException extends Exception {

	/**
	 * 没反应
	 */
	public static class NoResponseException extends Exception {

	}

	/**
	 * 操作异常
	 */
	public static class OperationException extends Exception {

		Adapter adapter;

		public OperationException(Adapter adapter) {
			this.adapter = adapter;
		}
	}

	public static class LoginScriptError extends Exception {

		Adapter adapter;
		Account account;

		public LoginScriptError(Adapter adapter, Account account) {
			this.adapter = adapter;
			this.account = account;
		}
	}

	/**
	 * 状态异常
	 */
	public static class IllegalStateException extends Exception {

		Adapter adapter;

		public IllegalStateException(Adapter adapter) {
			this.adapter = adapter;
		}
	}
}
