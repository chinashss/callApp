package com.hv.calllib.peermgr.group;

import android.os.Handler;
import android.util.Log;


import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.util.ArrayList;

import com.google.gson.Gson;
import com.hv.calllib.bean.MajorCommand;
import com.hv.calllib.group.JsonMsg;
import com.hv.calllib.peermgr.SSLWebSocketChannel;
import com.hv.calllib.peermgr.WebSocketPeer;


/**
 * Created by liuhongyu on 2017/2/13.
 */

public class WebSocketGroupComand implements GroupCommand {
    private static final String TAG = "WebSocketGroupComand";

    private final Handler handler;

    protected GroupCommand.CmdEvents events;

    private SSLWebSocketChannel wsChannel;
    private WebSocketPeer.ConnectionState mPeerState;

    private boolean is_Loopback = false;

    public WebSocketGroupComand(Handler handler, SSLWebSocketChannel channel) {

        this.events = null;
        wsChannel = channel;

        this.handler = handler;
        mPeerState = WebSocketPeer.ConnectionState.NEW;
    }

    @Override
    public void RegisterEvent(GroupCommand.CmdEvents events) {
        this.events = events;
    }

    @Override
    public void JoinCallGroup(final String uid, final String roomNo) {

        handler.post(new Runnable() {
            @Override
            public void run() {
                String json = JsonMsg.EncodeJoinRoom(uid, roomNo);

                wsChannel.send(json);
            }
        });
    }

    @Override
    public void LeaveGroup() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                String json = JsonMsg.EncodeLeave();

                wsChannel.send(json);
            }
        });
    }

    /**
     * 加入视音频群组
     *
     * @param uid 群组用户id
     * @param sdp 本端sdp
     */
    @Override
    public void ReceiveVideoFrom(final String uid, final SessionDescription sdp) {

        handler.post(new Runnable() {
            @Override
            public void run() {
                String json = JsonMsg.EncodeReceiveVideoFrom(uid, sdp.description);

                //is_initator = true;

                wsChannel.send(json);
            }
        });
    }

    // Send Ice candidate to the other participant.
    @Override
    public void sendLocalIceCandidate(final String uid, final IceCandidate candidate) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                String json = JsonMsg.EncodeOnIceCandidate(uid, candidate);

                // Call initiator sends ice candidates to GAE server.
                wsChannel.send(json);
            }
        });
    }

    // Send removed Ice candidates to the other participant.
    @Override
    public void sendLocalIceCandidateRemovals(final IceCandidate[] candidates) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                String json = JsonMsg.EncodeOnIceCandidateRemovals(candidates);
                if (json == null) return;

                wsChannel.send(json.toString());
            }
        });
    }

    @Override
    public void onSocketMessage(String message) {
        if (mPeerState != WebSocketPeer.ConnectionState.CONNECTED) {
            Log.e(TAG, "Got WebSocket message in non registered state.");
            return;
        }
        try {
            JSONObject json = JsonMsg.ParseJsonMsg(message);
            switch (json.getString("id")) {
                case JsonMsg.CALLMSG_ID_JOINGROUP_RESP:  // 进入房间应答
                    events.onJoinRoom(json.getString("name"),
                            json.getString("room"),
                            json.getBoolean("disableMic"),
                            json.getBoolean("result"));
                    break;
                case JsonMsg.CALLMSG_ID_USERENTER:       // 服务器返回用户进入房间
                    onUserEnterGroup(json.getString("name"));
                    break;
                case JsonMsg.CALLMSG_ID_EXISTUSERS:      // 服务器返回房间内用户列表
                    onGetRoomUserList(json.getJSONArray("data"));
                    break;
                case JsonMsg.CALLMSG_ID_ICECANDIDATE:    // 服务器返回remote icecandidate
                    onRemoteICECandidate(json);
                    break;
                case JsonMsg.CALLMSG_ID_RECEIVEVIDEOANSWER: //  服务器返回视频接受的sdpanswer
                    onReceiveVideoAnswerSDP(json.getString("name"), json.getString("sdpAnswer"));
                    break;
                case JsonMsg.CALLMSG_ID_ONLEAVEGROUP:   //  有用户离开房间
                    onUserLeaveGroup(json.getString("name"));
                    break;
                case JsonMsg.CALLMSG_ID_CAPTUREMJPEG:
                    onRemoteCaptureJPEG(json.getString("fromuid"), json.getString("touid"));
                    break;
                case JsonMsg.REMOTE_CTRL:
                    MajorCommand command = new Gson().fromJson(message, MajorCommand.class);
                    EventBus.getDefault().postSticky(command);
                    break;
                default:
                    break;
            }
        } catch (JSONException e) {
            reportError("WebSocket message JSON parsing error: " + e.toString());
        }
    }

    private void onRemoteICECandidate(JSONObject json) {
        try {
            events.onRemoteIceCandidate(json.getString("name"), JsonMsg.ParseCandidate(json));
        } catch (JSONException e) {
            reportError("WebSocket message JSON parsing error: " + e.toString());
        }
    }

    @Override
    public void SetNetState(WebSocketPeer.ConnectionState state) {
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
    public void onSocketOpen(boolean isServer) {

    }

    private void send(String message) {
        switch (mPeerState) {
            case NEW:
            case CONNECTED:
                // Store outgoing messages and send them after websocket client
                // is registered.
                wsChannel.send(message);
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

    protected void onGetRoomUserList(JSONArray json) {

        try {
            ArrayList roomGuests = new ArrayList();

            for (int i = 0; i < json.length(); i++) {
                roomGuests.add(json.getString(i));
            }

            events.onUserExistList(roomGuests);

        } catch (JSONException e) {
            reportError("WebSocket message JSON parsing error: " + e.toString());
        }

    }

    protected void onUserEnterGroup(String name) {
        events.onUserEnter(name);
    }

    protected void onUserLeaveGroup(String name) {
        events.onUserLeaved(name);
    }

    protected void onReceiveVideoAnswerSDP(String name, String sdpInfo) {
        SessionDescription sdp = new SessionDescription(SessionDescription.Type.ANSWER, sdpInfo);
        events.onRemoteDescription(name, sdp);
    }

    protected void onRemoteCaptureJPEG(String fromeuid, String touid) {
        events.onRemoteCaptureJPEG(fromeuid, touid);
    }

}
