package com.hv.calllib.record;

/**
 * Created by zhenhuiyang on 2018/7/2:15:51.
 * Holoview studio.
 */
public class UploadCallIdReq {
    /**
     * uid : 1000110139570
     * callid :
     * sid : 1000110139570
     * device : 1111
     */

    private long uid;
    private String callid;
    private long sid;
    private int device;

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public String getCallid() {
        return callid;
    }

    public void setCallid(String callid) {
        this.callid = callid;
    }

    public long getSid() {
        return sid;
    }

    public void setSid(long sid) {
        this.sid = sid;
    }

    public int getDevice() {
        return device;
    }

    public void setDevice(int device) {
        this.device = device;
    }
}
