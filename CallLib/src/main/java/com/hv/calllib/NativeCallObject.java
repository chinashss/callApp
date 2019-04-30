package com.hv.calllib;

/**
 * Created by yukening on 17/7/18.
 */

public class NativeCallObject {
   // private NativeCallObject() {
//        this.setCallJNIEnv(this);
//    }
//
//    public static NativeCallObject getInstance() {
//        return NativeCallObject.SingletonHolder.sInstance;
//    }

//    native void registerVideoFrameObserver(IVideoFrameListener var1);
//
//    native void registerAudioFrameObserver(IAudioFrameListener var1);
//
//    native void unregisterVideoFrameObserver();
//
//    native void unregisterAudioFrameObserver();
//
//    protected native void setCallJNIEnv(NativeCallObject var1);

    static {
        System.loadLibrary("RongCallLib");
    }

    private static class SingletonHolder {
        static NativeCallObject sInstance = new NativeCallObject();

        private SingletonHolder() {
        }
    }
}
