package com.hv.calllib.ar.render;

import android.opengl.GLES20;

import com.hv.calllib.ar.objtrack.ObjectTracker;

import org.webrtc.EglRenderer;
import org.webrtc.GlUtil;
import org.webrtc.RendererCommon;
import org.webrtc.VideoRenderer;

/**
 * Created by liuhongyu on 2017/7/16.
 */

public class EglRendererExt extends EglRenderer {

    private StrokedRectangle mStrokedRectangle  = null;
    private ObjectTracker mObjTracker           = null;

    private VideoRenderer.I420FrameExt copyBuffer          = null;
    private VideoRenderer.I420FrameExt pendingRenderFrame  = null;

    public EglRendererExt() {
        mObjTracker = new ObjectTracker();
    }

    public void SetRectangle( StrokedRectangle rectDraw ) {
        mStrokedRectangle = rectDraw;
    }

    public ObjectTracker GetObjectTracker() {
        return mObjTracker;
    }

    @Override
    public void renderFrame(VideoRenderer.I420Frame frame) {
        if (frame == null){
            return;
        }
        synchronized (statisticsLock) {
            ++framesReceived;
        }
        final boolean dropOldFrame;
        synchronized (handlerLock) {
            if (renderThreadHandler == null) {
                logD("Dropping frame - Not initialized or already released.");
                VideoRenderer.renderFrameDone(frame);
                return;
            }
            // Check if fps reduction is active.
            synchronized (fpsReductionLock) {
                if (minRenderPeriodNs > 0) {
                    final long currentTimeNs = System.nanoTime();
                    if (currentTimeNs < nextFrameTimeNs) {
                        logD("Dropping frame - fps reduction is active.");
                        VideoRenderer.renderFrameDone(frame);
                        return;
                    }
                    nextFrameTimeNs += minRenderPeriodNs;
                    // The time for the next frame should always be in the future.
                    nextFrameTimeNs = Math.max(nextFrameTimeNs, currentTimeNs);
                }
            }
            synchronized (frameLock) {
                dropOldFrame = (pendingFrame != null);
                if (dropOldFrame) {
                    VideoRenderer.renderFrameDone(pendingFrame);
                }

                pendingFrame = frame;

                //  liuhongyu write to prepare renderframe at begin

                if( copyBuffer == null ) {
                    copyBuffer =frame.Clone();//ByteBuffer.allocateDirect(frame.width*frame.height);
                }else{
                    if( copyBuffer.width != frame.width || frame.height != copyBuffer.height ) {
                        copyBuffer = null;
                        copyBuffer =frame.Clone();
                    }
                }

                if( pendingRenderFrame == null ) {
                    //VideoRenderer.nativeCopyPlane(
                    //        frame.yuvPlanes[0], frame.width, frame.height, frame.yuvStrides[0], copyBuffer, frame.width);
                    frame.CopyTo(copyBuffer);
                    pendingRenderFrame = copyBuffer;

                    renderThreadHandler.post(renderFrameRunnable);
                }

                // liuhongyu write to prepare renderframe at end

                VideoRenderer.renderFrameDone(frame);
            }
        }
        if (dropOldFrame) {
            synchronized (statisticsLock) {
                ++framesDropped;
            }
        }
    }

    /**
     * Renders and releases |pendingFrame|.
     */
    /*
    protected void renderFrameOnRenderThread() {
        // Fetch and render |pendingFrame|.
        final VideoRenderer.I420Frame frame;
        synchronized (frameLock) {
            if (pendingFrame == null) {
                return;
            }
            frame = pendingFrame;
            pendingFrame = null;
        }
        if (eglBase == null || !eglBase.hasSurface()) {
            logD("Dropping frame - No surface");
            VideoRenderer.renderFrameDone(frame);
            return;
        }

        final long startTimeNs = System.nanoTime();
        float[] texMatrix =
                RendererCommon.rotateTextureMatrix(frame.samplingMatrix, frame.rotationDegree);

        // After a surface size change, the EGLSurface might still have a buffer of the old size in the
        // pipeline. Querying the EGLSurface will show if the underlying buffer dimensions haven't yet
        // changed. Such a buffer will be rendered incorrectly, so flush it with a black frame.
        synchronized (layoutLock) {
            int surfaceClearCount = 0;
            while (eglBase.surfaceWidth() != surfaceWidth || eglBase.surfaceHeight() != surfaceHeight) {
                ++surfaceClearCount;
                if (surfaceClearCount > MAX_SURFACE_CLEAR_COUNT) {
                    logD("Failed to get surface of expected size - dropping frame.");
                    VideoRenderer.renderFrameDone(frame);
                    //pendingRenderFrame = null;
                    return;
                }
                logD("Surface size mismatch - clearing surface.");
                clearSurfaceOnRenderThread();
            }
            final float[] layoutMatrix;
            if (layoutAspectRatio > 0) {
                layoutMatrix = RendererCommon.getLayoutMatrix(
                        mirror, frame.rotatedWidth() / (float) frame.rotatedHeight(), layoutAspectRatio);
            } else {
                layoutMatrix =
                        mirror ? RendererCommon.horizontalFlipMatrix() : RendererCommon.identityMatrix();
            }
            texMatrix = RendererCommon.multiplyMatrices(texMatrix, layoutMatrix);
        }
        //

        GLES20.glClearColor(0 , 0 , 0 , 0 );
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        if (frame.yuvFrame) {
            // Make sure YUV textures are allocated.
            if (yuvTextures == null) {
                yuvTextures = new int[3];
                for (int i = 0; i < 3; i++) {
                    yuvTextures[i] = GlUtil.generateTexture(GLES20.GL_TEXTURE_2D);
                }
            }

            yuvUploader.uploadYuvData(
                            yuvTextures, frame.width, frame.height, frame.yuvStrides, frame.yuvPlanes);
            drawer.drawYuv(yuvTextures, texMatrix, frame.rotatedWidth(), frame.rotatedHeight(), 0, 0,
                            surfaceWidth, surfaceHeight);
        } else {
            drawer.drawOes(frame.textureId, texMatrix, frame.rotatedWidth(), frame.rotatedHeight(), 0, 0,
                        surfaceWidth, surfaceHeight);
        }

        //  draw ar by liuhongyu
        if (mObjTracker != null) {
            int status = mObjTracker.tracking2( pendingRenderFrame );//frame

            if( status == 1 ) {
                if (mStrokedRectangle != null) {
                    mStrokedRectangle.onDrawFrame(  mObjTracker.GetFlowX(),
                                mObjTracker.GetFlowY(),
                                mObjTracker.GetFlowWidth(),
                                mObjTracker.GetFlowHeight()
                    );
                        //mStrokedRectangle.onDrawFrame(100, 100, 100, 100);
                }
            }else if( status == 2 ) {

            }
        }

        //  draw ar end by liuhongyu
        final long swapBuffersStartTimeNs = System.nanoTime();
        eglBase.swapBuffers();

        VideoRenderer.renderFrameDone(frame);

        final long currentTimeNs = System.nanoTime();
        synchronized (statisticsLock) {
            ++framesRendered;
            renderTimeNs += (currentTimeNs - startTimeNs);
            renderSwapBufferTimeNs += (currentTimeNs - swapBuffersStartTimeNs);
        }
    }
    */

