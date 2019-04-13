package com.realview.holo.call;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import com.realview.commonlibrary.audiorecord.XAudioRecordMgr;
import com.realview.commonlibrary.server.manager.UserManager;
import com.tencent.bugly.Bugly;
import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.crashreport.CrashReport;


/**
 * Created by admin on 2019/1/28.
 */

public class HoloCallApp extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        Bugly.init(getApplicationContext(), "3f45ddcfa3", false);
        Bugly.setAppChannel(getApplicationContext(), "HoloCall");
        Beta.autoInit = true;
        Beta.autoCheckUpgrade = true;
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
