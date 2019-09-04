package com.hv.calllib.peermgr.group;

import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.voiceengine.WebRtcAudioManager;
import org.webrtc.voiceengine.WebRtcAudioUtils;

import android.content.Context;
import android.os.Handler;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import android.os.Message;
import android.util.Log;

import com.hv.calllib.bean.CaptureImageEvent;
import com.hv.calllib.peermgr.CmdParameters;
import com.hv.calllib.peermgr.PeerConnectionParameters;
import com.hv.calllib.peermgr.WebSocketPeer;

/**
 * Created by liuhongyu on 2017/4/25.
 */

public class VideoCallGroup implements GroupCommand.CmdEvents {
    private static final String TAG = "VideoCallGroup";
    public static final int     MaxSubStreamCount       = 2;

    //protected final ScheduledExecutorService            executor;
    private String              roomNo                  = null;
    private String              hostUserID              = null;         //  本端用户id
    private boolean             disableMic              = false;
    private boolean             inRoom                  = false;
    private Context             mContext                = null;
    private WebSocketPeer wsPeerClient            = null;
    private PublishVideoPeer    mPublishVideoCallPeer   = null;
    private PeerConnectionParameters mPeerparams        = null;
    private Map<String,SubscribeVideoPeer> mSubcribeVideoCallPeers = null;
    private Iterator iterSubscribe = null;

    private WebSocketGroupComand mGroupCommandPeer = null;

    public static final int VideoCall_Event_Disconnect= -1;
    public static final int VideoCall_Event_Calling   = 0;
    public static final int VideoCall_Event_OnCall    = 1;
    public static final int VideoCall_Event_NewUser   = 2;
    public static final int VideoCall_SubscribeStop   = 3;
    public static final int VideoCall_SubscribeStart  = 4;
    public static final int VideoCall_Event_CaptureJPEG  = 5;

    private EglBase.Context mRenderEGLContext             = null;
    private VideoRenderer.Callbacks mLocalRender          = null;
    //private final HashMap<String,VideoRenderer.Callbacks> mapRemoteRenders  = new HashMap<>();

    protected PeerConnectionFactory factory               = null;

    private VideoCapturer mVideoCapturer                  = null;
    private CmdParameters mCmdParams                      = null;

    private Handler mUIHandler                            = null;

    private boolean videoEnabled = true;
    //private LinkedList<Object> llStartRenders = null;

    public int GetSubScribeSize() {
        return mSubcribeVideoCallPeers.size();
    }

    public boolean HasVideo(String uid ) {

        SubscribeVideoPeer subscribe = mSubcribeVideoCallPeers.get(uid);
        if( subscribe == null  ) {
            return false;
        }

        return subscribe.isbHasVideo();
    }

    public void StartVideoCall( final EglBase.Context renderEGLContext,
                                final VideoRenderer.Callbacks localRender,
                                //final List<VideoRenderer.Callbacks> remoteRenders,
                                final VideoCapturer videoCapturer,
                                final CmdParameters params ) {

        mRenderEGLContext = renderEGLContext;
        mLocalRender      = localRender;
        //mRemoteRenders    = remoteRenders;
        mVideoCapturer    = videoCapturer;
        mCmdParams        = params;

        if (mPublishVideoCallPeer != null){
            mPublishVideoCallPeer.createPeerConnection(mRenderEGLContext, mLocalRender, mVideoCapturer,mCmdParams,factory );

        }
    }

    public void close() {
        for (String key : mSubcribeVideoCallPeers.keySet()){
            SubscribeVideoPeer subscribe = mSubcribeVideoCallPeers.get(key);
            if( subscribe != null ) {
                subscribe.close(false);
                subscribe = null;
            }
        }

        mSubcribeVideoCallPeers.clear();

        if( mPublishVideoCallPeer != null ) {
            mPublishVideoCallPeer.close(false);
            mPublishVideoCallPeer = null;
        }

        if( wsPeerClient != null ) {
            wsPeerClient = null;
        }

        if(  mGroupCommandPeer != null ) {
            mGroupCommandPeer = null;
        }

        if( mRenderEGLContext != null ) {
            mRenderEGLContext = null;
        }

        if( mPeerparams != null ) {
            mPeerparams = null;
        }

        if( mLocalRender!= null ){
            mLocalRender = null;
        }

        if( mVideoCapturer!= null ) {
            mVideoCapturer = null;
        }

        if( mCmdParams!= null ) {
            mCmdParams = null;
        }

        if( mUIHandler != null ) {
            mUIHandler = null;
        }
    }

    public void FreeFactory() {
        if( factory!= null ) {
            factory.dispose();
            factory = null;
        }
    }

