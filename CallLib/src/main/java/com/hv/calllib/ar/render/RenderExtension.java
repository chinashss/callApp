package com.hv.calllib.ar.render;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.webrtc.GlRectDrawer;
import org.webrtc.GlUtil;
import org.webrtc.RendererCommon;

import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by liuhongyu on 2017/7/2.
 */

public class RenderExtension extends GlRectDrawer implements GLSurfaceView.Renderer {
    //private int           readFrameIndex   = 0;
    //private int           writeFrameIndex  = 0;
    private static int    renderFrameCount = 0;

    private I420FrameSP[] renderFrame  = null;
    private I420FrameSP   pendingFrame = null;

    protected int mLayoutFrameWidth;
    protected int mLayoutFrameHeight;
    private final RendererCommon.YuvUploader yuvUploader = new RendererCommon.YuvUploader();

    public class I420FrameSP  {
        private boolean      Updated = false;
        private ByteBuffer[] mYuvPlanes;
        //private Mat          mGrayFrame;
        private Mat mYuvFrame;

        private int[]      yuvTextures   = null;
        private int        totalLength   = 0;
        private final float[] samplingMatrix;

        private int         mFrameWidth;
        private int         mFrameHeight;

        private int         rotationDegree;


        public Mat Gray(){
            if( !mYuvFrame.empty() ) {
                //Mat grayMat = new Mat();
                //Imgproc.cvtColor(mYuvFrame,grayMat,Imgproc.COLOR_YUV2GRAY_NV21);
                //return grayMat;//
                return mYuvFrame.submat(0, mFrameHeight, 0, mFrameWidth);
            }else{
                return null;
            }
        }

        public Mat Rgba(){
            if( !mYuvFrame.empty() ) {
                Mat rgbaMat = new Mat();
                Imgproc.cvtColor(mYuvFrame,rgbaMat, Imgproc.COLOR_YUV2RGBA_NV21);
                return rgbaMat;//mYuvFrame.submat(0, mFrameHeight, 0, mFrameWidth);
            }else{
                return null;
            }
        }

        public I420FrameSP( int width,int height ) {
            mFrameWidth  = width;
            mFrameHeight = height;

            mYuvPlanes = new ByteBuffer[2];
            totalLength  = width * height;

            mYuvPlanes[0] = ByteBuffer.allocateDirect(totalLength);
            mYuvPlanes[1] = ByteBuffer.allocateDirect(totalLength/2);

            samplingMatrix = new float[] {
                    1,  0, 0, 0,
                    0, -1, 0, 0,
                    0,  0, 1, 0,
                    0,  1, 0, 1};

            //mYuvFrame  = new Mat();
            //mYuvFrame = new Mat(mFrameHeight + (mFrameHeight/2), mFrameWidth,CvType.CV_8UC1);
            mYuvFrame = new Mat(mFrameHeight + (mFrameHeight/2),mFrameWidth, CvType.CV_8UC1);
        }

        public void release() {
            mYuvPlanes[0] = null;
            mYuvPlanes[1] = null;

            mYuvPlanes    = null;
        }

        public void putRaw( byte[] data ) {
            if( Updated == false ) {
                mYuvPlanes[0].put(data, 0, totalLength);
                mYuvPlanes[0].rewind();
                mYuvPlanes[1].put(data,totalLength,totalLength/2);
                mYuvPlanes[1].rewind();

                //mYuvFrame.put(0,0,data);
                mYuvFrame.put(0,0,data);

                Updated = true;
            }
        }

        public void Draw() {

        }

        public int[] GenerateTexture() {
            if (yuvTextures == null) {
                yuvTextures = new int[2];
                for (int i = 0; i < 2; i++) {
                    yuvTextures[i] = GlUtil.generateTexture(GLES20.GL_TEXTURE_2D);
                }
            }

            return yuvTextures;
        }

        public boolean isUpdated() {
            return Updated;
        }

        public void RenderFrameDone() {
            Updated = false;
        }

        public ByteBuffer[] getPlanes() {
            return mYuvPlanes;
        }

        public float[] getSamplingMatrix() {
            return samplingMatrix;
        }

        public int rotatedWidth() {
            return (rotationDegree % 180 == 0) ? mFrameWidth : mFrameHeight;
        }

        public int rotatedHeight() {
            return (rotationDegree % 180 == 0) ? mFrameHeight : mFrameWidth;
        }

        public int getRotationDegree() {
            return rotationDegree;
        }

        public int Width() {
            return mFrameWidth;
        }

        public int Height() {
            return mFrameHeight;
        }
    };

    public class I420FrameExt  {
        private boolean      Updated = false;
        private ByteBuffer[] mYuvPlanes;
        private Mat mYuvFrame;

