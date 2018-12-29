package one.rewind.android.automator.test.file;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
public class ImgUtilTest {


	public static void main(String[] args) throws IOException {

		File newfile = new File("/usr/local/java-workplace/wechat-android-automator/data/1.jpeg");
		BufferedImage bufferedimage = ImageIO.read(newfile);

//		bufferedimage = ImgUtils.cropImage(bufferedimage, 0, 1065, 56, 1918);
		bufferedimage = ImgUtils.cropImage(bufferedimage, 0, 56, 770, 1918);

		ImageIO.write(bufferedimage, "jpg", new File("/usr/local/java-workplace/wechat-android-automator/data/1.jpeg"));    //输出裁剪图片
	}
}
