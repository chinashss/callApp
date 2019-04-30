package com.hv.calllib;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Created by yukening on 17/7/18.
 */

public class CallEngineListener implements ICallEngineListener{
    private static final String TAG = "RongCallEngineListener";
    private Handler handler;

    public CallEngineListener(Handler handler) {
        this.handler = handler;
    }

    public void onJoinChannelSuccess(String channel, String mediaId, int elapsed) {
        Log.d("RongCallEngineListener", "onJoinChannelSuccess, mediaId = " + mediaId + ", channelId = " + channel);
        Message message = Message.obtain();
        message.what = 201;
        message.obj = mediaId;
        this.handler.sendMessage(message);
    }

    public void onRejoinChannelSuccess(String channel, String mediaId, int elapsed) {
    }

    public void onWarning(int warn) {
    }

    public void onError(int err) {
        Log.e("RongCallEngineListener", "onError, err = " + err);
        if(err == 16) {
            this.handler.sendEmptyMessage(404);
        } else if(err != 18) {
            this.handler.sendEmptyMessage(403);
        }

    }

    public void onApiCallExecuted(String api, int error) {
    }

    public void onCameraReady() {
    }

    public void onVideoStopped() {
    }

    public void onAudioQuality(String mediaId, int quality, short delay, short lost) {
    }

    public void onLeaveChannel() {
        Log.d("RongCallEngineListener", "onLeaveChannel");
        CallSession callInfo = new CallSession();
        Message message = Message.obtain();
        message.what = 202;
        message.obj = callInfo;
        this.handler.sendMessage(message);
    }

    public void onRtcStats() {
    }

    public void onAudioVolumeIndication(int totalVolume) {
    }

    public void onUserJoined(String mediaId, int elapsed) {
        Log.d("RongCallEngineListener", "onUserJoined, mediaId = " + mediaId);
        Message message = Message.obtain();
        message.what = 203;
        message.obj = mediaId;
        this.handler.sendMessage(message);
    }

    public void onUserOffline(String mediaId, int reason) {
        Log.d("RongCallEngineListener", "onUserOffline, mediaId = " + mediaId);
        Message message = Message.obtain();
        message.what = 204;
        message.obj = mediaId;
        this.handler.sendMessage(message);
    }

    public void onUserMuteAudio(String mediaId, boolean muted) {
    }

    public void onUserMuteVideo(String mediaId, boolean muted) {
        Log.d("RongCallEngineListener", "onUserMuteVideo, mediaId = " + mediaId);
        Message message = Message.obtain();
        message.what = 208;
        message.obj = mediaId;
        message.arg1 = muted?1:0;
        this.handler.sendMessage(message);
    }

    public void onRemoteVideoStat(String mediaId, int delay, int receivedBitrate, int receivedFrameRate) {
    }

    public void onLocalVideoStat(int sentBitrate, int sentFrameRate) {
    }

    public void onFirstRemoteVideoFrame(String mediaId, int width, int height, int elapsed) {
    }

    public void onFirstLocalVideoFrame(int width, int height, int elapsed) {
    }

    public void onFirstRemoteVideoDecoded(String mediaId, int width, int height, int elapsed) {
    }

    public void onConnectionLost() {
        Log.d("RongCallEngineListener", "onConnectionLost");
        this.handler.sendEmptyMessage(403);
    }

    public void onConnectionInterrupted() {
        Log.d("RongCallEngineListener", "onConnectionInterrupted");
    }

    public void onMediaEngineEvent(int code) {
    }

    public void onVendorMessage(String mediaId, byte[] data) {
        Log.d("RongCallEngineListener", "onVendorMessage : msg = " + new String(data));
    }

    public void onRefreshRecordingServiceStatus(int status) {
    }
}