        private int[]      yuvTextures   = null;
        private int        totalLength   = 0;
        private final float[] samplingMatrix;

        private int         mFrameWidth;
        private int         mFrameHeight;

        private int         rotationDegree;

        public Mat Gray(){
            if( !mYuvFrame.empty() ) {
                return mYuvFrame.submat(0, mFrameHeight, 0, mFrameWidth);
            }else{
                return null;
            }
        }

        public I420FrameExt( int width,int height ) {
            mFrameWidth  = width;
            mFrameHeight = height;

            mYuvPlanes = new ByteBuffer[3];
            totalLength  = width * height;

            mYuvPlanes[0] = ByteBuffer.allocateDirect(totalLength);
            mYuvPlanes[1] = ByteBuffer.allocateDirect(totalLength/4);
            mYuvPlanes[2] = ByteBuffer.allocateDirect(totalLength/4);

            samplingMatrix = new float[] {
                    1,  0, 0, 0,
                    0, -1, 0, 0,
                    0,  0, 1, 0,
                    0,  1, 0, 1};
        }

        public void release() {
            mYuvPlanes[0] = null;
            mYuvPlanes[1] = null;
            mYuvPlanes[2] = null;

            mYuvPlanes    = null;
        }

        public void putRaw( byte[] data ) {
            if( Updated == false ) {
                mYuvPlanes[0].put(data, 0, totalLength);
                mYuvPlanes[0].rewind();
                int nULen = totalLength / 2;

                for (int i = totalLength; i < totalLength + nULen; i += 2) {
                    mYuvPlanes[1].put(data[i + 1]);
                    mYuvPlanes[2].put(data[i]);
                }
                mYuvPlanes[1].rewind();
                mYuvPlanes[2].rewind();

                Updated = true;
            }
        }

        public void Draw() {

        }

        public int[] GenerateTexture() {
            if (yuvTextures == null) {
                yuvTextures = new int[3];
                for (int i = 0; i < 3; i++) {
                    yuvTextures[i] = GlUtil.generateTexture(GLES20.GL_TEXTURE_2D);
                }
            }

            return yuvTextures;
        }

        public boolean isUpdated() {
            return Updated;
        }

        public void RenderFrameDone() {
            Updated = false;
        }

        public ByteBuffer[] getPlanes() {
            return mYuvPlanes;
        }

        public float[] getSamplingMatrix() {
            return samplingMatrix;
        }

        public int rotatedWidth() {
            return (rotationDegree % 180 == 0) ? mFrameWidth : mFrameHeight;
        }

        public int rotatedHeight() {
            return (rotationDegree % 180 == 0) ? mFrameHeight : mFrameWidth;
        }

        public int getRotationDegree() {
            return rotationDegree;
        }

        public int Width() {
            return mFrameWidth;
        }

        public int Height() {
            return mFrameHeight;
        }
    };

    public RenderExtension( int width ,int height) {
        super();

        Init(width,height);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        AllocateCache();
    }

    public void onPause() {
        Release();
    }

