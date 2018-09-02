package com.sdyk.android.automator.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * 进程构造器，用于与GenoShell通讯
 */
public class ProcessBuilder {

	static class CommandThread extends Thread{
		PrintWriter writer;
		BufferedReader br = null;
		CommandThread(PrintWriter writer){
			this.writer = writer;
			br = new BufferedReader(new InputStreamReader(System.in));
			this.setDaemon(true);
		}

		@Override
		public void run() {
			try {
				String cmd = null;
				while((cmd = br.readLine()) != null){
					writer.println(cmd);
					writer.flush();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 运行后可以在下面的界面与命令行进行交互，输入exit回车后退出
	 * @param args
	 */
	public static void main(String[] args) {

		try {
			Process process = Runtime.getRuntime().exec("cmd");  //cmd /c start 可以打开另一个窗口
			PrintWriter writer = new PrintWriter(process.getOutputStream());
			new CommandThread(writer).start();
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));

			String s = null;
			while ((s = br.readLine()) != null) {
				System.out.println(s);
			}
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
