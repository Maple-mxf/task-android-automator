package one.rewind.android.automator.adapter;

import one.rewind.android.automator.AndroidDevice;
import io.appium.java_client.android.AndroidDriver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Adapter {

	static final Logger logger = LogManager.getLogger(Adapter.class.getName());

	AndroidDevice device;
	AndroidDriver driver;
	String udid;

	public Adapter(AndroidDevice device) {
		this.device = device;
		this.driver = device.driver;
		this.udid = device.udid;
	}
}