    public void onResume() {

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

        synchronized (this) {
            mLayoutFrameWidth     = width;
            mLayoutFrameHeight    = height;
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        if( pendingFrame!= null )
        {
            float[] texMatrix = RendererCommon.rotateTextureMatrix(pendingFrame.getSamplingMatrix(), 0/*90*/);

            final float[] layoutMatrix;
            float layoutAspectRatio = (float) mLayoutFrameWidth / (float) mLayoutFrameHeight;
            if (layoutAspectRatio > 0) {
                layoutMatrix = RendererCommon.getLayoutMatrix(
                        false, pendingFrame.rotatedWidth() / (float) pendingFrame.rotatedHeight(), layoutAspectRatio);
            } else {
                layoutMatrix = RendererCommon.identityMatrix();
            }
            texMatrix = RendererCommon.multiplyMatrices(texMatrix, layoutMatrix);

            if(true) {

                yuvUploader.uploadNV21SPData(
                        pendingFrame.GenerateTexture(),
                        pendingFrame.Width(),
                        pendingFrame.Height(),
                        pendingFrame.getPlanes());

                drawYuvNV21(pendingFrame.GenerateTexture(),
                        texMatrix,
                        pendingFrame.rotatedWidth(),
                        pendingFrame.rotatedHeight(), 0, 0,
                        mLayoutFrameWidth, mLayoutFrameHeight);

                //this.draw
            }else{

                yuvUploader.uploadNV21Data(
                        pendingFrame.GenerateTexture(),
                        pendingFrame.Width(),
                        pendingFrame.Height(),
                        pendingFrame.getPlanes());

                drawYuv(pendingFrame.GenerateTexture(),
                        texMatrix,
                        pendingFrame.rotatedWidth(),
                        pendingFrame.rotatedHeight(), 0, 0,
                        mLayoutFrameWidth, mLayoutFrameHeight);

            }
            //

            //pendingFrame.RenderFrameDone();
        }
    }

    public void onFrameDone() {
        synchronized (this) {
            if (pendingFrame != null) {
                pendingFrame.RenderFrameDone();
                pendingFrame = null;
            }
        }
    }

    public void onUpdate() {
        synchronized (this){
            if (renderFrame[renderFrameCount] != null && renderFrame[renderFrameCount].isUpdated()) {
                pendingFrame = renderFrame[renderFrameCount];
                renderFrameCount = 1-renderFrameCount;
            } else {
                pendingFrame = null;
            }
        }
    }

    public I420FrameSP GetFrame() {
        return pendingFrame;
    }

    public boolean ProcessFrame(byte[] frame) {
        boolean update = false;
        synchronized (this) {
            if( renderFrame[renderFrameCount]!= null ) {
                renderFrame[renderFrameCount].putRaw(frame);

                if( pendingFrame == null ) {
                    pendingFrame = renderFrame[renderFrameCount];
                    renderFrameCount = 1- renderFrameCount;
                    update = true;
                }
            }
        }

        return update;
    }

    private void Init( int width,int height) {
        pendingFrame     = null;

        renderFrameCount = 0;

        renderFrame  = new I420FrameSP[2];
        for( int i = 0;i <2;i++) {
            renderFrame[i] = new I420FrameSP(width,height);
        }
    }

    private void Release() {
        if (pendingFrame != null) {
            pendingFrame.release();
            pendingFrame = null;
        }
    }

    final int MAX_COUNT = 400;
    Size subPixWinSize = new Size(10, 10);
    Size winSize = new Size(31, 31);;
    private Bitmap mCacheBitmap;
    protected float mScale = 0;

    TermCriteria term = new TermCriteria(TermCriteria.COUNT | TermCriteria.EPS, 20, 0.03);

    public void deliverAndDrawFrame( Canvas canvas ) {
        if( pendingFrame!= null ) {
            Mat modified;
            Mat gray = pendingFrame.Gray();
            modified = pendingFrame.Rgba();

            boolean bmpValid = true;
            if (modified != null) {

                MatOfPoint corners = new MatOfPoint();
                Imgproc.goodFeaturesToTrack(gray, corners, MAX_COUNT, 0.01, 10, new Mat(), 3, false, 0.04);
                if (corners.toArray().length > 0) {
                    MatOfPoint2f points = new MatOfPoint2f(corners.toArray());
                    Imgproc.cornerSubPix(gray, points, subPixWinSize, new Size(-1, -1), term);
                    if (points.toArray().length > 0) {
                        for (Point p : points.toArray()) {
                            Imgproc.circle(modified, p, 3, new Scalar(255, 255, 0), -1, 8, 0);
                        }
                    }
                }

                try {
                    Utils.matToBitmap(modified, mCacheBitmap);
                } catch (Exception e) {
                    bmpValid = false;
                }
            }

            if (bmpValid && mCacheBitmap != null) {
                if (canvas != null) {
                    canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);

                    if (mScale != 0) {
                        canvas.drawBitmap(mCacheBitmap, new Rect(0, 0, mCacheBitmap.getWidth(), mCacheBitmap.getHeight()),
                                new Rect((int) ((canvas.getWidth() - mScale * mCacheBitmap.getWidth()) / 2),
                                        (int) ((canvas.getHeight() - mScale * mCacheBitmap.getHeight()) / 2),
                                        (int) ((canvas.getWidth() - mScale * mCacheBitmap.getWidth()) / 2 + mScale * mCacheBitmap.getWidth()),
                                        (int) ((canvas.getHeight() - mScale * mCacheBitmap.getHeight()) / 2 + mScale * mCacheBitmap.getHeight())), null);
                    } else {
                        canvas.drawBitmap(mCacheBitmap, new Rect(0, 0, mCacheBitmap.getWidth(), mCacheBitmap.getHeight()),
                                new Rect((canvas.getWidth() - mCacheBitmap.getWidth()) / 2,
                                        (canvas.getHeight() - mCacheBitmap.getHeight()) / 2,
                                        (canvas.getWidth() - mCacheBitmap.getWidth()) / 2 + mCacheBitmap.getWidth(),
                                        (canvas.getHeight() - mCacheBitmap.getHeight()) / 2 + mCacheBitmap.getHeight()), null);
                    }
                }
            }

            pendingFrame.RenderFrameDone();
        }
    }

    protected void AllocateCache()
    {
        mCacheBitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);
    }
}
