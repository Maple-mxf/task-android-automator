package one.rewind.android.automator.task;

import one.rewind.android.automator.adapter.WeChatAdapter;
import one.rewind.data.raw.model.Media;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author scisaga@gmail.com
 * @date 2019/1/14
 */
public class GetWXPublicAccountEssaysTask extends Task {

	public WeChatAdapter adapter;

	// 采集的公众号
	public Media media;

	// 已经访问过的微信公众号文章页面
	public List<EssayTitle> visitedEssays = new ArrayList<>();

	// 已经保存过的微信公众号文章
	public List<EssayTitle> collectedEssays = new ArrayList<>();

	/**
	 * 内部类 文章标题-发布时间
	 */
	class EssayTitle {

		public String title;
		public Date pubdate;

		EssayTitle(String title, Date pubdate) {
			this.title = title;
			this.pubdate = pubdate;
		}

	}

	@Override
	public Boolean call() throws Exception {

		return null;
	}
}
