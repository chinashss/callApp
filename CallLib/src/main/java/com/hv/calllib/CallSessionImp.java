package com.hv.calllib;

import android.os.Message;
import android.view.SurfaceView;


import com.hv.imlib.model.ConversationType;

import java.util.Iterator;
import java.util.List;


import cn.holo.call.bean.CallUserProfile;
import cn.holo.call.bean.type.CallEngineType;
import cn.holo.call.bean.type.CallMediaType;

/**
 * Created by yukening on 17/7/18.
 */

public class CallSessionImp {
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
    private Message cachedMsg;
    private String dynamicKey;

    public CallSessionImp() {
    }


    public Message getCachedMsg() {
        return this.cachedMsg;
    }

    public void setCachedMsg(Message cachedMsg) {
        this.cachedMsg = cachedMsg;
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

    public SurfaceView getLocalVideo() {
        SurfaceView localVideo = null;
        Iterator var2 = this.usersProfileList.iterator();

        while(var2.hasNext()) {
            CallUserProfile profile = (CallUserProfile)var2.next();
            if(profile.getUserId().equals(this.selfUserId)) {
                localVideo = profile.getVideoView();
            }
        }

        return localVideo;
    }

    public void setLocalVideo(SurfaceView localVideo) {
        Iterator var2 = this.usersProfileList.iterator();

        while(var2.hasNext()) {
            CallUserProfile profile = (CallUserProfile)var2.next();
            if(profile.getUserId().equals(this.selfUserId)) {
                profile.setVideoView(localVideo);
            }
        }

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

    public String getDynamicKey() {
        return this.dynamicKey;
    }

    public void setDynamicKey(String dynamicKey) {
        this.dynamicKey = dynamicKey;
    }
}
