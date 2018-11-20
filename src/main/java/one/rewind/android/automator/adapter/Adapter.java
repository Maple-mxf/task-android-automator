package one.rewind.android.automator.adapter;

import io.appium.java_client.android.AndroidDriver;
import one.rewind.android.automator.AndroidDevice;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

abstract class Adapter {

    static final Logger logger = LogManager.getLogger(Adapter.class.getName());

    AndroidDevice device;
    AndroidDriver driver;
    String udid;

    Adapter(AndroidDevice device) {
        this.device = device;
        this.driver = device.driver;
        this.udid = device.udid;
    }

    public AndroidDevice getDevice() {
        return device;
    }

    public void setDevice(AndroidDevice device) {
        this.device = device;
    }
}