    private void createPeerConnectionFactoryInternal() {
        if( factory == null && mPeerparams != null) {

            // Initialize field trials.
            PeerConnectionFactory.initializeFieldTrials("");

            // Check preferred video codec.
            // Enable/disable OpenSL ES playback.
            if (mPeerparams != null && !mPeerparams.useOpenSLES) {
                Log.d(TAG, "Disable OpenSL ES audio even if device supports it");
                WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(true);
            } else {
                Log.d(TAG, "Allow OpenSL ES audio if device supports it");
                WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(false);
            }

            if (mPeerparams != null && mPeerparams.disableBuiltInAEC) {
                Log.d(TAG, "Disable built-in AEC even if device supports it");
                WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(true);
            } else {
                Log.d(TAG, "Enable built-in AEC if device supports it");
                WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(false);
            }

            if (mPeerparams != null && mPeerparams.disableBuiltInAGC) {
                Log.d(TAG, "Disable built-in AGC even if device supports it");
                WebRtcAudioUtils.setWebRtcBasedAutomaticGainControl(true);
            } else {
                Log.d(TAG, "Enable built-in AGC if device supports it");
                WebRtcAudioUtils.setWebRtcBasedAutomaticGainControl(false);
            }

            if (mPeerparams != null && mPeerparams.disableBuiltInNS) {
                Log.d(TAG, "Disable built-in NS even if device supports it");
                WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(true);
            } else {
                Log.d(TAG, "Enable built-in NS if device supports it");
                WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(false);
            }

            // Create peer connection factory.
            if (mPeerparams != null && !PeerConnectionFactory.initializeAndroidGlobals(
                    mContext, true, true, true)) {
                //events.onPeerConnectionError("Failed to initializeAndroidGlobals");
            }

            PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
            options.networkIgnoreMask = 0;

            factory = new PeerConnectionFactory( options );
            Log.d(TAG, "Peer connection factory created.");
        }
    }

    public void JoinRoom( String no ) {
        roomNo = no;
        if (mGroupCommandPeer != null) {
            mGroupCommandPeer.SetNetState( WebSocketPeer.ConnectionState.CONNECTED );
            mGroupCommandPeer.JoinCallGroup(hostUserID,roomNo );
        }
    }

    public void LeaveRoom(){
        if (mGroupCommandPeer != null) {
            mGroupCommandPeer.LeaveGroup();
        }
    }

    public int StartNextNewStream( String uid,boolean video,boolean audio,VideoRenderer.Callbacks render ) {

        if( hostUserID.compareTo(uid) == 0 ) {
            return -1;
        }

        if(mPublishVideoCallPeer == null || !mPublishVideoCallPeer.isIceConnected() ){
            return -3;
        }

        int count = mSubcribeVideoCallPeers.size();
        if( count< MaxSubStreamCount ) {

            SubscribeVideoPeer subscribe = new SubscribeVideoPeer(mUIHandler,mGroupCommandPeer,uid,video,audio,mContext, mPeerparams);
            //subscribe.setVideoEnabled(videoEnabled);
            subscribe.createPeerConnection(mRenderEGLContext, render,mCmdParams,factory );

            mSubcribeVideoCallPeers.put(uid,subscribe);
            subscribe.createOffer();
        }else{
            return -2;
        }

        return 0;
    }

    /*
    public void StartNextNewStream(){
        if( llStartRenders.size() > 0 ) {
            Object peer = llStartRenders.getFirst();

            if (llStartRenders.removeFirst() == null) {
                return;
            }

            if (peer != null) {
                if (peer instanceof SubscribeVideoPeer) {
                    SubscribeVideoPeer subscribe = (SubscribeVideoPeer) peer;
                    subscribe.createOffer();
                } else if (peer instanceof PublishVideoPeer) {
                    PublishVideoPeer publish = (PublishVideoPeer) peer;
                    publish.createOffer();
                }
            }
        }
    }
    */

    public void ReleaseSubscribe( String name ) {
        String gustUID = name;

        if( mSubcribeVideoCallPeers!= null ) {
            SubscribeVideoPeer subscribe = mSubcribeVideoCallPeers.get(gustUID);
            if( subscribe != null ) {
                subscribe.close( false );
                try {
                    Thread.sleep(500);
                }catch(InterruptedException e) {
                    e.printStackTrace();
                }
                mSubcribeVideoCallPeers.remove(gustUID);
                subscribe = null;
            }
        }
    }

    public void enableStatsEvents(boolean enable, int periodMs) {

    }

    public void switchCamera() {
        if( mPublishVideoCallPeer!= null ) {
            mPublishVideoCallPeer.switchCamera();
        }
    }

