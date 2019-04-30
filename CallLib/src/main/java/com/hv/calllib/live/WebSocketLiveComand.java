package com.hv.calllib.live;

import android.os.Handler;
import android.util.Log;

import com.hv.calllib.peermgr.SSLWebSocketChannel;
import com.hv.calllib.peermgr.WebSocketPeer;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

/**
 * Created by liuhongyu on 2017/2/13.
 */

public class WebSocketLiveComand implements LiveCommand {
    private static final String TAG = "WebSocketLiveComand";

    private final Handler handler;

    protected LiveCommand.CmdEvents events;

    private SSLWebSocketChannel wsChannel;
    private WebSocketPeer.ConnectionState          mPeerState;

    private boolean         is_Loopback = false;

    public WebSocketLiveComand( Handler handler,SSLWebSocketChannel channel ) {

        this.events       = null;
        wsChannel         = channel;

        this.handler = handler;
        mPeerState = WebSocketPeer.ConnectionState.NEW;
    }

    @Override
    public void RegisterEvent( LiveCommand.CmdEvents events ) {
        this.events = events;
    }

    @Override
    public void Present( final String userName,final SessionDescription sdp ) {

        handler.post(new Runnable() {
            @Override
            public void run() {
                String json = JsonMsg.EncodePresent( userName,sdp.description );

                wsChannel.send(json);
            }
        });
    }

    @Override
    public void PresentWithRTMP( final String userName,final SessionDescription sdp,final String rtmpURL ) {

        handler.post(new Runnable() {
            @Override
            public void run() {
                String json = JsonMsg.EncodePresentWithRTMP( userName,sdp.description,rtmpURL );

                wsChannel.send(json);
            }
        });
    }

    // Send Ice candidate to the other participant.
    @Override
    public void sendLocalIceCandidate(final String uid,final IceCandidate candidate) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                String json = JsonMsg.EncodeOnIceCandidate( uid,candidate );

                // Call initiator sends ice candidates to GAE server.
                wsChannel.send( json );
            }
        });
    }

    // Send removed Ice candidates to the other participant.
    @Override
    public void sendLocalIceCandidateRemovals(final IceCandidate[] candidates) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                String json = JsonMsg.EncodeOnIceCandidateRemovals( candidates );
                if( json == null )return ;

                wsChannel.send(json.toString());
            }
        });
    }

    @Override
    public void onSocketMessage(String message) {
        if ( mPeerState != WebSocketPeer.ConnectionState.CONNECTED) {
            Log.e(TAG, "Got WebSocket message in non registered state.");
            return;
        }
        try {
            JSONObject json = JsonMsg.ParseJsonMsg(message);

            switch (json.getString("id")) {
                case JsonMsg.CALLMSG_ID_PRESENT_RESP:  // 进入房间应答
                    onPresentResp(json.getString("response"),
                                  json.getString("sdpAnswer"));
                    break;
                case JsonMsg.CALLMSG_ID_ICECANDIDATE:    // 服务器返回remote icecandidate
                    onRemoteICECandidate(json);
                    break;
                default:
                    break;
            }
        }catch (JSONException e) {
            reportError("WebSocket message JSON parsing error: " + e.toString());
        }
    }

    private void onRemoteICECandidate( JSONObject json ) {
        try {
            events.onRemoteIceCandidate( JsonMsg.ParseCandidate(json));
        }catch (JSONException e) {
            reportError("WebSocket message JSON parsing error: " + e.toString());
        }
    }

    @Override
    public void SetNetState( WebSocketPeer.ConnectionState state ) {
        this.mPeerState = state;
    }

    @Override
    public void onSocketClose() {
    }

    @Override
    public void onSocketError(String description) {
        reportError("WebSocket error: " + description);
    }

    @Override
    public void onSocketOpen( boolean isServer ) {

    }

    private void send(String message) {
        switch (mPeerState) {
            case NEW:
            case CONNECTED:
                // Store outgoing messages and send them after websocket client
                // is registered.
                wsChannel.send( message );
                return;
            case ERROR:
            case CLOSED:
                Log.e(TAG, "WebSocket send() in error or closed state : " + message);
                return;
        }
    }

    // --------------------------------------------------------------------
    // Helper functions.
    protected void reportError(final String errorMessage) {
        Log.e(TAG, errorMessage);
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mPeerState != WebSocketPeer.ConnectionState.ERROR) {
                    mPeerState = WebSocketPeer.ConnectionState.ERROR;

                }
            }
        });
    }

    protected void onPresentResp( String answer,String sdpInfo ) {
        SessionDescription sdp = new SessionDescription( SessionDescription.Type.ANSWER, sdpInfo );
        if(answer.compareToIgnoreCase("accepted") == 0 ) {
            events.onRemoteDescription( sdp );
        }
    }
}
