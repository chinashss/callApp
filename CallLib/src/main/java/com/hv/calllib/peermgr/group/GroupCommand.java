package com.hv.calllib.peermgr.group;


import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.List;

import com.hv.calllib.peermgr.WebSocketPeer;

/**
 * Created by liuhongyu on 2016/12/14.
 */

public interface GroupCommand {
    public static final String  CallResp_Allow = "";
    public static final String  CallResp_Deny  = "";

    /**
     *  注册事件响应函数
     */
    void RegisterEvent( GroupCommand.CmdEvents events );

    /**
     *  JoinRoom to message server
     */
    void JoinCallGroup( final String uid,final String roomNo );

    /**
     * Send offer SDP to the other participant.
     */
    void ReceiveVideoFrom( final String sender,final SessionDescription sdp );

    /**
     * Send Ice candidate to the other participant.
     */
    void sendLocalIceCandidate( final String uid,final IceCandidate candidate);

    /**
     * Send removed ICE candidates to the other participant.
     */
    void sendLocalIceCandidateRemovals(final IceCandidate[] candidates);

    void SetNetState( WebSocketPeer.ConnectionState state );

    /**
     *  send stop
     */
    void LeaveGroup();

    /**
     *  onwebsocketmessage
     *  called by the peerwebsocket
     */
    void onSocketMessage(String message);

    void onSocketClose();

    void onSocketError(String description);

    void onSocketOpen( boolean isServer );

    class CmdParameters {
        public final List<PeerConnection.IceServer> iceServers;
        //public final boolean initiator;
        public final String clientId;
        public final String wssUrl;
        public final SessionDescription offerSdp;
        public final List<IceCandidate> iceCandidates;

        public CmdParameters(List<PeerConnection.IceServer> iceServers,
                                   /*boolean initiator,*/
                             String clientId,String wssUrl,
                             SessionDescription offerSdp,
                             List<IceCandidate> iceCandidates
        ) {
            this.iceServers = iceServers;
            //this.initiator  = initiator;
            this.clientId   = clientId;
            this.wssUrl     = wssUrl;

            this.offerSdp       = offerSdp;
            this.iceCandidates  = iceCandidates;
        }
    }

    /**
     * Callback interface for messages delivered on signaling channel.
     *
     * <p>Methods are guaranteed to be invoked on the UI thread of |activity|.
     */
    interface CmdEvents {
        /**
         *  对端停止视音频通讯
         */
        void onJoinRoom( String name,String roomNo,boolean diableMic,boolean result );

        /**
         *  主叫发送终止呼叫过程
         */
        void onUserLeaved( String name );

        void onUserExistList( ArrayList roomGuests );

        void onUserEnter( String name );

        /**
         * Callback fired once remote SDP is received.
         */
        void onRemoteDescription( String name,final SessionDescription sdp);

        /**
         * Callback fired once remote Ice candidate is received.
         */
        void onRemoteIceCandidate( String name,final IceCandidate candidate);

        /**
         * Callback fired once remote Ice candidate removals are received.
         */
        void onRemoteIceCandidatesRemoved(final IceCandidate[] candidates);

        void onRemoteCaptureJPEG(String fromuid, String touid);
    }
}
