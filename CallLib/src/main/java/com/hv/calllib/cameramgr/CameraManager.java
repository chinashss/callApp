package com.hv.calllib.cameramgr;

import android.content.Context;
import android.os.Bundle;

import org.webrtc.Camera1Enumerator;
import org.webrtc.R;

import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.FileVideoCapturer;
import org.webrtc.Logging;
import org.webrtc.VideoCapturer;

import java.io.IOException;
import com.hv.calllib.peermgr.PeerConnectionParameters;

/**
 * Created by liuhongyu on 2017/2/20.
 */

public class CameraManager {
    //public static final String EXTRA_PEERTYPE = "com.trios.videocall.PEERTYPE";
    public static final String EXTRA_LOOPBACK = "com.trios.videocall.LOOPBACK";
    public static final String EXTRA_TRACING = "com.trios.videocall.TRACING";
    public static final String EXTRA_VIDEO_CALL = "com.trios.videocall.VIDEO_CALL";
    public static final String EXTRA_CAMERA2 = "com.trios.videocall.CAMERA2";
    public static final String EXTRA_VIDEO_WIDTH = "com.trios.videocall.VIDEO_WIDTH";
    public static final String EXTRA_VIDEO_HEIGHT = "com.trios.videocall.VIDEO_HEIGHT";
    public static final String EXTRA_VIDEO_FPS = "com.trios.videocall.VIDEO_FPS";
    public static final String EXTRA_VIDEO_BITRATE = "com.trios.videocall.VIDEO_BITRATE";
    public static final String EXTRA_VIDEOCODEC = "com.trios.videocall.VIDEOCODEC";
    public static final String EXTRA_HWCODEC_ENABLED = "com.trios.videocall.HWCODEC";
    public static final String EXTRA_CAPTURETOTEXTURE_ENABLED = "com.trios.videocall.CAPTURETOTEXTURE";
    public static final String EXTRA_AUDIO_BITRATE = "com.trios.videocall.AUDIO_BITRATE";
    public static final String EXTRA_AUDIOCODEC = "com.trios.videocall.AUDIOCODEC";
    public static final String EXTRA_NOAUDIOPROCESSING_ENABLED = "com.trios.videocall.NOAUDIOPROCESSING";
    public static final String EXTRA_AECDUMP_ENABLED = "com.trios.videocall.AECDUMP";
    public static final String EXTRA_OPENSLES_ENABLED = "com.trios.videocall.OPENSLES";
    public static final String EXTRA_DISABLE_BUILT_IN_AEC = "com.trios.videocall.DISABLE_BUILT_IN_AEC";
    public static final String EXTRA_DISABLE_BUILT_IN_AGC = "com.trios.videocall.DISABLE_BUILT_IN_AGC";
    public static final String EXTRA_DISABLE_BUILT_IN_NS = "com.trios.videocall.DISABLE_BUILT_IN_NS";
    public static final String EXTRA_ENABLE_LEVEL_CONTROL = "com.trios.videocall.ENABLE_LEVEL_CONTROL";
    public static final String EXTRA_DISPLAY_HUD = "com.trios.videocall.DISPLAY_HUD";

    public static final String EXTRA_VIDEO_FILE_AS_CAMERA = "com.trios.videocall.VIDEO_FILE_AS_CAMERA";
    public static final String EXTRA_SAVE_REMOTE_VIDEO_TO_FILE = "com.trios.videocall.SAVE_REMOTE_VIDEO_TO_FILE";
    public static final String EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH = "com.trios.videocall.SAVE_REMOTE_VIDEO_TO_FILE_WIDTH";
    public static final String EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT = "com.trios.videocall.SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT";
    public static final String EXTRA_USE_VALUES_FROM_INTENT = "com.trios.videocall.USE_VALUES_FROM_INTENT";

    private static CameraManager ourInstance = new CameraManager();

    private static final String TAG = "CameraManager";

    public static CameraManager getInstance() {

        return ourInstance;
    }

    private CameraManager() {
    }

    public VideoCapturer createVideoCapturer(String capAsFile,
                                             boolean useCamera2,
                                             boolean capTextureEnabled,
                                             Context context,
                                             StringBuffer errorInfo ) {
        VideoCapturer videoCapturer = null;
        String videoFileAsCamera = capAsFile;//intent.getStringExtra(EXTRA_VIDEO_FILE_AS_CAMERA);
        if (videoFileAsCamera != null) {
            try {
                videoCapturer = new FileVideoCapturer(videoFileAsCamera);
            } catch (IOException e) {
                errorInfo.append("Failed to open video file for emulated camera");
                return null;
            }
        } else if (useCamera2(useCamera2,context)) {
            if (!capTextureEnabled/*captureToTexture(capTextureEnabled)*/) {
                errorInfo.append( context.getString( R.string.camera2_texture_only_error ) );
                return null;
            }

            Logging.d(TAG, "Creating capturer using camera2 API.");
            videoCapturer = createCameraCapturer(new Camera2Enumerator(context));
//            UsbCameraEnumerator cameraEnumerator = new UsbCameraEnumerator(context);
//            videoCapturer = createCameraCapturer( cameraEnumerator );
        } else {
            Logging.d(TAG, "Creating capturer using camera1 API.");
            videoCapturer = createCameraCapturer(new Camera1Enumerator(capTextureEnabled/*captureToTexture(intent)*/));
//            UsbCameraEnumerator cameraEnumerator = new UsbCameraEnumerator(context);
//            videoCapturer = createCameraCapturer( cameraEnumerator );
        }
        if (videoCapturer == null) {
            //errorInfo = "Failed to open camera");
            return null;
        }
        return videoCapturer;
    }

    private boolean useCamera2(boolean useCamer2,Context context) {
        return Camera2Enumerator.isSupported(context) && useCamer2/*intent.getBooleanExtra(EXTRA_CAMERA2, true)*/;
    }

    /*
    private boolean captureToTexture(Intent intent) {
        return intent.getBooleanExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, false);
    }
    */

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Logging.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        Logging.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    public PeerConnectionParameters GetConnectionParameters( Bundle in ) {
        PeerConnectionParameters params = new PeerConnectionParameters(
                        in.getBoolean(EXTRA_VIDEO_CALL, true),
                        in.getBoolean(EXTRA_LOOPBACK, false),
                        false,
                        in.getInt(EXTRA_VIDEO_WIDTH, 0),
                        in.getInt(EXTRA_VIDEO_HEIGHT, 0),
                        in.getInt(EXTRA_VIDEO_FPS, 0),
                        in.getInt(EXTRA_VIDEO_BITRATE, 0),
                        in.getString(EXTRA_VIDEOCODEC),
                        in.getBoolean(EXTRA_HWCODEC_ENABLED, false),
                        in.getInt(EXTRA_AUDIO_BITRATE, 0),
                        in.getString(EXTRA_AUDIOCODEC),
                        in.getBoolean(EXTRA_NOAUDIOPROCESSING_ENABLED, false),
                        in.getBoolean(EXTRA_AECDUMP_ENABLED, false),
                        in.getBoolean(EXTRA_OPENSLES_ENABLED, false),
                        in.getBoolean(EXTRA_DISABLE_BUILT_IN_AEC, false),
                        in.getBoolean(EXTRA_DISABLE_BUILT_IN_AGC, false),
                        in.getBoolean(EXTRA_DISABLE_BUILT_IN_NS, false),
                        in.getBoolean(EXTRA_ENABLE_LEVEL_CONTROL, false));

        return params;
    }
}
