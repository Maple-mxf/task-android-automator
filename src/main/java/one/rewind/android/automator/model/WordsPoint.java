package one.rewind.android.automator.model;

/**
 * 描述： 文字坐标模型
 */
public class WordsPoint {

	public String words;

	public int top;

	public int left;

	public int width;

	public int height;

	public WordsPoint(int top, int left, int width, int height, String words) {
		this.top = top;
		this.left = left;
		this.width = width;
		this.height = height;
		this.words = words;
	}


	@Override
	public String toString() {
		return "WordsPoint{" +
				"top=" + top +
				", left=" + left +
				", width=" + width +
				", height=" + height +
				'}';
	}
}
