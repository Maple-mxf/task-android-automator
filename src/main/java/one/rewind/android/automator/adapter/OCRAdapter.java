package one.rewind.android.automator.adapter;

import one.rewind.android.automator.util.ImageUtil;

import java.awt.image.BufferedImage;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public interface OCRAdapter {

	/**
	 * 头部裁剪距离  在分析完坐标之后需要将裁剪的距离加上去,以保证像素的正确性
	 */
	int CROP_TOP = 190;

	/**
	 * 右侧裁剪距离  右侧裁剪不需要附加坐标
	 */
	int CROP_RIGHT = 1056;

	/**
	 * 图片高度
	 */
	int IMAGE_HEIGHT = 2550;

	/**
	 * 图片宽度  1920-190
	 */
	int IMAGE_WIDTH = 1080;

	/**
	 * 裁剪图片
	 *
	 * @param bufferedImage bin
	 * @return bout
	 */
	static BufferedImage cropEssayListImage(BufferedImage bufferedImage) {
		return ImageUtil.cropImage(bufferedImage, 0, 0, CROP_RIGHT, IMAGE_HEIGHT);
	}

	/**
	 * 解析获取到文章的真实标题
	 */
	static void realTitle() {

	}
}
