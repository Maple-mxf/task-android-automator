package one.rewind.android.automator.adapter;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import one.rewind.android.automator.model.BaiduTokens;
import one.rewind.android.automator.util.*;
import org.apache.commons.lang3.StringUtils;
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
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author maxuefeng [m17793873123@163.com]
 * <p>
 * OCRAdapter:图像识别  得出正确的结果用于业务操作,在数据去重上有很大的作用
 * <p>
 * tesseract弊端:不可以识别出颜色比较暗的文字
 * tesseract  <image path> <out name> -l chi_sim hocr(附带坐标的command)
 * github wiki:https://github.com/tesseract-ocr/tesseract/wiki/Command-Line-Usage
 */
public class OCRAdapter {

	private static Logger logger = LoggerFactory.getLogger(OCRAdapter.class);

	// tesseract OCR 图像识别

	// 识别文章列表页

	// -----------------------------------tesseract图像识别----------------------------------------------


	public static List<String> imageOcrOfTesseract(File file) throws IOException, InterruptedException {

		String fileName = file.getName();

		int index = fileName.lastIndexOf(".");

		String filePrefix = fileName.substring(0, index);


		String pathPrefix = file.getAbsolutePath().replace(fileName, "");

		// tesseract command
		// tesseract <img_dir> <output_name> <options>
		// 附带文字坐标command

		String command = "tesseract " + file.getAbsolutePath() + " " + pathPrefix + filePrefix + "  -l chi_sim";

		Process process = Runtime.getRuntime().exec(command);

		// 确认命令执行完毕
		process.waitFor();

		// 构建txt文件对象
		File txtFile = new File(file.getAbsolutePath().replace(fileName, "") + filePrefix + ".txt");

		// 识别得出文字集合
		return Files.readLines(txtFile, Charset.forName("UTF-8"));
	}


	// 默认覆盖原来的图片
	public static BufferedImage cropEssayListImage(BufferedImage bufferedImage) {
		// 头部的数据是不可以裁剪的   会影响坐标的准确度
		return ImageUtil.cropImage(bufferedImage, 0, 0, 770, 1918);
	}

	/**
	 * tesseract图像识别识别出来的数据空格相对较多;需要将数据的所有空白行去掉,所有的标题合成为一行
	 * 下面两种场景都是基于去掉图片中的顶部的时间数据的处理场景
	 * 第一种场景:如果是首页,存在发"消息按钮"等无用数据
	 * 第二种场景:如果是正常的页面截图  无需去除头部无用数据
	 *
	 * @param origin 未经处理的原始数据
	 */
	public static List<String> ocrRealEassyTitleOfTessseract(List<String> origin) {

		if (origin.contains("发消息")) {

			int index = origin.indexOf("发消息");

			// 去除index索引前的所有数据   此处不存在index为-1的情况
			for (int i = 0; i <= index; i++) {
				origin.add(i, "");
			}

		}
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

		// GC origin 对象
		return result;
	}

	// --------------------------------------百度图像识别--------------------------------------------------

	/**
	 * @param filePath 文件路径
	 * @param crop     是否裁剪图片
	 * @return 返回origin;保罗log_id words location等字段
	 */
	public static JSONObject imageOCR(String filePath, boolean crop) throws Exception {

		if (crop) {
			File file = new File(filePath);
			// 首先线裁剪图片
			BufferedImage bufferedImage = OCRAdapter.cropEssayListImage(ImageIO.read(file));

			// 覆盖原有图片  TODO 第二个参数formatName设置为png文件是否会变名字
			ImageIO.write(bufferedImage, "png", new File(file.getAbsolutePath()));
		}


		String otherHost = "https://aip.baidubce.com/rest/2.0/ocr/v1/general";
		byte[] imgData = FileUtil.readFileByBytes(filePath);
		String imgStr = Base64Util.encode(imgData);
		String params = URLEncoder.encode("image", "UTF-8") + "=" + URLEncoder.encode(imgStr, "UTF-8");
		BaiduTokens token = BaiduAPIUtil.obtainToken();
		String accessToken = BaiduAPIUtil.getAuth(token.app_k, token.app_s);
		String rs = HttpUtil.post(otherHost, accessToken, params);
		return new JSONObject(rs);

	}


	/**
	 * @param origin 百度API得到的原始数据
	 * @return 返回经过处理的数据  并且每个数据都是唯一的标题    精度达不到100%正确
	 * result :
	 * {
	 * "log_id": 8053627142307370140,
	 * "words_result_num": 25,
	 * "words_result": [{
	 * "location": {
	 * "width": 307,
	 * "top": 10,
	 * "left": 1113,
	 * "height": 62
	 * },
	 * "words": "R∠B12:14"
	 * }]
	 * }
	 */
	public static JSONArray ocrRealEassyTitleOfBaidu(JSONObject origin) {

		JSONArray resultArray = new JSONArray();

		// 手动构建的JSON
		JSONObject inJSONLocation = new JSONObject("{\"width\":0,\"top\": 0,\"left\": 0,\"height\":0}");

		JSONArray wordsResult = origin.getJSONArray("words_result");

		// 一处第一个无用元素
		wordsResult.remove(0);

		int length = wordsResult.length();

		for (int i = 0; i < length; i++) {


			JSONObject var = (JSONObject) wordsResult.get(i);

			StringBuilder title = new StringBuilder();

			boolean flag = true;

			int left = var.getJSONObject("location").getInt("left");

			JSONObject outJSON = new JSONObject();

			while (flag) {

				if (i == length) break;

				JSONObject tmpNode = (JSONObject) wordsResult.get(i);

				String words = tmpNode.getString("words");

				String tmpWords;

				if (words.length() >= 11) {
					tmpWords = words.substring(0, 11);
				} else {
					tmpWords = "";
				}

				if (tmpWords.contains("年") && tmpWords.contains("月") && tmpWords.contains("日") && left <= 80) {

					flag = false;

					JSONObject location = tmpNode.getJSONObject("location");

					outJSON.put("location", location);

					outJSON.put("words", words);

				} else {
					title.append(words);
					// 此时的words是文章标题
					i++;
				}

				left = tmpNode.getJSONObject("location").getInt("left");
			}

			// 构建title JSON
			JSONObject titleJSON = new JSONObject();

			titleJSON.put("location", inJSONLocation);
			titleJSON.put("words", title.toString());

			resultArray.put(titleJSON);
			resultArray.put(outJSON);
		}

		System.out.println("result Array : " + resultArray);
		return resultArray;
	}

	/**
	 * 返回的数据格式如下
	 * JSONObject{"words_result":[{"words":"xxx","location":{"top":0,"left":0,"width":"","right"}}]}
	 * <p>
	 * TODO:目前为止解析出来的数据位置信息没有任何特征(除了日期标记之外)  原生HTML文件中可以利用p标签将图片中的文字进行分段落
	 * </p>
	 *
	 * @param rs html源码
	 * @throws IOException IO read exception 分析每行文字的坐标
	 */
	public static JSONObject jsoupParseHtml2JSON(String rs) throws IOException {

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

//		System.out.println(wordsResult);

		return wordsResult;
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

	private OCRAdapter() {
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
}
