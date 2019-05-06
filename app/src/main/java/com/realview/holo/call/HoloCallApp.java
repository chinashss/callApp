package com.realview.holo.call;

import android.app.ActivityManager;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import com.holoview.usbcameralib.UVCCameraHelper;
import com.realview.commonlibrary.server.manager.UserManager;
import com.serenegiant.usb.USBMonitor;
import com.tencent.bugly.Bugly;
import com.tencent.bugly.beta.Beta;


/**
 * Created by admin on 2019/1/28.
 */

public class HoloCallApp extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        Beta.upgradeDialogLayoutId = R.layout.activity_bugly_upgrade;
        Beta.tipsDialogLayoutId = R.layout.activity_bugly_upgrade;
        Beta.autoInit = true;
        Beta.autoCheckUpgrade = true;
        Bugly.init(getApplicationContext(), "3f45ddcfa3", false);
        Bugly.setAppChannel(getApplicationContext(), "HoloCall");
        UserManager.init(this);

    }


    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    /**
     * 分割 Dex 支持
     *
     * @param base
     */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static boolean isActivityTop(Class cls, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        String name = manager.getRunningTasks(1).get(0).topActivity.getClassName();
        return name.equals(cls.getName());
    }


}
