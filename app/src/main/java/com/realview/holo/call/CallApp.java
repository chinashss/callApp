package com.realview.holo.call;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.blankj.utilcode.util.AppUtils;
import com.holoview.aidl.ProcessServiceIAidl;
import com.hv.calllib.CallCommon;
import com.hv.calllib.CallListener;
import com.hv.calllib.CallManager;
import com.hv.calllib.CallSession;
import com.hv.calllib.HoloCall;
import com.hv.imlib.HoloMessage;
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


    public void bindOrderService() {
        Intent intent = new Intent("com.holoview.hololauncher.BackgroundService");
        intent.setPackage("com.holoview.hololauncher");
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        HoloCallApp.getApp().bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }


    ServiceConnection conn = new ServiceConnection() {//这个最重要，用于连接Service
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i("MainActivity.conn", "@@ onServiceConnected name=" + name);
            ProcessServiceIAidl aidl = ProcessServiceIAidl.Stub.asInterface(service);
            mProcessAidl = aidl;
            try {
                mProcessAidl.initCallMessage();
                mProcessAidl.onBindSuccess("com.realview.holo.call", "com.realview.holo.call.service.CallBackgroundService");
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            try {
                service.linkToDeath(deathRecipient, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i("MainActivity.conn", "@@ onServiceDisconnected name=" + name);
        }
    };


    private ProcessServiceIAidl mProcessAidl;

    //注册一个死亡代理，监测连接状态
    IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            Log.i("MainActivity", "@@ binderDied " + (mProcessAidl == null));
            if (mProcessAidl == null) {
                return;
            }
            mProcessAidl.asBinder().unlinkToDeath(deathRecipient, 0);
            mProcessAidl = null;
            //重新绑定
            bindOrderService();
        }
    };

    public void initListener(Context mContext) {
        HoloCall holoCall = HoloCall.getInstance();
        holoCall.init(mContext);
        holoCall.setVoIPCallListener(this);
        holoCall.setEnableAllRemoteVideo(false);
    }

    public void disConn() {
        HoloCallApp.getApp().unbindService(conn);
    }


    public void sendMessage(HoloMessage message){
        try {
            String jsonString = JSON.toJSONString(message, SerializerFeature.WriteMapNullValue);
            Log.i("TAG", jsonString);
            if (mProcessAidl != null) {
                mProcessAidl.sendMessage(jsonString);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
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
                long callTargerId = ConversationType.P2P == type ? roomUserIds.get(0) : roomId;
                HoloCall.getInstance().startCall(type, callTargerId,
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
        this.mSession = var1;
        this.mSurfaceView = var2;
        EventBus.getDefault().postSticky(CallStateMessage.CallOutgoing);
    }

    @Override
    public void onCallConnected(CallSession var1, SurfaceView var2) {
        this.mSession = var1;
        this.mSurfaceView = var2;
        EventBus.getDefault().postSticky(CallStateMessage.CallConnected);
    }

    @Override
    public void onCallDisconnected(CallSession session, CallDisconnectedReason reason) {
        switch (reason) {
            case BUSY_LINE:
            case REMOTE_BUSY_LINE:
                Toast.makeText(HoloCallApp.getApp(), "对方正忙，请稍后再试", Toast.LENGTH_LONG).show();
                break;
            case HANGUP:
            case REMOTE_HANGUP:
            case REJECT:
            case REMOTE_CANCEL:
            case REMOTE_REJECT:
                Toast.makeText(HoloCallApp.getApp(), "对方已拒绝", Toast.LENGTH_LONG).show();
                break;
            case NO_RESPONSE:
            case REMOTE_NO_RESPONSE:
                Toast.makeText(HoloCallApp.getApp(), "暂时无人接听", Toast.LENGTH_LONG).show();
                break;
        }
        EventBus.getDefault().post(CallStateMessage.CallDisconnected);
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
