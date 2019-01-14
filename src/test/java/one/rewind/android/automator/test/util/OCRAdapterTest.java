package one.rewind.android.automator.test.util;

import org.junit.Test;

import java.io.IOException;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class OCRAdapterTest {

	@Test
	@Deprecated
	public void testTesseract() throws IOException, InterruptedException {

//		File file = new File("/usr/local/java-workplace/wechat-android-automator/data/3.jpeg");
//
//		// 首先线裁剪图片
//		BufferedImage bufferedImage = BaiDuOCRParser.cropImage(ImageIO.read(file));
//
//		// 覆盖原有图片
//		ImageIO.write(bufferedImage, "jpg", new File(file.getAbsolutePath()));
//
//		//
//		List<String> result = BaiDuOCRParser.imageOcrOfTesseract(file);
//
//		for (String var : result) {
//			System.out.println(var);
//		}
	}

	/**
	 * 义
	 * <p>
	 * 云南省卫健委最新公示! 澄江人口中
	 * 的“大医院“迎来发展新机遇!
	 * <p>
	 * 全是干货! 个税6项专项附加扣除常
	 * 见疑问50答，很实用
	 * <p>
	 * 澄江电视台新闻线索征集
	 * 澄江新闻 (2018年12月26日)
	 * <p>
	 * 改革开放四十年|档风沐雨铸辉煌 教
	 * 育强县奏华章
	 * <p>
	 * 澄江化石也有属于自己的VR电影
	 * 啦!
	 *
	 * @throws IOException io ex
	 */
	// 测试文章标题
	@Test
	public void testOcrTesseractRealEassyTitile() throws IOException {

//		List<String> origin = Files.readLines(new File("/usr/local/java-workplace/wechat-android-automator/data/3.txt"), Charset.forName("UTF-8"));
//
//		List<String> result = BaiDuOCRParser.realTitleOfTesseract(origin);
//
//		result.forEach(System.out::println);
//
//		System.out.println(result.size());
	}


	@Test
	public void testBaiduAPI() {

//		JSONObject jsonObject = new JSONObject("{\"log_id\":1295913070724108125,\"words_result\":[{\"words\":\"×\",\"location\":{\"top\":46,\"left\":25,\"width\":52,\"height\":47}},{\"words\":\"Q搜索\",\"location\":{\"top\":181,\"left\":459,\"width\":172,\"height\":53}},{\"words\":\"回澄江电视台掌上澄\",\"location\":{\"top\":342,\"left\":286,\"width\":480,\"height\":63}},{\"words\":\"电视新闻信息发布\",\"location\":{\"top\":443,\"left\":379,\"width\":324,\"height\":51}},{\"words\":\"发消息\",\"location\":{\"top\":621,\"left\":477,\"width\":124,\"height\":44}},{\"words\":\"历史消息\",\"location\":{\"top\":869,\"left\":34,\"width\":168,\"height\":46}},{\"words\":\"澄江新闻(2018年12月27日)\",\"location\":{\"top\":994,\"left\":31,\"width\":586,\"height\":54}},{\"words\":\"2018年12月28日\",\"location\":{\"top\":1077,\"left\":34,\"width\":281,\"height\":42}},{\"words\":\"创文高位统筹全力推进全国文明城\",\"location\":{\"top\":1272,\"left\":30,\"width\":707,\"height\":55}},{\"words\":\"市创建\",\"location\":{\"top\":1333,\"left\":31,\"width\":148,\"height\":53}},{\"words\":\"2018年12月28日\",\"location\":{\"top\":1415,\"left\":32,\"width\":283,\"height\":43}},{\"words\":\"又是澄江动物化石!这次是它做了大\",\"location\":{\"top\":1546,\"left\":30,\"width\":730,\"height\":58}},{\"words\":\"贡献!\",\"location\":{\"top\":1611,\"left\":33,\"width\":119,\"height\":55}},{\"words\":\"2018年12月28日\",\"location\":{\"top\":1692,\"left\":32,\"width\":284,\"height\":45}}],\"words_result_num\":14}");
//
//		JSONArray realTitles = BaiDuOCRParser.imageOcr(jsonObject);
//
//		System.out.println(realTitles);
//
//		for (int i = 0; i < realTitles.length(); i++) {
//
//			JSONObject outJSON = (JSONObject) realTitles.get(i);
//
//			JSONObject inJSON = outJSON.getJSONObject("location");
//
//			String words = outJSON.getString("words");
//
//			System.out.println(inJSON);
//
//			System.out.println(words);
//		}

	}
}
