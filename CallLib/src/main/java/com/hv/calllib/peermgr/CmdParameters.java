package com.hv.calllib.peermgr;

import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.util.List;

/**
 * Created by liuhongyu on 2017/4/25.
 */

public class CmdParameters {
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
