package one.rewind.android.automator.adapter;

import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidElement;
import io.appium.java_client.touch.offset.PointOption;
import one.rewind.android.automator.deivce.AndroidDevice;
import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AdapterException;
import one.rewind.android.automator.exception.AndroidException;
import one.rewind.android.automator.util.ShellUtil;
import one.rewind.db.exception.DBInitException;
import one.rewind.util.FileUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.sql.SQLException;
import java.util.List;

public class ContactsAdapter extends Adapter {


	public ContactsAdapter(AndroidDevice androidDevice) throws AndroidException.IllegalStatusException {
		super(androidDevice);
		this.appInfo = new AppInfo(
				"com.google.android.contacts",
				"com.android.contacts.activities.PeopleActivity");
	}

	@Override
	public void checkAccount() throws InterruptedException, AdapterException.LoginScriptError, AccountException.Broken {

	}

	@Override
	public void switchAccount(Account.Status... statuses) throws InterruptedException, AdapterException.LoginScriptError, AccountException.NoAvailableAccount, DBInitException, SQLException {

	}

	public void init() throws InterruptedException, AdapterException.OperationException, AccountException.NoAvailableAccount {

	}

	/**
	 * 清空通讯录
	 * 该方法是利用adb命令中的clear，同时可以以应用程序的包名为参数，修改为重置app的方法
	 */
	public void clearContacts() {
		String commandStr = "adb -s " + device.udid + " shell pm clear com.android.providers.contacts";
		ShellUtil.exeCmd(commandStr);
	}

	/**
	 * 添加指定姓名和电话的单个联系人
	 * 通过+按钮，输入姓名和电话号码后返回
	 *
	 * @param name   姓名
	 * @param number 电话
	 * @throws InterruptedException 操作中断异常
	 */
	public void addContact(String name, String number) throws InterruptedException {

		WebElement addButton = device.driver.findElementByAccessibilityId("添加新联系人");

		// 点击添加新联系人
		addButton.click();

		Thread.sleep(500);
		device.driver.findElement(By.xpath("//android.widget.EditText[contains(@text,'姓名')]")).sendKeys(name);
		device.driver.findElement(By.xpath("//android.widget.EditText[contains(@text,'电话')]")).sendKeys(number);

		// 点击保存
		device.driver.findElementById("com.android.contacts:id/menu_save").click();
		Thread.sleep(1500);

		// 安卓机器的返回键
		//driver.pressKeyCode(4);
		device.driver.navigate().back();
		Thread.sleep(500);
	}

	/**
	 * 通过txt文件批量导入联系人
	 * 通过通讯录界面的+按钮，加入联系人后返回，循环添加
	 *
	 * @param filePath 通讯录文件
	 * @throws InterruptedException 中断异常
	 */
	public void addContactsFromFile(String filePath) throws InterruptedException {

		// adb 启动 contacts

		String src = FileUtil.readFileByLines(filePath);

		for (String contact : src.split("\\n|\\r\\n")) {

			String[] token = contact.split("\\t");

			WebElement addButton = device.driver.findElementByAccessibilityId("添加新联系人");
			addButton.click();//点击添加新联系人

			Thread.sleep(500);

			device.driver.findElement(By.xpath("//android.widget.EditText[contains(@text,'姓名')]")).sendKeys(token[0]);
			device.driver.findElement(By.xpath("//android.widget.EditText[contains(@text,'电话')]")).sendKeys(token[1]);

			Thread.sleep(500);
			device.driver.findElementById("com.android.contacts:id/menu_save").click();//点击保存

			Thread.sleep(500);
			//driver.pressKeyCode(4);//安卓机器的返回键

			device.driver.navigate().back();//安卓机器的返回键
			Thread.sleep(500);

		}
	}

	/**
	 * 删除指定姓名的联系人
	 *
	 * @param name 联系人姓名
	 * @throws InterruptedException 中断异常
	 */
	public void deleteOneContact(String name) throws InterruptedException {

		for (int i = 0; i < 3; i++) {

			List<AndroidElement> lis = device.driver.findElements(By.xpath("//android.widget.TextView[contains(@text,nick)]"));

			if (lis.size() != 0) {

				device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,nick)]")).click();
				Thread.sleep(700);

				device.driver.findElementByAccessibilityId("更多选项").click();
				Thread.sleep(700);

				device.driver.findElement(By.xpath("//android.widget.TextView[contains(@text,'删除')]")).click();
				Thread.sleep(700);

				device.driver.findElement(By.xpath("//android.widget.Button[contains(@text,'删除'"));
				Thread.sleep(200);
			}

			TouchAction action = new TouchAction(device.driver).press(PointOption.point(700, 2360))
					.waitAction()
					.moveTo(PointOption.point(700, 540))
					.release();

			action.perform();

			Thread.sleep(300);
		}
	}
}
