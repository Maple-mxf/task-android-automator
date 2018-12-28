package one.rewind.android.automator.api;

import java.io.Serializable;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public enum ErrorEnum implements Serializable {

	PARAM_ERROR(1000, "参数错误");

	int code;

	String message;

	ErrorEnum(int code, String message) {
		this.code = code;
		this.message = message;
	}
}
