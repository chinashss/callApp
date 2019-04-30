package com.hv.calllib.audiomgr;

import android.content.Context;

import com.realview.commonlibrary.audiorecord.XAudioRecord;
import com.realview.commonlibrary.audiorecord.XAudioRecordMgr;

/**
 * Created by admin on 2019/2/1.
 */

public class VoiceCmdEngine extends XAudioRecord {
    private static VoiceCmdEngine instance = new VoiceCmdEngine();

    public static VoiceCmdEngine getInstance() {
        return instance;
    }

    private XAudioRecordMgr mAudioMgr = null;


    public void InitEngine(Context cxt) {
        mAudioMgr = XAudioRecordMgr.newInstance(cxt);
        if (mAudioMgr.isAudioRecorded() == false) {
            mAudioMgr.Subscribe(this);
            mAudioMgr.startRecording();
        } else {
            mAudioMgr.Subscribe(this);

        }
    }


    @Override
    public void onAudioData(byte[] audioData) {

    }

}