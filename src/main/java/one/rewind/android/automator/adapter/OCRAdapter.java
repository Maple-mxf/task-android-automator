package one.rewind.android.automator.adapter;

import one.rewind.android.automator.util.ImageUtil;

import java.awt.image.BufferedImage;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public interface OCRAdapter {

	static BufferedImage cropEssayListImage(BufferedImage bufferedImage) {
		// 头部的数据是不可以裁剪的   会影响坐标的准确度
		return ImageUtil.cropImage(bufferedImage, 0, 0, 770, 1918);
	}
}
