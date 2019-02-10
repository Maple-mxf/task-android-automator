package one.rewind.android.automator.adapter.wechat.exception;

import one.rewind.android.automator.account.Account;

/**
 * @author scisaga@gmail.com
 * @date 2019/1/24
 *
 * 获取公众号文章列表没反应
 */
public class GetPublicAccountEssayListFrozenException extends Exception {

	public Account account;

	public GetPublicAccountEssayListFrozenException(Account account) {
		super(account.username);
		this.account = account;
	}
}
