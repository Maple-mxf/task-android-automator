package one.rewind.android.automator.adapter.wechat.exception;

import one.rewind.android.automator.account.Account;

/**
 * 在指定账号的订阅列表中找不到指定的公众号的异常
 *
 * @author maxuefeng [m17793873123@163.com]
 */
public class MediaException extends Exception {

    public String media_nick;

    public MediaException(String media_nick) {
        this.media_nick = media_nick;
    }

    /**
     * 公众号失效
     */
    public static class Illegal extends MediaException {

        public Illegal(String media_nick) {
            super(media_nick);
        }
    }

    /**
     * 未订阅
     */
    public static class NotSubscribe extends MediaException {

    	public Account account;

        public NotSubscribe(Account account, String media_nick) {
            super(media_nick);
            this.account = account;
        }
    }

    /**
     * 找不到
     */
    public static class NotFound extends MediaException {

        public NotFound(String media_nick) {
            super(media_nick);
        }
    }

    /**
     *
     */
    public static class NotEqual extends MediaException {

    	public String media_nick_expected;

        public NotEqual(String media_nick_expected, String media_nick) {
            super(media_nick);
            this.media_nick_expected = media_nick_expected;
        }
    }
}
