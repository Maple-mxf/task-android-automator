package one.rewind.android.automator.test.util;

import com.google.common.io.Files;
import one.rewind.android.automator.adapter.OCRAdapter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class OCRAdapterTest {

	@Test
	@Deprecated
	public void testTesseract() throws IOException, InterruptedException {

		File file = new File("/usr/local/java-workplace/wechat-android-automator/data/3.jpeg");

		// 首先线裁剪图片
		BufferedImage bufferedImage = OCRAdapter.cropEssayListImage(ImageIO.read(file));

		// 覆盖原有图片
		ImageIO.write(bufferedImage, "jpg", new File(file.getAbsolutePath()));

		//
		List<String> result = OCRAdapter.imageOcrOfTesseract(file);

		for (String var : result) {
			System.out.println(var);
		}
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

		List<String> origin = Files.readLines(new File("/usr/local/java-workplace/wechat-android-automator/data/3.txt"), Charset.forName("UTF-8"));

		List<String> result = OCRAdapter.ocrRealEassyTitleOfTessseract(origin);

		result.forEach(System.out::println);

		System.out.println(result.size());
	}


	@Test
	public void testBaiduAPI() throws Exception {

		JSONObject jsonObject = OCRAdapter.imageOCR("/usr/local/java-workplace/wechat-android-automator/data/3.jpeg", false);

		System.out.println(jsonObject);


		JSONArray array = OCRAdapter.ocrRealEassyTitleOfBaidu(jsonObject);

		System.out.println(array);

	}
}
