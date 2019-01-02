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

		File image = new File("/usr/local/java-workplace/wechat-android-automator/data/4068.jpg");

		// 裁剪图片
//		BufferedImage bufferedImage = BaiDuOCRAdapter.cropEssayListImage(ImageIO.read(image));
//
//		// 覆盖原有图片  TODO
//		ImageIO.write(bufferedImage, "png", new File(image.getAbsolutePath()));

		// 将图片进行灰度化  为了让tesseract识别出灰色文字
//		ImageUtil.grayImage();

		// 执行命令  并且得到hocr的结果(是html代码)
//		String document = BaiDuOCRAdapter.imageOcrOfTesseractByPoint(image);
//		// 识别结果
//		JSONObject result = BaiDuOCRAdapter.jsoupParseHtml2JSON(document);
//
//		System.out.println(result);
	}

	@Test
	public void testChangeImageColor1() {
		BufferedImage img = null;
		File f;
		try {
			f = new File("/usr/local/java-workplace/wechat-android-automator/data/wxIn.jpg");
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
			f = new File("/usr/local/java-workplace/wechat-android-automator/data/wxOn.jpg");
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
			f = new File("/usr/local/java-workplace/wechat-android-automator/data/wxIn.jpg");
			ImageIO.write(img, "png", f);
		} catch (IOException e) {
			System.out.println("Error: " + e);
		}
	}

	@Deprecated
	public static void binaryImage() throws IOException {
		File file = new File("/usr/local/java-workplace/wechat-android-automator/data/wxOn.jpg");
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

		File newFile = new File("/usr/local/java-workplace/wechat-android-automator/data/4028.jpg");
		ImageIO.write(grayImage, "jpg", newFile);
	}

	public static void grayImage() throws IOException {
		File file = new File("/usr/local/java-workplace/wechat-android-automator/data/wxOn.jpg");
		BufferedImage image = ImageIO.read(file);

		int width = image.getWidth();
		int height = image.getHeight();

		BufferedImage grayImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);//重点，技巧在这个参数BufferedImage.TYPE_BYTE_GRAY
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				int rgb = image.getRGB(i, j);
				grayImage.setRGB(i, j, rgb);
			}
		}

		File newFile = new File("/usr/local/java-workplace/wechat-android-automator/data/kkkk.png");
		ImageIO.write(grayImage, "png", newFile);
	}

	public static void main(String[] args) throws IOException {
		grayImage();
	}
}
