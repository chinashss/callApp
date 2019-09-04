package com.hv.calllib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.hv.calllib.ar.render.SurfaceViewRendererExt;
import com.hv.calllib.audiomgr.PeerAudioManager;
import com.hv.calllib.cameramgr.CameraManager;
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
import org.webrtc.VideoRenderer;
import org.webrtc.YuvConverter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import cn.holo.call.bean.message.ar.ARRectangle;
import cn.holo.call.bean.message.ar.ARShape;

/**
 * Created by yukening on 17/7/20.
 */

public class CallEngine {

    private ICallEngineListener iCallEngineListener;

    private static final String TAG = "VideoFragment";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    protected EglBase rootEglBase;
    public Set<String> uid_list;
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

    private String mRoomNo;
    private String groupWssURL;
    private String sslCert;
    private Bundle mBundle = null;

    private WebSocketPeer mSignalPeer = null;

    private CmdParameters cmdParameters = null;
    private VideoCallGroup mGroupVideoCallPeer = null;

    private Handler mUIHandler = null;
    private Handler mLocalHandler = null;

    protected SurfaceViewRenderer localRender;


    private boolean videoEnabled = true;
    private boolean isAr = false;
    private boolean isEnd = true;
    private boolean subscribeVideo = true;

    protected final LinkedList<SurfaceViewRenderer> llRemoteRenderScreens = new LinkedList<>();
    protected final HashMap<String, SurfaceViewRenderer> mapRemoteRenderingScreens = new HashMap<String, SurfaceViewRenderer>();
    private HandlerThread mCallbackThread;


    VideoCapturer videoCapturer = null;


    private static CallEngine mCallEngine = null;

    private Context context;

