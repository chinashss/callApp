package com.hv.calllib;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.hv.calllib.ar.render.SurfaceViewRendererExt;
import com.hv.calllib.audiomgr.PeerAudioManager;
import com.hv.calllib.cameramgr.CameraManager;
import com.hv.calllib.live.VideoCallLive;
import com.hv.calllib.peermgr.CmdParameters;
import com.hv.calllib.peermgr.PeerConnectionParameters;
import com.hv.calllib.peermgr.PeerFactory;
import com.hv.calllib.peermgr.WebSocketPeer;
import com.hv.calllib.peermgr.group.VideoCallGroup;

import org.webrtc.EglBase;
import org.webrtc.PeerConnection;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/**
 * Created by yukening on 17/7/20.
 */

public class CallEngineLive {

    private ICallEngineListener iCallEngineListener;

    private static final String TAG = "CallEngineLive";

    protected EglBase rootEglBase;
    private Set<String> uid_list;
    private boolean publisstatus;


    private static final int CAMERA_VIDEO_WIDTH = 1280;
    private static final int CAMERA_VIDEO_HEIGHT = 720;

    protected static final int STAT_CALLBACK_PERIOD = 1000;

    protected PeerAudioManager audioManager = null;

    //  calling sound
    protected static final String EXTRA_MEDIAURI = "com.trios.videocall.MEDIAURI";
    protected static final String EXTRA_INITIA = "com.trios.videocall.INITIA";
    protected static final String EXTRA_PEERID = "com.trios.videocall.PEERID";
    protected static final String HOST_UID = "com.trios.videocall.HOST_UID";
    protected static final String EXTRA_ROOM = "com.trios.videocall.ROOM_ID";
    protected static final String EXTRA_GROUPURL = "com.trios.videocall.GROUPURL";
    protected static final String EXTRA_GROUPCERT = "com.trios.videocall.SSLCERT";
    protected static final String EXTRA_VISIBLELOCAL = "com.trios.videocall.VISIBLELOCAL";
    protected static final String EXTRA_CMDLINE = "com.trios.videocall.CMDLINE";
    protected static final String EXTRA_RUNTIME = "com.trios.videocall.RUNTIME";

    protected PeerConnectionParameters peerConnectionParameters;

    protected long callStartedTimeMs = 0;
    protected boolean isError;

    private Toast logToast;
    private boolean bIsVisibleLocal = true;

    // List of mandatory application permissions.
    private static final String[] MANDATORY_PERMISSIONS = {"android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET"};

    //private boolean remoteiceConnected
    private boolean iceConnected = false;

    private RendererCommon.ScalingType scalingType = null;
    private String mhostuid;

    private String pushurl;
    private String mRoomNo;
    private String groupWssURL;
    private String sslCert;
    private Bundle mBundle = null;

    private WebSocketPeer mSignalPeer = null;

    private CmdParameters cmdParameters = null;
    private VideoCallLive mGroupVideoCallPeer = null;

    private Handler mUIHandler = null;
    private Handler mLocalHandler = null;

    protected SurfaceViewRenderer localRender;


    private boolean videoEnabled = true;
    private boolean isAr = false;
    private boolean isEnd = true;


    private HandlerThread mCallbackThread;


    VideoCapturer videoCapturer = null;


    private static CallEngineLive mCallEngine = null;

    private Context context;

    public static CallEngineLive newInstance() {
        if (mCallEngine == null) {
            mCallEngine = new CallEngineLive();
        }
        return mCallEngine;
    }

