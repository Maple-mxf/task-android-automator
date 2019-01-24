package one.rewind.android.automator.test.adapter;

import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class OCRAdapterTest {


	@Test
	public void testParsePoint() throws IOException, InterruptedException {

		File image = new File("/usr/local/java-workplace/task-android-automator/data/4068.jpg");

		// 裁剪图片
//		BufferedImage bufferedImage = BaiDuOCRParser.cropImage(ImageIO.read(image));
//
//		// 覆盖原有图片  TODO
//		ImageIO.write(bufferedImage, "png", new File(image.getAbsolutePath()));

		// 将图片进行灰度化  为了让tesseract识别出灰色文字
//		ImageUtil.grayImage();

		// 执行命令  并且得到hocr的结果(是html代码)
//		String document = BaiDuOCRParser.imageOcrOfTesseractByPoint(image);
//		// 识别结果
//		JSONObject result = BaiDuOCRParser.parseHtml2JSON(document);
//
//		System.out.println(result);
	}

	@Test
	public void testChangeImageColor1() {
		BufferedImage img = null;
		File f;
		try {
			f = new File("/usr/local/java-workplace/task-android-automator/data/wxIn.jpg");
			img = ImageIO.read(f);
		} catch (IOException e) {
			System.out.println(e);
		}
		int width = img.getWidth();
		int height = img.getHeight();
		int p = img.getRGB(0, 0);
		int a = (p >> 24) & 0xff;
		int r = (p >> 16) & 0xff;
		int g = (p >> 8) & 0xff;
		int b = p & 0xff;
		a = 255;
		r = 100;
		g = 150;
		b = 200;
		p = (a << 24) | (r << 16) | (g << 8) | b;
		img.setRGB(0, 0, p);
		try {
			f = new File("/usr/local/java-workplace/task-android-automator/data/wxOn.jpg");
			ImageIO.write(img, "jpg", f);
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	// 解析出来的图片问题很大
	@Test
	@Deprecated
	public void testChangeImageColor2() {
		int width = 640, height = 320;
		BufferedImage img;
		img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		// file object
		File f;
		// create random values pixel by pixel
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int a = (int) (Math.random() * 256); //generating
				int r = (int) (Math.random() * 256); //values
				int g = (int) (Math.random() * 256); //less than
				int b = (int) (Math.random() * 256); //256
				int p = (a << 24) | (r << 16) | (g << 8) | b; //pixel
				img.setRGB(x, y, p);
			}
		}
		// write image
		try {
			f = new File("/usr/local/java-workplace/task-android-automator/data/wxIn.jpg");
			ImageIO.write(img, "png", f);
		} catch (IOException e) {
			System.out.println("Error: " + e);
		}
	}

	@Deprecated
	public static void binaryImage() throws IOException {
		File file = new File("/usr/local/java-workplace/task-android-automator/data/wxOn.jpg");
		BufferedImage image = ImageIO.read(file);

		int width = image.getWidth();
		int height = image.getHeight();

		BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);//重点，技巧在这个参数BufferedImage.TYPE_BYTE_BINARY
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				int rgb = image.getRGB(i, j);
				grayImage.setRGB(i, j, rgb);
			}
		}

		File newFile = new File("/usr/local/java-workplace/task-android-automator/data/4028.jpg");
		ImageIO.write(grayImage, "jpg", newFile);
	}

	public static void grayImage() throws IOException {
		File file = new File("/usr/local/java-workplace/task-android-automator/data/653354255.jpg");
		BufferedImage image = ImageIO.read(file);

		int width = image.getWidth();
		int height = image.getHeight();

		BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);//重点，技巧在这个参数BufferedImage.TYPE_BYTE_GRAY
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				int rgb = image.getRGB(i, j);
				grayImage.setRGB(i, j, rgb);
			}
		}

		File newFile = new File("/usr/local/java-workplace/task-android-automator/data/llll.png");
		ImageIO.write(grayImage, "png", newFile);
	}

	@Test
	public void grayImageTest() throws IOException {
		grayImage();
	}

	// JSON Data
	/*@Test
	public void testJSONData() throws Exception {

//		TODO
		final List<OCRParser.TouchableTextArea> textAreas = TesseractOCRParser.getInstance().imageOcr("/usr/local/java-workplace/task-android-automator/data/653354255.jpg", false);

		JSONObject jsonObject = null;

		JSONArray result = jsonObject.getJSONArray("words_result");

		int count = 0;

		System.out.println("result: " + result);

		for (Object var : result) {
			JSONObject tmp = (JSONObject) var;
			String words = tmp.getString("words");
			JSONObject location = tmp.getJSONObject("location");

			int left = location.getInt("left");
			if (words.contains("年") && words.contains("月") && words.contains("日") && left <= 80) {
				count++;
			}
		}
		if (count < 1) throw new AndroidCollapseException("未知异常!没有检测到任务文章数据!");
		System.out.println("count: " + count);
	}

	@Test
	public void testParseJSONObject() {
		JSONObject var = new JSONObject("{\n" +
				"\t\"words_result\": [{\n" +
				"\t\t\"words\": \"N\",\n" +
				"\t\t\"location\": {\n" +
				"\t\t\t\"top\": 21,\n" +
				"\t\t\t\"left\": 38,\n" +
				"\t\t\t\"width\": 88,\n" +
				"\t\t\t\"height\": 63\n" +
				"\t\t}\n" +
				"\t}, {\n" +
				"\t\t\"words\": \"〉〈\",\n" +
				"\t\t\"location\": {\n" +
				"\t\t\t\"top\": 144,\n" +
				"\t\t\t\"left\": 46,\n" +
				"\t\t\t\"width\": 94,\n" +
				"\t\t\t\"height\": 192\n" +
				"\t\t}\n" +
				"\t}, {\n" +
				"\t\t\"words\": \"金晓:债牛仍然在途，但仍\",\n" +
				"\t\t\"location\": {\n" +
				"\t\t\t\"top\": 377,\n" +
				"\t\t\t\"left\": 55,\n" +
				"\t\t\t\"width\": 191,\n" +
				"\t\t\t\"height\": 432\n" +
				"\t\t}\n" +
				"\t}, {\n" +
				"\t\t\"words\": \"复\",\n" +
				"\t\t\"location\": {\n" +
				"\t\t\t\"top\": 457,\n" +
				"\t\t\t\"left\": 55,\n" +
				"\t\t\t\"width\": 109,\n" +
				"\t\t\t\"height\": 512\n" +
				"\t\t}\n" +
				"\t}, {\n" +
				"\t\t\"words\": \"2018年4月20曰\",\n" +
				"\t\t\"location\": {\n" +
				"\t\t\t\"top\": 561,\n" +
				"\t\t\t\"left\": 55,\n" +
				"\t\t\t\"width\": 279,\n" +
				"\t\t\t\"height\": 606\n" +
				"\t\t}\n" +
				"\t}, {\n" +
				"\t\t\"words\": \"张睿:统_利率双轨制需把\",\n" +
				"\t\t\"location\": {\n" +
				"\t\t\t\"top\": 748,\n" +
				"\t\t\t\"left\": 58,\n" +
				"\t\t\t\"width\": 191,\n" +
				"\t\t\t\"height\": 803\n" +
				"\t\t}\n" +
				"\t}, {\n" +
				"\t\t\"words\": \"2018年4月17曰\",\n" +
				"\t\t\"location\": {\n" +
				"\t\t\t\"top\": 851,\n" +
				"\t\t\t\"left\": 55,\n" +
				"\t\t\t\"width\": 383,\n" +
				"\t\t\t\"height\": 896\n" +
				"\t\t}\n" +
				"\t}, {\n" +
				"\t\t\"words\": \"徐翔:债市孕盲新机会″追污\",\n" +
				"\t\t\"location\": {\n" +
				"\t\t\t\"top\": 1119,\n" +
				"\t\t\t\"left\": 54,\n" +
				"\t\t\t\"width\": 191,\n" +
				"\t\t\t\"height\": 1174\n" +
				"\t\t}\n" +
				"\t}, {\n" +
				"\t\t\"words\": \"芒\",\n" +
				"\t\t\"location\": {\n" +
				"\t\t\t\"top\": 1199,\n" +
				"\t\t\t\"left\": 56,\n" +
				"\t\t\t\"width\": 109,\n" +
				"\t\t\t\"height\": 1250\n" +
				"\t\t}\n" +
				"\t}, {\n" +
				"\t\t\"words\": \"2018年4月13曰\",\n" +
				"\t\t\"location\": {\n" +
				"\t\t\t\"top\": 1303,\n" +
				"\t\t\t\"left\": 55,\n" +
				"\t\t\t\"width\": 383,\n" +
				"\t\t\t\"height\": 1348\n" +
				"\t\t}\n" +
				"\t}, {\n" +
				"\t\t\"words\": \"肖乐鸣:债券违约分析系列\",\n" +
				"\t\t\"location\": {\n" +
				"\t\t\t\"top\": 1490,\n" +
				"\t\t\t\"left\": 61,\n" +
				"\t\t\t\"width\": 251,\n" +
				"\t\t\t\"height\": 1545\n" +
				"\t\t}\n" +
				"\t}, {\n" +
				"\t\t\"words\": \"技术性违约的冰山_角\",\n" +
				"\t\t\"location\": {\n" +
				"\t\t\t\"top\": 1570,\n" +
				"\t\t\t\"left\": 55,\n" +
				"\t\t\t\"width\": 640,\n" +
				"\t\t\t\"height\": 1626\n" +
				"\t\t}\n" +
				"\t}, {\n" +
				"\t\t\"words\": \"2018年4月12曰\",\n" +
				"\t\t\"location\": {\n" +
				"\t\t\t\"top\": 1674,\n" +
				"\t\t\t\"left\": 55,\n" +
				"\t\t\t\"width\": 383,\n" +
				"\t\t\t\"height\": 1719\n" +
				"\t\t}\n" +
				"\t}, {\n" +
				"\t\t\"words\": \"章凯恺二_个″不着急，慢慢\",\n" +
				"\t\t\"location\": {\n" +
				"\t\t\t\"top\": 1860,\n" +
				"\t\t\t\"left\": 55,\n" +
				"\t\t\t\"width\": 251,\n" +
				"\t\t\t\"height\": 1916\n" +
				"\t\t}\n" +
				"\t}]\n" +
				"}");
		JSONArray array = var.getJSONArray("words_result");

		int count = 0;

		for (Object t : array) {
			JSONObject tmp = (JSONObject) t;

			String words = tmp.getString("words");

			JSONObject location = tmp.getJSONObject("location");

			int left = location.getInt("left");

			if (words.contains("年") && words.contains("月") && left <= 80 && (words.contains("曰") || words.contains("日"))) {

				count++;
			}
		}

		System.out.println("count: " + count);
	}

	@Test
	public void parseImage2() throws Exception {

//		TODO ====================
		TesseractOCRParser.getInstance().imageOcr("", false);
		JSONObject jsonObject = null;
		System.out.println(jsonObject);
	}

	@Test
	public void testCropImage() throws IOException {
		// 1 裁剪图片
		File inImage = new File("/usr/local/java-workplace/task-android-automator/data/1100265578.jpg");

		BufferedImage bufferedImage = OCRParser.cropImage(ImageIO.read(inImage));

		// 覆盖原有图片  TODO 第二个参数formatName设置为png文件是否会变名字
		ImageIO.write(bufferedImage, "png", new File(inImage.getAbsolutePath()));
	}

	@Test
	public void testOcrResult() throws Exception {
		final List<OCRParser.TouchableTextArea> textAreas = TesseractOCRParser.getInstance().imageOcr("/usr/local/java-workplace/task-android-automator/data/1426002855.jpg", true);

		// TODO
		final JSONObject jsonObject = null;

		System.out.println(jsonObject);

		final JSONArray array = jsonObject.getJSONArray("words_result");

		for (Object var : array) {
			JSONObject tmp = (JSONObject) var;

			final String words = tmp.getString("words");

			if (words.equals("已无更多")) {
				System.out.println("jjjjjjjjjjjjjjjjjjjjjjjjjjjj");
			}
		}
	}

	@Test
	public void testOcrByChi() throws Exception {

//		TesseractOCRParser.imageOcr("/usr/local/java-workplace/task-android-automator/data/1426002855.jpg", false);
		List<OCRParser.TouchableTextArea> textAreas = TesseractOCRParser.getInstance().imageOcr("/usr/local/java-workplace/task-android-automator/data/1426002855.jpg", false);
		JSONObject jsonObject = null;

		System.out.println(jsonObject);
	}*/
}
