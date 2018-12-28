package one.rewind.android.automator.callback;

import one.rewind.android.automator.AndroidDevice;
import one.rewind.android.automator.adapter.Adapter;

/**
 * @author maxuefeng [m17793873123@163.com]
 */
@FunctionalInterface
public interface DeviceCallback {

	void run(AndroidDevice d, Adapter a);
}
