package com.hv.calllib;

/**
 * Created by yukening on 17/7/20.
 */

public interface ICallEngineListener {
    void onJoinChannelSuccess(String var1, String var2, int var3);

    void onRejoinChannelSuccess(String var1, String var2, int var3);

    void onWarning(int var1);

    void onError(int var1);

    void onApiCallExecuted(String var1, int var2);

    void onCameraReady();

    void onVideoStopped();

    void onAudioQuality(String var1, int var2, short var3, short var4);

    void onLeaveChannel();

    void onRtcStats();

    void onAudioVolumeIndication(int var1);

    void onUserJoined(String var1, int var2);

    void onUserOffline(String var1, int var2);

    void onUserMuteAudio(String var1, boolean var2);

    void onUserMuteVideo(String var1, boolean var2);

    void onRemoteVideoStat(String var1, int var2, int var3, int var4);

    void onLocalVideoStat(int var1, int var2);

    void onFirstRemoteVideoFrame(String var1, int var2, int var3, int var4);

    void onFirstLocalVideoFrame(int var1, int var2, int var3);

    void onFirstRemoteVideoDecoded(String var1, int var2, int var3, int var4);

    void onConnectionLost();

    void onConnectionInterrupted();

    void onMediaEngineEvent(int var1);

    void onVendorMessage(String var1, byte[] var2);

    void onRefreshRecordingServiceStatus(int var1);
}