    public static CallEngine newInstance() {
        if (mCallEngine == null) {
            mCallEngine = new CallEngine();
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
    public void create(Context context, ICallEngineListener iCallEngineListener, boolean videoEnabled, String mUid, String roomNo, boolean isAr) {
        isEnd = false;
        this.context = context;
        this.iCallEngineListener = iCallEngineListener;
        this.videoEnabled = videoEnabled;
        this.mhostuid = mUid;
        this.mRoomNo = roomNo;
        this.isAr = isAr;
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
        mBundle.putString(EXTRA_GROUPURL, HoloCall.routeUrl);
        mBundle.putString(CameraManager.EXTRA_VIDEOCODEC, "H264");

        //signalingParameters = null;
        scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL;

        peerConnectionParameters = CameraManager.getInstance().GetConnectionParameters(mBundle);

        //peerConnectionParameters.videoCallEnabled = videoEnabled;
        mGroupVideoCallPeer = PeerFactory.getInstance().CreateGroupCallPeer();

        //"wss://119.235.144.28:8443/groupcall"
        groupWssURL = mBundle.getString(EXTRA_GROUPURL, "wss://199.168.11.252:8443/groupcall");//

        //groupWssURL     = mBundle.getString(EXTRA_GROUPURL,"wss://122.225.234.90:8443/groupcall");//
        //groupWssURL     = mBundle.getString(EXTRA_GROUPURL,"wss://114.80.18.11:8995/groupcall");//
        //groupWssURL     = mBundle.getString(EXTRA_GROUPURL,"wss://192.168.11.252:8995/groupcall");//
        //groupWssURL     = mBundle.getString(EXTRA_GROUPURL,"wss://119.235.144.28:8443/groupcall");//
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
//                try {
//                    Thread.sleep(500);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                if (rootEglBase == null) {
                    return;
                }
                localRender.init(rootEglBase.getEglBaseContext(), null);

                llRemoteRenderScreens.clear();
                mapRemoteRenderingScreens.clear();

                updateVideoView();


//                try {
//                    Thread.sleep(200);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
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


        mSignalPeer = WebSocketPeer.GetWebSocketPeer(context, "groupcall");
        mSignalPeer.RegisterEvent(new WebSocketPeer.SignalingEvents() {
            @Override
            public void onRegistered(final boolean status) {
                Log.d(TAG, "registered");


                if (context == null) return;


                if (status) {
                    mGroupVideoCallPeer.JoinRoom(mRoomNo);
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
//        if( callEvents != null ) {
//            if( iceConnected) {
//                callEvents.onPublishMediaStatus(GroupVideo_Media_Disconnect);
//            }else{
//                callEvents.onPublishMediaStatus( GroupVideo_Media_Failed );
//            }
//        }
            }

            @Override
            public void onWebSocketError(final String description) {
//        if( callEvents != null ) {
//            callEvents.onPublishMediaStatus( GroupVideo_Media_Failed );
//        }
            }
        });

        // Store existing audio settings and change audio mode to
        // MODE_IN_COMMUNICATION for best possible VoIP performance.
        Log.d(TAG, "Initializing the audio manager...");

//        mUIHandler.post(new Runnable() {
//            @Override
//            public void run() {

        audioManager = PeerAudioManager.create(context, new Runnable() {
            // This method will be called each time the audio state (number and
            // type of devices) has been changed.
            @Override
            public void run() {
                onAudioManagerChangedState();
            }
        });


        audioManager.init();
//            }
//        });


        this.mCallbackThread = new HandlerThread("IM_SDK_CALLBACK");
        this.mCallbackThread.start();
        mLocalHandler = new Handler(this.mCallbackThread.getLooper()) {

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case VideoCallGroup.VideoCall_Event_OnCall:
                        iceConnected = true;
                        callConnected();
                        break;
                    case VideoCallGroup.VideoCall_Event_Disconnect:
                        iceConnected = false;
                        callDisConnect();
                        //StopRing();
                        //disconnect();
                        break;
                    case VideoCallGroup.VideoCall_Event_NewUser:
                        onNewUserEntered((String) msg.obj);
                        break;
                    case VideoCallGroup.VideoCall_SubscribeStart:
                        onNewStreamConnected((String) msg.obj);
                        break;
                    case VideoCallGroup.VideoCall_SubscribeStop:
//                        onUserLeftRoom((String) msg.obj);
                        break;
                    case VideoCallGroup.VideoCall_Event_CaptureJPEG:
                        onCaptureJPEG();
                        break;
                    default:
                        break;
                }
            }
        };

        if (mGroupVideoCallPeer.Initialize(mLocalHandler,
                peerConnectionParameters,
                context,
                mhostuid,
                mSignalPeer,
                videoEnabled) != 0) {
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
                    false,//false,
                    false,//false,
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
            Iterator<String> it = uid_list.iterator();

            while (it.hasNext()) {
                String str = it.next();
                this.SubscribeMeida(str);

                if (iCallEngineListener != null) {
                    iCallEngineListener.onUserJoined(str, 0);
                }
            }
        }
    }


    private void callDisConnect() {
        if (context == null || isEnd == true) return;

        if (iCallEngineListener != null) {
            iCallEngineListener.onConnectionLost();
        }
    }

    protected void onNewUserEntered(final String name) {
        if (context == null || isEnd == true) return;

        if (name.compareTo(mhostuid) == 0)
            return;

        uid_list.add(name);
        if (publisstatus) {
            SubscribeMeida(name);
            if (iCallEngineListener != null) {
                iCallEngineListener.onUserJoined(name, 0);
            }
        }
    }

    protected void onNewStreamConnected(final String name) {


        if (context == null || isEnd == true) return;

        if (iCallEngineListener != null) {
            if (mhostuid.compareTo(name) == 0) {
                iCallEngineListener.onJoinChannelSuccess(name, name, 0);
                publisstatus = true;
                Iterator<String> it = uid_list.iterator();

                while (it.hasNext()) {
                    String str = it.next();
                    this.SubscribeMeida(str);
                    if (iCallEngineListener != null) {
                        iCallEngineListener.onUserJoined(str, 0);
                    }
                }
            } else {
                //iCallEngineListener.onJoinChannelSuccess(name, name, 0);
            }
        }

    }

