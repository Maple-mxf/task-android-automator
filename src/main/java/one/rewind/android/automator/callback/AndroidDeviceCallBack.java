package one.rewind.android.automator.callback;

import one.rewind.android.automator.deivce.AndroidDevice;
import one.rewind.android.automator.exception.AccountException;
import one.rewind.android.automator.exception.AndroidException;
import one.rewind.android.automator.exception.TaskException;
import one.rewind.db.exception.DBInitException;

import java.sql.SQLException;

/**
 * @author scisaga@gmail.com
 * @date 2019/1/18
 */
public interface AndroidDeviceCallBack {

	void call(AndroidDevice ad) throws
			AndroidException.IllegalStatusException,
			AndroidException.NoSuitableAdapter,
			AndroidException.NoAvailableDeviceException,
			AccountException.AccountNotLoad,
			AccountException.Broken,
			TaskException.IllegalParamException,
			InterruptedException,
			SQLException,
			DBInitException;


	interface InitCallBack {
		void call(AndroidDevice ad) throws
				InterruptedException,
				SQLException,
				DBInitException;
	}
}
