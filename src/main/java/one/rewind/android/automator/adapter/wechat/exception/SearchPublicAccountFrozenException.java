package one.rewind.android.automator.adapter.wechat.exception;

import one.rewind.android.automator.account.Account;

/**
 * @author scisaga@gmail.com
 * @date 2019/1/24
 *
 * 搜索公众号没响应
 */
public class SearchPublicAccountFrozenException extends Exception {

	public Account account;

	public SearchPublicAccountFrozenException(Account account) {
		super(account.username);
		this.account = account;
	}
}
