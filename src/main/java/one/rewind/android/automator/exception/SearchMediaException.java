package one.rewind.android.automator.exception;

import org.openqa.selenium.NoSuchElementException;

/**
 * 搜索微信公众可能出现搜索无响应异常  导致手机操作链路出现无法继续的异常
 *
 * @author maxuefeng [m17793873123@163.com]
 */
public class SearchMediaException extends NoSuchElementException {

	public SearchMediaException(String message) {
		super(message);
	}
}
