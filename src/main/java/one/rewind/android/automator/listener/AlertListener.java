package one.rewind.android.automator.listener;

import io.appium.java_client.events.api.general.AlertEventListener;
import org.openqa.selenium.Alert;
import org.openqa.selenium.WebDriver;

/**
 * Create By 2018/10/29
 * Description
 */
public class AlertListener implements AlertEventListener {

    @Override
    public void beforeAlertAccept(WebDriver driver, Alert alert) {

    }

    @Override
    public void afterAlertAccept(WebDriver driver, Alert alert) {
        System.out.println("one.rewind.android.automator.test.listener.AlertListener.beforeAlertAccept");
        System.out.println("弹框出现的内容是：" + alert.getText());
    }

    @Override
    public void afterAlertDismiss(WebDriver driver, Alert alert) {

    }

    @Override
    public void beforeAlertDismiss(WebDriver driver, Alert alert) {

    }

    @Override
    public void beforeAlertSendKeys(WebDriver driver, Alert alert, String keys) {

    }

    @Override
    public void afterAlertSendKeys(WebDriver driver, Alert alert, String keys) {

    }
}
