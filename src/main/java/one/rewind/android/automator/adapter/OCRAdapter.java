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
	int CROP_RIGHT = 770;

	/**
	 * 图片高度
	 */
	int IMAGE_HEIGHT = 1920;

	/**
	 * 图片宽度
	 */
	int IMAGE_WIDTH = 1080;

	static BufferedImage cropEssayListImage(BufferedImage bufferedImage) {
		// 头部的数据是不可以裁剪的   会影响坐标的准确度
		return ImageUtil.cropImage(bufferedImage, 0, CROP_TOP, 770, 1920);
	}
	///usr/local/wechat-android-automator-1.0-SNAPSHOT/screen/20069cba-df67-4033-a4fa-99d7e1c7cc4a.png
}
