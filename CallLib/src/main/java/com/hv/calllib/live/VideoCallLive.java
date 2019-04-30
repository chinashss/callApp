package com.hv.calllib.live;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.hv.calllib.peermgr.CmdParameters;
import com.hv.calllib.peermgr.PeerConnectionParameters;
import com.hv.calllib.peermgr.WebSocketPeer;

import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.voiceengine.WebRtcAudioManager;
import org.webrtc.voiceengine.WebRtcAudioUtils;

/**
 * Created by liuhongyu on 2017/4/25.
 */

public class VideoCallLive implements LiveCommand.CmdEvents {
    private static final String TAG = "VideoCallLive";
    public static final int MaxSubStreamCount = 2;

    //protected final ScheduledExecutorService            executor;
    private String roomNo = null;
    private String hostUserID = null;         //  本端用户id

    private Context mContext = null;
    private WebSocketPeer wsPeerClient = null;
    private PresentVideoPeer mPresentVideoCallPeer = null;
    private PeerConnectionParameters mPeerparams = null;

    private WebSocketLiveComand mLiveCommandPeer = null;

    public static final int VideoCall_Event_Disconnect = -1;
    public static final int VideoCall_Event_StartPresenting = 0;
    public static final int VideoCall_Event_onPresent = 1;

    private EglBase.Context mRenderEGLContext = null;
    private VideoRenderer.Callbacks mLocalRender = null;

    protected PeerConnectionFactory factory = null;

    private VideoCapturer mVideoCapturer = null;
    private CmdParameters mCmdParams = null;

    private Handler mUIHandler = null;


    public VideoCapturer GetVideoCapture() {
        return mVideoCapturer;
    }


    public void StartVideoCall(final EglBase.Context renderEGLContext,
                               final VideoRenderer.Callbacks localRender,
                               final VideoCapturer videoCapturer,
                               final CmdParameters params) {

        mRenderEGLContext = renderEGLContext;
        mLocalRender = localRender;
        mVideoCapturer = videoCapturer;
        mCmdParams = params;
    }

    public void close() {
        if (mPresentVideoCallPeer != null) {
            mPresentVideoCallPeer.close(false);
            mPresentVideoCallPeer = null;
        }
        if (wsPeerClient != null) {
            wsPeerClient = null;
        }
        if (mLiveCommandPeer != null) {
            mLiveCommandPeer = null;
        }
        if (mRenderEGLContext != null) {
            mRenderEGLContext = null;
        }

        if (mPeerparams != null) {
            mPeerparams = null;
        }

        if (mLocalRender != null) {
            mLocalRender = null;
        }

        if (mVideoCapturer != null) {
            mVideoCapturer = null;
        }

        if (mCmdParams != null) {
            mCmdParams = null;
        }

        if (mUIHandler != null) {
            mUIHandler = null;
        }
    }

    public void FreeFactory() {
        if (factory != null) {
            factory.dispose();
            factory = null;
        }
    }

    private void createPeerConnectionFactoryInternal() {
        if (factory == null) {

            // Initialize field trials.
            PeerConnectionFactory.initializeFieldTrials("");

            // Check preferred video codec.
            // Enable/disable OpenSL ES playback.
            if (!mPeerparams.useOpenSLES) {
                Log.d(TAG, "Disable OpenSL ES audio even if device supports it");
                WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(true);
            } else {
                Log.d(TAG, "Allow OpenSL ES audio if device supports it");
                WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(false);
            }

            if (mPeerparams.disableBuiltInAEC) {
                Log.d(TAG, "Disable built-in AEC even if device supports it");
                WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(true);
            } else {
                Log.d(TAG, "Enable built-in AEC if device supports it");
                WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(false);
            }

            if (mPeerparams.disableBuiltInAGC) {
                Log.d(TAG, "Disable built-in AGC even if device supports it");
                WebRtcAudioUtils.setWebRtcBasedAutomaticGainControl(true);
            } else {
                Log.d(TAG, "Enable built-in AGC if device supports it");
                WebRtcAudioUtils.setWebRtcBasedAutomaticGainControl(false);
            }

            if (mPeerparams.disableBuiltInNS) {
                Log.d(TAG, "Disable built-in NS even if device supports it");
                WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(true);
            } else {
                Log.d(TAG, "Enable built-in NS if device supports it");
                WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(false);
            }

            // Create peer connection factory.
            if (!PeerConnectionFactory.initializeAndroidGlobals(
                    mContext, true, true, mPeerparams.videoCodecHwAcceleration)) {
                //events.onPeerConnectionError("Failed to initializeAndroidGlobals");
            }

            PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
            options.networkIgnoreMask = 0;

            factory = new PeerConnectionFactory(options);
            Log.d(TAG, "Peer connection factory created.");
        }
    }

    public void Present() {
        if (mLiveCommandPeer != null) {
            mLiveCommandPeer.SetNetState(WebSocketPeer.ConnectionState.CONNECTED);

            mPresentVideoCallPeer.createPeerConnection(mRenderEGLContext, mLocalRender, mVideoCapturer, mCmdParams, factory);
            mPresentVideoCallPeer.createOffer();
        }


    }

    public void enableStatsEvents(boolean enable, int periodMs) {

    }

    public void switchCamera() {
        if (mPresentVideoCallPeer != null) {
            mPresentVideoCallPeer.switchCamera();
        }
    }

    public int Initialize(Handler handler, PeerConnectionParameters params, final Context context, String uid, String url, WebSocketPeer ws) {
        mUIHandler = handler;
        mContext = context;
        mPeerparams = params;
        hostUserID = uid;
        wsPeerClient = ws;

        Reset();

        createPeerConnectionFactoryInternal();

        if (mLiveCommandPeer == null) {
            mLiveCommandPeer = (WebSocketLiveComand) wsPeerClient.getLiveCommand();
            mLiveCommandPeer.RegisterEvent(this);
        }

        mLiveCommandPeer.SetNetState(wsPeerClient.GetConnectState());

        if (mPresentVideoCallPeer == null) {
            mPresentVideoCallPeer = new PresentVideoPeer(mUIHandler, mLiveCommandPeer, uid, url, context, mPeerparams);
        }

        return 0;
    }

    public int GetLocalUID() {
        return Integer.parseInt(hostUserID);
    }

    public VideoCallLive() {
    }

    public void Reset() {

    }

    @Override
    public void onRemoteDescription(final SessionDescription sdp) {
        if (mPresentVideoCallPeer != null) {
            mPresentVideoCallPeer.SetRemoteDescription(sdp);
        }
    }

    @Override
    public void onRemoteIceCandidate(final IceCandidate candidate) {
        if (mPresentVideoCallPeer != null) {
            mPresentVideoCallPeer.onAddRemoteIceCandidate(candidate);
        }
    }

    @Override
    public void onRemoteIceCandidatesRemoved(final IceCandidate[] candidates) {

    }
}
