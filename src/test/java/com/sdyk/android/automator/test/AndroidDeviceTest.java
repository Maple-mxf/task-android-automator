package com.sdyk.android.automator.test;

import com.sdyk.android.automator.adapter.ContactsAdapter;
import com.sdyk.android.automator.util.AppInfo;
import com.sdyk.android.automator.AndroidDevice;
import net.lightbody.bmp.filters.RequestFilter;
import net.lightbody.bmp.filters.ResponseFilter;
import org.junit.Before;
import org.junit.Test;

import static java.awt.SystemColor.info;

public class AndroidDeviceTest {

	String udid = "ZX1G323GNB";
	int appiumPort = 47454;
	int localProxyPort = 48454;
	AndroidDevice device;

	/**
	 * 初始化设备
	 * @throws Exception
	 */
	@Before
	public void setup() throws Exception {

		device = new AndroidDevice(udid, appiumPort);

		// 从AppInfo中选择需要启动的程序
		AppInfo appInfo = AppInfo.get(AppInfo.Defaults.Contacts);

		device.initAppiumServiceAndDriver(appInfo);

		Thread.sleep(3000);
	}

	/**
	 * 测试安装APK
	 */
	@Test
	public void testInstallApk() {

		System.out.println(device.getHeight());
		System.out.println(device.getWidth());

		String apkPath = "com.facebook.katana_180.0.0.35.82_free-www.apkhere.com.apk";
		device.installApk(apkPath);
		device.listApps();

		// TODO 测试删除APK
	}

	/**
	 * 测试通讯录功能
	 * @throws InterruptedException
	 */
	@Test
	public void testAddContact() throws InterruptedException {

		String name = "name";
		String number = "123456";
		String filePath = "newFriend.txt";

		AppInfo info = AppInfo.get(AppInfo.Defaults.Contacts);
		device.startActivity(info);

		ContactsAdapter ca = new ContactsAdapter(device);
		ca.clearContacts();
		ca.addContact(name, number);
		ca.addContactsFromFile(filePath);
		ca.deleteOneContact(name);
	}

	@Test
	public void testProxyFilters() {

		device.startProxy(localProxyPort);
		device.setupWifiProxy();

		/**
		 * TODO 请求过滤器
		 */
		RequestFilter requestFilter = (request, contents, messageInfo) -> {

			//logger.util(messageInfo.getOriginalUrl());
			//logger.util(contents.getTextContents());
			return null;
		};


		/**
		 * TODO 返回过滤器
		 */
		ResponseFilter responseFilter = (response, contents, messageInfo) -> {

			//logger.util(messageInfo.getOriginalUrl());
			//logger.util(contents.getTextContents());
		};

		device.setProxyRequestFilter(requestFilter);
		device.setProxyResponseFilter(responseFilter);

	}
}

