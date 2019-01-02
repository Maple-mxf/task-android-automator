package one.rewind.android.automator.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class ImageUtil {
	/**
	 * 裁剪图片方法
	 *
	 * @param bufferedImage 图像源
	 * @param startX        裁剪开始x坐标  0-1065
	 * @param startY        裁剪开始y坐标  56-1918
	 * @param endX          裁剪结束x坐标
	 * @param endY          裁剪结束y坐标
	 * @return BufferedImage
	 */
	public static BufferedImage cropImage(BufferedImage bufferedImage, int startX, int startY, int endX, int endY) {
		int width = bufferedImage.getWidth();
		int height = bufferedImage.getHeight();
		if (startX == -1) {
			startX = 0;
		}
		if (startY == -1) {
			startY = 0;
		}
		if (endX == -1) {
			endX = width - 1;
		}
		if (endY == -1) {
			endY = height - 1;
		}
		BufferedImage result = new BufferedImage(endX - startX, endY - startY, 4);
		for (int x = startX; x < endX; ++x) {
			for (int y = startY; y < endY; ++y) {
				int rgb = bufferedImage.getRGB(x, y);
				result.setRGB(x - startX, y - startY, rgb);
			}
		}
		return result;
	}

	/**
	 * 将图片进行灰度化 为了方便tesseract识别
	 *
	 * @param inPath     输入文件
	 * @param outPath    输出文件
	 * @param formatName 图片文件格式  截图必须为png 其他一般为jpg或者jpeg
	 * @throws IOException 读取文件异常
	 */
	public static void grayImage(String inPath, String outPath, String formatName) throws IOException {

		File file = new File(inPath);

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

		File newFile = new File(outPath);

		ImageIO.write(grayImage, formatName, newFile);
	}
}
