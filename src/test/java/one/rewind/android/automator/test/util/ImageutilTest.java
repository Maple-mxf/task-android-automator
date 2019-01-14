package one.rewind.android.automator.test.util;

import one.rewind.android.automator.ocr.OCRParser;
import one.rewind.android.automator.util.ImageUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class ImageutilTest {


	public static void main(String[] args) throws IOException {
		File inImage = new File("/usr/local/java-workplace/wechat-android-automator/data/1.png");
		BufferedImage bufferedImage = OCRParser.cropImage(ImageIO.read(inImage));
		final BufferedImage result = ImageUtil.cropImage(bufferedImage, 0, OCRParser.CROP_TOP, 770, 1920);
		ImageIO.write(result, "png", new File("/usr/local/java-workplace/wechat-android-automator/data/2.png"));
	}
}