    protected void renderFrameOnRenderThread() {
        // Fetch and render |pendingFrame|.

        final VideoRenderer.I420FrameExt frame;
        synchronized (frameLock) {
            if (pendingRenderFrame == null) {
                return;
            }
            frame = pendingRenderFrame;
        }
        if (eglBase == null || !eglBase.hasSurface()) {
            logD("Dropping frame - No surface");
            pendingRenderFrame = null;
            return;
        }

        final long startTimeNs = System.nanoTime();
        float[] texMatrix =
                RendererCommon.rotateTextureMatrix(frame.samplingMatrix, frame.rotationDegree);

        // After a surface size change, the EGLSurface might still have a buffer of the old size in the
        // pipeline. Querying the EGLSurface will show if the underlying buffer dimensions haven't yet
        // changed. Such a buffer will be rendered incorrectly, so flush it with a black frame.
        synchronized (layoutLock) {
            int surfaceClearCount = 0;
            while (eglBase.surfaceWidth() != surfaceWidth || eglBase.surfaceHeight() != surfaceHeight) {
                ++surfaceClearCount;
                if (surfaceClearCount > MAX_SURFACE_CLEAR_COUNT) {
                    logD("Failed to get surface of expected size - dropping frame.");
                    pendingRenderFrame = null;
                    return;
                }
                logD("Surface size mismatch - clearing surface.");
                clearSurfaceOnRenderThread();
            }
            final float[] layoutMatrix;
            if (layoutAspectRatio > 0) {
                layoutMatrix = RendererCommon.getLayoutMatrix(
                        mirror, frame.rotatedWidth() / (float) frame.rotatedHeight(), layoutAspectRatio);
            } else {
                layoutMatrix =
                        mirror ? RendererCommon.horizontalFlipMatrix() : RendererCommon.identityMatrix();
            }
            texMatrix = RendererCommon.multiplyMatrices(texMatrix, layoutMatrix);
        }

        //

        GLES20.glClearColor(0 /* red */, 0 /* green */, 0 /* blue */, 0 /* alpha */);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        if (frame.yuvFrame) {
            // Make sure YUV textures are allocated.
            if (yuvTextures == null) {
                yuvTextures = new int[3];
                for (int i = 0; i < 3; i++) {
                    yuvTextures[i] = GlUtil.generateTexture(GLES20.GL_TEXTURE_2D);
                }
            }

            yuvUploader.uploadYuvData(
                    yuvTextures, frame.width, frame.height, frame.yuvStrides, frame.yuvPlanes);
            drawer.drawYuv(yuvTextures, texMatrix, frame.rotatedWidth(), frame.rotatedHeight(), 0, 0,
                    surfaceWidth, surfaceHeight);
        }

        //  draw ar by liuhongyu
        if (mObjTracker != null) {
            int status = mObjTracker.tracking5( pendingRenderFrame );//frame
            //int status = 1;
            if( status == 1 ) {
                if (mStrokedRectangle != null) {
                    mStrokedRectangle.onDrawFrame(  mObjTracker.GetFlowX(),
                            mObjTracker.GetFlowY(),
                            mObjTracker.GetFlowWidth(),
                            mObjTracker.GetFlowHeight()
                    );
                    //mStrokedRectangle.onDrawFrame(100, 100, 100, 100);
                }
            }else if( status == 2 ) {

            }
        }

        //  draw ar end by liuhongyu

        final long swapBuffersStartTimeNs = System.nanoTime();
        eglBase.swapBuffers();

        final long currentTimeNs = System.nanoTime();
        synchronized (statisticsLock) {
            ++framesRendered;
            renderTimeNs += (currentTimeNs - startTimeNs);
            renderSwapBufferTimeNs += (currentTimeNs - swapBuffersStartTimeNs);
        }

        synchronized (frameLock) {
            pendingRenderFrame = null;
        }
    }
}
