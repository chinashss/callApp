package com.hv.calllib.ar.render;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.hv.calllib.ar.objtrack.ObjectTracker;

/**
 * Created by liuhongyu on 2017/7/13.
 */

public class ObjTrackSurface extends SurfaceView implements SurfaceHolder.Callback {
    private int xDown = 0;
    private int yDown = 0;
    private int widthUp = 0;
    private int heightUp = 0;

    private int xImageDown      = 0;
    private int yImageDown      = 0;
    private int widthImageUp    = 0;
    private int heightImageUp   = 0;

    private int mWidth;
    private int mHeight;

    private Thread mThread;
    private boolean mStopThread;
    private boolean mCameraFrameReady = false;
    private RenderExtension mWikitudeRenderExtension = null;
    private ObjectTracker mObjTracker  = new ObjectTracker();

    public ObjTrackSurface(Context context, AttributeSet attrs) {
        super(context, attrs);

        getHolder().addCallback(this);

        this.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int xOffset = (getWidth() - mWidth) / 2;
                int yOffset = (getHeight() - mHeight) / 2;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        xDown = (int) event.getX();
                        yDown = (int) event.getY();

                        xImageDown = (int) event.getX() - xOffset;
                        yImageDown = (int) event.getY() - yOffset;

                        widthUp     = 0;
                        heightUp    = 0;

                        widthImageUp    = 0;
                        heightImageUp   = 0;

                        break;
                    case MotionEvent.ACTION_UP:
                        int xUp = (int) event.getX() - xOffset;
                        int yUp = (int) event.getY() - yOffset;

                        widthImageUp    = Math.abs(xUp - xImageDown);
                        heightImageUp   = Math.abs(yUp - yImageDown);

                        widthUp         = Math.abs((int) event.getX() - xDown);
                        heightUp        = Math.abs((int) event.getY() - yDown);

                        if (0 == widthImageUp || 0 == heightImageUp ) {
                            // Toast.makeText(getContext(), "目标太小", Toast.LENGTH_SHORT).show();
                            break;
                        }

                        if( mObjTracker != null ) {
                            //mObjTracker.SetROI( xImageDown,yImageDown,widthImageUp,heightImageUp );
                            //mObjTracker.SetROI( xImageDown,yImageDown,widthImageUp,heightImageUp );
                            mObjTracker.SetROI( 0,0,640,480 );
                        }
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
    }

    public void ProcessFrame( byte[] frame ) {
        mWikitudeRenderExtension.ProcessFrame( frame );
        mCameraFrameReady = true;
        synchronized (this) {
            this.notify();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if( mObjTracker != null ) {
            mObjTracker.Release();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mWikitudeRenderExtension != null) {
            mWikitudeRenderExtension.onSurfaceCreated(null, null);

            mCameraFrameReady = false;
            mStopThread = false;
            mThread = new Thread(new ObjTrackSurface.CameraWorker());
            mThread.start();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mWikitudeRenderExtension != null) {
            mWikitudeRenderExtension.onSurfaceChanged(null, width, height);
        }
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

    }

    public void onPause() {
        if (mWikitudeRenderExtension != null) {
            mWikitudeRenderExtension.onPause();
        }
    }

    public void onResume() {
        if (mWikitudeRenderExtension != null) {
            mWikitudeRenderExtension.onResume();
        }
    }

    public void setConfig(Context context, int width,int height ) {

        this.mWikitudeRenderExtension = new RenderExtension( width,height);
        //this.mWikitudeRenderExtension.useSeparatedRenderAndLogicUpdates();

        if( mObjTracker != null ) {
            mObjTracker.Init( width,height);
        }

        mWidth  = width;
        mHeight = height;
    }

    public RenderExtension getExtRender() {
        return mWikitudeRenderExtension;

    }

    public void ResetImageTracker() {
        if(mObjTracker != null) {
            mObjTracker.ResetImageTracker();
        }
    }

    public void EnableROI(boolean bROIEnable) {
        if( mObjTracker != null ) {
            mObjTracker.EnableROI( bROIEnable );
        }
    }

    private class CameraWorker implements Runnable {

        @Override
        public void run() {
            do {
                boolean hasFrame = false;
                synchronized (ObjTrackSurface.this) {
                    try {
                        while (!mCameraFrameReady && !mStopThread) {
                            ObjTrackSurface.this.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (mCameraFrameReady)
                    {
                        mWikitudeRenderExtension.onUpdate();
                        mCameraFrameReady = false;
                        hasFrame = true;
                    }
                }

                if (!mStopThread && hasFrame) {
                    Canvas canvas = ObjTrackSurface.this.getHolder().lockCanvas();
                    if( canvas != null ) {
                        mWikitudeRenderExtension.deliverAndDrawFrame(canvas);
                        getHolder().unlockCanvasAndPost(canvas);
                    }
                }
            } while (!mStopThread);
        }
    }
}
