package com.realview.holo.call;

import android.app.ActivityManager;
import android.content.Context;
import android.support.multidex.MultiDexApplication;

import com.realview.commonlibrary.server.manager.UserManager;
import com.realview.holo.call.activity.MainActivity;
import com.tencent.bugly.Bugly;
import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.beta.UpgradeInfo;
import com.tencent.bugly.beta.upgrade.UpgradeListener;


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
        Beta.autoCheckUpgrade = false;
        Beta.canShowUpgradeActs.add(MainActivity.class);
//        Beta.upgradeListener = new UpgradeListener() {
//            @Override
//            public void onUpgrade(int i, UpgradeInfo upgradeInfo, boolean b, boolean b1) {
//                if (upgradeInfo != null) {
//
//                }else{
//
//                }
//            }
//        };
        Bugly.init(getApplicationContext(), "3f45ddcfa3", false);
        Bugly.setAppChannel(getApplicationContext(), "HoloCall");
        UserManager.init(this);
    }


    @Override
    public void onTerminate() {
        super.onTerminate();
    }


    public static boolean isActivityTop(Class cls, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        String name = manager.getRunningTasks(1).get(0).topActivity.getClassName();
        return name.equals(cls.getName());
    }


}