    public int Initialize(Handler handler, PeerConnectionParameters params, final Context context, String uid, WebSocketPeer ws , boolean videoEnabled) {
        if(ws == null || params == null){
            return -1;
        }
        mUIHandler        = handler;
        mContext          = context;
        mPeerparams       = params;
        hostUserID        = uid;
        wsPeerClient      = ws;
        this.videoEnabled = videoEnabled;
        Reset();

        createPeerConnectionFactoryInternal();

        if (wsPeerClient != null) {
            if (mGroupCommandPeer == null){
                mGroupCommandPeer = (WebSocketGroupComand) wsPeerClient.getGroupCommand();
                mGroupCommandPeer.RegisterEvent(this);
            }

            mGroupCommandPeer.SetNetState(wsPeerClient.GetConnectState());

            if (mPublishVideoCallPeer == null) {
                mPublishVideoCallPeer = new PublishVideoPeer(mUIHandler, mGroupCommandPeer, uid, context, mPeerparams);
                mPublishVideoCallPeer.setVideoEnabled(this.videoEnabled);
            }
        }else {
            return  -1;
        }

        return 0;
    }

    public int GetLocalUID() {
        return Integer.parseInt(hostUserID);
    }

    public VideoCallGroup( ) {

        Logging.d(TAG, "VideoCallGroup created.");

        if( mSubcribeVideoCallPeers == null ) {
            mSubcribeVideoCallPeers = new HashMap<String,SubscribeVideoPeer>();
        }

        mSubcribeVideoCallPeers.clear();

        /*
        if( llStartRenders == null ) {
            llStartRenders = new LinkedList<Object>();
        }

        llStartRenders.clear();
        */
    }

    public void Reset() {
        if( mSubcribeVideoCallPeers != null ) {
            mSubcribeVideoCallPeers.clear();
        }

        /*
        if( llStartRenders != null ) {
            llStartRenders.clear();
        }
        */
    }


    public void setPublishVideoEnabled(boolean videoEnabled){
        this.videoEnabled = videoEnabled;

        if (mPublishVideoCallPeer != null){
            mPublishVideoCallPeer.setVideoEnabled(videoEnabled);
        }
    }
    public void setAllVideoEnabled(boolean videoEnabled){
        this.videoEnabled = videoEnabled;

        if (mPublishVideoCallPeer != null){
            mPublishVideoCallPeer.setVideoEnabled(videoEnabled);
        }
        for (String key : mSubcribeVideoCallPeers.keySet()){
            SubscribeVideoPeer subscribe = mSubcribeVideoCallPeers.get(key);
            if( subscribe != null ) {
                subscribe.setVideoEnabled(videoEnabled);
            }
        }
    }
    public void setSubscribeVideoEnabled(boolean videoEnabled){
        this.videoEnabled = videoEnabled;

        for (String key : mSubcribeVideoCallPeers.keySet()){
            SubscribeVideoPeer subscribe = mSubcribeVideoCallPeers.get(key);
            if( subscribe != null ) {
                subscribe.setVideoEnabled(videoEnabled);
            }
        }
    }

    @Override
    public void onJoinRoom( String name,String roomNo,boolean mic,boolean result )
    {
        this.hostUserID  = name;
        this.inRoom      = result;
        this.disableMic  = mic;
        this.roomNo      = roomNo;
    }

    /**
     *  主叫发送终止呼叫过程
     */
    @Override
    public void onUserLeaved( String name )
    {
        if( mUIHandler!= null ) {
            Message msg = new Message();
            msg.what = VideoCallGroup.VideoCall_SubscribeStop;
            msg.obj = (String) name;

            mUIHandler.sendMessage(msg);
        }
    }

    /*
    @Override
    public void onUserExistList(  ArrayList roomGuests )
    {
        for( int i =0 ;i < roomGuests.size();i++)
        {
            String gustUID = (String)roomGuests.get(i);
            SubscribeVideoPeer subscribe = new SubscribeVideoPeer(mUIHandler,mGroupCommandPeer,gustUID,mContext, mPeerparams);
            subscribe.createPeerConnection(mRenderEGLContext, mRemoteRenders.get(i),mCmdParams,factory );

            llStartRenders.addLast( subscribe );
            mSubcribeVideoCallPeers.put(gustUID,subscribe);
            //subscribe.createOffer();

        }

        mPublishVideoCallPeer.createPeerConnection(mRenderEGLContext, mLocalRender, mVideoCapturer,mCmdParams,factory );
        //mPublishVideoCallPeer.createOffer();
        llStartRenders.addLast( mPublishVideoCallPeer );

        StartNextNewStream();
    }

    @Override
    public void onUserEnter( String name )
    {
        String gustUID = name;

        if( mSubcribeVideoCallPeers!= null ) {
            SubscribeVideoPeer subscribe = mSubcribeVideoCallPeers.get(gustUID);
            if( subscribe == null ) {
                int count = mSubcribeVideoCallPeers.size();
                if( count < 2 ) {
                    subscribe = new SubscribeVideoPeer(mUIHandler, mGroupCommandPeer, gustUID, mContext, mPeerparams);
                    mSubcribeVideoCallPeers.put(gustUID,subscribe);

                    subscribe.createPeerConnection(mRenderEGLContext, mRemoteRenders.get(count),mCmdParams,factory );
                    subscribe.createOffer();
                }
            }
        }

    }
    */

