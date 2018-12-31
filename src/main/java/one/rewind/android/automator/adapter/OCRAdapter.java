package one.rewind.android.automator.adapter;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import one.rewind.android.automator.model.BaiduTokens;
import one.rewind.android.automator.util.*;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;

/**
 * @author maxuefeng [m17793873123@163.com]
 * <p>
 * OCRAdapter:图像识别  得出正确的结果用于业务操作,在数据去重上有很大的作用
 * <p>
 * tesseract弊端:不可以识别出颜色比较暗的文字
 */
public class OCRAdapter {

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
	 *  {
	 *     "log_id": 8053627142307370140,
	 *     "words_result_num": 25,
	 *     "words_result": [{
	 *         "location": {
	 *             "width": 307,
	 *             "top": 10,
	 *             "left": 1113,
	 *             "height": 62
	 *         },
	 *         "words": "R∠B12:14"
	 *     }]
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

				if (words.length() >= 11){
					 tmpWords= words.substring(0,11);
				}else{
					tmpWords = "";
				}

				if (tmpWords.contains("年") && tmpWords.contains("月") && tmpWords.contains("日") && left <= 80) {

					flag = false;

					JSONObject location = tmpNode.getJSONObject("location");

					outJSON.put("location",location);

					outJSON.put("words",words);

				}else{
					title.append(words);
					// 此时的words是文章标题
					i++;
				}

				left = tmpNode.getJSONObject("location").getInt("left");
			}

			// 构建title JSON
			JSONObject titleJSON = new JSONObject();

			titleJSON.put("location",inJSONLocation);
			titleJSON.put("words",title.toString());

			resultArray.put(titleJSON);
			resultArray.put(outJSON);
		}

		System.out.println("result Array : " + resultArray);
		return resultArray;
	}

	private OCRAdapter(){}

}
