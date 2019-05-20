package com.realview.holo.call;

import android.content.Context;
import android.view.SurfaceView;

import com.blankj.utilcode.util.AppUtils;
import com.hv.calllib.CallCommon;
import com.hv.calllib.CallListener;
import com.hv.calllib.CallManager;
import com.hv.calllib.CallSession;
import com.hv.calllib.HoloCall;
import com.hv.imlib.model.ConversationType;
import com.realview.holo.call.bean.CallStateMessage;
import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.beta.UpgradeInfo;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.holo.call.bean.type.CallDisconnectedReason;
import cn.holo.call.bean.type.CallEngineType;
import cn.holo.call.bean.type.CallMediaType;

public class CallApp implements CallListener {
    private static CallApp singletonCallApp = new CallApp();

    private ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

    private int converstaionType;
    private long roomId;
    private long userSelfid;
    private CallSession mSession;


    private SurfaceView mSurfaceView;
    

    private List<Long> roomUserIds = new ArrayList<>();


    private CallApp() {

    }

    public static CallApp getInstance() {
        return singletonCallApp;
    }


    public void initListener(Context mContext) {
        HoloCall holoCall = HoloCall.getInstance();
        holoCall.init(mContext);
        holoCall.setVoIPCallListener(this);
        holoCall.setEnableAllRemoteVideo(false);
    }


    public void start() {
        Beta.checkUpgrade(false, true);
        UpgradeInfo upgradeInfo = Beta.getUpgradeInfo();
        if (upgradeInfo != null) {
            if (AppUtils.getAppVersionCode() < upgradeInfo.versionCode) {
                Beta.checkUpgrade();
                return;
            }
        }


        singleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                ConversationType type = ConversationType.setValue(converstaionType);
                CallManager.getInstance().setUserSelfId(userSelfid);
                HoloCall.needAudio = true;
                HoloCall.getInstance().startCall(type,
                        ConversationType.P2P == type ? roomUserIds.get(0) : roomId,
                        roomUserIds,
                        CallMediaType.VIDEO,
                        CallEngineType.ENGINE_TYPE_NORMAL,
                        null,
                        103,
                        userSelfid);
            }
        });
    }


    public int getConverstaionType() {
        return converstaionType;
    }

    public void setConverstaionType(int converstaionType) {
        this.converstaionType = converstaionType;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public long getUserSelfid() {
        return userSelfid;
    }

    public void setUserSelfid(long userSelfid) {
        this.userSelfid = userSelfid;
    }

    public List<Long> getRoomUserIds() {
        return roomUserIds;
    }

    public void addAllRoomUserIds(List<Long> roomUserIds) {
        this.roomUserIds.clear();
        this.roomUserIds.addAll(roomUserIds);
    }

    public CallSession getSession() {
        return mSession;
    }

    public void setSession(CallSession mSession) {
        this.mSession = mSession;
    }



    @Override
    public void onCallOutgoing(CallSession var1, SurfaceView var2) {
        this.mSession=var1;
        this.mSurfaceView = var2;
        EventBus.getDefault().postSticky(CallStateMessage.CallOutgoing);
    }

    @Override
    public void onCallConnected(CallSession var1, SurfaceView var2) {
        this.mSession=var1;
        this.mSurfaceView=var2;
        EventBus.getDefault().postSticky(CallStateMessage.CallConnected);
    }

    @Override
    public void onCallDisconnected(CallSession var1, CallDisconnectedReason var2) {
        EventBus.getDefault().postSticky(CallStateMessage.CallDisconnected);
    }

    @Override
    public void onRemoteUserRinging(long var1) {

    }

    @Override
    public void onRemoteUserJoined(long var1, CallMediaType var2, SurfaceView var3) {
        this.mSurfaceView = var3;
        EventBus.getDefault().postSticky(CallStateMessage.RemoteUserJoined);
    }

    @Override
    public void onRemoteUserInvited(long var1, CallMediaType var2) {

    }

    @Override
    public void onRemoteUserLeft(long var1, CallDisconnectedReason var2) {

    }

    @Override
    public void onMediaTypeChanged(long var1, CallMediaType var2, SurfaceView var3) {
        this.mSurfaceView = var3;
        EventBus.getDefault().postSticky(CallStateMessage.MediaTypeChanged);
    }

    @Override
    public void onError(CallCommon.CallErrorCode var1) {

    }

    @Override
    public void onRemoteCameraDisabled(long var1, boolean var2) {

    }

    public SurfaceView getSurfaceView() {
        return mSurfaceView;
    }

    public void setSurfaceView(SurfaceView mSurfaceView) {
        this.mSurfaceView = mSurfaceView;
    }
}