    protected void onUserLeftRoom(final String name) {


        if (context == null || isEnd == true) return;


        if (iceConnected) {
            mGroupVideoCallPeer.ReleaseSubscribe(name);
        }

        if (iCallEngineListener != null) {
            iCallEngineListener.onUserOffline(name, 0);
        }

        SurfaceViewRenderer render = mapRemoteRenderingScreens.get(name);
        if (render != null) {
            mapRemoteRenderingScreens.remove(name);
            llRemoteRenderScreens.addLast(render);
        }

        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                updateVideoView();
            }
        });
    }

    /**
     * 加入视频房间
     */
    public void JoinChannel() {

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
     * 获取远端视频的SurfaceView
     *
     * @param remoteUid 远端用户id
     * @return
     */
    public SurfaceView getRemoteSurfaceView(String remoteUid) {

        if (mapRemoteRenderingScreens.get(remoteUid) != null) {
            return mapRemoteRenderingScreens.get(remoteUid);
        }

        final SurfaceViewRenderer remoteRender = new SurfaceViewRenderer(context);
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                if (rootEglBase == null) {
                    return;
                }
                remoteRender.init(rootEglBase.getEglBaseContext(), null);

                updateVideoView();
            }
        });
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mapRemoteRenderingScreens.put(remoteUid, remoteRender);
        return remoteRender;
    }

    boolean havevideo = true;

    /**
     * 切换摄像头
     */
    public void switchCamera() {
        if (context == null) return;

        if (mGroupVideoCallPeer != null) {
            mGroupVideoCallPeer.switchCamera();
        }
//        havevideo = !havevideo;
//        setVideoCaptureStates(havevideo);


    }

    /**
     * 开关视频
     *
     * @param videoEnabled true:video+audio  false:audio
     */
    public void setPublishVideoEnabled(boolean videoEnabled) {
        this.videoEnabled = videoEnabled;
        if (mGroupVideoCallPeer != null) {
            mGroupVideoCallPeer.setPublishVideoEnabled(videoEnabled);
        }

    }

    /**
     * 开关视频
     *
     * @param videoEnabled true:video+audio  false:audio
     */
    public void setVideoEnabled(boolean videoEnabled) {
        this.videoEnabled = videoEnabled;
        if (mGroupVideoCallPeer != null) {
            mGroupVideoCallPeer.setAllVideoEnabled(videoEnabled);
        }

    }

    /**
     * 拉取视频开关
     *
     * @param videoEnabled true:video+audio  false:audio
     */
    public void setSubscribeVideoEnabled(boolean videoEnabled) {
        this.subscribeVideo = videoEnabled;
        if (mGroupVideoCallPeer != null) {
            mGroupVideoCallPeer.setSubscribeVideoEnabled(videoEnabled);
        }

    }

    /**
     * 离开视频房间
     */
    public void leaveChannel() {


        if (mGroupVideoCallPeer != null) {
            mGroupVideoCallPeer.LeaveRoom();
        }

        iCallEngineListener.onLeaveChannel();


    }


    public void showArMark(ARShape shape) {
        if (shape == null || shape.getType() != ARShape.ARShapeType_Rect) {
            return;
        }


        if (localRender != null && localRender instanceof SurfaceViewRendererExt
                && localRender.getVisibility() == View.VISIBLE) {
            ARRectangle rectangle = (ARRectangle) shape;
            SurfaceViewRendererExt rendererExt = (SurfaceViewRendererExt) localRender;
            rendererExt.SetImageROI((int) rectangle.getLeft(),
                    (int) rectangle.getTop(),
                    (int) (rectangle.getRight() - rectangle.getLeft()),
                    (int) (rectangle.getBottom() - rectangle.getTop()));

            rendererExt.EnableROI(true);
            rendererExt.ResetImageTracker();
        }
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

                SurfaceViewRenderer remoteScreen = null;
                while ((remoteScreen = llRemoteRenderScreens.pollFirst()) != null) {
                    remoteScreen.release();
                    remoteScreen = null;
                }
                llRemoteRenderScreens.clear();

                for (HashMap.Entry<String, SurfaceViewRenderer> entry : mapRemoteRenderingScreens.entrySet()) {
                    remoteScreen = entry.getValue();
                    if (remoteScreen != null) {
                        remoteScreen.release();
                        remoteScreen = null;
                    }
                }
                mapRemoteRenderingScreens.clear();

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
            WebSocketPeer.ReleaseWebSocketPeer("groupcall");
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


    /*
       订阅房间内其他人的视频
       视音频单独切换控制暂没实现，这两天实现
    */
    public int SubscribeMeida(final String uid) {

        if (mGroupVideoCallPeer != null && iceConnected) {
            SurfaceViewRenderer render = mapRemoteRenderingScreens.get(uid);
            if (render != null) {

            } else {
                render = (SurfaceViewRenderer) getRemoteSurfaceView(uid);
            }

            mGroupVideoCallPeer.StartNextNewStream(uid, subscribeVideo, true, render);

            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateVideoView();
                }
            });
        }

        return 0;
    }

    /*
        取消订阅其他视频
     */
    public int UnSubscribeMedia(final String uid) {
        if (context == null) return -1000;

        if (mGroupVideoCallPeer != null && iceConnected) {
            mGroupVideoCallPeer.ReleaseSubscribe(uid);
        }

        SurfaceViewRenderer render = mapRemoteRenderingScreens.get(uid);
        if (render != null) {
            mapRemoteRenderingScreens.remove(uid);
            llRemoteRenderScreens.addLast(render);
        }

        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                updateVideoView();
            }
        });
        return 0;
    }


    private void disconnect() {

//        if( mSignalPeer != null ) {
//            WebSocketPeer.ReleaseWebSocketPeer("groupcall");
//            mSignalPeer = null;
//        }
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

    public void setMicrophoneMute(boolean on) {
        if (audioManager != null) {
            audioManager.setMicrophoneMute(on);
        }
    }

    public void setSpeakerphoneOn(boolean on) {
        if (audioManager != null) {
            audioManager.setSpeakerphoneOn(on);
        }
    }

    public void setVideoCaptureStates(boolean on) {
        if (videoCapturer != null) {
            if (!on) {
                try {
                    videoCapturer.stopCapture();
                    updateVideoView();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                videoCapturer.startCapture(CAMERA_VIDEO_WIDTH, CAMERA_VIDEO_HEIGHT, 20);
            }
        }
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
            for (HashMap.Entry<String, SurfaceViewRenderer> entry : mapRemoteRenderingScreens.entrySet()) {
                focusRender = entry.getValue();

                focusRender.setVisibility(View.VISIBLE);
                focusRender.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
            }

            if (localRender != null) {

                localRender.setVisibility(View.VISIBLE);
                localRender.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
            }

        }
    }

    static int c = 0;

    private void onCaptureJPEG() {
        if (localRender != null) {
            VideoRenderer.I420Frame i420Frame = localRender.getI420Frame();
        }
    }

    public static void generateWarnPic(final int[] picData, final int width, final int height) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String fileName = System.currentTimeMillis() + ".jpg";
                    File file = new File("/mnt/sdcard/shownow/image" + fileName);

                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }

                    // int [] argb=I420toARGB(picData,width, height);
                    Bitmap bitmap = Bitmap.createBitmap(picData, width, height, Bitmap.Config.ARGB_8888);

                    FileOutputStream fileOutputStream;
                    try {
                        fileOutputStream = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream);

                        fileOutputStream.close();
                    } catch (Exception e) {

                        Log.i("yy", e.getMessage());
                    }

                    //update db
