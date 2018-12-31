package one.rewind.android.automator.util;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * 解析html文件
 *
 * @author maxuefeng [m17793873123@163.com]
 */
public class ParseLocalHtml {

	private static Logger logger = LoggerFactory.getLogger(ParseLocalHtml.class);


	/**
	 * 返回的数据格式如下
	 * JSONObject{"words_result":[{"words":"xxx","location":{"top":0,"left":0,"width":"","right"}}]}
	 *
	 * @throws IOException IO read exception 分析每行文字的坐标
	 */
	public static void jsoupParseHtml2JSON() throws IOException {

		JSONObject wordsResult = new JSONObject();

		JSONArray array = new JSONArray();
		//
		String rs = FileUtil.allLines("/usr/local/java-workplace/jopen-common/src/main/resources/index.html");

		Document document = Jsoup.parse(rs);

		Element body = document.body();

		// parse div
		Elements div = body.getElementsByClass("ocr_carea");

		// 便利所有的div元素
		for (Element var0 : div) {

			// 获取P标签  每一个var0之后一个P标签
			Element p = var0.child(0);

			// 获取P标签下面的span标签
			Elements oneLevelSpans = p.children();

			// 遍历一级下的所有span标签
			for (Element oneLevelSpan : oneLevelSpans) {
				// span下面可能没有子标签  也可能存在子标签

				// 获取二级的span标签
				Elements secondLevelSpans = oneLevelSpan.children();

				// 此处的span合起来是一行文字
				StringBuilder line = new StringBuilder();

				JSONObject outJSON = new JSONObject();

				// 文字坐标位置
				JSONObject locationJSON = null;

				// 每个二级的第一个span的title属性中包含文字的坐标  只取第一个坐标
				int size = secondLevelSpans.size();

				for (int i = 0; i < size; i++) {
					line.append(secondLevelSpans.get(i).text());
					if (i == 0) {
						// 存储坐标

						// 获取当前span的title属性
						String point = secondLevelSpans.get(i).attr("title");

						locationJSON = parsePoint(point);

					}
				}

				if (StringUtils.isNotBlank(line.toString())) {

					// put words
					outJSON.put("words", line.toString());

					// put word point
					outJSON.put("location", locationJSON);

					array.put(outJSON);
				}
			}
		}
		logger.info("---------------------------------JSONObject--------------------------------------");

		wordsResult.put("words_result", array);

		System.out.println(wordsResult);
	}


	// 解析坐标
	private static JSONObject parsePoint(String title) {

		// 利用封号进行分解
		String[] result = title.split(";");
		if (result.length < 2) return null;

		String prefix = result[0];

		// 利用空格分割所有的坐标
		String[] nodes = prefix.split(" ");

		int[] point = new int[4];

		int index = 0;

		for (String node : nodes) {
			if (isInteger(node)) {
				point[index++] = Integer.parseInt(node);
			}
		}
		JSONObject json = new JSONObject();
		json.put("top", point[0]);
		json.put("left", point[1]);
		json.put("width", point[2]);
		json.put("height", point[3]);
		return json;
	}

	/**
	 * 判断是否为整数
	 *
	 * @param str 传入的字符串
	 * @return 是整数返回true, 否则返回false
	 */

	private static boolean isInteger(String str) {
		Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
		return pattern.matcher(str).matches();
	}
}
