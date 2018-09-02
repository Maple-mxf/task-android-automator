package com.sdyk.android.automator.genomotion;

import com.sdyk.android.automator.util.ShellUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class GenymotionManager {

	public GenymotionManager(){

	}

	/**
	 * 启动genymotion.exe
	 */
	public static void startGenyMotion(){

		ShellUtil.exeCall("C:\\Program Files\\Genymobile\\Genymotion\\genymotion.exe");
	}
}
