package com.hv.calllib.group;

/**
 * Created by liuhongyu on 2016/12/13.
 */

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.webrtc.IceCandidate;

public class JsonMsg {

    public static final String CALLMSG_ID_JOINGROUP = "joinRoom";               // 进入群组->用户登陆信令服务
    public static final String CALLMSG_ID_JOINGROUP_RESP = "joinRoomResp";           // 发送方收到->登陆应答

    public static final String CALLMSG_ID_USERENTER = "newParticipantArrived";  // 新人进入群组->信令服务器发送
    public static final String CALLMSG_ID_EXISTUSERS = "existingParticipants";   // 房间内的用户列表->信令服务器发送

    public static final String CALLMSG_ID_RECEIVEVIDEOFROM = "receiveVideoFrom";       // 客户端请求建立通讯链路->客户端发起
    public static final String CALLMSG_ID_RECEIVEVIDEOANSWER = "receiveVideoAnswer";     // 服务器应答链路建立请求->信令服务器发送

    public static final String CALLMSG_ID_ONICECANDIDATE = "onIceCandidate";          // 客户端发送->交换本地ice candidate
    public static final String CALLMSG_ID_ICECANDIDATE = "iceCandidate";            // 双方收到->对方ice candidate

    public static final String CALLMSG_ID_ONLEAVEGROUP = "participantLeft";         // 信令服务器发送->某个用户推出群组
    public static final String CALLMSG_ID_LEAVEGROUP = "leaveRoom";               // 客户端发送->信令服务器,离开房间

    public static final String CALLMSG_ID_CAPTUREMJPEG = "captureMJPEG";            //视频中截图


    public static final String REMOTE_CTRL = "remotectrl";

    ///
    /// encode
    ///
    public static String EncodeJoinRoom(String userName, String roomNo) {
        JSONObject json = new JSONObject();

        jsonPut(json, "id", "joinRoom");
        jsonPut(json, "name", userName);
        jsonPut(json, "room", roomNo);

        return json.toString();
    }

    public static String EncodeJoinRoomResp(String userName, String roomNo, boolean disableMic, boolean result) {
        JSONObject json = new JSONObject();

        jsonPut(json, "id", "joinRoomResp");
        jsonPut(json, "name", userName);
        jsonPut(json, "room", roomNo);
        jsonPut(json, "disableMic", disableMic);
        jsonPut(json, "result", result);

        return json.toString();
    }

    public static String EncodeExistParticipants(String data[]) {
        JSONObject json = new JSONObject();
        JSONArray jsonArray = new JSONArray();

        jsonPut(json, "id", "existingParticipants");

        //  添加数组成员
        for (int i = 0; i < data.length; i++) {
            jsonPutArray(jsonArray, data[i]);
        }

        jsonPut(json, "data", jsonArray);


        return json.toString();
    }

    public static String EncodeNewParticipantArrived(String userName) {
        JSONObject json = new JSONObject();

        jsonPut(json, "id", "newParticipantArrived");
        jsonPut(json, "name", userName);

        return json.toString();
    }

    public static String EncodeLeave() {
        JSONObject json = new JSONObject();

        jsonPut(json, "id", "leaveRoom");

        return json.toString();
    }

    public static String EncodeOnParticipantLeft(String name) {
        JSONObject json = new JSONObject();

        jsonPut(json, "id", "participantLeft");
        jsonPut(json, "name", name);

        return json.toString();
    }

    public static String EncodeReceiveVideoFrom(String sender, String sdp) {
        JSONObject json = new JSONObject();

        jsonPut(json, "id", "receiveVideoFrom");
        jsonPut(json, "sender", sender);
        jsonPut(json, "sdpOffer", sdp);

        return json.toString();
    }

    public static String EncodeReceiveVideoAnswer(String name, String sdpAnswer) {
        JSONObject json = new JSONObject();

        jsonPut(json, "id", "receiveVideoAnswer");
        jsonPut(json, "name", name);
        jsonPut(json, "sdpAnswer", sdpAnswer);

        return json.toString();
    }

    public static String EncodeOnIceCandidate(String name, IceCandidate candidate) {
        JSONObject json = new JSONObject();

        jsonPut(json, "id", "onIceCandidate");
        jsonPut(json, "candidate", JsonMsg.EncodeCandidate(candidate));
        jsonPut(json, "name", name);

        return json.toString();
    }

    private static JSONObject EncodeCandidate(IceCandidate candidate) {
        JSONObject jsonCandidate = new JSONObject();//jsonSdp

        jsonPut(jsonCandidate, "candidate", candidate.sdp);
        jsonPut(jsonCandidate, "sdpMid", candidate.sdpMid);
        jsonPut(jsonCandidate, "sdpMLineIndex", candidate.sdpMLineIndex);

        return jsonCandidate;
    }

    public static String EncodeOnIceCandidateRemovals(final IceCandidate[] candidates) {
        /*
        JSONObject json = new JSONObject();

        jsonPut(json, "id", "onIceCandidate");
        jsonPut(json, "candidate", JsonMsg.EncodeCandidate(candidate) );
        return json.toString();
        */

        return null;
    }

    public static IceCandidate ParseCandidate(JSONObject json) throws JSONException {
        JSONObject jsonCandidate = json.getJSONObject("candidate");
        if (jsonCandidate != null) {
            return new IceCandidate(jsonCandidate.getString("sdpMid"),
                    jsonCandidate.getInt("sdpMLineIndex"),
                    jsonCandidate.getString("candidate"));
        } else {
            return null;
        }
    }

    public static JSONObject ParseJsonMsg(String jsonMsg) {

        JSONObject json = null;

        try {
            json = new JSONObject(jsonMsg);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return json;
    }

    // Put a |key|->|value| mapping in |json|.
    private static void jsonPut(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    // Put a |key|->|value| mapping in |json|.
    private static void jsonPutArray(JSONArray json, Object value) {
        //try {
        json.put(value);
        //} catch (JSONException e) {
        //    throw new RuntimeException(e);
        // }
    }
    ///
    /// decode
    ///
}
