package com.hv.calllib.peermgr;

/**
 * Created by liuhongyu on 2017/5/3.
 */

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;

/**
 * Peer connection events.
 * 这里主要是peerconnection(打洞部分）的事件
 */
public interface PeerConnectionEvents {
    /**
     * Callback fired once local SDP is created and set.
     */
    void onLocalDescription(final SessionDescription sdp);

    /**
     * Callback fired once local Ice candidate is generated.
     */
    void onIceCandidate(final IceCandidate candidate);

    /**
     * Callback fired once local ICE candidates are removed.
     */
    void onIceCandidatesRemoved(final IceCandidate[] candidates);

    /**
     * Callback fired once connection is established (IceConnectionState is
     * CONNECTED).
     */
    void onIceConnected();

    /**
     * Callback fired once connection is closed (IceConnectionState is
     * DISCONNECTED).
     */
    void onIceDisconnected();

    /**
     * Callback fired once peer connection is closed.
     */
    void onPeerConnectionClosed();

    /**
     * Callback fired once peer connection statistics is ready.
     */
    void onPeerConnectionStatsReady(final StatsReport[] reports);

    /**
     * Callback fired once peer connection error happened.
     */
    void onPeerConnectionError(final String description);
}

