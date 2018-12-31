package one.rewind.android.automator.util;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

/**
 * @author maxuefeng[m17793873123@163.com]
 */
public class FileUtil {

	/**
	 * 读取文件内容，作为字符串返回
	 */
	public static String readFileAsString(String filePath) throws IOException {
		File file = new File(filePath);
		if (!file.exists()) {
			throw new FileNotFoundException(filePath);
		}

		if (file.length() > 1024 * 1024 * 1024) {
			throw new IOException("File is too large");
		}

		StringBuilder sb = new StringBuilder((int) (file.length()));
		// 创建字节输入流
		FileInputStream fis = new FileInputStream(filePath);
		// 创建一个长度为10240的Buffer
		byte[] bbuf = new byte[10240];
		// 用于保存实际读取的字节数
		int hasRead = 0;
		while ((hasRead = fis.read(bbuf)) > 0) {
			sb.append(new String(bbuf, 0, hasRead));
		}
		fis.close();
		return sb.toString();
	}

	/**
	 * 根据文件路径读取byte[] 数组
	 */
	public static byte[] readFileByBytes(String filePath) throws IOException {
		File file = new File(filePath);
		if (!file.exists()) {
			throw new FileNotFoundException(filePath);
		} else {
			ByteArrayOutputStream bos = new ByteArrayOutputStream((int) file.length());
			BufferedInputStream in = null;

			try {
				in = new BufferedInputStream(new FileInputStream(file));
				short bufSize = 1024;
				byte[] buffer = new byte[bufSize];
				int len1;
				while (-1 != (len1 = in.read(buffer, 0, bufSize))) {
					bos.write(buffer, 0, len1);
				}

				byte[] var7 = bos.toByteArray();
				return var7;
			} finally {
				try {
					if (in != null) {
						in.close();
					}
				} catch (IOException var14) {
					var14.printStackTrace();
				}

				bos.close();
			}
		}
	}

	/**
	 * 删除一个文件
	 *
	 * @param filePath
	 * @return
	 */
	public static boolean deleteFile(String filePath) {
		File file = new File(filePath);
		return file.delete();
	}

	/**
	 * 将某个文件的所有内容读出来拼接成一个字符串
	 *
	 * @return all line but It is a string
	 * @see com.google.common.io.Files#readLines(java.io.File, java.nio.charset.Charset)
	 * @see java.nio.file.Files#readAllLines(Path)
	 * @see java.nio.file.Files#readAllBytes(Path)
	 */
	public static String allLines(String path) throws IOException {

		File file = new File(path);

		List<String> lines = com.google.common.io.Files.readLines(file, Charset.forName("UTF-8"));

		StringBuilder rs = new StringBuilder();

		lines.forEach(t -> rs.append(t.trim()));

		return rs.toString();
	}
}
