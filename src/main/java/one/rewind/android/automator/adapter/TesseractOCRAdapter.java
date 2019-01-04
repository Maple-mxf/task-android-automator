package one.rewind.android.automator.adapter;

import com.google.common.collect.Lists;
import one.rewind.android.automator.util.FileUtil;
import one.rewind.android.automator.util.ImageUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author maxuefeng [m17793873123@163.com]
 * tesseract弊端:不可以识别出颜色比较暗的文字
 * tesseract  <image path> <out name> -l chi_sim hocr(附带坐标的command)
 * github wiki:https://github.com/tesseract-ocr/tesseract/wiki/Command-Line-Usage
 */
public class TesseractOCRAdapter implements OCRAdapter {

	private TesseractOCRAdapter() {
	}

	private static Logger logger = LoggerFactory.getLogger(TesseractOCRAdapter.class);


	/**
	 * @param filePath 文件原始路径
	 * @return 返回图片文字坐标
	 * @throws Exception IOException cropException
	 */
	public static JSONObject imageOcr(String filePath, boolean crop) throws Exception {

		// 1 裁剪图片
		File inImage = new File(filePath);

		if (crop) {
			BufferedImage bufferedImage = OCRAdapter.cropEssayListImage(ImageIO.read(inImage));

			// 覆盖原有图片  TODO 第二个参数formatName设置为png文件是否会变名字
			ImageIO.write(bufferedImage, "png", new File(inImage.getAbsolutePath()));

		}
		// 2 灰度化图片
		ImageUtil.grayImage(filePath, filePath, "png");

		// 3 tesseract图像识别
		String allLines = imageOcrOfTesseractByPoint(inImage);

		// 4 解析tesseract out 得到JSONObject

		JSONObject result = parseHtml2JSON(allLines);
//		JSONObject result = parseHtml2JSONByTitle(allLines);
		System.out.println("结果为: " + result);
		return result;
	}

	//  tesseract /usr/local/java-workplace/wechat-android-automator/data/wx.jpeg wx -l chi_sim hocr

	public static String imageOcrOfTesseractByPoint(File file) throws IOException, InterruptedException {

		String fileName = file.getName();

		int index = fileName.lastIndexOf(".");

		String filePrefix = fileName.substring(0, index);

		String pathPrefix = file.getAbsolutePath().replace(fileName, "");
		// tesseract command
		// tesseract <img_dir> <output_name> <options>
		// 附带文字坐标command
		String command = "tesseract " + file.getAbsolutePath() + " " + pathPrefix + filePrefix + "  -l chi_sim hocr";

		Process process = Runtime.getRuntime().exec(command);

		// 确认命令执行完毕
		process.waitFor();

		// 识别得出文字集合
		return FileUtil.allLines(file.getAbsolutePath().replace(fileName, "") + filePrefix + ".hocr");
	}

