package com.realview.holo.call;

import android.content.Context;
import android.util.Log;

import com.realview.holo.call.basic.ActivityCollector;

/**
 * Created by Mr.kk on 2019/4/15.
 * This Project is android-glass-callApp
 */
public class CrashCollectHandler implements Thread.UncaughtExceptionHandler {
    Thread.UncaughtExceptionHandler mDefaultHandler;

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        Log.i("BUGG", "---------", e);
        ActivityCollector.finishAll();
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    public void init() {
        // 获取系统默认的UncaughtException处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        // 设置该CrashHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

}
