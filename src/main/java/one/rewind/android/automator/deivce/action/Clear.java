package one.rewind.android.automator.deivce.action;

import one.rewind.android.automator.deivce.AndroidDevice;
import one.rewind.android.automator.deivce.AndroidUtil;

import java.io.IOException;

/**
 * @author scisaga@gmail.com
 * @date 2019/2/4
 */
public class Clear extends Action {

	public Clear(AndroidDevice device) {
		super(device);
	}

	@Override
	public Boolean call() throws IOException {

		logger.info("[{}] start to clean...", udid);

		AndroidUtil.killAllBackgroundProcess(udid);
		AndroidUtil.clearCacheLog(udid);
		AndroidUtil.clearAllLog(udid);

		device.flags.add(AndroidDevice.Flag.Cleaned);

		logger.info("[{}] cleaned", udid);

		return true;
	}
}
