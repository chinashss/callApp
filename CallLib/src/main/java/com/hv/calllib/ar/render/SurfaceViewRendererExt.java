package com.hv.calllib.ar.render;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;


import com.hv.calllib.ar.objtrack.ObjectTracker;

import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoRenderer;

/**
 * Created by liuhongyu on 2017/7/16.
 */

public class SurfaceViewRendererExt extends SurfaceViewRenderer {

    private StrokedRectangle mStrokedRectangle = new StrokedRectangle(StrokedRectangle.Type.STANDARD, Color.GREEN,10);
    private ObjectTracker mObjTracker  = null;

    private int xDown = 0;
    private int yDown = 0;
    private int widthUp = 0;
    private int heightUp = 0;

    private int mWidth          = 640;
    private int mHeight         = 480;
    private VideoRenderer.I420Frame i420Frame;

    /**
     * Standard View constructor. In order to render something, you must first call init().
     */
    public SurfaceViewRendererExt(Context context, AttributeSet attrs) {
        super(context,attrs,new EglRendererExt());

        EglRendererExt render = (EglRendererExt)this.eglRenderer;
        mObjTracker = render.GetObjectTracker();

        render.SetRectangle(mStrokedRectangle);

        /*
        this.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        xDown = (int) event.getX();
                        yDown = (int) event.getY();

                        widthUp     = 0;
                        heightUp    = 0;

                        widthUp         = 150;
                        heightUp        = 150;

                        if (0 == widthUp || 0 == heightUp ) {
                            // Toast.makeText(getContext(), "目标太小", Toast.LENGTH_SHORT).show();
                            break;
                        }

                        if( mObjTracker != null ) {

                            mObjTracker.SetROI( xDown-widthUp/2,yDown-heightUp/2,widthUp,heightUp );
                            mObjTracker.EnableROI(true);
                            mObjTracker.ResetImageTracker();

                            //mObjTracker.SetROI( 0,0,640,480 );
                        }

                        break;
                    case MotionEvent.ACTION_UP:

                        widthUp         = Math.abs((int) event.getX() - xDown);
                        heightUp        = Math.abs((int) event.getY() - yDown);

                        if (0 == widthUp || 0 == heightUp ) {
                            // Toast.makeText(getContext(), "目标太小", Toast.LENGTH_SHORT).show();
                            break;
                        }

                        if( mObjTracker != null ) {

                            mObjTracker.SetROI( xDown,yDown,widthUp,heightUp );
                            mObjTracker.EnableROI(true);
                            mObjTracker.ResetImageTracker();

                            //mObjTracker.SetROI( 0,0,640,480 );
                        }
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
        */
    }

    @Override
    public void renderFrame(final VideoRenderer.I420Frame frame) {

        if (frame == null){
            return;
        }
        if(clickSum > 0) {
            VideoRenderer.I420FrameExt i420FrameExt;
            i420FrameExt = frame.Clone();
            frame.CopyTo(i420FrameExt);
            queue.add(i420FrameExt);
            clickSum--;
        }
        eglRenderer.renderFrame(frame);
    }

    public VideoRenderer.I420Frame getI420Frame() {
        clickSum++;
        return null;
    }
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed,  left,  top,  right,  bottom);

        if (mObjTracker != null) {
            mObjTracker.onLayout(right-left,bottom-top);
        }

        mStrokedRectangle.updateLayout(right-left,bottom-top);
    }

    public void setConfig(int width,int height ) {

        if (mObjTracker != null) {
            mObjTracker.Init(width, height);
        }

        mWidth  = width;
        mHeight = height;
    }

    //  重置开始跟踪
    public void ResetImageTracker() {
        synchronized (this) {
            if (mObjTracker != null) {
                mObjTracker.ResetImageTracker();
            }
        }
    }

    //  进行热点跟踪，或者全局跟踪
    public void EnableROI(boolean bROIEnable) {
        synchronized (this) {
            if (mObjTracker != null) {
                mObjTracker.EnableROI(bROIEnable);
            }
        }
    }

    //  设置显示区域的踪热点区域
    public void SetROI( int x,int y,int width,int height ) {
        synchronized (this) {
            if (mObjTracker != null) {
                mObjTracker.SetROI(x, y, width, height);
            }
        }
    }

    //  设置原始图像跟踪热点区域
    public void SetImageROI( int x,int y,int width,int height ) {
        synchronized (this) {
            if (mObjTracker != null) {
                mObjTracker.SetImageROI(x, y, width, height);
            }
        }
    }
}
