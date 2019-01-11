package one.rewind.android.automator.adapter;

import one.rewind.android.automator.model.BaiduTokens;
import one.rewind.android.automator.util.*;
import org.apache.commons.lang3.time.DateUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author maxuefeng [m17793873123@163.com]
 * <p>
 * BaiDuOCRAdapter:图像识别  得出正确的结果用于业务操作,在数据去重上有很大的作用
 * <p>
 */
public class BaiDuOCRAdapter implements OCRAdapter {

	/**
	 * @param filePath 文件路径
	 * @param crop     是否裁剪图片
	 * @return 返回origin;保罗log_id words location等字段
	 */
	public static JSONObject imageOcr(String filePath, boolean crop) throws Exception {

		if (crop) {
			File file = new File(filePath);
			// 首先线裁剪图片
			BufferedImage bufferedImage = OCRAdapter.cropEssayListImage(ImageIO.read(file));

			// 覆盖原有图片  TODO 第二个参数formatName设置为png文件是否会变名字
			ImageIO.write(bufferedImage, "png", new File(file.getAbsolutePath()));
		}


		String otherHost = "https://aip.baidubce.com/rest/2.0/imageOcr/v1/general";
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
	public static JSONArray realTitles(JSONObject origin) {

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

	private BaiDuOCRAdapter() {
	}

	/**
	 * 重置百度接口token
	 */
	public static void resetOCRToken() {
		Timer timer = new Timer(false);
		Date nextDay = DateUtil.buildDate();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					List<BaiduTokens> tokens = Tab.tokenDao.queryForAll();
					for (BaiduTokens v : tokens) {
						if (!DateUtils.isSameDay(v.update_time, new Date())) {
							v.count = 0;
							v.update_time = new Date();
							v.update();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, nextDay, 1000 * 60 * 60 * 24);

	}
}