    @Override
    public void onUserExistList(  ArrayList roomGuests )
    {
        Log.i(TAG, "onUserExistList gest :" + roomGuests.toString());
        //mPublishVideoCallPeer.createPeerConnection(mRenderEGLContext, mLocalRender, mVideoCapturer,mCmdParams,factory );

        int nCount = ( MaxSubStreamCount < roomGuests.size() )?MaxSubStreamCount:roomGuests.size();
        for( int i =0 ;i < nCount;i++)
        {
            String gustUID = (String)roomGuests.get(i);
            /*
            SubscribeVideoPeer subscribe = new SubscribeVideoPeer(mUIHandler,mGroupCommandPeer,gustUID,mContext, mPeerparams);
            subscribe.createPeerConnection(mRenderEGLContext, mRemoteRenders.get(i),mCmdParams,factory );

            mSubcribeVideoCallPeers.put(gustUID,subscribe);
            */
            if( mUIHandler!= null ) {
                Message msg = new Message();
                msg.what = VideoCallGroup.VideoCall_Event_NewUser;
                msg.obj = (String) gustUID;

                mUIHandler.sendMessage(msg);
            }
        }

        //  通知逻辑
        if (mPublishVideoCallPeer != null) {
            mPublishVideoCallPeer.createOffer();
        }

    }

    @Override
    public void onUserEnter( String name )
    {
        Log.i(TAG, "onUserEnter name :" + name);

        String gustUID = name;

        if( mSubcribeVideoCallPeers!= null ) {
            SubscribeVideoPeer subscribe = mSubcribeVideoCallPeers.get(gustUID);
            if( subscribe == null ) {
                int count = mSubcribeVideoCallPeers.size();
                if( count < MaxSubStreamCount ) {
                    /*
                    subscribe = new SubscribeVideoPeer(mUIHandler, mGroupCommandPeer, gustUID, mContext, mPeerparams);
                    mSubcribeVideoCallPeers.put(gustUID,subscribe);

                    subscribe.createPeerConnection(mRenderEGLContext, mRemoteRenders.get(count),mCmdParams,factory );
                    */
                    //  通知逻辑
                    if( mUIHandler!= null ) {
                        Message msg = new Message();
                        msg.what = VideoCallGroup.VideoCall_Event_NewUser;
                        msg.obj = (String) gustUID;

                        mUIHandler.sendMessage(msg);

//                        if( mUIHandler!= null ) {
//                            //Message msg = new Message();
//                            msg.what = VideoCallGroup.VideoCall_Event_CaptureJPEG;
//                           // msg.obj = (String) ;
//
//                            mUIHandler.sendMessage(msg);
//                        }
                    }
                }
            }
        }

    }

    @Override
    public void onRemoteDescription( String name,final SessionDescription sdp)
    {
        if( name.compareTo(hostUserID) == 0 ) {
            if( mPublishVideoCallPeer != null ) {
                mPublishVideoCallPeer.SetRemoteDescription(sdp);
            }
        }else{
            if( mSubcribeVideoCallPeers!= null ) {
                SubscribeVideoPeer subscribe = mSubcribeVideoCallPeers.get(name);
                if( subscribe != null ) {
                    subscribe.SetRemoteDescription(sdp);
                }
            }
        }
    }

    @Override
    public void onRemoteIceCandidate( String name,final IceCandidate candidate)
    {
        if( name.compareTo(hostUserID) == 0 ) {
            if( mPublishVideoCallPeer != null ) {
                mPublishVideoCallPeer.onAddRemoteIceCandidate(candidate);
            }
        }else{
            if( mSubcribeVideoCallPeers!= null ) {
                SubscribeVideoPeer subscribe = mSubcribeVideoCallPeers.get(name);
                if (subscribe != null) {
                    subscribe.onAddRemoteIceCandidate(candidate);
                }
            }
        }
    }

    @Override
    public void onRemoteIceCandidatesRemoved(final IceCandidate[] candidates)
    {

    }

    @Override
    public void onRemoteCaptureJPEG(String fromuid, String touid) {
        if( mUIHandler!= null ) {
            Message msg = new Message();
            CaptureImageEvent event = new CaptureImageEvent();
            event.setFromeid(fromuid);
            event.setTouid(touid);
            msg.what = VideoCallGroup.VideoCall_Event_CaptureJPEG;
            msg.obj = event;
            mUIHandler.sendMessage(msg);
        }
    }

}
