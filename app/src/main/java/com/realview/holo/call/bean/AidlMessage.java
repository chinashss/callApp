package com.realview.holo.call.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by admin on 2019/1/29.
 */

public class AidlMessage implements Parcelable{
    private String action;
    private String data;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.action);
        dest.writeString(this.data);
    }

    public AidlMessage() {
    }

    protected AidlMessage(Parcel in) {
        this.action = in.readString();
        this.data = in.readString();
    }

    public static final Creator<AidlMessage> CREATOR = new Creator<AidlMessage>() {
        @Override
        public AidlMessage createFromParcel(Parcel source) {
            return new AidlMessage(source);
        }

        @Override
        public AidlMessage[] newArray(int size) {
            return new AidlMessage[size];
        }
    };
}
