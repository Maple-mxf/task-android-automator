package one.rewind.android.automator.adapter;

import one.rewind.android.automator.AndroidDevice;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Adapter {

    static final Logger logger = LogManager.getLogger(Adapter.class.getName());

    AndroidDevice device;
    String udid;

    Adapter(AndroidDevice device) {
        this.device = device;
        this.udid = device.udid;
    }

    public AndroidDevice getDevice() {
        return device;
    }

    public void setDevice(AndroidDevice device) {
        this.device = device;
    }
}
