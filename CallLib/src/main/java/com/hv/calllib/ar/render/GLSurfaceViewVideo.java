package com.hv.calllib.ar.render;

import android.content.Context;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;

import com.hv.calllib.ar.objtrack.ObjectTracker;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

//import com.wikitude.common.rendering.RenderExtension;
//import com.wikitude.tracker.Target;

/**
 * Created by AndrewZJ on 2015/7/24.
 */
//extends GLSurfaceView implements GLSurfaceView.Renderer
public class GLSurfaceViewVideo extends GLSurfaceView implements GLSurfaceView.Renderer {
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

    private Context context;
    private RenderExtension mWikitudeRenderExtension = null;
    private ObjectTracker mObjTracker  = new ObjectTracker();

    private Thread mThread;
    private boolean mStopThread;
    private boolean mCameraFrameReady = false;

    private StrokedRectangle mStrokedRectangle = new StrokedRectangle(StrokedRectangle.Type.STANDARD, Color.GREEN,10);

    public GLSurfaceViewVideo(Context context, AttributeSet attrs) {

        super(context, attrs);

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
                            //mObjTracker.SetROI( 0,0,640,480 );
                        }
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
        */
    }

    public void ProcessFrame( byte[] frame ) {
        synchronized (this) {
            if( mWikitudeRenderExtension.ProcessFrame(frame) ) {
                mCameraFrameReady = true;
                this.notify();
            }
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        synchronized (this) {
            if (mWikitudeRenderExtension != null) {
                mWikitudeRenderExtension.onSurfaceCreated(gl, config);
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        super.surfaceCreated(holder);
    }

    @Override
    public void onPause() {
        super.onPause();
        synchronized (this) {
            if (mWikitudeRenderExtension != null) {
                mWikitudeRenderExtension.onPause();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        synchronized (this) {
            if (mWikitudeRenderExtension != null) {
                mWikitudeRenderExtension.onResume();
            }
        }

        mCameraFrameReady = false;
        mStopThread = false;
        mThread = new Thread(new GLSurfaceViewVideo.CameraWorker());
        mThread.start();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        synchronized (this) {
            if (mWikitudeRenderExtension != null) {
                mWikitudeRenderExtension.onSurfaceChanged(gl, width, height);
            }

            if (mStrokedRectangle != null) {
                mStrokedRectangle.updateLayout(width, height);
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
        synchronized (this) {
            if (mObjTracker != null) {
                mObjTracker.Release();
            }
        }
    }


    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClearColor(0 , 0, 0 , 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        synchronized (this) {
            System.out.printf("test onDrawFrame");
            if (mWikitudeRenderExtension != null) {
                // Will trigger a logic update in the SDK
                //mWikitudeRenderExtension.onUpdate();

                RenderExtension.I420FrameSP frame = mWikitudeRenderExtension.GetFrame();
                if( mObjTracker != null&& frame!=null ) {
                    mObjTracker.tracking( frame );
                }

                // will trigger drawing of the camera frame
                mWikitudeRenderExtension.onDrawFrame(gl);

                if (mStrokedRectangle != null) {
                    if( mObjTracker.IsTrackObjectLosted() ==false ) {
                        mStrokedRectangle.onDrawFrame(  mObjTracker.GetFlowX(),
                                                        mObjTracker.GetFlowY(),
                                                        mObjTracker.GetFlowWidth(),
                                                        mObjTracker.GetFlowHeight()
                                                      );

                        //mStrokedRectangle.onDrawFrame(100, 100, 100, 100);
                    }
                    //mStrokedRectangle.onDrawFrame(0, 0, 50, 50);
                    //Vector<Point> pts = mObjTracker.getFeaturePoints();
                    //for( int i = 0;i < pts.size();i++) {
                    //mStrokedRectangle.onDrawFrame(getWidth() - (int)(pts.get(i).y)*3/2, getHeight() - (int)(pts.get(i).x)*3/2, 5, 5);
                    //    mStrokedRectangle.onDrawFrame((int)(pts.get(i).x), getHeight() - (int)(pts.get(i).y), 2, 2);
                    // }

                    //mStrokedRectangle.onDrawFrame(320*3/2, getHeight() - 240*3/2, 10, 10);
                    //mStrokedRectangle.onDrawFrame(mObjTracker.GetXFlow() * 3 / 2, getHeight() - mObjTracker.GetYFlow() * 3 / 2, 10, 10);
                }
                mWikitudeRenderExtension.onFrameDone();
            }
        }
    }

    public void setConfig(Context context, int width,int height ) {
        synchronized (this) {
            this.context = context;
            this.mWikitudeRenderExtension = new RenderExtension(width, height);
            //this.mWikitudeRenderExtension.useSeparatedRenderAndLogicUpdates();

            if (mObjTracker != null) {
                mObjTracker.Init(width, height);
            }

            mWidth = width;
            mHeight = height;

            this.setEGLContextClientVersion(2);
            this.setRenderer(this);
            this.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        }
    }

    public void ResetImageTracker() {
        synchronized (this) {
            if (mObjTracker != null) {
                mObjTracker.ResetImageTracker();
            }
        }
    }

    public void EnableROI(boolean bROIEnable) {
        synchronized (this) {
            if (mObjTracker != null) {
                mObjTracker.EnableROI(bROIEnable);
            }
        }
    }

    //public void setCurrentlyRecognizedTarget(final Target currentlyRecognizedTarget) {
       // mCurrentlyRecognizedTarget = currentlyRecognizedTarget;
    //}

   // public void setStrokedRectangleType(StrokedRectangle.Type strokedRectangleType) {
   //     mStrokedRectangleType = strokedRectangleType;
   // }

    private class CameraWorker implements Runnable {

        @Override
        public void run() {
            do {
                synchronized (GLSurfaceViewVideo.this) {
                    try {
                        while ( !mCameraFrameReady &&!mStopThread) {
                            System.out.printf("test");
                            GLSurfaceViewVideo.this.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                mCameraFrameReady = false;

                if (!mStopThread ) {
                    GLSurfaceViewVideo.this.requestRender();
                }
            } while (!mStopThread);
        }
    }

}
