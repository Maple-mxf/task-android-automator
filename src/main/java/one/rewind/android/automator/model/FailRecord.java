package one.rewind.android.automator.model;

import one.rewind.data.raw.model.base.ModelL;

public class FailRecord extends ModelL {

	public FailRecord() {
	}

	/**
	 * 已经抓取文章的数量
	 */
	public int finishNum;

	/**
	 * 下一次抓取需要滑动多少页
	 */
	public int slideNumByPage;

	/**
	 * 当前微信公众号的名称
	 */
	public String mediaName;

	/**
	 * 那个设备关注了当前公众号
	 */
	public String udid;
}
