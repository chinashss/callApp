package com.hv.calllib.ar.objtrack;

/**
 * Created by liuhongyu on 2017/7/4.
 */


import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;
import org.webrtc.VideoRenderer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.hv.calllib.ar.render.RenderExtension;
public class ObjectTracker {
    private static String TAG  = "track loc:";

    //private Mat            mRgba;
    //private Mat            mIntermediateMat;
    //private Mat            mGray;
    private Mat mPrevGray;
    private MatOfPoint2f prevFeatures;
    private MatOfPoint2f nextFeatures;
    private MatOfPoint features;

    private boolean        resetArTracking = false;

    private MatOfByte status;
    private MatOfFloat err;

    private int     maxDetectionCount   = 200;    //  最大检测的角点数（实际就是灰度梯度），值越大，计算量越大，但是跟踪准确
    private double  qualityLevel        = 0.01;   //  角点的质量因子（值越高，特征越明显）
    private double  minDistance         = 5;      //  更强角点的搜索范围半径
    private int     blockSize           = 3;
    private boolean useHarrisDetector   = false;
    private double  mHarrisK            = 0.04;

    private int     mROIXOffset         = 0;
    private int     mROIYOffset         = 0;
    private int     mROIWidth           = 0;
    private int     mROIHeight          = 0;

    private int     mROISurfaceXOffset  = 0;
    private int     mROISurfaceYOffset  = 0;
    private int     mROISurfaceWidth    = 0;
    private int     mROISurfaceHeight   = 0;

    private double  mWidthRatio         = 1.0f;
    private double  mHeightRatio        = 1.0f;

    private boolean mSetROIEnable       = false;

    private boolean mIsTrackObjLosted   = false;
    private List<Point> mptStartup      = null;
    private boolean mIsProcessTrackObj  = false;
    private Mat mSrcROIMask         = null;
    private Mat mGrayFrame          = null;
    private byte[]  mByteCached         = null;

    private int     mFrameWidth         = 0;
    private int     mFrameHeight        = 0;
    private int     mSurfaceHeight      = 0;

    private Thread mThread;
    private boolean mStopThread;
    private boolean mTrackingFrameReady = false;

    private ByteBuffer pendingFrame     = null;
    private ByteBuffer copyBuffer       = null;

    public ObjectTracker(){
        mStopThread = false;
        mThread = new Thread(new ObjectTracker.TrackWorker());
        mThread.start();
    }

    public void Init(int width,int height) {

        assert(width!= 0&&height!=0);

        mFrameWidth  = width;
        mFrameHeight = height;

        mSurfaceHeight = height;

        mWidthRatio  = 1.0f;
        mHeightRatio = 1.0f;

        //mRgba            = new Mat(height, width, CvType.CV_8UC4);
        //mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        //mGray            = new Mat(height, width, CvType.CV_8UC1);
        mGrayFrame         = new Mat(mFrameHeight, mFrameWidth, CvType.CV_8UC1);
        mByteCached        = new byte[mFrameHeight*mFrameWidth];

        Reset();
    }

    public void onLayout(int width,int height ) {
        mSurfaceHeight = height;

        mWidthRatio  = (double)((double)width/(double)mFrameWidth);
        mHeightRatio = (double)((double)height/(double)mFrameHeight);
    }

    private void Reset() {
        mPrevGray    = null;//new Mat(mGray.rows(), mGray.cols(), CvType.CV_8UC1);
        features     = new MatOfPoint();
        prevFeatures = new MatOfPoint2f();
        nextFeatures = new MatOfPoint2f();
        status       = new MatOfByte();
        err          = new MatOfFloat();
    }

    public void SetROI( int x,int y,int width,int height ) {
        mROIXOffset         = (int)((double)x/mWidthRatio);
        mROIYOffset         = (int)((double)y/mHeightRatio);
        mROIWidth           = (int)((double)width/mWidthRatio);
        mROIHeight          = (int)((double)height/mHeightRatio);

        mROISurfaceXOffset  = x;
        mROISurfaceYOffset  = y;
        mROISurfaceWidth    = width;
        mROISurfaceHeight   = height;
    }

