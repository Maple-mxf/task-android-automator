package one.rewind.android.automator.adapter.wechat.exception;

import one.rewind.android.automator.account.Account;

/**
 * 在指定账号的订阅列表中找不到指定的公众号的异常
 *
 * @author maxuefeng [m17793873123@163.com]
 */
public class MediaException extends Exception {

    public Account account;
    public String media_nick;

    public MediaException(Account account, String media_nick) {
        this.account = account;

    }

    /**
     * 公众号失效
     */
    public static class Illegal extends MediaException {

        public Illegal(Account account, String media_nick) {
            super(account, media_nick);
        }
    }

    /**
     * 未订阅
     */
    public static class NotSubscribe extends MediaException {

        public NotSubscribe(Account account, String media_nick) {
            super(account, media_nick);
        }
    }

    /**
     * 找不到
     */
    public static class NotFound extends MediaException {

        public NotFound(Account account, String media_nick) {
            super(account, media_nick);
        }
    }

    /**
     *
     */
    public static class NotEqual extends MediaException {

        public NotEqual(Account account, String media_nick) {
            super(account, media_nick);
        }
    }
}
