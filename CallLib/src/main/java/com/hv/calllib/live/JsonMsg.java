package com.hv.calllib.live;

/**
 * Created by liuhongyu on 2016/12/13.
 */

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;

public class JsonMsg {

    public static final String CALLMSG_ID_PRESENT             = "presenter";               // 进入群组->用户登陆信令服务
    public static final String CALLMSG_ID_PRESENT_RESP        = "presenterResponse";           // 发送方收到->登陆应答

    public static final String CALLMSG_ID_ONICECANDIDATE        = "onIceCandidate";          // 客户端发送->交换本地ice candidate
    public static final String CALLMSG_ID_ICECANDIDATE          = "iceCandidate";            // 双方收到->对方ice candidate


    ///
    /// encode
    ///
    public static String EncodePresent( String username,String sdpOffer ) {
        JSONObject json = new JSONObject();

        jsonPut(json, "id", "presenter");
        jsonPut(json,"username",username);
        jsonPut(json, "sdpOffer", sdpOffer );
        return json.toString();
    }

    ///
    /// encode
    ///
    public static String EncodePresentWithRTMP( String username,String sdpOffer,String rtmpURL ) {
        JSONObject json = new JSONObject();

        jsonPut(json, "id", "presentrtmp");
        jsonPut(json,"username",username);
        jsonPut(json, "sdpOffer", sdpOffer );
        jsonPut(json, "url", rtmpURL );
        return json.toString();
    }

    public static String EncodePresentResp( String response,String sdpAnswer ) {
        JSONObject json = new JSONObject();

        jsonPut(json, "id", "presenterResponse");
        jsonPut(json, "response", response );
        jsonPut(json, "sdpAnswer", sdpAnswer );

        return json.toString();
    }

    public static String EncodeOnIceCandidate( String name, IceCandidate candidate ) {
        JSONObject json = new JSONObject();

        jsonPut(json, "id", "onIceCandidate");
        jsonPut(json, "candidate", JsonMsg.EncodeCandidate(candidate) );

        return json.toString();
    }

    private static JSONObject EncodeCandidate( IceCandidate candidate )
    {
        JSONObject jsonCandidate = new JSONObject();//jsonSdp

        jsonPut(jsonCandidate,"candidate",candidate.sdp);
        jsonPut(jsonCandidate,"sdpMid",candidate.sdpMid);
        jsonPut(jsonCandidate,"sdpMLineIndex",candidate.sdpMLineIndex);

        return jsonCandidate;
    }

    public static String EncodeOnIceCandidateRemovals( final IceCandidate[] candidates ) {
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
        if( jsonCandidate!= null ) {
            return new IceCandidate( jsonCandidate.getString("sdpMid"),
                                     jsonCandidate.getInt("sdpMLineIndex"),
                                     jsonCandidate.getString("candidate"));
        }else {
            return null;
        }
    }

    public static JSONObject ParseJsonMsg( String jsonMsg ) {

        JSONObject json = null;

        try {
            json = new JSONObject( jsonMsg );
        }catch(JSONException e) {
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