    public void SetImageROI( int x,int y,int width,int height ) {
        mROIXOffset         = x;
        mROIYOffset         = y;
        mROIWidth           = width;
        mROIHeight          = height;

        mROISurfaceXOffset  = (int)((double)x*mWidthRatio);
        mROISurfaceYOffset  = (int)((double)y*mHeightRatio);
        mROISurfaceWidth    = (int)((double)width*mWidthRatio);
        mROISurfaceHeight   = (int)((double)height*mHeightRatio);
    }

    public void EnableROI(boolean bROIEnable) {
        mSetROIEnable = bROIEnable;
    }

    public int GetFlowX() {

        return (int)((double)(xStartupXOffset + mROIXOffset)*mWidthRatio);
    }

    public int GetFlowY() {
        return mSurfaceHeight - (int)((double)(mROIYOffset + yStartupYOffset + mROIHeight)*mHeightRatio);
    }

    public int GetFlowWidth() {
        return mROISurfaceWidth;
    }

    public int GetFlowHeight() {
        return mROISurfaceHeight;
    }

    private int xStartupXOffset = 0;
    private int yStartupYOffset = 0;
    private int FeatureXOffset  = 0;
    private int FeatureYOffset  = 0;

    //private Vector<Point> mPtFeatures = new Vector<Point>();

    public boolean IsTrackObjectLosted() {
        return mIsTrackObjLosted;
    }

    private final TermCriteria term = new TermCriteria(TermCriteria.COUNT | TermCriteria.EPS, 20, 0.03);
    private final Size subPixWinSize = new Size(10, 10);

    public void tracking( RenderExtension.I420FrameSP frame ) {
        if( frame == null || frame.Gray() == null )return;

        Mat mGray = frame.Gray();

        if(resetArTracking ) {

            if (mSetROIEnable) {
                ArrayList<MatOfPoint> pts = new ArrayList<MatOfPoint>();
                Mat srcROIMask = Mat.zeros( mGray.rows(),mGray.cols(), CvType.CV_8UC1);
                //Rect SrcImgROI = new Rect(0,0,480,640);
                //Rect SrcImgROI = new Rect(mROIXOffset,mROIYOffset,mROIWidth,mROIHeight );
                pts.add(new MatOfPoint(new Point(mROIXOffset,mROIYOffset),new Point(mROIXOffset+mROIWidth,mROIYOffset),new Point(mROIXOffset+mROIWidth,mROIYOffset+mROIHeight),new Point(mROIXOffset,mROIYOffset+mROIHeight)));

                //Imgproc.rectangle(srcROIMask,new Point(SrcImgROI.x,SrcImgROI.y),new Point(SrcImgROI.x+SrcImgROI.width,SrcImgROI.y+SrcImgROI.height),Scalar.all(255));
                Imgproc.fillPoly(srcROIMask,pts, Scalar.all(255));

                Imgproc.goodFeaturesToTrack(mGray, features, maxDetectionCount, qualityLevel, minDistance, srcROIMask, blockSize, useHarrisDetector, mHarrisK);
            } else {
                Imgproc.goodFeaturesToTrack(mGray, features, maxDetectionCount, qualityLevel, minDistance, new Mat(), blockSize, useHarrisDetector, mHarrisK);
            }

            prevFeatures.fromList(features.toList());
            Imgproc.cornerSubPix(mGray, prevFeatures, subPixWinSize, new Size(-1, -1), term);

            mPrevGray = mGray.clone();
            resetArTracking = false;
            xStartupXOffset = 0;
            yStartupYOffset = 0;
            mIsTrackObjLosted = false;
            mptStartup = prevFeatures.toList();

        }else if( features.toArray().length > 0 ){

            Video.calcOpticalFlowPyrLK(mPrevGray, mGray, prevFeatures, nextFeatures, status, err);

            List<Point> ptTo    = nextFeatures.toList();
            List<Point> ptFrom  = prevFeatures.toList();
            List<Byte>  bStatus = status.toList();

            if( ptFrom.size() == ptTo.size() ) {
                //mPtFeatures.clear();
                boolean islosted = true;
                //double maxXPos = 0;
                //double minXPos = 100000;
                //double maxYPos = 0;
                //double minYPos = 100000;
                for (int i = 0; i < ptTo.size(); i++) {
                    if (bStatus.get(i) == 1) {
                        //System.out.printf("feature:",pt.get(i).x,pt.get(i).y);
                        //Log.d(TAG, "feature:x=" + (ptTo.get(i).x - ptFrom.get(i).x) + " y=" + (ptTo.get(i).y-ptFrom.get(i).y));
                        //mxROIFlow = ((int)(ptTo.get(i).x - ptFrom.get(i).x) > mxROIFlow)?(int)(ptTo.get(i).x - ptFrom.get(i).x):mxROIFlow;
                        //myROIFlow = ((int)(ptTo.get(i).y-ptFrom.get(i).y)>myROIFlow)?(int)(ptTo.get(i).y-ptFrom.get(i).y):myROIFlow;

                        if( islosted ) {
                            xStartupXOffset += (int)(ptTo.get(i).x - ptFrom.get(i).x);
                            yStartupYOffset += (int)(ptTo.get(i).y - ptFrom.get(i).y);
                            islosted = false;
                            break;
                        }
                    }
                }

                if( islosted ) {
                    mIsTrackObjLosted = true;
                }else{
                    mIsTrackObjLosted = false;
                }

            }

            mPrevGray = mGray.clone();
            prevFeatures.fromArray(nextFeatures.toArray());
            mGray = null;
        }
    }