//                    WarnPicDao dao = new WarnPicDao();
//                    dao.updateImages(fileName);
                } catch (Exception e) {
                    Log.i("yy", e.getMessage());
                }

            }
        }).start();

    }

    public static void generateWarnPic2(final Bitmap bitmap_) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String fileName = System.currentTimeMillis() + ".png";
                    File file = new File("/mnt/sdcard/work/mywork" + fileName);

                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }

                    // int [] argb=I420toARGB(picData,width, height);
                    Bitmap bitmap = Bitmap.createBitmap(bitmap_);


                    FileOutputStream fileOutputStream;
                    try {
                        fileOutputStream = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, fileOutputStream);

                        fileOutputStream.close();
                    } catch (Exception e) {

                        Log.i("yy", e.getMessage());
                    }

                    //update db
//                    WarnPicDao dao = new WarnPicDao();
//                    dao.updateImages(fileName);
                } catch (Exception e) {
                    Log.i("yy", e.getMessage());
                }

            }
        }).start();

    }

    public static int[] I420toARGB(byte[] yuv, int width, int height) {

        boolean invertHeight = false;
        if (height < 0) {
            height = -height;
            invertHeight = true;
        }

        boolean invertWidth = false;
        if (width < 0) {
            width = -width;
            invertWidth = true;
        }


        int iterations = width * height;
//if ((iterations*3)/2 > yuv.length){throw new IllegalArgumentException();}
        int[] rgb = new int[iterations];

        for (int i = 0; i < iterations; i++) {
    /*int y = yuv[i] & 0x000000ff;
    int u = yuv[iterations+(i/4)] & 0x000000ff;
    int v = yuv[iterations + iterations/4 + (i/4)] & 0x000000ff;*/
            int nearest = (i / width) / 2 * (width / 2) + (i % width) / 2;

            int y = yuv[i] & 0x000000ff;
            int u = yuv[iterations + nearest] & 0x000000ff;


            int v = yuv[iterations + iterations / 4 + nearest] & 0x000000ff;

            //int b = (int)(1.164*(y-16) + 2.018*(u-128));
            //int g = (int)(1.164*(y-16) - 0.813*(v-128) - 0.391*(u-128));
            //int r = (int)(1.164*(y-16) + 1.596*(v-128));

            //double Y = (y/255.0);
            //double Pr = (u/255.0-0.5);
            //double Pb = (v/255.0-0.5);



    /*int b = (int)(1.164*(y-16)+1.8556*(u-128));

    int g = (int)(1.164*(y-16) - (0.4681*(v-128) + 0.1872*(u-128)));
    int r = (int)(1.164*(y-16)+1.5748*(v-128));*/

            int b = (int) (y + 1.8556 * (u - 128));

            int g = (int) (y - (0.4681 * (v - 128) + 0.1872 * (u - 128)));

            int r = (int) (y + 1.5748 * (v - 128));


    /*double B = Y+1.8556*Pb;

    double G = Y - (0.4681*Pr + 0.1872*Pb);
    double R = Y+1.5748*Pr;*/

            //int b = (int)B*255;
            //int g = (int)G*255;
            //int r = (int)R*255;


            if (b > 255) {
                b = 255;
            } else if (b < 0) {
                b = 0;
            }
            if (g > 255) {
                g = 255;
            } else if (g < 0) {
                g = 0;
            }
            if (r > 255) {
                r = 255;
            } else if (r < 0) {
                r = 0;
            }

    /*rgb[i]=(byte)b;
    rgb[i+1]=(byte)g;
    rgb[i+2]=(byte)r;*/
            int targetPosition = i;

            if (invertHeight) {
                targetPosition = ((height - 1) - targetPosition / width) * width + (targetPosition % width);
            }
            if (invertWidth) {
                targetPosition = (targetPosition / width) * width + (width - 1) - (targetPosition % width);
            }


            rgb[targetPosition] = (0xff000000) | (0x00ff0000 & r << 16) | (0x0000ff00 & g << 8) | (0x000000ff & b);
        }
        return rgb;

    }

    // Copy the bytes out of |src| and into |dst|, ignoring and overwriting
    // positon & limit in both buffers.
