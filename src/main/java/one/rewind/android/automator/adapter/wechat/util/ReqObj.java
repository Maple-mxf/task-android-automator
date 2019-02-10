package one.rewind.android.automator.adapter.wechat.util;

import io.netty.handler.codec.http.HttpMethod;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import one.rewind.util.FileUtil;

import java.util.Map;

/**
 * @author scisaga@gmail.com
 * @date 2019/2/11
 */
public class ReqObj implements JSONable<ReqObj> {

	public String url;
	public HttpMethod method;
	public Map<String, String> headers;
	public String req;
	public String res;

	public ReqObj(String url, HttpMethod method, Map<String, String> headers, String req) {
		this.url = url;
		this.method = method;
		this.headers = headers;
		this.req = req;
	}

	public ReqObj setRes(String res) {
		this.res = res;
		return this;
	}

	@Override
	public String toJSON() {
		return JSON.toPrettyJson(this);
	}

	public static ReqObj parseFromFile(String filePath) {
		String src = FileUtil.readFileByLines(filePath);
		return JSON.fromJson(src, ReqObj.class);
	}
}
