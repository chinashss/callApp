package com.hv.calllib.peermgr.group;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.VideoRenderer;

import com.hv.calllib.peermgr.CmdParameters;
import com.hv.calllib.peermgr.PeerConnectionEvents;
import com.hv.calllib.peermgr.PeerConnectionParameters;
import com.hv.calllib.peermgr.StreamVideoCallPeer;


/**
 * Created by liuhongyu on 2017/2/13.
 */

public class SubscribeVideoPeer extends StreamVideoCallPeer implements PeerConnectionEvents {

    private static final String TAG = "SubscribeVideoPeer";

    private String HostPeerID ;
    private Handler mUIHandler                              = null;
    private static WebSocketGroupComand mGroupCommandPeer   = null;
    private boolean iceConnected = false;

    protected SubscribeVideoPeer( Handler handler,WebSocketGroupComand mcdPeer,String uid,boolean hasVideo,boolean hasAudio,final Context context, final PeerConnectionParameters paramPeer ) {

        mGroupCommandPeer   = mcdPeer;
        mUIHandler          = handler;
        HostPeerID          = uid;
        iceConnected        = false;
        events              = this;
        bHasVideo           = hasVideo;
        bHasAudio           = hasAudio;

        remoteRender        = null;

        peerConnectionParameters = paramPeer;
        peerConnectionParameters.videoCallEnabled = bHasVideo;

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

        //  创建一个单线程定时执行的程序
        //createPeerConnectionFactory( context, peerConnectionParameters,this);
    }

    public boolean isbHasVideo() {
        return bHasVideo;
    }

    public boolean isbHasAudio() {
        return bHasAudio;
    }

    public boolean isIceConnected() {
        return iceConnected;
    }

    public void createPeerConnection(final EglBase.Context renderEGLContext,
                                     final VideoRenderer.Callbacks RemoteRender,
                                     final CmdParameters params,
                                     final PeerConnectionFactory fc ) {

        if (peerConnectionParameters == null) {
            Log.e(TAG, "Creating peer connection without initializing factory.");
            return;
        }

        this.localRender    = null;
        this.remoteRender   = RemoteRender;
        this.videoCapturer  = null;
        this.mParameters    = params;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    createMediaConstraintsInternal( false,true );
                    createPeerConnectionInternal(renderEGLContext,false,fc );
                } catch (Exception e) {
                    reportError("Failed to create peer connection: " + e.getMessage());
                    throw e;
                }
            }
        });
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

    public void onAddRemoteIceCandidate( final IceCandidate candidate ) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                addRemoteIceCandidate(candidate);
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
                    mGroupCommandPeer.ReceiveVideoFrom( HostPeerID,sdp );
                }
                if (peerConnectionParameters.videoMaxBitrate > 0) {
                    Log.d(TAG, "Set video maximum bitrate: " + peerConnectionParameters.videoMaxBitrate);
                    setVideoMaxBitrate(peerConnectionParameters.videoMaxBitrate);
                }
            }
        });

    }

    @Override
    public void onIceCandidate( final IceCandidate candidate){

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

    public void SetRemoteDescription(final SessionDescription sdp)
    {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if( mUIHandler != null ) {
                    if (sdp == null) { //  rejected by the remote
                        mUIHandler.sendEmptyMessage(VideoCallGroup.VideoCall_SubscribeStop);
                    } else {

                        setRemoteDescription(sdp);
                        //mUIHandler.sendEmptyMessage(VideoCallGroup.VideoCall_SubscribeStart);
                    }
                }
            }
        });
    }

    @Override
    public void onIceConnected(){

        executor.execute(new Runnable() {
            @Override
            public void run() {
                if( mUIHandler != null ) {
                    Message msg = new Message();
                    msg.what = VideoCallGroup.VideoCall_SubscribeStart;
                    msg.obj = (String) HostPeerID;

                    mUIHandler.sendMessage(msg);

                    iceConnected = true;
                    //callConnected();
                    //mUIHandler.sendEmptyMessage(VideoCallGroup.VideoCall_Event_OnCall);
                }
            }
        });

    }

    @Override
    public void onIceDisconnected(){

        executor.execute(new Runnable() {
            @Override
            public void run() {
                if( mUIHandler!= null ) {
                    //Message msg = new Message();
                    //msg.what = VideoCallGroup.VideoCall_SubscribeStop;
                    //msg.obj = (String) HostPeerID;

                   // mUIHandler.sendMessage(msg);
                    //mUIHandler.sendEmptyMessage(VideoCallGroup.VideoCall_Event_Disconnect);
                    //iceConnected = false;
                    //disconnect();
                    iceConnected = false;
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
