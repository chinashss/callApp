package com.hv.calllib.peermgr.group;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;

import com.hv.calllib.peermgr.CmdParameters;
import com.hv.calllib.peermgr.StreamVideoCallPeer;
import com.hv.calllib.peermgr.PeerConnectionParameters;
import com.hv.calllib.peermgr.PeerConnectionEvents;

/**
 * Created by liuhongyu on 2017/2/13.
 */

public class PublishVideoPeer extends StreamVideoCallPeer implements PeerConnectionEvents {

    private static final String TAG = "PublishVideoPeer";
    private static WebSocketGroupComand mGroupCommandPeer = null;

    private String HostPeerID;
    private Handler mUIHandler   = null;
    private boolean iceConnected = false;

    protected PublishVideoPeer( Handler handler,WebSocketGroupComand mcdPeer,String uid,final Context context, final PeerConnectionParameters paramPeer ) {
        //  创建一个单线程定时执行的程序
        mGroupCommandPeer = mcdPeer;
        mUIHandler = handler;
        HostPeerID = uid;

        events = this;

        //peerConnectionParameters.videoCallEnabled = false;
        peerConnectionParameters = paramPeer;
        videoCallEnabled = peerConnectionParameters.videoCallEnabled;

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
    public void startPreview(){
//        executor.execute(new Runnable() {
//            @Override
//            public void run() {
//                if (videoCallEnabled && videoCapturer != null){
//                    videoCapturer.startCapture();
//                }
//            }
//        });
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
                if (mGroupCommandPeer != null) {
                   //
                    String temp = setAudioSampleRate(sdp.description,AUDIO_CODEC_OPUS,16000);
                    SessionDescription sdpOffer = new SessionDescription(SessionDescription.Type.OFFER,temp);

                    //mGroupCommandPeer.ReceiveVideoFrom( HostPeerID,sdp );
                    mGroupCommandPeer.ReceiveVideoFrom( HostPeerID,sdpOffer );
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
                if (mGroupCommandPeer != null) {
                    mGroupCommandPeer.sendLocalIceCandidate(HostPeerID,candidate);
                }
            }
        });
    }

    @Override
    public void onIceCandidatesRemoved(final IceCandidate[] candidates){

        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (mGroupCommandPeer != null) {
                    mGroupCommandPeer.sendLocalIceCandidateRemovals(candidates);
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
                    mUIHandler.sendEmptyMessage(VideoCallGroup.VideoCall_Event_OnCall);
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
                        mUIHandler.sendEmptyMessage(VideoCallGroup.VideoCall_Event_Disconnect);
                    } else {

                        setRemoteDescription(sdp);
                        mUIHandler.sendEmptyMessage(VideoCallGroup.VideoCall_Event_Calling);
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
                    mUIHandler.sendEmptyMessage(VideoCallGroup.VideoCall_Event_Disconnect);
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