//** copied from org/webrtc/VideoRenderer.java **//
    private static void copy_Plane(ByteBuffer src, ByteBuffer dst) {
        src.position(0).limit(src.capacity());
        dst.put(src);
        dst.position(0).limit(dst.capacity());
    }

    public static android.graphics.YuvImage ConvertTo(org.webrtc.VideoRenderer.I420Frame src, int imageFormat) {
        switch (imageFormat) {
            default:
                return null;

            case android.graphics.ImageFormat.YV12: {
                byte[] bytes = new byte[src.yuvStrides[0] * src.height +
                        src.yuvStrides[1] * src.height / 2 +
                        src.yuvStrides[2] * src.height / 2];
                ByteBuffer tmp = ByteBuffer.wrap(bytes, 0, src.yuvStrides[0] * src.height);
                copy_Plane(src.yuvPlanes[0], tmp);
                tmp = ByteBuffer.wrap(bytes, src.yuvStrides[0] * src.height, src.yuvStrides[2] * src.height / 2);
                copy_Plane(src.yuvPlanes[2], tmp);
                tmp = ByteBuffer.wrap(bytes, src.yuvStrides[0] * src.height + src.yuvStrides[2] * src.height / 2, src.yuvStrides[1] * src.height / 2);
                copy_Plane(src.yuvPlanes[1], tmp);
                int[] strides = src.yuvStrides.clone();
                return new YuvImage(bytes, imageFormat, src.width, src.height, strides);
            }

            case android.graphics.ImageFormat.NV21: {
                if (src.yuvStrides[0] != src.width)
                    return convertLineByLine(src);
                if (src.yuvStrides[1] != src.width / 2)
                    return convertLineByLine(src);
                if (src.yuvStrides[2] != src.width / 2)
                    return convertLineByLine(src);

                byte[] bytes = new byte[src.yuvStrides[0] * src.height +
                        src.yuvStrides[1] * src.height / 2 +
                        src.yuvStrides[2] * src.height / 2];
                ByteBuffer tmp = ByteBuffer.wrap(bytes, 0, src.width * src.height);
                copy_Plane(src.yuvPlanes[0], tmp);

                byte[] tmparray = new byte[src.width / 2 * src.height / 2];
                tmp = ByteBuffer.wrap(tmparray, 0, src.width / 2 * src.height / 2);

                copy_Plane(src.yuvPlanes[2], tmp);
                for (int row = 0; row < src.height / 2; row++) {
                    for (int col = 0; col < src.width / 2; col++) {
                        bytes[src.width * src.height + row * src.width + col * 2] = tmparray[row * src.width / 2 + col];
                    }
                }
                copy_Plane(src.yuvPlanes[1], tmp);
                for (int row = 0; row < src.height / 2; row++) {
                    for (int col = 0; col < src.width / 2; col++) {
                        bytes[src.width * src.height + row * src.width + col * 2 + 1] = tmparray[row * src.width / 2 + col];
                    }
                }
                return new YuvImage(bytes, imageFormat, src.width, src.height, null);
            }
        }
    }

    public static android.graphics.YuvImage convertLineByLine(org.webrtc.VideoRenderer.I420Frame src) {
        byte[] bytes = new byte[src.width * src.height * 3 / 2];
        int i = 0;
        for (int row = 0; row < src.height; row++) {
            for (int col = 0; col < src.width; col++) {
                bytes[i++] = src.yuvPlanes[0].array()[col + row * src.yuvStrides[0]];
            }
        }
        for (int row = 0; row < src.height / 2; row++) {
            for (int col = 0; col < src.width / 2; col++) {
                bytes[i++] = src.yuvPlanes[2].array()[col + row * src.yuvStrides[2]];
                bytes[i++] = src.yuvPlanes[1].array()[col + row * src.yuvStrides[1]];
            }
        }
        return new YuvImage(bytes, android.graphics.ImageFormat.NV21, src.width, src.height, null);

    }

    public static byte[] I420toBGRA(byte[] yuv, int width, int height) {

        int iterations = width * height;
        byte[] rgb = new byte[iterations * 4];
        for (int i = 0; i < rgb.length; i++) {
            rgb[i] = 0x00;
        }

        int[] lut = new int[256];
        double gamma = 1 / 2.2;

        for (int i = 0; i < 256; i++) {
            lut[i] = (int) (Math.exp(Math.log(i / 255.0) * gamma) * 255.0);
        }

        for (int i = 0; i < iterations; i++) {
            int nearest = (i / width) / 2 * (width / 2) + (i % width) / 2;

            int y = yuv[i] & 0x000000ff;
            int u = yuv[iterations + nearest] & 0x000000ff;

            int idx = iterations + iterations / 4 + nearest;
            if (idx >= (yuv.length - 1)) {
                continue;
            }
            int v = yuv[idx] & 0x000000ff;

            int b = (int) (1.164383 * (y - 16) + 2.017230 * (u - 128) + 0);

            int g = (int) (1.164383 * (y - 16) - 0.391762 * (u - 128) - 0.812969 * (v - 128));

            int r = (int) (1.164383 * (y - 16) + 0 + 1.596016 * (v - 128));


            if (b > 255) {
                b = 255;
            } else if (b < 0) {
                b = 0;
            }
            if (g > 255) {
                g = 255;
            } else if (g < 0) {
                g = 0;
            }
            if (r > 255) {
                r = 255;
            } else if (r < 0) {
                r = 0;
            }


            rgb[i * 4 + 3] = (byte) b;
            rgb[i * 4 + 2] = (byte) g;
            rgb[i * 4 + 1] = (byte) r;
            rgb[i * 4 + 0] = (byte) 0xff;
        }
        return rgb;

    }

    private Bitmap captureBitmapFromTexture(org.webrtc.VideoRenderer.I420Frame i420Frame) {
        int width = i420Frame.rotatedWidth();
        int height = i420Frame.rotatedHeight();
        int outputFrameSize = width * height * 3 / 2;
        ByteBuffer outputFrameBuffer = ByteBuffer.allocateDirect(outputFrameSize);
        final float frameAspectRatio = (float) i420Frame.rotatedWidth() /
                (float) i420Frame.rotatedHeight();
        final float[] rotatedSamplingMatrix =
                RendererCommon.rotateTextureMatrix(i420Frame.samplingMatrix,
                        i420Frame.rotationDegree);
        final float[] layoutMatrix = RendererCommon.getLayoutMatrix(false,
                frameAspectRatio,
                (float) width / height);
        final float[] texMatrix = RendererCommon.multiplyMatrices(rotatedSamplingMatrix,
                layoutMatrix);
        /*
         * YuvConverter must be instantiated on a thread that has an active EGL context. We know
         * that renderFrame is called from the correct render thread therefore
         * we defer instantiation of the converter until frame arrives.
         */
        YuvConverter yuvConverter = new YuvConverter();
        yuvConverter.convert(outputFrameBuffer,
                width,
                height,
                width,
                i420Frame.textureId,
                texMatrix);

        // Now we need to unpack the YUV data into planes
        byte[] data = outputFrameBuffer.array();
        int offset = outputFrameBuffer.arrayOffset();
        int stride = width;
        ByteBuffer[] yuvPlanes = new ByteBuffer[]{
                ByteBuffer.allocateDirect(width * height),
                ByteBuffer.allocateDirect(width * height / 4),
                ByteBuffer.allocateDirect(width * height / 4)
        };
        int[] yuvStrides = new int[]{
                width,
                (width + 1) / 2,
                (width + 1) / 2
        };

        // Write Y
        yuvPlanes[0].put(data, offset, width * height);

        // Write U
        for (int r = height; r < height * 3 / 2; ++r) {
            yuvPlanes[1].put(data, offset + r * stride, stride / 2);
        }

        // Write V
        for (int r = height; r < height * 3 / 2; ++r) {
            yuvPlanes[2].put(data, offset + r * stride + stride / 2, stride / 2);
        }

        // Convert the YuvImage
        YuvImage yuvImage = i420ToYuvImage(yuvPlanes, yuvStrides, width, height);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Rect rect = new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight());

        // Compress YuvImage to jpeg
        yuvImage.compressToJpeg(rect, 100, stream);

        // Convert jpeg to Bitmap
        byte[] imageBytes = stream.toByteArray();

        // Release YUV Converter
        yuvConverter.release();

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
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
        return new YuvImage(bytes, android.graphics.ImageFormat.NV21, width, height, null);
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
        return new YuvImage(bytes, android.graphics.ImageFormat.NV21, width, height, null);
    }

    private void copyPlane(ByteBuffer src, ByteBuffer dst) {
        src.position(0).limit(src.capacity());
        dst.put(src);
        dst.position(0).limit(dst.capacity());
    }
}