    public int  tracking2( ByteBuffer frame ) {
        int ret = 0;
        if( frame == null )return -1;


        mGrayFrame.put(0,0,frame.array());

        if(resetArTracking ) {

            if (mSetROIEnable) {
                ArrayList<MatOfPoint> pts = new ArrayList<MatOfPoint>();
                Mat srcROIMask = Mat.zeros( mGrayFrame.rows(),mGrayFrame.cols(), CvType.CV_8UC1);
                //Rect SrcImgROI = new Rect(0,0,480,640);
                //Rect SrcImgROI = new Rect(mROIXOffset,mROIYOffset,mROIWidth,mROIHeight );
                pts.add(new MatOfPoint(new Point(mROIXOffset,mROIYOffset),new Point(mROIXOffset+mROIWidth,mROIYOffset),new Point(mROIXOffset+mROIWidth,mROIYOffset+mROIHeight),new Point(mROIXOffset,mROIYOffset+mROIHeight)));

                //Imgproc.rectangle(srcROIMask,new Point(SrcImgROI.x,SrcImgROI.y),new Point(SrcImgROI.x+SrcImgROI.width,SrcImgROI.y+SrcImgROI.height),Scalar.all(255));
                Imgproc.fillPoly(srcROIMask,pts, Scalar.all(255));

                Imgproc.goodFeaturesToTrack(mGrayFrame, features, maxDetectionCount, qualityLevel, minDistance, srcROIMask, blockSize, useHarrisDetector, mHarrisK);
            } else {
                Imgproc.goodFeaturesToTrack(mGrayFrame, features, maxDetectionCount, qualityLevel, minDistance, new Mat(), blockSize, useHarrisDetector, mHarrisK);
            }

            prevFeatures.fromList(features.toList());
            Imgproc.cornerSubPix(mGrayFrame, prevFeatures, subPixWinSize, new Size(-1, -1), term);

            mPrevGray = mGrayFrame.clone();
            resetArTracking = false;
            xStartupXOffset = 0;
            yStartupYOffset = 0;
            mIsTrackObjLosted = false;
            mptStartup = prevFeatures.toList();

            ret = 1;
        }else if( features.toArray().length > 0 ){

            Video.calcOpticalFlowPyrLK(mPrevGray, mGrayFrame, prevFeatures, nextFeatures, status, err);

            List<Point> ptTo    = nextFeatures.toList();
            List<Point> ptFrom  = prevFeatures.toList();
            List<Byte>  bStatus = status.toList();
            List<Float> bErrors = err.toList();

            if( ptFrom.size() == ptTo.size() ) {
                //mPtFeatures.clear();
                boolean islosted = true;
                int cnt = 0;
                int xOffset = 0;
                int yOffset = 0;
                int xMinOffset = 9999;
                int yMinOffset = 9999;
                for (int i = 0; i < ptTo.size(); i++) {
                    if ( bStatus.get(i) == 1 /*&& bErrors.get(i) <= 10*/ ) {
                        if(true) {
                            if (islosted) {
                                xStartupXOffset += (int) (ptTo.get(i).x - ptFrom.get(i).x);
                                yStartupYOffset += (int) (ptTo.get(i).y - ptFrom.get(i).y);
                                islosted = false;
                                break;
                            }
                        }else{
                            cnt++;
                            xOffset = (int) (ptTo.get(i).x - ptFrom.get(i).x);
                            yOffset = (int) (ptTo.get(i).y - ptFrom.get(i).y);

                            if( Math.abs(xMinOffset) > Math.abs(xOffset) ) {
                                xMinOffset = xOffset;
                            }

                            if( Math.abs(yMinOffset) > Math.abs(yOffset) ) {
                                yMinOffset = yOffset;
                            }
                            islosted = false;
                        }
                    }
                }

                /*
                if( cnt > 0 ) {
                    xOffset = xOffset/cnt;
                    yOffset = yOffset/cnt;
                }
                */

                if( islosted ) {
                    mIsTrackObjLosted = true;
                    xStartupXOffset  += 0;
                    yStartupYOffset += 0;
                    resetArTracking = false;
                    features.release();
                    ret = 2;
                }else{
                    if(false) {
                        xStartupXOffset += xMinOffset;
                        yStartupYOffset += yMinOffset;
                    }
                    mIsTrackObjLosted = false;
                    ret = 1;
                }
            }
            else{
                mIsTrackObjLosted = true;
                xStartupXOffset += 0;
                yStartupYOffset += 0;
                resetArTracking = false;
                features.release();
                ret = 2;
            }
            mPrevGray = mGrayFrame.clone();
            prevFeatures.fromArray(nextFeatures.toArray());
            //mGrayFrame = null;
        }else{
            ret = 0;
        }

        return ret;
    }

