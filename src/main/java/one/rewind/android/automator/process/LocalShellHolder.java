package one.rewind.android.automator.process;

import java.io.*;
import java.util.Scanner;

public class LocalShellHolder {

	Process p;

	String cmd = "\"C:\\Program Files\\Genymobile\\Genymotion\\genyshell.exe\"";

	OutputStream stdin;
	InputStream stdout;

	public LocalShellHolder() {

	}

	public static void main(String[] args) {

		Process p;

		String cmd = "\"C:\\Program Files\\Genymobile\\Genymotion\\genyshell.exe\"";

		Scanner console = new Scanner(System.in);
		while(console.hasNextLine()) {
			System.out.println(console.nextLine());
			try {
				// 执行命令
				p = Runtime.getRuntime().exec(String.valueOf(console));
				// 取得命令结果的输出流
				InputStream fis = p.getInputStream();
				// 用一个读输出流类去读
				InputStreamReader isr = new InputStreamReader(fis);
				// 用缓冲器读行
				BufferedReader br = new BufferedReader(isr);

				String line = null;
				// 直到读完为止
				while ((line = br.readLine()) != null) {
					System.out.println(line);
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}


	}
}
