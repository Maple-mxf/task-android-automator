package one.rewind.android.automator.adapter.wechat.exception;

import one.rewind.android.automator.account.Account;

/**
 * 在指定账号的订阅列表中找不到指定的公众号的异常
 *
 * @author maxuefeng [m17793873123@163.com]
 */
public class NoSubscribeMediaException extends Exception {

    public Account account;

    public NoSubscribeMediaException(Account account) {
        this.account = account;
    }
}