    public int  tracking3( VideoRenderer.I420Frame frame ) {
        int ret = 0;
        if( frame == null || !frame.yuvFrame )return -1;


        if( frame.yuvPlanes[0].isDirect() ) {
            frame.yuvPlanes[0].get(mByteCached);
            mGrayFrame.put(0,0,mByteCached);
        }else{
            mGrayFrame.put(0,0,frame.yuvPlanes[0].array());
        }

        if(resetArTracking ) {

            if (mSetROIEnable) {
                ArrayList<MatOfPoint> pts = new ArrayList<MatOfPoint>();
                Mat srcROIMask = Mat.zeros( mGrayFrame.rows(),mGrayFrame.cols(), CvType.CV_8UC1);
                //Rect SrcImgROI = new Rect(0,0,480,640);
                //Rect SrcImgROI = new Rect(mROIXOffset,mROIYOffset,mROIWidth,mROIHeight );
                pts.add(new MatOfPoint(new Point(mROIXOffset,mROIYOffset),new Point(mROIXOffset+mROIWidth,mROIYOffset),new Point(mROIXOffset+mROIWidth,mROIYOffset+mROIHeight),new Point(mROIXOffset,mROIYOffset+mROIHeight)));

                //Imgproc.rectangle(srcROIMask,new Point(SrcImgROI.x,SrcImgROI.y),new Point(SrcImgROI.x+SrcImgROI.width,SrcImgROI.y+SrcImgROI.height),Scalar.all(255));
                Imgproc.fillPoly(srcROIMask,pts, Scalar.all(255));

                Imgproc.goodFeaturesToTrack(mGrayFrame, features, maxDetectionCount, qualityLevel, minDistance, srcROIMask, blockSize, useHarrisDetector, mHarrisK);
            } else {
                Imgproc.goodFeaturesToTrack(mGrayFrame, features, maxDetectionCount, qualityLevel, minDistance, new Mat(), blockSize, useHarrisDetector, mHarrisK);
            }

            prevFeatures.fromList(features.toList());
            Imgproc.cornerSubPix(mGrayFrame, prevFeatures, subPixWinSize, new Size(-1, -1), term);

            mPrevGray = mGrayFrame.clone();
            resetArTracking = false;
            xStartupXOffset = 0;
            yStartupYOffset = 0;
            mIsTrackObjLosted = false;
            mptStartup = prevFeatures.toList();

            ret = 1;
        }else if( features.toArray().length > 0 ){

            Video.calcOpticalFlowPyrLK(mPrevGray, mGrayFrame, prevFeatures, nextFeatures, status, err);

            List<Point> ptTo    = nextFeatures.toList();
            List<Point> ptFrom  = prevFeatures.toList();
            List<Byte>  bStatus = status.toList();
            List<Float> bErrors = err.toList();

            if( ptFrom.size() == ptTo.size() ) {
                //mPtFeatures.clear();
                boolean islosted = true;
                int cnt = 0;
                int xOffset = 0;
                int yOffset = 0;
                int xMinOffset = 9999;
                int yMinOffset = 9999;
                for (int i = 0; i < ptTo.size(); i++) {
                    if ( bStatus.get(i) == 1 /*&& bErrors.get(i) <= 10*/ ) {
                        if(true) {
                            if (islosted) {
                                xStartupXOffset += (int) (ptTo.get(i).x - ptFrom.get(i).x);
                                yStartupYOffset += (int) (ptTo.get(i).y - ptFrom.get(i).y);
                                islosted = false;
                                break;
                            }
                        }else{
                            cnt++;
                            xOffset = (int) (ptTo.get(i).x - ptFrom.get(i).x);
                            yOffset = (int) (ptTo.get(i).y - ptFrom.get(i).y);

                            if( Math.abs(xMinOffset) > Math.abs(xOffset) ) {
                                xMinOffset = xOffset;
                            }

                            if( Math.abs(yMinOffset) > Math.abs(yOffset) ) {
                                yMinOffset = yOffset;
                            }
                            islosted = false;
                        }
                    }
                }

                /*
                if( cnt > 0 ) {
                    xOffset = xOffset/cnt;
                    yOffset = yOffset/cnt;
                }
                */

                if( islosted ) {
                    mIsTrackObjLosted = true;
                    xStartupXOffset  += 0;
                    yStartupYOffset += 0;
                    resetArTracking = false;
                    features.release();
                    ret = 2;
                }else{
                    if(false) {
                        xStartupXOffset += xMinOffset;
                        yStartupYOffset += yMinOffset;
                    }
                    mIsTrackObjLosted = false;
                    ret = 1;
                }
            }
            else{
                mIsTrackObjLosted = true;
                xStartupXOffset += 0;
                yStartupYOffset += 0;
                resetArTracking = false;
                features.release();
                ret = 2;
            }
            mPrevGray = mGrayFrame.clone();
            prevFeatures.fromArray(nextFeatures.toArray());
            //mGrayFrame = null;
        }else{
            ret = 0;
        }

        return ret;
    }

