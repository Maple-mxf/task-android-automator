package one.rewind.android.automator.exception;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class WeChatRateLimitException extends RuntimeException {

	public WeChatRateLimitException(String reason) {
		super(reason);
	}
}
