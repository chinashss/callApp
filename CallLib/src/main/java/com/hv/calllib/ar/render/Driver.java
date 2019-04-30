package com.hv.calllib.ar.render;

import android.opengl.GLSurfaceView;

import java.util.Timer;
import java.util.TimerTask;

public class Driver {

    private final GLSurfaceView mCustomSurfaceView;
    private final int mFps;
    private Timer mRenderTimer = null;


    public Driver(final GLSurfaceView customSurfaceView, int fps) {
        mCustomSurfaceView = customSurfaceView;
        mFps = fps;

    }

    public void start() {

        if (mRenderTimer != null) {
            mRenderTimer.cancel();
        }

        mRenderTimer = new Timer();
        mRenderTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                mCustomSurfaceView.requestRender();
            }
        }, 0, 1000 / mFps);

    }

    public void stop() {
        mRenderTimer.cancel();
        mRenderTimer = null;
    }
}
