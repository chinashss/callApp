/*
 *  Copyright 2015 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.webrtc;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


import com.google.gson.Gson;
import com.hv.calllib.bean.CaptureImageEvent;
import com.hv.calllib.bean.HoloEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Implements org.webrtc.VideoRenderer.Callbacks by displaying the video stream on a SurfaceView.
 * renderFrame() is asynchronous to avoid blocking the calling thread.
 * This class is thread safe and handles access from potentially four different threads:
 * Interaction from the main app in init, release, setMirror, and setScalingtype.
 * Interaction from C++ rtc::VideoSinkInterface in renderFrame.
 * Interaction from the Activity lifecycle in surfaceCreated, surfaceChanged, and surfaceDestroyed.
 * Interaction with the layout framework in onMeasure and onSizeChanged.
 */
public class SurfaceViewRenderer
        extends SurfaceView implements SurfaceHolder.Callback, VideoRenderer.Callbacks {
    private static final String TAG = "SurfaceViewRenderer";

    // Cached resource name.
    protected final String resourceName;
    private final RendererCommon.VideoLayoutMeasure videoLayoutMeasure =
            new RendererCommon.VideoLayoutMeasure();
    protected EglRenderer eglRenderer;

    // Callback for reporting renderer events. Read-only after initilization so no lock required.
    private RendererCommon.RendererEvents rendererEvents;

    private final Object layoutLock = new Object();
    private boolean isFirstFrameRendered;
    private int rotatedFrameWidth;
    private int rotatedFrameHeight;
    private int frameRotation;
    //private OnImageAvailableListener   mImageListener = null;
    public Bitmap bitmap;
    private YuvConverter yuvConverter;
    private HandlerThread renderThread;
    private Handler renderThreadHandler;
    private boolean runing;

    public Queue<VideoRenderer.I420FrameExt> queue = new LinkedList<VideoRenderer.I420FrameExt>();
    public int clickSum;

    public interface OnImageAvailableListener {
        public void onImageAvailable(VideoRenderer.I420Frame frame);
    }

  /*
  public void setImageAvailableListener( OnImageAvailableListener listener ) {
    mImageListener = listener;
  }
  */

    /**
     * Standard View constructor. In order to render something, you must first call init().
     */
    public SurfaceViewRenderer(Context context) {
        super(context);
        this.resourceName = getResourceName();
        eglRenderer = new EglRenderer(resourceName);
        getHolder().addCallback(this);
    }

    /**
     * Standard View constructor. In order to render something, you must first call init().
     */
    public SurfaceViewRenderer(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.resourceName = getResourceName();
        eglRenderer = new EglRenderer(resourceName);
        getHolder().addCallback(this);
    }

    public SurfaceViewRenderer(Context context, AttributeSet attrs, EglRenderer render) {
        super(context, attrs);
        this.resourceName = getResourceName();
        eglRenderer = render;
        render.SetResourceName(this.resourceName);
        getHolder().addCallback(this);
    }

    /**
     * Initialize this class, sharing resources with |sharedContext|. It is allowed to call init() to
     * reinitialize the renderer after a previous init()/release() cycle.
     */
    public void init(EglBase.Context sharedContext, RendererCommon.RendererEvents rendererEvents) {
        init(sharedContext, rendererEvents, EglBase.CONFIG_PLAIN, new GlRectDrawer());
        runing = true;
        clickSum = 0;
        checkThread.start();
    }

    /**
     * Initialize this class, sharing resources with |sharedContext|. The custom |drawer| will be used
     * for drawing frames on the EGLSurface. This class is responsible for calling release() on
     * |drawer|. It is allowed to call init() to reinitialize the renderer after a previous
     * init()/release() cycle.
     */
    public void init(final EglBase.Context sharedContext,
                     RendererCommon.RendererEvents rendererEvents, final int[] configAttributes,
                     RendererCommon.GlDrawer drawer) {
        ThreadUtils.checkIsOnMainThread();
        this.rendererEvents = rendererEvents;
        synchronized (layoutLock) {
            rotatedFrameWidth = 0;
            rotatedFrameHeight = 0;
            frameRotation = 0;
        }
        eglRenderer.init(sharedContext, configAttributes, drawer);
    }

    /**
     * Block until any pending frame is returned and all GL resources released, even if an interrupt
     * occurs. If an interrupt occurs during release(), the interrupt flag will be set. This function
     * should be called before the Activity is destroyed and the EGLContext is still valid. If you
     * don't call this function, the GL resources might leak.
     */
    public void release() {
        eglRenderer.release();
    }

    /**
     * Set if the video stream should be mirrored or not.
     */
    public void setMirror(final boolean mirror) {
        eglRenderer.setMirror(mirror);
    }

    /**
     * Set how the video will fill the allowed layout area.
     */
    public void setScalingType(RendererCommon.ScalingType scalingType) {
        ThreadUtils.checkIsOnMainThread();
        videoLayoutMeasure.setScalingType(scalingType);
    }

    public void setScalingType(RendererCommon.ScalingType scalingTypeMatchOrientation,
                               RendererCommon.ScalingType scalingTypeMismatchOrientation) {
        ThreadUtils.checkIsOnMainThread();
        videoLayoutMeasure.setScalingType(scalingTypeMatchOrientation, scalingTypeMismatchOrientation);
    }

    // VideoRenderer.Callbacks interface.
    int i = 0;

    @Override
    public void renderFrame(final VideoRenderer.I420Frame frame) {
        updateFrameDimensionsAndReportEvents(frame);
        if (clickSum > 0) {
            VideoRenderer.I420FrameExt i420FrameExt;
            i420FrameExt = frame.Clone();
            frame.CopyTo(i420FrameExt);
            queue.add(i420FrameExt);
            clickSum--;
        }
        if (!runing) {
            if (queue.size() > 0) {
                CaptureImageEvent captureImageEvent = new CaptureImageEvent();
                captureImageEvent.setImagepath("");
                captureImageEvent.setFromeid("0");
                captureImageEvent.setTouid("0");
                Gson gson = new Gson();
                String jsonString = gson.toJson(captureImageEvent);

                HoloEvent hermesEvent = new HoloEvent();
                hermesEvent.setAction("api.camera.capture");
                hermesEvent.setBody(jsonString);

                EventBus.getDefault().postSticky(hermesEvent);
                queue.clear();
            }
        }

        eglRenderer.renderFrame(frame);
    }

    // View layout interface.
    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        ThreadUtils.checkIsOnMainThread();
        final Point size;
        synchronized (layoutLock) {
            size =
                    videoLayoutMeasure.measure(widthSpec, heightSpec, rotatedFrameWidth, rotatedFrameHeight);
        }
        setMeasuredDimension(size.x, size.y);
        logD("onMeasure(). New size: " + size.x + "x" + size.y);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        ThreadUtils.checkIsOnMainThread();
        eglRenderer.setLayoutAspectRatio((right - left) / (float) (bottom - top));
    }

    // SurfaceHolder.Callback interface.
    @Override
    public void surfaceCreated(final SurfaceHolder holder) {
        ThreadUtils.checkIsOnMainThread();
        eglRenderer.createEglSurface(holder.getSurface());
    }

    Thread checkThread = new Thread(new Runnable() {
        @Override
        public void run() {
            while (runing&&!Thread.currentThread().isInterrupted()) {
                if (queue != null && !queue.isEmpty()) {
                    VideoRenderer.I420FrameExt i420FrameExt = queue.poll();
                    if (i420FrameExt != null) {
                        captureBitmapFromTexture(i420FrameExt);
                    }
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    });


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        ThreadUtils.checkIsOnMainThread();
//        runing = false;
        eglRenderer.releaseEglSurface();
        checkThread.interrupt();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        ThreadUtils.checkIsOnMainThread();
        eglRenderer.surfaceSizeChanged(width, height);
    }

    private String getResourceName() {
        try {
            return getResources().getResourceEntryName(getId()) + ": ";
        } catch (NotFoundException e) {
            return "";
        }
    }

    // Update frame dimensions and report any changes to |rendererEvents|.
    private void updateFrameDimensionsAndReportEvents(VideoRenderer.I420Frame frame) {
        synchronized (layoutLock) {
            if (!isFirstFrameRendered) {
                isFirstFrameRendered = true;
                logD("Reporting first rendered frame.");
                if (rendererEvents != null) {
                    rendererEvents.onFirstFrameRendered();
                }
            }
            if (rotatedFrameWidth != frame.rotatedWidth() || rotatedFrameHeight != frame.rotatedHeight()
                    || frameRotation != frame.rotationDegree) {
                logD("Reporting frame resolution changed to " + frame.width + "x" + frame.height
                        + " with rotation " + frame.rotationDegree);
                if (rendererEvents != null) {
                    rendererEvents.onFrameResolutionChanged(frame.width, frame.height, frame.rotationDegree);
                }
                rotatedFrameWidth = frame.rotatedWidth();
                rotatedFrameHeight = frame.rotatedHeight();
                frameRotation = frame.rotationDegree;
                post(new Runnable() {
                    @Override
                    public void run() {
                        requestLayout();
                    }
                });
            }
        }
    }

    private void logD(String string) {
        Logging.d(TAG, resourceName + string);
    }

    public VideoRenderer.I420Frame getI420Frame() {
        //runing = true;
        clickSum++;
        return null;
    }

    public Bitmap getI420Frame_Bitmap() {
        return bitmap;
    }

    public void captureBitmapFromTexture(org.webrtc.VideoRenderer.I420FrameExt i420FrameExt_) {
        captureBitmapFromYuvFrame(i420FrameExt_);
    }

    private YuvImage i420ToYuvImage(ByteBuffer[] yuvPlanes,
                                    int[] yuvStrides,
                                    int width,
                                    int height) {
        if (yuvStrides[0] != width) {
            return fastI420ToYuvImage(yuvPlanes, yuvStrides, width, height);
        }
        if (yuvStrides[1] != width / 2) {
            return fastI420ToYuvImage(yuvPlanes, yuvStrides, width, height);
        }
        if (yuvStrides[2] != width / 2) {
            return fastI420ToYuvImage(yuvPlanes, yuvStrides, width, height);
        }

        byte[] bytes = new byte[yuvStrides[0] * height +
                yuvStrides[1] * height / 2 +
                yuvStrides[2] * height / 2];
        ByteBuffer tmp = ByteBuffer.wrap(bytes, 0, width * height);
        copyPlane(yuvPlanes[0], tmp);

        byte[] tmpBytes = new byte[width / 2 * height / 2];
        tmp = ByteBuffer.wrap(tmpBytes, 0, width / 2 * height / 2);

        copyPlane(yuvPlanes[2], tmp);
        for (int row = 0; row < height / 2; row++) {
            for (int col = 0; col < width / 2; col++) {
                bytes[width * height + row * width + col * 2]
                        = tmpBytes[row * width / 2 + col];
            }
        }
        copyPlane(yuvPlanes[1], tmp);
        for (int row = 0; row < height / 2; row++) {
            for (int col = 0; col < width / 2; col++) {
                bytes[width * height + row * width + col * 2 + 1] =
                        tmpBytes[row * width / 2 + col];
            }
        }

        return new YuvImage(bytes, ImageFormat.NV21, width, height, null);
    }

    private YuvImage fastI420ToYuvImage(ByteBuffer[] yuvPlanes,
                                        int[] yuvStrides,
                                        int width,
                                        int height) {
        byte[] bytes = new byte[width * height * 3 / 2];
        int i = 0;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                bytes[i++] = yuvPlanes[0].get(col + row * yuvStrides[0]);
            }
        }
        for (int row = 0; row < height / 2; row++) {
            for (int col = 0; col < width / 2; col++) {
                bytes[i++] = yuvPlanes[2].get(col + row * yuvStrides[2]);
                bytes[i++] = yuvPlanes[1].get(col + row * yuvStrides[1]);
            }
        }
        return new YuvImage(bytes, ImageFormat.NV21, width, height, null);
    }

    private void copyPlane(ByteBuffer src, ByteBuffer dst) {
        src.position(0).limit(src.capacity());
        dst.put(src);
        dst.position(0).limit(dst.capacity());
    }

    private Bitmap captureBitmapFromYuvFrame(VideoRenderer.I420FrameExt i420FrameExt) {
        YuvImage yuvImage = i420ToYuvImage(i420FrameExt.yuvPlanes,
                i420FrameExt.yuvStrides,
                i420FrameExt.width,
                i420FrameExt.height);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Rect rect = new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight());

        // Compress YuvImage to jpeg
        yuvImage.compressToJpeg(rect, 100, stream);
        try {
            String path = "/mnt/sdcard/shownow/" + System.currentTimeMillis() + "capture.jpg";
            File pictureFile = new File(path);
            if (!pictureFile.getParentFile().exists()) {
                pictureFile.getParentFile().mkdirs();
            }
            FileOutputStream output = new FileOutputStream(path);
            output.write(stream.toByteArray());
            output.flush();
            output.close();

            CaptureImageEvent captureImageEvent = new CaptureImageEvent();
            captureImageEvent.setImagepath(path);
            captureImageEvent.setFromeid("0");
            captureImageEvent.setTouid("0");

            Gson gson = new Gson();
            String jsonString = gson.toJson(captureImageEvent);

            HoloEvent hermesEvent = new HoloEvent();
            hermesEvent.setAction("api.camera.capture");
            hermesEvent.setBody(jsonString);


            EventBus.getDefault().postSticky(hermesEvent);
        } catch (FileNotFoundException e) {
            System.out.println("Saving to file failed");
        } catch (IOException e) {
            System.out.println("Saving to file failed");
        }

        return bitmap;
    }
}