    /**
     * 创建视频引擎
     *
     * @param context             Context
     * @param iCallEngineListener 事件回调
     * @param videoEnabled        视频可用
     * @param mUid                自己的UID
     * @param roomNo              房间号
     */
    public void create(Context context, ICallEngineListener iCallEngineListener, boolean videoEnabled, String mUid, String url, String roomNo, boolean isAr) {
        isEnd = false;
        this.context = context;
        this.iCallEngineListener = iCallEngineListener;
        this.videoEnabled = videoEnabled;
        this.mhostuid = mUid;
        this.mRoomNo = roomNo;
        this.isAr = isAr;
        this.pushurl = url;

        InitData();
        InitUI();
        //InitGroupVideoCall();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    /**
     * 初始化数据
     */
    private void InitData() {
        iceConnected = false;
        uid_list = new HashSet<>();
        publisstatus = false;

        mBundle = new Bundle();
        mBundle.putInt(CameraManager.EXTRA_VIDEO_WIDTH, 1280);
        mBundle.putInt(CameraManager.EXTRA_VIDEO_HEIGHT, 720);
        mBundle.putInt(CameraManager.EXTRA_VIDEO_FPS, 20);
        mBundle.putInt(CameraManager.EXTRA_VIDEO_BITRATE, 60000);

        mBundle.putString(CameraManager.EXTRA_VIDEOCODEC, "H264");
        //signalingParameters = null;
        scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL;

        peerConnectionParameters = CameraManager.getInstance().GetConnectionParameters(mBundle);

        //peerConnectionParameters.videoCallEnabled = videoEnabled;
        mGroupVideoCallPeer = PeerFactory.getInstance().CreateLivevideoPeer();

        //"wss://119.235.144.28:8443/groupcall"
        //groupWssURL     = mBundle.getString(EXTRA_GROUPURL,"wss://192.168.11.252:8443/live");//
        if (!TextUtils.isEmpty(this.pushurl)) {
            groupWssURL = mBundle.getString(EXTRA_GROUPURL, pushurl);//
        } else {
            groupWssURL = mBundle.getString(EXTRA_GROUPURL, "wss://122.225.234.90:8443/live");//
        }

        //groupWssURL     = mBundle.getString(EXTRA_GROUPURL,"wss://114.80.18.11:8995/groupcall");//
        //groupWssURL     = mBundle.getString(EXTRA_GROUPURL,"wss://192.168.11.252:8995/groupcall");//
        //groupWssURL     = mBundle.getString(EXTRA_GROUPURL,"wss://119.235.144.28:8995/groupcall");//
        sslCert = mBundle.getString(EXTRA_GROUPCERT, "groupcall.cer");
        bIsVisibleLocal = mBundle.getBoolean(EXTRA_VISIBLELOCAL, true);

    }


    /**
     * 初始化UI
     * 初始化本地SurfaceView
     */
    private void InitUI() {
        mUIHandler = new Handler(context.getMainLooper());

        if (isAr) {
            localRender = new SurfaceViewRendererExt(context, null);
            ((SurfaceViewRendererExt) localRender).setConfig(CAMERA_VIDEO_WIDTH, CAMERA_VIDEO_HEIGHT);


        } else {
            localRender = new SurfaceViewRenderer(context);
        }
        // Create UI controls.

        //  初始化渲染引擎
        if (rootEglBase == null) {
            rootEglBase = EglBase.create();
        }

        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                if (rootEglBase == null) {
                    return;
                }
                localRender.init(rootEglBase.getEglBaseContext(), null);

                updateVideoView();


                InitGroupVideoCall();
            }
        });

    }


    /**
     * 初始化群组视频相关内容
     * 1.{@link WebSocketPeer}
     * 2.{@link PeerAudioManager}
     * 3.{@link VideoCallGroup}
     */
    private void InitGroupVideoCall() {
        if (context == null) return;


        mSignalPeer = WebSocketPeer.GetWebSocketPeer(context, "livevideo");
        mSignalPeer.RegisterEvent(new WebSocketPeer.SignalingEvents() {
            @Override
            public void onRegistered(final boolean status) {
                Log.d(TAG, "registered");


                if (context == null) return;


                if (status) {
                    //mGroupVideoCallPeer.Present();
                    mGroupVideoCallPeer.Present();
                }

            }

            @Override
            public void OnIncomingCall(WebSocketPeer.ClientPeerState type, String from) {

            }

            @Override
            public void onPeerStatusChange(int status) {

            }

            @Override
            public void onWebSocketClose() {
            }

            @Override
            public void onWebSocketError(final String description) {
            }
        });

        Log.d(TAG, "Initializing the audio manager...");


        audioManager = PeerAudioManager.create(context, new Runnable() {
            // This method will be called each time the audio state (number and
            // type of devices) has been changed.
            @Override
            public void run() {
                onAudioManagerChangedState();
            }
        });


        audioManager.init();

        this.mCallbackThread = new HandlerThread("IM_SDK_CALLBACK");
        this.mCallbackThread.start();
        mLocalHandler = new Handler(this.mCallbackThread.getLooper()) {

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case VideoCallLive.VideoCall_Event_StartPresenting:
                        //StopRing();
                        break;
                    case VideoCallLive.VideoCall_Event_onPresent:
                        iceConnected = true;
                        callConnected();
                        break;
                    case VideoCallLive.VideoCall_Event_Disconnect:
                        iceConnected = false;
                        callDisConnect();
                        //StopRing();
                        //disconnect();
                        break;
                }
            }
        };

        if (mGroupVideoCallPeer.Initialize(mLocalHandler,
                peerConnectionParameters,
                context,
                mhostuid,
                pushurl,
                mSignalPeer) != 0) {
            Log.e(TAG, "InitGroupVideoCall init failed.");
            reportError("InitGroupVideoCall init failed.");
            return;
        }

        if (cmdParameters == null) {
            cmdParameters = new CmdParameters(
                    // Ice servers are not needed for direct connections.
                    new LinkedList<PeerConnection.IceServer>(),
                    //true, // This code will only be run on the client side. So, we are not the initiator.
                    mhostuid, // clientId
                    "ws://192.168.11.17:8443/call", // wssUrl
                    null, // offerSdp
                    null // iceCandidates
            );
        }

        final long delta = System.currentTimeMillis() - callStartedTimeMs;

        if (peerConnectionParameters.videoCallEnabled) {
            StringBuffer errorInfo = new StringBuffer();
            videoCapturer = CameraManager.getInstance().createVideoCapturer(null,
                    false,
                    true,
                    context,
                    errorInfo
            );

            if (videoCapturer == null) {

                reportError("Failed to open camera");
            }
        }

        mGroupVideoCallPeer.StartVideoCall(rootEglBase == null ? null : rootEglBase.getEglBaseContext(),
                localRender,
                videoCapturer,
                cmdParameters);


    }


    /**
     * mLocalHandler中接收到消息的具体处理
     */
    private void callConnected() {
        if (context == null || isEnd == true) return;

        iceConnected = true;

        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        Log.i(TAG, "Call connected: delay=" + delta + "ms");
        if (mGroupVideoCallPeer == null || isError) {
            Log.w(TAG, "Call is connected in closed or error state");
            return;
        }
        // Update video view.
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                updateVideoView();
            }
        });
        // Enable statistics callback.
        mGroupVideoCallPeer.enableStatsEvents(true, STAT_CALLBACK_PERIOD);


        if (iCallEngineListener != null) {
            iCallEngineListener.onJoinChannelSuccess("", "", 0);
            publisstatus = true;

        }
    }


    private void callDisConnect() {
        if (context == null || isEnd == true) return;

        if (iCallEngineListener != null) {
            iCallEngineListener.onConnectionLost();
        }
    }


    /**
     * 加入视频房间
     */
    public void startLive() {

        callStartedTimeMs = System.currentTimeMillis();
        mSignalPeer.ConnectTestServer(mhostuid, groupWssURL);
    }

    /**
     * 获取本地视频的SurfaceView
     *
     * @return
     */
    public SurfaceView getLocalSurfaceView() {
        return localRender;
    }


    /**
     * 切换摄像头
     */
    public void switchCamera() {
        if (context == null) return;

        if (mGroupVideoCallPeer != null) {
            mGroupVideoCallPeer.switchCamera();
        }

    }


    /**
     * 离开视频房间
     */
    public void stopLive() {


        iCallEngineListener.onLeaveChannel();


    }


    /**
     * 释放视频引擎
     */
    public void destroy() {
        isEnd = true;
        iceConnected = false;

        if (mGroupVideoCallPeer != null) {
            mGroupVideoCallPeer.close();
        }


        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                if (localRender != null) {
                    localRender.release();
                    localRender = null;
                }

                //
                if (rootEglBase != null) {
                    rootEglBase.release();
                    rootEglBase = null;
                }


                if (audioManager != null) {
                    audioManager.close();
                    audioManager = null;
                }

            }
        });

        if (mSignalPeer != null) {
            WebSocketPeer.ReleaseWebSocketPeer("livevideo");
            mSignalPeer = null;
        }

        //dispose();
        if (mGroupVideoCallPeer != null) {
            mGroupVideoCallPeer.Reset();
        }

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    protected void reportError(final String description) {
        if (context == null) return;

        if (!isError) {
            isError = true;
            disconnectWithErrorMessage(description);
        }

    }

    private void disconnectWithErrorMessage(final String errorMessage) {

    }

    private void dispose() {
        peerConnectionParameters = null;

        if (mGroupVideoCallPeer != null) {
            mGroupVideoCallPeer.FreeFactory();

            mGroupVideoCallPeer = null;
        }
    }


    private void onAudioManagerChangedState() {
        // TODO(henrika): disable video if AppRTCAudioManager.AudioDevice.EARPIECE
        // is active.
    }

    /**
     * 更新渲染窗口
     */
    private void updateVideoView() {

        if (!videoEnabled) {
            return;
        }
        if (mGroupVideoCallPeer != null) {

            SurfaceViewRenderer focusRender = null;


            if (localRender != null) {

                localRender.setVisibility(View.VISIBLE);
                localRender.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
            }

        }
    }


}
