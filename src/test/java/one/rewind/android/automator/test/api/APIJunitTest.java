package one.rewind.android.automator.test.api;

import com.google.common.collect.Maps;
import one.rewind.io.requester.task.Task;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;

/**
 * Create By  2018/10/25
 * Description:  接口测试用例
 */

public class APIJunitTest {


	@Test
	public void testJson() throws MalformedURLException, URISyntaxException {

		HashMap<Object, Object> heads = Maps.newHashMap();
		heads.put("Content-Type", "application/json");

		Task task = new Task("http://127.0.0.1:8080/api/json",
				"[{\"media_nick\":\"福建水院电力系团总支\",\"media_id\":\"fjsydlgcx\"},{\"media_nick\":\"爱链杭黔\",\"media_id\":\"ilhzqdn\"},{\"media_nick\":\"思l埠新微商花茵美雅顺洗发水\",\"media_id\":\"sibugongsi\"},{\"media_nick\":\"任者股线\",\"media_id\":\"rdy468\"},{\"media_nick\":\"天津融煦律师事务所\",\"media_id\":\"RongXu-Lawyer\"},{\"media_nick\":\"万金解盘\",\"media_id\":\"hw2645\"},{\"media_nick\":\"一葉知秋 yiyezhiqiu\",\"media_id\":\"gh_d7fd64a1d083\"},{\"media_nick\":\"大宗内参\",\"media_id\":\"qihuozhoukan\"},{\"media_nick\":\"代写商业计划书\",\"media_id\":\"weiqin9983\"},{\"media_nick\":\"会活会火\",\"media_id\":\"Woman-Enjoy-Life\"},{\"media_nick\":\"北京理工大学研究生教育\",\"media_id\":\"BIT_grad_edu_cn\"},{\"media_nick\":\"吉林省信耀资产管理有限公司\",\"media_id\":\"xinyaozichan\"},{\"media_nick\":\"财经商业\",\"media_id\":\"gh_dadc1070d56e\"},{\"media_nick\":\"明佣宝\",\"media_id\":\"mingyongbao666\"},{\"media_nick\":\"华龙证券平凉分公司\",\"media_id\":\"HZF-01\"},{\"media_nick\":\"中国消费者报\",\"media_id\":\"zxbccn\"},{\"media_nick\":\"优财CMA校友会\",\"media_id\":\"CMAA2015\"},{\"media_nick\":\"民间股神007\",\"media_id\":\"www600360\"},{\"media_nick\":\"鑫火资本投资管理\",\"media_id\":\"xhcapital\"},{\"media_nick\":\"股海大涨停\",\"media_id\":\"gm58881\"}]");

		Task.Response response = task.getResponse();

		boolean actionDone = response.isActionDone();

		System.out.println(actionDone);

		String text = response.getText();

		System.out.println("text: " + text);
	}

}
