package one.rewind.android.automator.test.util;

import org.junit.Test;
import org.opencv.core.Mat;
import org.opencv.utils.Converters;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 描述：
 * 1、利用Java的文件操作将文件读入（字节流）；
 * <p>
 * 2、将字节流转换为数组（或Vector）；
 * <p>
 * 3、利用org.opencv.utils包中的Converters类将数组（或Vector）转换为Mat对象。
 * <p>
 * 作者：MaXFeng
 * 时间：2018/10/14
 */
public class OpencvTest {


	@Test
	public void testOpencvAPI() throws IOException {
		File file = new File("C:\\ubuntu\\workspace\\wechat-android-automator\\screen\\accounts.png");
		byte[] byteFromFile = getByteFromFile(file);
		List<Byte> byteList = new ArrayList<>();
		for (int i = 0; i < byteFromFile.length; i++) {
			byteList.add(byteFromFile[i]);
		}
		Mat mat = Converters.vector_char_to_Mat(byteList);
		System.out.println(mat);
	}

	public static void main(String[] args) {
		String s = "2018年10月15日";
		if (s.contains("年")){
			System.out.println("包含日期");
		}
	}

	public static byte[] getByteFromFile(File file) throws IOException {
		InputStream in = new FileInputStream(file);
		long length = file.length();
		byte[] fileBytes = new byte[(int) length];
		int offset = 0;
		int numRead = 0;
		while (offset < fileBytes.length && (numRead = (int) in.read(fileBytes, offset, fileBytes.length - offset)) >= 0) {
			offset += numRead;
		}
		if (offset < fileBytes.length) {
			throw new IOException("数据没有读取完毕");
		}
		in.close();
		return fileBytes;
	}
}
