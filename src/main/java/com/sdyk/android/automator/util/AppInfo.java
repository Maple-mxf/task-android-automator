package com.sdyk.android.automator.util;

import one.rewind.json.JSON;
import one.rewind.json.JSONable;

import java.util.HashMap;
import java.util.Map;

import static com.sdyk.android.automator.util.AppInfo.Defaults.*;

/**
 * 在本类中定义应用程序及其包名和appActivity，在其他类的initAppiumDriver中可以通过更改应用程序名进行初始化的设置修改
 */
public class AppInfo implements JSONable<AppInfo> {

    public static Map<Defaults, AppInfo> apps = new HashMap<>();

    public static enum Defaults {
        WeChat,
        Contacts,
        IndexPage, //主页面，相当于什么也不做，只是初始化AppiumDriver
        DingDing
    }

    static {
        apps.put(WeChat, new AppInfo("com.tencent.mm", ".ui.LauncherUI"));
        apps.put(Contacts, new AppInfo("com.google.android.contacts", "com.android.contacts.activities.PeopleActivity"));
        apps.put(IndexPage, new AppInfo("com.google.android.googlequicksearchbox", "com.google.android.launcher.GEL"));
        apps.put(DingDing, new AppInfo("com.alibaba.android.rimet", "com.alibaba.android.rimet.biz.SplashActivity"));
    }

    public static AppInfo get(Defaults name) {
        return apps.get(name);
    }

    public String appPackage;
    public String appActivity;

    public AppInfo(String appPackage, String appActivity) {
        this.appPackage = appPackage;
        this.appActivity = appActivity;
    }

    @Override
    public String toJSON() {
        return JSON.toJson(this);
    }
}
