package com.hv.calllib.record;

import com.google.gson.Gson;
import com.hv.imlib.imservice.manager.IMNaviManager;
import com.hv.imlib.imservice.network.OkHttpUtil;
import com.hv.imlib.protocol.ProtoConstant;
import com.hv.imlib.protocol.ProtoUtil;
import com.realview.commonlibrary.network.HttpClient;
import com.realview.commonlibrary.server.manager.CommLib;


import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by zhenhuiyang on 2018/7/2:15:46.
 * Holoview studio.
 */
public class RecordManager {

    private static RecordManager sInstance;

    public static RecordManager getInstance() {
        if (sInstance == null) {
            sInstance = new RecordManager();
        }

        return sInstance;
    }

    private RecordManager() {
    }


    public interface UploadCallIdCallback {
        void onSuccess();

        void onFailure(String err);
    }

    public void commitCallIds(long uid, long targetId, String callId, int deviceType) {
        UploadCallIdReq req = new UploadCallIdReq();
        req.setUid(uid);
        req.setSid(targetId);
        req.setCallid(callId);
        req.setDevice(deviceType);
        Gson gson = new Gson();
        String jsonReq = gson.toJson(req);

        String url = CommLib.instance().getAppSrvUrl() + "/"
                + ProtoConstant.HTTP_PROTO_VER + "/session/createvideorec";
        HttpClient client = new HttpClient();
        String result = client.post(url, jsonReq);
        UploadCallIdRes res = gson.fromJson(result, UploadCallIdRes.class);
    }

    public void commitCallIdsAsync(long uid, long targetId, String callId, int deviceType, final UploadCallIdCallback callback) {

        UploadCallIdReq req = new UploadCallIdReq();
        req.setUid(uid);
        req.setSid(targetId);
        req.setCallid(callId);
        req.setDevice(deviceType);

        final Gson gson = new Gson();
        String jsonReq = gson.toJson(req);
        String url = "http://demo.holoview-lab.com" + "/"
                + ProtoConstant.HTTP_PROTO_VER + "/session/createvideorec";


        OkHttpUtil.instance().post(url, jsonReq, new OkHttpUtil.OnRequestListener() {
            @Override
            public void onFailure(ProtoUtil.HttpError error) {
                callback.onFailure("http error");
            }

            @Override
            public void onResponse(String result) {

                try {
                    if (ProtoUtil.getResponseError(new JSONObject(result)).code == 0 || true) {

                        UploadCallIdRes res = gson.fromJson(result, UploadCallIdRes.class);
                        callback.onSuccess();
                    } else {
                        callback.onFailure("server error");

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    callback.onFailure("json error");

                }

            }
        });
    }
}
