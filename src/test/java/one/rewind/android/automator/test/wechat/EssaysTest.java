package one.rewind.android.automator.test.wechat;

import one.rewind.android.automator.model.Essays;
import one.rewind.android.automator.model.Comments;
import one.rewind.json.JSON;
import one.rewind.util.FileUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class EssaysTest {

	String content;
	String stats;
	String comments;

	@Before
	public void loadTxt() {
		content = FileUtil.readFileByLines("tmp/wx_essay_content_demo.txt");
		stats = FileUtil.readFileByLines("tmp/wx_essay_stats_demo.txt");
		comments = FileUtil.readFileByLines("tmp/wx_essay_comments_demo.txt");
	}

	@Test
	public void testParseWechatEssay() throws Exception {

		Essays we = new Essays().parseContent(content).parseStat(stats);

		System.err.println(JSON.toPrettyJson(we));

		List<Comments> comments_ = Comments.parseComments(we.src_id, comments);

		System.err.println(JSON.toPrettyJson(comments_));

		comments_.stream().forEach(c -> {
			try {
				c.insert();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

	}
}
