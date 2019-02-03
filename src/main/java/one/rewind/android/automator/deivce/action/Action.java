package one.rewind.android.automator.deivce.action;

import one.rewind.android.automator.deivce.AndroidDevice;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Callable;

/**
 * @author scisaga@gmail.com
 * @date 2019/2/4
 */
public abstract class Action implements Callable<Boolean> {

	static final Logger logger = LogManager.getLogger(AndroidDevice.class.getName());

	AndroidDevice device;

	String udid;

	public Action(AndroidDevice device) {
		this.device = device;
		this.udid = device.udid;
	}
}
