package one.rewind.android.automator.adapter;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import one.rewind.android.automator.util.FileUtil;
import one.rewind.android.automator.util.ImageUtil;
import one.rewind.json.JSON;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * @author maxuefeng [m17793873123@163.com]
 * tesseract弊端:不可以识别出颜色比较暗的文字
 * tesseract  <image path> <out name> -l chi_sim hocr(附带坐标的command)
 * github wiki:https://github.com/tesseract-ocr/tesseract/wiki/Command-Line-Usage
 */
public class TesseractOCRAdapter implements OCRAdapter {

	protected static TesseractOCRAdapter instance;

	/**
	 * 单例模式
	 * @return
	 */
	public static TesseractOCRAdapter getInstance() {

		if (instance == null) {
			synchronized (TesseractOCRAdapter.class) {
				if (instance == null) {
					instance = new TesseractOCRAdapter();
				}
			}
		}

		return instance;
	}


	/**
	 * @param filePath 文件原始路径
	 * @return 返回图片文字坐标
	 * @throws Exception IOException cropException
	 */
	public List<TouchableTextArea> imageOcr(String filePath, boolean crop) throws Exception {

		// 1 裁剪图片
		File inImage = new File(filePath);

		if (crop) {
			BufferedImage bufferedImage = OCRAdapter.cropImage(ImageIO.read(inImage));

			// 覆盖原有图片  TODO 第二个参数formatName设置为png文件是否会变名字
			ImageIO.write(bufferedImage, "png", new File(inImage.getAbsolutePath()));

		}

		// 2 灰度化图片
		ImageUtil.grayImage(filePath, filePath, "png");

		// 3 tesseract图像识别

		// 4 解析tesseract out 得到JSONObject
		List<TouchableTextArea> textAreas = parseHtml(parse2Html(inImage));

		logger.info(JSON.toJson(textAreas));

		return textAreas;
	}

	/**
	 * 具体调用命令
	 * 	tesseract /usr/local/java-workplace/wechat-android-automator/data/wx.jpeg wx -l chi_sim hocr
	 * @param file
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static String parse2Html(File file) throws IOException, InterruptedException {

		String fileName = file.getName();

		int index = fileName.lastIndexOf(".");

		String filePrefix = fileName.substring(0, index);

		String pathPrefix = file.getAbsolutePath().replace(fileName, "");

		// tesseract command
		// tesseract <img_dir> <output_name> <options>
		// 附带文字坐标command
		String command = "tesseract " + file.getAbsolutePath() + " " + pathPrefix + filePrefix + "  -l chi_sim hocr"; // chi_sim 是字体参数
		/*String command = "tesseract " + file.getAbsolutePath() + " " + pathPrefix + filePrefix + "  -l chi hocr";*/

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
	private Rectangle parseRectangle(String title) {

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

		return new Rectangle(point[0], point[1], point[2], point[3]);
	}


	/**
	 * <p>
	 * TODO:目前为止解析出来的数据位置信息没有任何特征(除了日期标记之外)  原生HTML文件中可以利用p标签将图片中的文字进行分段落
	 * </p>
	 *
	 * @param source html源码
	 */
	public List<TouchableTextArea> parseHtml(String source) {

		List<TouchableTextArea> textAreas = new LinkedList<>();

		Document document = Jsoup.parse(source);
		Element body = document.body();

		// parse div
		Elements div = body.getElementsByClass("ocr_carea");

		// 便利所有的div元素
		for (Element el : div) {

			// 获取P标签  每一个var0之后一个P标签
			Element p = el.child(0);

			// 获取P标签下面的span标签
			Elements oneLevelSpans = p.children();

			// 遍历一级下的所有span标签
			for (Element oneLevelSpan : oneLevelSpans) {
				// span下面可能没有子标签  也可能存在子标签

				// 获取二级的span标签
				Elements secondLevelSpans = oneLevelSpan.children();

				// 此处的span合起来是一行文字
				StringBuilder line = new StringBuilder();

				// 文字坐标位置
				Rectangle rectangle = null;

				// 每个二级的第一个span的title属性中包含文字的坐标  只取第一个坐标
				int size = secondLevelSpans.size();

				for (int i=0; i< size; i++) {

					Element tmpElement = secondLevelSpans.get(i);

					if (!Strings.isNullOrEmpty(tmpElement.text())) {
						line.append(tmpElement.text());
					}

					if (i == 0) {

						// 存储坐标
						// 获取当前span的title属性
						rectangle = parseRectangle(secondLevelSpans.get(i).attr("title"));
					}
				}

				if (!Strings.isNullOrEmpty(line.toString())) {

					Optional.ofNullable(rectangle).ifPresent(r -> {

						if (r.top >= CROP_TOP) {

							textAreas.add(new TouchableTextArea(line.toString(), r));
						}
					});
				}
			}
		}

		return textAreas;
	}

	/**
	 * 由于默认的解析方法会把两行标题解析成两个文本框，此时需要根据顺序关系和坐标关系，对文本框进行合并
	 *
	 * @param originalTextAreas 初始解析的文本框列表
	 * @return 合并后的文本框列表
	 */
	private List<TouchableTextArea> mergeForTitle(List<TouchableTextArea> originalTextAreas, int gap) {

		List<TouchableTextArea> newTextAreas = new LinkedList<>();

		TouchableTextArea lastArea = null;
		for(TouchableTextArea area : originalTextAreas) {

			if(lastArea != null) {

				if(area.left == lastArea.left && (area.top - (lastArea.top + lastArea.height)) < gap) {
					lastArea = lastArea.add(area);
				}
				else {
					newTextAreas.add(area);
					lastArea = area;
				}

			} else {
				newTextAreas.add(area);
				lastArea = area;
			}
		}

		return newTextAreas;
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
}