    public int  tracking4(VideoRenderer.I420FrameExt frame ) {
        synchronized (this) {
            if( copyBuffer == null ) {
                copyBuffer = ByteBuffer.allocateDirect(frame.width*frame.height);
            }
            if( pendingFrame == null ) {
                VideoRenderer.nativeCopyPlane(
                        frame.yuvPlanes[0], frame.width, frame.height, frame.yuvStrides[0], copyBuffer, frame.width);

                pendingFrame = copyBuffer;

                mTrackingFrameReady = true;

                this.notify();
            }
        }
        return 1;
    }

    /*
    public Vector<Point> getFeaturePoints() {
        return mPtFeatures;
    }
    */

    public void Release() {
        //mRgba.release();
        //mGray.release();
        //mIntermediateMat.release();
    }

    public void ResetImageTracker() {
        mIsProcessTrackObj  = false;
        resetArTracking     = true;
    }

    private class TrackWorker implements Runnable {

        @Override
        public void run() {
            do {
                synchronized (ObjectTracker.this) {
                    try {
                        while ( !mTrackingFrameReady &&!mStopThread) {
                            System.out.printf("test");
                            ObjectTracker.this.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                mTrackingFrameReady = false;

                if (!mStopThread ) {
                    if( pendingFrame != null ) {
                        ObjectTracker.this.tracking2(pendingFrame);
                        pendingFrame = null;
                    }
                }
            } while (!mStopThread);
        }
    }

    public int  tracking5( VideoRenderer.I420FrameExt frame ) {
        int ret = 0;
        if( frame == null || !frame.yuvFrame )return -1;

        mGrayFrame.put(0,0,frame.yuvPlanes[0].array());

        if(resetArTracking ) {

            if (mSetROIEnable) {
                ArrayList<MatOfPoint> pts = new ArrayList<MatOfPoint>();
                Mat srcROIMask = Mat.zeros( mGrayFrame.rows(),mGrayFrame.cols(), CvType.CV_8UC1);
                //Rect SrcImgROI = new Rect(0,0,480,640);
                //Rect SrcImgROI = new Rect(mROIXOffset,mROIYOffset,mROIWidth,mROIHeight );
                pts.add(new MatOfPoint(new Point(mROIXOffset,mROIYOffset),new Point(mROIXOffset+mROIWidth,mROIYOffset),new Point(mROIXOffset+mROIWidth,mROIYOffset+mROIHeight),new Point(mROIXOffset,mROIYOffset+mROIHeight)));

                //Imgproc.rectangle(srcROIMask,new Point(SrcImgROI.x,SrcImgROI.y),new Point(SrcImgROI.x+SrcImgROI.width,SrcImgROI.y+SrcImgROI.height),Scalar.all(255));
                Imgproc.fillPoly(srcROIMask,pts, Scalar.all(255));

                Imgproc.goodFeaturesToTrack(mGrayFrame, features, maxDetectionCount, qualityLevel, minDistance, srcROIMask, blockSize, useHarrisDetector, mHarrisK);
            } else {
                Imgproc.goodFeaturesToTrack(mGrayFrame, features, maxDetectionCount, qualityLevel, minDistance, new Mat(), blockSize, useHarrisDetector, mHarrisK);
            }

            prevFeatures.fromList(features.toList());
            Imgproc.cornerSubPix(mGrayFrame, prevFeatures, subPixWinSize, new Size(-1, -1), term);

            mPrevGray = mGrayFrame.clone();
            resetArTracking = false;
            xStartupXOffset = 0;
            yStartupYOffset = 0;
            mIsTrackObjLosted = false;
            mptStartup = prevFeatures.toList();

            ret = 1;
        }else if( features.toArray().length > 0 ){

            Video.calcOpticalFlowPyrLK(mPrevGray, mGrayFrame, prevFeatures, nextFeatures, status, err);

            List<Point> ptTo    = nextFeatures.toList();
            List<Point> ptFrom  = prevFeatures.toList();
            List<Byte>  bStatus = status.toList();
            List<Float> bErrors = err.toList();

            if( ptFrom.size() == ptTo.size() ) {
                //mPtFeatures.clear();
                boolean islosted = true;
                int cnt = 0;
                int xOffset = 0;
                int yOffset = 0;
                int xMinOffset = 9999;
                int yMinOffset = 9999;
                for (int i = 0; i < ptTo.size(); i++) {
                    if ( bStatus.get(i) == 1 /*&& bErrors.get(i) <= 10*/ ) {
                        if(true) {
                            if (islosted) {
                                xStartupXOffset += (int) (ptTo.get(i).x - ptFrom.get(i).x);
                                yStartupYOffset += (int) (ptTo.get(i).y - ptFrom.get(i).y);
                                islosted = false;
                                break;
                            }
                        }else{
                            cnt++;
                            xOffset = (int) (ptTo.get(i).x - ptFrom.get(i).x);
                            yOffset = (int) (ptTo.get(i).y - ptFrom.get(i).y);

                            if( Math.abs(xMinOffset) > Math.abs(xOffset) ) {
                                xMinOffset = xOffset;
                            }

                            if( Math.abs(yMinOffset) > Math.abs(yOffset) ) {
                                yMinOffset = yOffset;
                            }
                            islosted = false;
                        }
                    }
                }

                /*
                if( cnt > 0 ) {
                    xOffset = xOffset/cnt;
                    yOffset = yOffset/cnt;
                }
                */

                if( islosted ) {
                    mIsTrackObjLosted = true;
                    xStartupXOffset  += 0;
                    yStartupYOffset += 0;
                    resetArTracking = false;
                    features.release();
                    ret = 2;
                }else{
                    if(false) {
                        xStartupXOffset += xMinOffset;
                        yStartupYOffset += yMinOffset;
                    }
                    mIsTrackObjLosted = false;
                    ret = 1;
                }
            }
            else{
                mIsTrackObjLosted = true;
                xStartupXOffset += 0;
                yStartupYOffset += 0;
                resetArTracking = false;
                features.release();
                ret = 2;
            }
            mPrevGray = mGrayFrame.clone();
            prevFeatures.fromArray(nextFeatures.toArray());
            //mGrayFrame = null;
        }else{
            ret = 0;
        }

        return ret;
    }
}

