package com.hv.calllib;

import android.content.Context;
import android.view.SurfaceView;

/**
 * Created by zhenhuiyang on 2018/3/2:12:16.
 * Holoview studio.
 */

public class LiveManager {

    private static LiveManager sInstance;

    private LiveManager() {

    }

    public  static LiveManager getInstance() {
        if(sInstance == null) {
            sInstance = new LiveManager();
        }

        return sInstance;
    }



    public interface LiveListener{
        void onLiveStart(SurfaceView view);
        void onLiveStop();
        void onError();
    }


    CallEngineLive engine;
    LiveListener mListener;

    public void setListener(LiveListener listener){
        mListener = listener;
    }
    public void initLive(Context context ,  String mUid, String url,String roomNo){

        engine = CallEngineLive.newInstance();
        engine.create(context, new ICallEngineListener() {
            @Override
            public void onJoinChannelSuccess(String var1, String var2, int var3) {
                if (mListener!=null){
                    mListener.onLiveStart(engine.getLocalSurfaceView());
                }
            }

            @Override
            public void onRejoinChannelSuccess(String var1, String var2, int var3) {

            }

            @Override
            public void onWarning(int var1) {

            }

            @Override
            public void onError(int var1) {

            }

            @Override
            public void onApiCallExecuted(String var1, int var2) {

            }

            @Override
            public void onCameraReady() {

            }

            @Override
            public void onVideoStopped() {

            }

            @Override
            public void onAudioQuality(String var1, int var2, short var3, short var4) {

            }

            @Override
            public void onLeaveChannel() {

            }

            @Override
            public void onRtcStats() {

            }

            @Override
            public void onAudioVolumeIndication(int var1) {

            }

            @Override
            public void onUserJoined(String var1, int var2) {

            }

            @Override
            public void onUserOffline(String var1, int var2) {

            }

            @Override
            public void onUserMuteAudio(String var1, boolean var2) {

            }

            @Override
            public void onUserMuteVideo(String var1, boolean var2) {

            }

            @Override
            public void onRemoteVideoStat(String var1, int var2, int var3, int var4) {

            }

            @Override
            public void onLocalVideoStat(int var1, int var2) {

            }

            @Override
            public void onFirstRemoteVideoFrame(String var1, int var2, int var3, int var4) {

            }

            @Override
            public void onFirstLocalVideoFrame(int var1, int var2, int var3) {

            }

            @Override
            public void onFirstRemoteVideoDecoded(String var1, int var2, int var3, int var4) {

            }

            @Override
            public void onConnectionLost() {
                if (mListener!=null){
                    mListener.onLiveStop();
                }
            }

            @Override
            public void onConnectionInterrupted() {

            }

            @Override
            public void onMediaEngineEvent(int var1) {

            }

            @Override
            public void onVendorMessage(String var1, byte[] var2) {

            }

            @Override
            public void onRefreshRecordingServiceStatus(int var1) {

            }
        }, true, mUid, url,roomNo, false);



    }
    public void startLive(){

        if (engine != null) {
            engine.startLive();
        }
    }
    public void stopLive(){
        if (engine != null) {
            engine.stopLive();
        }
    }
    public void uninitLive(){
        if (engine != null) {
            engine.destroy();
            engine = null;
        }
    }
}