	/**
	 * @param title 每一行文字
	 * @return 当前行文字内容及其坐标
	 */
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
			if (NumberUtils.isDigits(node.trim())) {
				point[index++] = Integer.parseInt(node);
			}
		}
		JSONObject json = new JSONObject();
		json.put("left", point[0]);
		// 相对位置
		json.put("top", point[1] + CROP_TOP);
		json.put("width", point[2]);
		json.put("height", point[3]);
		return json;
	}


	/**
	 * 返回的数据格式如下
	 * JSONObject{"words_result":[{"words":"xxx","location":{"top":0,"left":0,"width":"","right"}}]}
	 * <p>
	 * TODO:目前为止解析出来的数据位置信息没有任何特征(除了日期标记之外)  原生HTML文件中可以利用p标签将图片中的文字进行分段落
	 * </p>
	 *
	 * @param rs html源码
	 */
	public static JSONObject parseHtml2JSON(String rs) {

		JSONObject wordsResult = new JSONObject();

		JSONArray array = new JSONArray();

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
		return wordsResult;
	}

	/**
	 * tesseract图像识别识别出来的数据空格相对较多;需要将数据的所有空白行去掉,所有的标题合成为一行
	 * 下面两种场景都是基于去掉图片中的顶部的时间数据的处理场景
	 * 第一种场景:如果是首页,存在发"消息按钮"等无用数据
	 * 第二种场景:如果是正常的页面截图  无需去除头部无用数据
	 *
	 * @param origin 未经处理的原始数据
	 */
	@Deprecated
	public static List<String> realTitleOfTesseract(List<String> origin) {

		List<String> result = Lists.newArrayList();

		int length = origin.size();

		for (int i = 0; i < length; i++) {
			String current = origin.get(i);
			if (StringUtils.isNotBlank(current)) {

				String title = null;

				boolean flag = true;

				while (flag) {
					i++;
					if (StringUtils.isBlank(origin.get(i))) {
						flag = false;
						title = current;
					} else {
						title = current + origin.get(i);
					}
				}

				// 得到title
				if (StringUtils.isNotBlank(title)) {
					result.add(title);
				}
			}
		}
		return result;
	}


	/**
	 * 相比于上面的方法不一样的地方是以P标签为中心来做  按照P标签解析偶尔会出现标题和文章发布日期成为一行文字
	 *
	 * @param rs html代码
	 * @return 返回详细坐标信息
	 */
	@Deprecated
	public static JSONObject parseHtml2JSONByTitle(String rs) {

		JSONObject wordsResult = new JSONObject();

		JSONArray array = new JSONArray();

		Document document = Jsoup.parse(rs);

		Element body = document.body();

		// parse div
		Elements div = body.getElementsByClass("ocr_carea");

		// 便利所有的div元素
		for (Element var0 : div) {

			// 获取P标签  每一个var0之后一个P标签  每个P标签是一个完整的标题  按照P标签去解析会清楚很多
			Element p = var0.child(0);

			// 外部JSON
			JSONObject outJSON1 = new JSONObject();

			// 用于存储日期的   可能由于tesseract的问题造成文章标题和日期混为一行
			JSONObject outJSON2 = new JSONObject();

			// 文字坐标位置
			JSONObject locationJSON1 = null;

			// 日期的位置坐标点
			JSONObject locationJSON2 = null;

			// 此处的span合起来是一个标题
			StringBuilder line = new StringBuilder();

			// 获取P标签下面的span标签  span标签只有两层
			Elements oneLevelSpans = p.children();

			for (Element oneLevelSpan : oneLevelSpans) {
				// span下面可能没有子标签  也可能存在子标签

				// 获取二级的span标签
				Elements secondLevelSpans = oneLevelSpan.children();

				// 每个二级的第一个span的title属性中包含文字的坐标  只取第一个坐标
				int size = secondLevelSpans.size();

				// 为了分解开日期  由于tesseract的问题造成的
				StringBuilder secondLine = new StringBuilder();

				for (int i = 0; i < size; i++) {

					line.append(secondLevelSpans.get(i).text());

					// 如果出现年月日  会有两个坐标位置
					if (i == 0) {

						// 获取当前span的title属性
						String point = secondLevelSpans.get(i).attr("title");
						locationJSON1 = parsePoint(point);
					}
				}
			}
			if (StringUtils.isNotBlank(line.toString())) {
				// put words
				outJSON1.put("words", line.toString());

				// put word point
				outJSON1.put("location", locationJSON1);

				array.put(outJSON1);
			}
		}
		wordsResult.put("words_result", array);

		return wordsResult;
	}


	/**
	 * @param origin 原始JSON
	 * @return 返回经过处理的JSON数据
	 */
	public static JSONObject realTitles(JSONObject origin) {


		JSONObject outJSON = new JSONObject();

		JSONArray newArray = new JSONArray();

		JSONArray wordsResult = origin.getJSONArray("words_result");

		// 处理原始数据
		for (int i = 0; i < wordsResult.length(); i++) {

			JSONObject tmp = (JSONObject) wordsResult.get(i);


			String words = tmp.getString("words");

			JSONObject location = tmp.getJSONObject("location");

			int left = location.getInt("left");

			// 1 处理当前words的长度

			if (words.length() <= 3) {

				if (newArray.length() == 0) continue; //丢弃当前字段

				else {
					// 获取上一个JSON数据
					JSONObject previous = newArray.getJSONObject(newArray.length() - 1);

					// 修改上一个JSON数据
					previous.put("words", previous.getString("words") + words);

					continue;
				}

			}

			// 2 处理文章标题 如果上一个JSON数据里面的内容是日期
			if (!NumberUtils.isDigits(words.substring(0, 4)) && !words.contains("年")) {

				// 直接放入数组
				if (newArray.length() == 0) {

					JSONObject inJSON = new JSONObject();
					inJSON.put("words", words);
					inJSON.put("location", location);

					newArray.put(inJSON);

					continue;
				} else {
					// 获取上一个数据是否是日期格式的
					// 获取上一个JSON数据
					JSONObject previous = newArray.getJSONObject(newArray.length() - 1);

					// TODO previousWords是否是日期格式的数据?

					String previousWords = previous.getString("words");

					String temp = previousWords;

					// 防止替换错误
					String var0 = previousWords.replace("曰", "日");

					// 判断前4位是否是数字  2017年08月12日
					if (NumberUtils.isDigits(words.substring(0, 4)) && words.contains("年") && words.contains("月") && words.contains("日")) {

					}


				}
			}
		}

		outJSON.put("words_result", newArray);
		return outJSON;
	}

	/**
	 * @return
	 */
	public static boolean isDate(String pa) {
		return false;
	}
}
