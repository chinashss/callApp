package com.hv.calllib.live;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.hv.calllib.peermgr.CmdParameters;
import com.hv.calllib.peermgr.PeerConnectionEvents;
import com.hv.calllib.peermgr.PeerConnectionParameters;
import com.hv.calllib.peermgr.StreamVideoCallPeer;

import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;

/**
 * Created by liuhongyu on 2017/2/13.
 */

public class PresentVideoPeer extends StreamVideoCallPeer implements PeerConnectionEvents {

    private static final String TAG = "PresentVideoPeer";
    private static WebSocketLiveComand mLiveCommandPeer = null;

    private String HostPeerID;
    private String PushRtmpURL;

    private Handler mUIHandler   = null;
    private boolean iceConnected = false;

    protected PresentVideoPeer( Handler handler,WebSocketLiveComand mcdPeer,String uid,String url,final Context context, final PeerConnectionParameters paramPeer ) {
        //  创建一个单线程定时执行的程序
        mLiveCommandPeer = mcdPeer;
        mUIHandler = handler;
        HostPeerID = uid;
        PushRtmpURL = url;
        events = this;

        //peerConnectionParameters.videoCallEnabled = false;
        peerConnectionParameters = paramPeer;
        videoCallEnabled = peerConnectionParameters.videoCallEnabled;

        //VIDEO_CODEC_H264;
        //VIDEO_CODEC_VP8;
        preferredVideoCodec = VIDEO_CODEC_VP8;
        if (videoCallEnabled && peerConnectionParameters.videoCodec != null) {
            if (peerConnectionParameters.videoCodec.equals(VIDEO_CODEC_VP9)) {
                preferredVideoCodec = VIDEO_CODEC_VP9;
            } else if (peerConnectionParameters.videoCodec.equals(VIDEO_CODEC_H264)) {
                preferredVideoCodec = VIDEO_CODEC_H264;
            }
        }
        Log.d(TAG, "Pereferred video codec: " + preferredVideoCodec);

        // Check if ISAC is used by default.
        preferIsac = peerConnectionParameters.audioCodec != null
                && peerConnectionParameters.audioCodec.equals(AUDIO_CODEC_ISAC);


        //createPeerConnectionFactory( context, peerConnectionParameters,this);
    }

    public void createPeerConnection(final EglBase.Context renderEGLContext,
                                     final VideoRenderer.Callbacks localRender,
                                     final VideoCapturer videoCapturer,
                                     final CmdParameters params,
                                     final PeerConnectionFactory fc) {

        if (peerConnectionParameters == null) {
            Log.e(TAG, "Creating peer connection without initializing factory.");
            return;
        }

        this.localRender    = localRender;
        this.remoteRender   = null;
        this.videoCapturer  = videoCapturer;
        this.mParameters    = params;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    createMediaConstraintsInternal( true,false );
                    createPeerConnectionInternal(renderEGLContext,true,fc );
                } catch (Exception e) {
                    reportError("Failed to create peer connection: " + e.getMessage());
                    throw e;
                }
            }
        });
    }

    public boolean isIceConnected() {
        return iceConnected;
    }

    public void createOffer() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (peerConnection != null && !isError) {
                    Log.d(TAG, "PC Create OFFER");
                    isInitiator = true;
                    peerConnection.createOffer(sdpObserver, sdpMediaConstraints);
                }
            }
        });
    }

    public void createAnswer() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (peerConnection != null && !isError) {
                    Log.d(TAG, "PC create ANSWER");
                    isInitiator = false;
                    peerConnection.createAnswer(sdpObserver, sdpMediaConstraints);
                }
            }
        });
    }

    @Override
    public void onLocalDescription(final SessionDescription sdp){

        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (mLiveCommandPeer != null) {
                   //
                    //mLiveCommandPeer.Present( HostPeerID,sdp );
                    mLiveCommandPeer.PresentWithRTMP( HostPeerID,sdp,PushRtmpURL );
                }
                if (peerConnectionParameters.videoMaxBitrate > 0) {
                    Log.d(TAG, "Set video maximum bitrate: " + peerConnectionParameters.videoMaxBitrate);
                    setVideoMaxBitrate(peerConnectionParameters.videoMaxBitrate);
                }
            }
        });

    }

    @Override
    public void onIceCandidate(final IceCandidate candidate){

        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (mLiveCommandPeer != null) {
                    mLiveCommandPeer.sendLocalIceCandidate(HostPeerID,candidate);
                }
            }
        });
    }

    @Override
    public void onIceCandidatesRemoved(final IceCandidate[] candidates){

        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (mLiveCommandPeer != null) {
                    mLiveCommandPeer.sendLocalIceCandidateRemovals(candidates);
                }
            }
        });
    }

    @Override
    public void onIceConnected(){

        executor.execute(new Runnable() {
            @Override
            public void run() {
                iceConnected = true;
                if( mUIHandler != null ) {
                    //iceConnected = true;
                    //callConnected();
                    mUIHandler.sendEmptyMessage(VideoCallLive.VideoCall_Event_onPresent);
                }
            }
        });

    }

    public void SetRemoteDescription(final SessionDescription sdp)
    {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if( mUIHandler != null ) {
                    if (sdp == null) { //  rejected by the remote
                        mUIHandler.sendEmptyMessage(VideoCallLive.VideoCall_Event_Disconnect);
                    } else {

                        setRemoteDescription(sdp);
                        mUIHandler.sendEmptyMessage(VideoCallLive.VideoCall_Event_StartPresenting);
                    }
                }
            }
        });
    }

    public void onAddRemoteIceCandidate( final IceCandidate candidate ) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                addRemoteIceCandidate(candidate);
            }
        });
    }

    @Override
    public void onIceDisconnected(){

        executor.execute(new Runnable() {
            @Override
            public void run() {
                iceConnected = false;
                if( mUIHandler != null ) {
                    mUIHandler.sendEmptyMessage(VideoCallLive.VideoCall_Event_Disconnect);
                    //iceConnected = false;
                    //disconnect();
                }
            }
        });

    }

    @Override
    public void onPeerConnectionClosed(){

    }

    @Override
    public void onPeerConnectionStatsReady(final StatsReport[] reports){

    }

    @Override
    public void onPeerConnectionError(final String description){

    }
}
