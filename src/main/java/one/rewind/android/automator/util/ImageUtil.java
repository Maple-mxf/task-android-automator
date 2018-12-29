package one.rewind.android.automator.util;

import java.awt.image.BufferedImage;

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
}
