package com.hv.calllib;

import android.os.Parcel;
import android.os.Parcelable;


import com.hv.imlib.model.ConversationType;


import java.util.List;

import cn.holo.call.bean.CallUserProfile;
import cn.holo.call.bean.type.CallEngineType;
import cn.holo.call.bean.type.CallMediaType;

/**
 * Created by yukening on 17/7/18.
 */

public class CallSession implements Parcelable {
    private String callId;
    private ConversationType conversationType;
    private long targetId;
    private CallMediaType mediaType;
    private CallEngineType engineType;
    private long startTime;
    private long activeTime;
    private long endTime;
    private long selfUserId;
    private long inviterUserId;
    private long callerUserId;
    private List<CallUserProfile> usersProfileList;
    private String extra;


    public CallSession() {
    }

    public String getExtra() {
        return this.extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public long getSelfUserId() {
        return this.selfUserId;
    }

    public void setSelfUserId(long selfUserId) {
        this.selfUserId = selfUserId;
    }

    public long getActiveTime() {
        return this.activeTime;
    }

    public void setActiveTime(long activeTime) {
        this.activeTime = activeTime;
    }

    public long getInviterUserId() {
        return this.inviterUserId;
    }

    public void setInviterUserId(long inviterUserId) {
        this.inviterUserId = inviterUserId;
    }

    public long getCallerUserId() {
        return this.callerUserId;
    }

    public void setCallerUserId(long callerUserId) {
        this.callerUserId = callerUserId;
    }

    public List<CallUserProfile> getParticipantProfileList() {
        return this.usersProfileList;
    }

    public void setParticipantUserList(List<CallUserProfile> participantsProfileList) {
        this.usersProfileList = participantsProfileList;
    }

    public CallEngineType getEngineType() {
        return this.engineType;
    }

    public void setEngineType(CallEngineType engineType) {
        this.engineType = engineType;
    }

    public String getCallId() {
        return this.callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public ConversationType getConversationType() {
        return this.conversationType;
    }

    public void setConversationType(ConversationType conversationType) {
        this.conversationType = conversationType;
    }

    public long getTargetId() {
        return this.targetId;
    }

    public void setTargetId(long targetId) {
        this.targetId = targetId;
    }

    public CallMediaType getMediaType() {
        return this.mediaType;
    }

    public void setMediaType(CallMediaType mediaType) {
        this.mediaType = mediaType;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return this.endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.callId);
        dest.writeInt(this.conversationType == null ? -1 : this.conversationType.ordinal());
        dest.writeLong(this.targetId);
        dest.writeInt(this.mediaType == null ? -1 : this.mediaType.ordinal());
        dest.writeInt(this.engineType == null ? -1 : this.engineType.ordinal());
        dest.writeLong(this.startTime);
        dest.writeLong(this.activeTime);
        dest.writeLong(this.endTime);
        dest.writeLong(this.selfUserId);
        dest.writeLong(this.inviterUserId);
        dest.writeLong(this.callerUserId);
        dest.writeTypedList(this.usersProfileList);
        dest.writeString(this.extra);
    }

    protected CallSession(Parcel in) {
        this.callId = in.readString();
        int tmpConversationType = in.readInt();
        this.conversationType = tmpConversationType == -1 ? null : ConversationType.values()[tmpConversationType];
        this.targetId = in.readLong();
        int tmpMediaType = in.readInt();
        this.mediaType = tmpMediaType == -1 ? null : CallMediaType.values()[tmpMediaType];
        int tmpEngineType = in.readInt();
        this.engineType = tmpEngineType == -1 ? null : CallEngineType.values()[tmpEngineType];
        this.startTime = in.readLong();
        this.activeTime = in.readLong();
        this.endTime = in.readLong();
        this.selfUserId = in.readLong();
        this.inviterUserId = in.readLong();
        this.callerUserId = in.readLong();
        this.usersProfileList = in.createTypedArrayList(CallUserProfile.CREATOR);
        this.extra = in.readString();
    }

    public static final Creator<CallSession> CREATOR = new Creator<CallSession>() {
        @Override
        public CallSession createFromParcel(Parcel source) {
            return new CallSession(source);
        }

        @Override
        public CallSession[] newArray(int size) {
            return new CallSession[size];
        }
    };
}