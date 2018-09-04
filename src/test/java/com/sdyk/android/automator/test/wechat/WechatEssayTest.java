package com.sdyk.android.automator.test.wechat;

import com.sdyk.android.automator.model.WechatEssay;
import com.sdyk.android.automator.model.WechatEssayComment;
import one.rewind.json.JSON;
import one.rewind.txt.DateFormatUtil;
import one.rewind.txt.NumberFormatUtil;
import one.rewind.txt.StringUtil;
import one.rewind.util.FileUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WechatEssayTest {

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

		WechatEssay we = new WechatEssay().parseContent(content).parseStat(stats);

		System.err.println(JSON.toPrettyJson(we));

		List<WechatEssayComment> comments_ = WechatEssayComment.parseComments(we.mid, comments);

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
