package one.rewind.android.automator.test.adapter;

import one.rewind.android.automator.deivce.AndroidDeviceManager;
import one.rewind.android.automator.account.Account;
import one.rewind.android.automator.adapter.wechat.WeChatAdapter;
import one.rewind.android.automator.adapter.wechat.task.GetSelfSubscribeMediaTask;
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

/**
 * @author maxuefeng [m17793873123@163.com]
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
    public void testGetSelfSubscribePublicAccountTest() throws InterruptedException, AndroidException.NoAvailableDeviceException, TaskException.IllegalParamException, AccountException.AccountNotLoad {

        TaskHolder holder = new TaskHolder(StringUtil.uuid(), WeChatAdapter.class.getName(), GetSelfSubscribeMediaTask.class.getName());

        Task task = TaskFactory.getInstance().generateTask(holder);

		AndroidDeviceManager.getInstance().submit(task);

        Thread.sleep(10000000);

    }

}
