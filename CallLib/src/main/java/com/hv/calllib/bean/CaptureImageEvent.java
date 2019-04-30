package com.hv.calllib.bean;

/**
 * Created by admin on 2019/1/25.
 */

public class CaptureImageEvent {
    String imagepath;
    String fromeid;
    String touid;

    public CaptureImageEvent() {
    }

    public String getImagepath() {
        return this.imagepath;
    }

    public void setImagepath(String imagepath) {
        this.imagepath = imagepath;
    }

    public String getFromeid() {
        return this.fromeid;
    }

    public void setFromeid(String fromeid) {
        this.fromeid = fromeid;
    }

    public String getTouid() {
        return this.touid;
    }

    public void setTouid(String touid) {
        this.touid = touid;
    }
}
