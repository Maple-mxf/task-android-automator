package one.rewind.android.automator.model;

import one.rewind.data.raw.model.base.ModelL;

public class TaskFailRecord extends ModelL {

	public TaskFailRecord() {
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
	public String wxPublicName;

	/**
	 * 那个设备关注了当前公众号
	 */
	public String deviceUdid;
}
