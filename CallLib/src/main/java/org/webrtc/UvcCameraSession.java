package org.webrtc;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import com.serenegiant.usb.IButtonCallback;
import com.serenegiant.usb.IStatusCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;


class UvcCameraSession implements UsbCameraSession {
    private static final String TAG = "relabo.UvcCameraSession";
    private final Handler cameraThreadHandler;
    private final Events events;
    private final Context applicationContext;
    private final SurfaceTextureHelper surfaceTextureHelper;
    private final CameraEnumerationAndroid.CaptureFormat captureFormat;
    private final long constructionTimeNs;
    private final USBMonitor mUSBMonitor;
    private final MediaRecorder mediaRecorder;
    private UvcCameraSession.SessionState state;
    private boolean firstFrameReported = false;
    private UVCCamera mUVCCamera;
    private final Object mSync = new Object();
    private Surface mPreviewSurface;


    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            if (mUSBMonitor.hasPermission(device)) mUSBMonitor.openDevice(device);
            else mUSBMonitor.requestPermission(device);
            Log.e(TAG, "OnDeviceConnectListener onAttach");
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onConnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, final boolean createNew) {
            releaseCamera();

            final UVCCamera camera = new UVCCamera();
            camera.open(ctrlBlock);
            camera.setStatusCallback(new IStatusCallback() {
                @Override
                public void onStatus(final int statusClass, final int event, final int selector,
                                     final int statusAttribute, final ByteBuffer data) {
                    Logging.e("relabo.UsbDevice.onStatus", "onStatus(statusClass=" + statusClass
                            + "; " +
                            "event=" + event + "; " +
                            "selector=" + selector + "; " +
                            "statusAttribute=" + statusAttribute + "; " +
                            "data=...)");
                }
            });
            camera.setButtonCallback(new IButtonCallback() {
                @Override
                public void onButton(final int button, final int state) {
                    Logging.e("relabo.UsbDevice.onStatus", "onButton(button=" + button + "; " + "state=" + state + ")");
                }
            });

            try {
                camera.setPreviewSize(captureFormat.width, captureFormat.height, captureFormat.framerate.max);
            } catch (final IllegalArgumentException e) {
                // fallback to YUV mode
                try {
                    camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE);
                } catch (final IllegalArgumentException e1) {
                    camera.destroy();
                    return;
                }
            }
            camera.setPreviewDisplay(mPreviewSurface);
            camera.startPreview();

            synchronized (mSync) {
                mUVCCamera = camera;
            }
        }

        @Override
        public void onDisconnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock) {
            // XXX you should check whether the coming device equal to camera device that currently using
            //UvcCameraSession.this.events.onCameraError(UvcCameraSession.this, errorMessage);
            releaseCamera();
        }

        @Override
        public void onDettach(final UsbDevice device) {
            //Toast.makeText(MainActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel(final UsbDevice device) {
            //UvcCameraSession.this.events.onFailure(FailureType.ERROR, "android.hardware.Camera.open returned null for camera id = " + cameraId);
        }
    };

    private synchronized void releaseCamera() {
        synchronized (mSync) {
            if (mUVCCamera != null) {
                try {
                    mUVCCamera.setStatusCallback(null);
                    mUVCCamera.setButtonCallback(null);
                    mUVCCamera.close();
                    mUVCCamera.destroy();
                } catch (final Exception e) {
                    //
                }
                mUVCCamera = null;
            }
        }
    }

    public static void create(CreateSessionCallback callback, Events events, Context applicationContext, SurfaceTextureHelper surfaceTextureHelper, MediaRecorder mediaRecorder, int width, int height, int framerate) {
        long constructionTimeNs = System.nanoTime();
        Logging.d("UvcCameraSession", "Open usb camera ");
        events.onCameraOpening();
        CameraEnumerationAndroid.CaptureFormat captureFormat = findClosestCaptureFormat(width, height, framerate);
        callback.onDone(new UvcCameraSession(events, applicationContext, surfaceTextureHelper, mediaRecorder, captureFormat, constructionTimeNs));
    }

    private static CameraEnumerationAndroid.CaptureFormat findClosestCaptureFormat(int width, int height, int framerate) {
        return new CameraEnumerationAndroid.CaptureFormat(width, height, framerate, framerate);
    }

    private UvcCameraSession(Events events, Context applicationContext, SurfaceTextureHelper surfaceTextureHelper, @Nullable MediaRecorder mediaRecorder, CameraEnumerationAndroid.CaptureFormat captureFormat, long constructionTimeNs) {
        Logging.d("UvcCameraSession", "Create new camera1 session on usb camera");
        this.cameraThreadHandler = new Handler();
        this.events = events;
        this.applicationContext = applicationContext;
        this.surfaceTextureHelper = surfaceTextureHelper;
        this.mUSBMonitor = new USBMonitor(applicationContext, mOnDeviceConnectListener);
        this.captureFormat = captureFormat;
        this.constructionTimeNs = constructionTimeNs;
        this.mediaRecorder = mediaRecorder;
        this.startCapturing();
    }

    public void stop() {
        Logging.d("UvcCameraSession", "Stop camera1 session on usb camera");
        this.checkIsOnCameraThread();
        if (this.state != UvcCameraSession.SessionState.STOPPED) {
            this.stopInternal();
        }
        mUSBMonitor.unregister();
        mUSBMonitor.destroy();
    }

    private void startCapturing() {
        Logging.d("UvcCameraSession", "Start capturing");
        this.checkIsOnCameraThread();
        final SurfaceTexture st = surfaceTextureHelper.getSurfaceTexture();
        if (st != null) {
            mPreviewSurface = new Surface(st);

            if (mediaRecorder != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mediaRecorder.setInputSurface(mPreviewSurface);
                }
            }
        }
        this.state = SessionState.RUNNING;
        this.listenForTextureFrames();
        mUSBMonitor.register();
        List<UsbDevice> devices = mUSBMonitor.getDeviceList();
        if (devices.size() > 0) mUSBMonitor.requestPermission(devices.get(0));
    }

    private void stopInternal() {
        Logging.d("UvcCameraSession", "Stop internal");
        this.checkIsOnCameraThread();
        if (this.state == UvcCameraSession.SessionState.STOPPED) {
            Logging.d("UvcCameraSession", "Camera is already stopped");
        } else {
            this.state = UvcCameraSession.SessionState.STOPPED;
            this.surfaceTextureHelper.stopListening();
            releaseCamera();
            this.events.onCameraClosed(this);
            Logging.d("UvcCameraSession", "Stop done");
        }
        if (mPreviewSurface != null) {
            mPreviewSurface.release();
            mPreviewSurface = null;
        }
    }

    private void listenForTextureFrames() {
        this.surfaceTextureHelper.startListening(new SurfaceTextureHelper.OnTextureFrameAvailableListener() {
            public void onTextureFrameAvailable(int oesTextureId, float[] transformMatrix, long timestampNs) {

                UvcCameraSession.this.checkIsOnCameraThread();
                if (UvcCameraSession.this.state != UvcCameraSession.SessionState.RUNNING) {
                    Logging.d("UvcCameraSession", "Texture frame captured but camera is no longer running.");
                    UvcCameraSession.this.surfaceTextureHelper.returnTextureFrame();
                } else {
                    int rotation;
                    if (!UvcCameraSession.this.firstFrameReported) {
                        rotation = (int) TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - UvcCameraSession.this.constructionTimeNs);
                        //UvcCameraSession.camera1StartTimeMsHistogram.addSample(rotation);
                        UvcCameraSession.this.firstFrameReported = true;
                    }

                    rotation = UvcCameraSession.this.getFrameOrientation();
                    //transformMatrix = RendererCommon.multiplyMatrices(transformMatrix, RendererCommon.horizontalFlipMatrix());

                    /*
                    VideoFrame.Buffer buffer = UvcCameraSession.this.surfaceTextureHelper.createTextureBuffer(UvcCameraSession.this.captureFormat.width, UvcCameraSession.this.captureFormat.height, RendererCommon.convertMatrixToAndroidGraphicsMatrix(transformMatrix));
                    VideoFrame frame = new VideoFrame(buffer, rotation, timestampNs);
                    UvcCameraSession.this.events.onFrameCaptured(UvcCameraSession.this, frame);
                    frame.release();
                    */
                    UvcCameraSession.this.events.onTextureFrameCaptured(UvcCameraSession.this, captureFormat.width,
                            captureFormat.height, oesTextureId, transformMatrix, rotation, timestampNs);
                }
            }
        });
    }

    private int getDeviceOrientation() {
        WindowManager wm = (WindowManager) this.applicationContext.getSystemService(Context.WINDOW_SERVICE);
        int orientation = 0;
        switch (wm.getDefaultDisplay().getRotation()) {
            case 0:
            default:
                orientation = 0;
                break;
            case 1:
                orientation = 90;
                break;
            case 2:
                orientation = 180;
                break;
            case 3:
                orientation = 270;
        }

        return orientation;
    }

    private int getFrameOrientation() {
        int rotation = this.getDeviceOrientation();
        rotation = 360 - rotation;

        return rotation % 360;
    }

    private void checkIsOnCameraThread() {
        if (Thread.currentThread() != this.cameraThreadHandler.getLooper().getThread()) {
            throw new IllegalStateException("Wrong thread");
        }
    }

    private static enum SessionState {
        RUNNING,
        STOPPED;

        private SessionState() {
        }
    }
}