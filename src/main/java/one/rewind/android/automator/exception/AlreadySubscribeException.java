package one.rewind.android.automator.exception;

import org.openqa.selenium.NoSuchElementException;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class AlreadySubscribeException extends NoSuchElementException {

	public AlreadySubscribeException(String reason) {
		super(reason);
	}

}
