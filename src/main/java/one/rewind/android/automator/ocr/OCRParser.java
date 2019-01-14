package one.rewind.android.automator.ocr;

import one.rewind.android.automator.util.ImageUtil;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import one.rewind.txt.DateFormatUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public interface OCRParser {

	Logger logger = LogManager.getLogger(OCRParser.class.getName());

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
	static BufferedImage cropImage(BufferedImage bufferedImage) {
		return ImageUtil.cropImage(bufferedImage, 0, 0, CROP_RIGHT, IMAGE_HEIGHT);
	}

	/**
	 * 解析图片中的文本框，并获取可点击的坐标
	 *
	 * @param filePath
	 * @param crop
	 * @return
	 */
	List<TouchableTextArea> getTextBlockArea(String filePath, boolean crop) throws Exception;

	/**
	 * 自定义点坐标类型
	 */
	class Rectangle implements JSONable<Rectangle> {

		public int left = 0;
		public int top = 0;
		public int width = 0;
		public int height = 0;

		public Rectangle(int left, int top, int width, int height) {
			this.left = left;
			this.top = top;
			this.width = width;
			this.height = height;
		}


		@Override
		public String toJSON() {
			return JSON.toJson(this);
		}
	}

	/**
	 * 自定义可点击文本框类型
	 */
	class TouchableTextArea extends Rectangle {

		public String content;

		public Date date;

		/**
		 * @param content
		 * @param rectangle
		 */
		public TouchableTextArea(String content, Rectangle rectangle) throws ParseException {
			super(rectangle.left, rectangle.top, rectangle.width, rectangle.height);

			if (content.matches("\\d{2,4}年\\d{1,2}月\\d{1,2}[日曰]]")) {
				date = DateFormatUtil.parseTime(content.replaceAll("曰", "日"));
			} else {
				this.content = content;
			}
		}

		/**
		 * 两个矩形合并成一个新矩形
		 *
		 * @param area
		 * @return
		 */
		public TouchableTextArea add(TouchableTextArea area) throws ParseException {

			if (area.content.matches("\\d{2,4}年\\d{1,2}月\\d{1,2}[日曰]]")) {
				date = DateFormatUtil.parseTime(area.content.replaceAll("曰", "日"));
			} else {
				this.content = this.content + " " + area.content;
			}
			// 第一个矩形右界
			int right = left + width;
			// 第二个矩形右界
			int right_ = area.left + area.width;

			// 第一个矩形下界
			int bottom = top + height;
			// 第二个矩形下界
			int bottom_ = area.top + area.height;

			left = left < area.left ? left : area.left;
			top = top < area.top ? top : area.top;

			width = right > right_ ? right - left : right_ - left;
			height = bottom > bottom_ ? bottom - top : bottom_ - top;

			return this;
		}

		@Override
		public String toJSON() {
			return JSON.toJson(this);
		}
	}
}
