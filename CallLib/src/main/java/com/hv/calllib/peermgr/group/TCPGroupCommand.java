package com.hv.calllib.peermgr.group;

import android.util.Log;


import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import com.hv.calllib.peermgr.TCPChannel;
import com.hv.calllib.peermgr.WebSocketPeer;

/**
 * Created by liuhongyu on 2017/2/13.
 */

public class TCPGroupCommand implements GroupCommand {
    private static final String TAG = "DirectPeerClient";
    private static final int DEFAULT_PORT = 8888;

    // Regex pattern used for checking if room id looks like an IP.
    static final Pattern IP_PATTERN = Pattern.compile("("
            // IPv4
            + "((\\d+\\.){3}\\d+)|"
            // IPv6
            + "\\[((([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4})?::"
            + "(([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4})?)\\]|"
            + "\\[(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4})\\]|"
            // IPv6 without []
            + "((([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4})?::(([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4})?)|"
            + "(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4})|"
            // Literals
            + "localhost"
            + ")"
            // Optional port number
            + "(:(\\d+))?");

    private String hostUserID   = null;
    private final ExecutorService executor;
    private TCPGroupCommand.CmdEvents events;
    private TCPChannel tcpChannel;

    private enum ConnectionState { NEW, CONNECTED, CLOSED, ERROR }

    // All alterations of the room state should be done from inside the looper thread.
    private TCPGroupCommand.ConnectionState roomState;

    public TCPGroupCommand( String uid ) {
        this.events = null;
        hostUserID = uid;
        executor = Executors.newSingleThreadExecutor();
        roomState = TCPGroupCommand.ConnectionState.NEW;
    }

    @Override
    public void SetNetState( WebSocketPeer.ConnectionState state ) {

    }

    @Override
    public void RegisterEvent( TCPGroupCommand.CmdEvents events )
    {
        this.events = events;
    }

    @Override
    public void JoinCallGroup( final String uid,final String roomNo ) {

    }

    @Override
    public void LeaveGroup() {

    }

    /**
     *  加入视音频群组
     * @param uid   群组用户id
     * @param sdp   本端sdp
     */
    @Override
    public void ReceiveVideoFrom( final String uid,final SessionDescription sdp) {

    }

    @Override
    public void sendLocalIceCandidate(final String uid,final IceCandidate candidate) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                JSONObject json = new JSONObject();
                jsonPut(json, "type", "candidate");
                jsonPut(json, "label", candidate.sdpMLineIndex);
                jsonPut(json, "id", candidate.sdpMid);
                jsonPut(json, "candidate", candidate.sdp);

                if (roomState != TCPGroupCommand.ConnectionState.CONNECTED) {
                    reportError("Sending ICE candidate in non connected state.");
                    return;
                }
                sendMessage(json.toString());
            }
        });
    }

    @Override
    public void sendLocalIceCandidateRemovals(final IceCandidate[] candidates) {
    /*
    executor.execute(new Runnable() {
      @Override
      public void run() {
        JSONObject json = new JSONObject();
        jsonPut(json, "type", "remove-candidates");
        JSONArray jsonArray = new JSONArray();
        for (final IceCandidate candidate : candidates) {
          jsonArray.put(toJsonCandidate(candidate));
        }
        jsonPut(json, "candidates", jsonArray);

        if (roomState != ConnectionState.CONNECTED) {
          reportError("Sending ICE candidate removals in non connected state.");
          return;
        }
        sendMessage(json.toString());
      }
    });
    */
    }

    // -------------------------------------------------------------------
    // TCPChannelClient event handlers

    /**
     * If the client is the server side, this will trigger onConnectedToRoom.
     */
    @Override
    public void onSocketOpen(boolean isServer) {
        if (isServer) {
            roomState = TCPGroupCommand.ConnectionState.CONNECTED;

            GroupCommand.CmdParameters parameters = new GroupCommand.CmdParameters(
                    // Ice servers are not needed for direct connections.
                    new LinkedList<PeerConnection.IceServer>(),
                    //isServer, // Server side acts as the initiator on direct connections.
                    null, // clientId
                    null, // wssUrl
                    null, // offerSdp
                    null // iceCandidates
            );
            //events.onConnectedToRoom(parameters);
        }
    }

    @Override
    public void onSocketMessage(String msg) {
        /*
        try {
            JSONObject json = new JSONObject(msg);
            String type = json.optString("type");
            if (type.equals("candidate")) {
                events.onRemoteIceCandidate(toJavaCandidate(json));
            } else if (type.equals("remove-candidates")) {
                JSONArray candidateArray = json.getJSONArray("candidates");
                IceCandidate[] candidates = new IceCandidate[candidateArray.length()];
                for (int i = 0; i < candidateArray.length(); ++i) {
                    candidates[i] = toJavaCandidate(candidateArray.getJSONObject(i));
                }
                events.onRemoteIceCandidatesRemoved(candidates);
            } else if (type.equals("answer")) {
                SessionDescription sdp = new SessionDescription(
                        SessionDescription.Type.fromCanonicalForm(type), json.getString("sdp"));
                events.onRemoteDescription(sdp);
            } else if (type.equals("offer")) {
                SessionDescription sdp = new SessionDescription(
                        SessionDescription.Type.fromCanonicalForm(type), json.getString("sdp"));

                One2OneCommand.CmdParameters parameters = new One2OneCommand.CmdParameters(
                        // Ice servers are not needed for direct connections.
                        new LinkedList<PeerConnection.IceServer>(),
                        //false, // This code will only be run on the client side. So, we are not the initiator.
                        null, // clientId
                        null, // wssUrl
                        sdp, // offerSdp
                        null // iceCandidates
                );
                roomState = TCPOne2OneCommand.ConnectionState.CONNECTED;
                //events.onConnectedToRoom(parameters);
            } else {
                reportError("Unexpected TCP message: " + msg);
            }
        } catch (JSONException e) {
            reportError("TCP message JSON parsing error: " + e.toString());
        }
        */
    }

    @Override
    public void onSocketError(String description) {
        reportError("TCP connection error: " + description);
    }

    @Override
    public void onSocketClose() {

    }

    // --------------------------------------------------------------------
    // Helper functions.
    private void reportError(final String errorMessage) {
        Log.e(TAG, errorMessage);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (roomState != TCPGroupCommand.ConnectionState.ERROR) {
                    roomState = TCPGroupCommand.ConnectionState.ERROR;

                }
            }
        });
    }

    private void sendMessage(final String message) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                tcpChannel.send(message);
            }
        });
    }

    // Put a |key|->|value| mapping in |json|.
    private static void jsonPut(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    // Converts a Java candidate to a JSONObject.
    private static JSONObject toJsonCandidate(final IceCandidate candidate) {
        JSONObject json = new JSONObject();
        jsonPut(json, "label", candidate.sdpMLineIndex);
        jsonPut(json, "id", candidate.sdpMid);
        jsonPut(json, "candidate", candidate.sdp);
        return json;
    }

    // Converts a JSON candidate to a Java object.
    private static IceCandidate toJavaCandidate(JSONObject json) throws JSONException {
        return new IceCandidate(
                json.getString("id"), json.getInt("label"), json.getString("candidate"));
    }
}
