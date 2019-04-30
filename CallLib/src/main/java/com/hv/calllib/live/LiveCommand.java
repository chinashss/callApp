package com.hv.calllib.live;

import com.hv.calllib.peermgr.WebSocketPeer;

import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.util.List;

/**
 * Created by liuhongyu on 2016/12/14.
 */

public interface LiveCommand {
    public static final String  CallResp_Allow = "";
    public static final String  CallResp_Deny  = "";

    /**
     *  注册事件响应函数
     */
    void RegisterEvent(LiveCommand.CmdEvents events);

    /**
     *  JoinRoom to message server
     */
    void Present(final String UserName, final SessionDescription sdpOffer);

    /**
     *
     */
    void PresentWithRTMP(final String UserName,final SessionDescription sdpOffer,final String rtmpURL );

    /**
     * Send Ice candidate to the other participant.
     */
    void sendLocalIceCandidate(final String uid, final IceCandidate candidate);

    /**
     * Send removed ICE candidates to the other participant.
     */
    void sendLocalIceCandidateRemovals(final IceCandidate[] candidates);

    void SetNetState(WebSocketPeer.ConnectionState state);

    /**
     *  onwebsocketmessage
     *  called by the peerwebsocket
     */
    void onSocketMessage(String message);

    void onSocketClose();

    void onSocketError(String description);

    void onSocketOpen(boolean isServer);

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
         * Callback fired once remote SDP is received.
         */
        void onRemoteDescription(final SessionDescription sdp);

        /**
         * Callback fired once remote Ice candidate is received.
         */
        void onRemoteIceCandidate(final IceCandidate candidate);

        /**
         * Callback fired once remote Ice candidate removals are received.
         */
        void onRemoteIceCandidatesRemoved(final IceCandidate[] candidates);
    }
}
