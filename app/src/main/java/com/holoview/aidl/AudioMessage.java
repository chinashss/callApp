package com.holoview.aidl;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by admin on 2019/2/14.
 */

public class AudioMessage implements Parcelable {
    private byte[] audioData;

    public byte[] getAudioData() {
        return audioData;
    }

    public void setAudioData(byte[] audioData) {
        this.audioData = audioData;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(this.audioData);
    }

    public AudioMessage() {
    }

    protected AudioMessage(Parcel in) {
        this.audioData = in.createByteArray();
    }

    public static final Creator<AudioMessage> CREATOR = new Creator<AudioMessage>() {
        @Override
        public AudioMessage createFromParcel(Parcel source) {
            return new AudioMessage(source);
        }

        @Override
        public AudioMessage[] newArray(int size) {
            return new AudioMessage[size];
        }
    };
}
