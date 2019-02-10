package one.rewind.android.automator.adapter.wechat.test;

import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.adapter.wechat.task.GetMediaEssaysTask;
import one.rewind.android.automator.adapter.wechat.task.GetSelfSubscribeMediaTask;
import one.rewind.android.automator.adapter.wechat.task.SubscribeMediaTask;
import one.rewind.android.automator.adapter.wechat.task.UnsubscribeMediaTask;
import one.rewind.android.automator.deivce.AndroidDeviceManager;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AndroidException;
import one.rewind.android.automator.exception.TaskException;
import one.rewind.android.automator.task.Task;
import one.rewind.android.automator.task.TaskFactory;
import one.rewind.android.automator.task.TaskHolder;
import one.rewind.db.exception.DBInitException;
import one.rewind.db.model.Model;
import one.rewind.txt.StringUtil;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Arrays;

/**
 * @author scisaga@gmail.com
 * @date 2019/2/10
 */
public class WeChatAdapterTaskTest {

	@Before
	public void initAndroidDeviceManager() throws Exception {

		Model.getAll(Account.class).stream().forEach(a-> {
			a.occupied = false;
			try {
				a.update();
			} catch (DBInitException | SQLException e) {
				e.printStackTrace();
			}
		});

		AndroidDeviceManager.getInstance().detectDevices();
	}

	@Test
	public void testGetSelfSubscribePublicAccountTest() throws InterruptedException, AndroidException.NoAvailableDevice, TaskException.IllegalParameters, AccountException.AccountNotLoad {

		TaskHolder holder = new TaskHolder(StringUtil.uuid(), WeChatAdapter.class.getName(), GetSelfSubscribeMediaTask.class.getName());

		Task task = TaskFactory.getInstance().generateTask(holder);

		AndroidDeviceManager.getInstance().submit(task);

		Thread.sleep(10000000);

	}

	@Test
	public void testSubscribe() throws InterruptedException, AndroidException.NoAvailableDevice, TaskException.IllegalParameters, AccountException.AccountNotLoad {

		TaskHolder holder = new TaskHolder(StringUtil.uuid(), WeChatAdapter.class.getName(), SubscribeMediaTask.class.getName(), Arrays.asList("拍拍贷"));

		Task task = TaskFactory.getInstance().generateTask(holder);

		AndroidDeviceManager.getInstance().submit(task);

		Thread.sleep(10000000);

	}

	@Test
	public void testUnsubscribe() throws InterruptedException, AndroidException.NoAvailableDevice, TaskException.IllegalParameters, AccountException.AccountNotLoad {

		TaskHolder holder = new TaskHolder(StringUtil.uuid(), WeChatAdapter.class.getName(), UnsubscribeMediaTask.class.getName(), Arrays.asList("拍拍贷"));

		Task task = TaskFactory.getInstance().generateTask(holder);

		AndroidDeviceManager.getInstance().submit(task);

		Thread.sleep(10000000);
	}

	@Test
	public void testSubscribeAndGetEssays() throws InterruptedException, AndroidException.NoAvailableDevice, TaskException.IllegalParameters, AccountException.AccountNotLoad {

		TaskHolder holder = new TaskHolder(StringUtil.uuid(), WeChatAdapter.class.getName(), SubscribeMediaTask.class.getName(), Arrays.asList("拍拍贷"));

		Task task = TaskFactory.getInstance().generateTask(holder);

		//AndroidDeviceManager.getInstance().submit(task);

		AndroidDeviceManager.getInstance().submit(
				TaskFactory.getInstance().generateTask(
						new TaskHolder(StringUtil.uuid(), WeChatAdapter.class.getName(), GetMediaEssaysTask.class.getName(), Arrays.asList("拍拍贷"))));

		Thread.sleep(10000000);
	}
}
