package com.hv.calllib;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;

import com.hv.calllib.CallCommon.CallErrorCode;
import com.hv.calllib.CallCommon.CallVideoProfile;
import com.hv.calllib.bean.CloseMessage;
import com.hv.imlib.HoloMessage;
import com.hv.imlib.ImLib;
import com.hv.imlib.model.ConversationType;
import com.hv.imlib.model.Message;
import com.hv.imlib.stateMachine.State;
import com.hv.imlib.stateMachine.StateMachine;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import cn.holo.call.bean.CallUserProfile;
import cn.holo.call.bean.message.ArMarkMessage;
import cn.holo.call.bean.message.CallAcceptMessage;
import cn.holo.call.bean.message.CallHangupMessage;
import cn.holo.call.bean.message.CallInviteMessage;
import cn.holo.call.bean.message.CallModifyMediaMessage;
import cn.holo.call.bean.message.CallModifyMemberMessage;
import cn.holo.call.bean.message.CallRingingMessage;
import cn.holo.call.bean.type.CallDisconnectedReason;
import cn.holo.call.bean.type.CallEngineType;
import cn.holo.call.bean.type.CallMediaType;
import cn.holo.call.bean.type.CallModifyMemType;
import cn.holo.call.bean.type.CallStatus;

/**
 * Created by yukening on 17/7/18.
 */

public class CallManager extends StateMachine {
    private static final String TAG = CallManager.class.getSimpleName();
    private static final int DEFAULT_VIDEO_PROFILE = 30;
    private static CallManager sInstance;
    private CallListener callListener;
    private CallSessionImp callSessionImp;
    private static final int CALL_TIMEOUT_INTERVAL = 60000;
    private Context context;
    private Timer timer;
    //    private IHandler libStub;
    private Map<Long, TimerTask> timerTasks;
    private CallEngine voIPEngine;
    private List<String> unknownMediaIdList;
    private Handler uiHandler;
    private static ReceivedCallListener receivedCallListener;
    private int videoProfile = 30;
    private static final int STRATEGY_NONE = 0;
    private static final int STRATEGY_PRIORITY = 1;
    //private IVideoFrameListener videoFrameListener = null;
    //private IAudioFrameListener audioFrameListener = null;
    private int strategy;
    private CallEngineType preferEngineType;
    private String vendorKey;
    private State mCheckPermissionState = new CheckPermissionState();
    private State mUnInitState = new UnInitState();
    private State mIdleState = new IdleState();
    private State mIncomingState = new IncomingState();
    private State mOutgoingState = new OutgoingState();
    private State mConnectingState = new ConnectingState();
    private State mConnectedState = new ConnectedState();
    private State mDisconnectingState = new DisconnectingState();


    private long userSelfId;


    private CallManager(String name) {
        super(name);
    }

    public static CallManager getInstance() {
        if (sInstance == null) {
            sInstance = new CallManager(TAG);
        }

        return sInstance;
    }

    public void setPreferEngineType(CallEngineType preferEngineType) {
        this.preferEngineType = preferEngineType;
    }

    static void setReceivedCallListener(final ReceivedCallListener listener) {
        Log.i(TAG, "setReceivedCallListener, listener = " + listener);
        receivedCallListener = new ReceivedCallListener() {
            public void onReceivedCall(final CallSession callSession) {
                if (CallManager.sInstance == null) {
                    //Log.e(CallManager.TAG, "RongVoIPManager does not init.");
                } else {
                    CallManager.sInstance.runOnUiThread(new Runnable() {
                        public void run() {
                            //Log.e(CallManager.TAG, "onReceivedCall.");
                            listener.onReceivedCall(callSession);
                        }
                    });
                }
            }

            public void onCheckPermission(final CallSession callSession) {
                if (CallManager.sInstance == null) {
                    // Log.e(CallManager.TAG, "RongVoIPManager does not init.");
                } else {
                    CallManager.sInstance.runOnUiThread(new Runnable() {
                        public void run() {
                            //Log.i(CallManager.TAG, "onCheckPermission.");
                            listener.onCheckPermission(callSession);
                        }
                    });
                }
            }
        };
    }

    public void setCallListener(final CallListener voIPCallListener) {
        // Log.i(TAG, "setCallListener, listener = " + voIPCallListener);
        this.callListener = new CallListener() {
            public void onCallOutgoing(final CallSession callProfile, final SurfaceView surfaceView) {
                CallManager.this.runOnUiThread(new Runnable() {
                    public void run() {
                        if (voIPCallListener != null) {
                            voIPCallListener.onCallOutgoing(callProfile, surfaceView);
                        }

                    }
                });
            }

            public void onRemoteUserRinging(final long userId) {
                CallManager.this.runOnUiThread(new Runnable() {
                    public void run() {
                        if (voIPCallListener != null) {
                            voIPCallListener.onRemoteUserRinging(userId);
                        }

                    }
                });
            }

            public void onCallDisconnected(final CallSession callProfile, final CallDisconnectedReason reason) {
                CallManager.this.runOnUiThread(new Runnable() {
                    public void run() {
                        if (voIPCallListener != null) {
                            voIPCallListener.onCallDisconnected(callProfile, reason);
                        }

                    }
                });
            }

            public void onRemoteUserInvited(final long userId, final CallMediaType mediaType) {
                CallManager.this.runOnUiThread(new Runnable() {
                    public void run() {
                        if (voIPCallListener != null) {
                            voIPCallListener.onRemoteUserInvited(userId, mediaType);
                        }

                    }
                });
            }

            public void onRemoteUserJoined(final long userId, final CallMediaType mediaType, final SurfaceView videoView) {
                CallManager.this.runOnUiThread(new Runnable() {
                    public void run() {
                        if (voIPCallListener != null) {
                            voIPCallListener.onRemoteUserJoined(userId, mediaType, videoView);
                        }

                    }
                });
            }

            public void onMediaTypeChanged(final long userId, final CallMediaType mediaType, final SurfaceView videoView) {
                CallManager.this.runOnUiThread(new Runnable() {
                    public void run() {
                        if (voIPCallListener != null) {
                            voIPCallListener.onMediaTypeChanged(userId, mediaType, videoView);
                        }

                    }
                });
            }

            public void onError(final CallErrorCode errorCode) {
                CallManager.this.runOnUiThread(new Runnable() {
                    public void run() {
                        if (voIPCallListener != null) {
                            voIPCallListener.onError(errorCode);
                        }

                    }
                });
            }

            public void onRemoteUserLeft(final long userId, final CallDisconnectedReason reason) {
                CallManager.this.runOnUiThread(new Runnable() {
                    public void run() {
                        if (voIPCallListener != null) {
                            voIPCallListener.onRemoteUserLeft(userId, reason);
                        }

                    }
                });
            }

            public void onCallConnected(final CallSession callProfile, final SurfaceView localVideo) {
                CallManager.this.runOnUiThread(new Runnable() {
                    public void run() {
                        if (voIPCallListener != null) {
                            voIPCallListener.onCallConnected(callProfile, localVideo);
                        }

                    }
                });
            }

            public void onRemoteCameraDisabled(final long userId, final boolean muted) {
                CallManager.this.runOnUiThread(new Runnable() {
                    public void run() {
                        if (voIPCallListener != null) {
                            voIPCallListener.onRemoteCameraDisabled(userId, muted);
                        }

                    }
                });
            }
        };
    }

    void init(Context context) {
        this.timer = new Timer();
        this.timerTasks = new HashMap();
        this.context = context.getApplicationContext();
        this.unknownMediaIdList = new ArrayList();
        this.uiHandler = new Handler(Looper.getMainLooper());
        this.addState(this.mUnInitState);
        this.addState(this.mCheckPermissionState);
        this.addState(this.mIdleState);
        this.addState(this.mIncomingState, this.mIdleState);
        this.addState(this.mOutgoingState, this.mIdleState);
        this.addState(this.mConnectingState, this.mIdleState);
        this.addState(this.mConnectedState, this.mIdleState);
        this.addState(this.mDisconnectingState, this.mIdleState);
        this.setInitialState(this.mUnInitState);
        this.start();
    }

    private void runOnUiThread(Runnable runnable) {
        this.uiHandler.post(runnable);
    }

    boolean updateEngineParameters() {
        return true;
//        try {
//            this.strategy = -1;
//            this.preferEngineType = null;
//            this.vendorKey = null;
//            String e =null; //this.libStub.getVoIPCallInfo();
//            if(libStub==null){
//                return false;
//           // }
//
//          //  if(e == null) {
//                //Log.e(TAG, "getVoIPCallInfo returns null while startEngine");
//               // return false;
//            } else {
//                JSONObject jsonObject = new JSONObject(e);
//                this.strategy = jsonObject.getInt("strategy");
//                JSONArray jsonArray = jsonObject.getJSONArray("callEngine");
//                JSONObject object = jsonArray.getJSONObject(0);
//                this.preferEngineType = CallCommon.CallEngineType.valueOf(object.optInt("engineType"));
//                this.vendorKey = object.optString("vendorKey");
//                return true;
//            }
//        } catch (Exception var5) {
//            var5.printStackTrace();
//            return false;
//        }
    }

    private boolean checkSupportEngine(CallEngineType engineType) {
        return true;
//        String engineName = null;
//        if(engineType == CallCommon.CallEngineType.ENGINE_TYPE_AGORA) {
//            engineName = "io.rong.agora.AgoraEngine";
//        } else if(engineType == CallCommon.CallEngineType.ENGINE_TYPE_RONG) {
//            engineName = "io.rong.webrtc.RongCallEngine";
//        }
//
//        Class clazz = null;
//
//        try {
//            clazz = Class.forName(engineName);
//        } catch (ClassNotFoundException var5) {
//            var5.printStackTrace();
//        }
//
//        Log.e(TAG, engineType + " engine support " + (clazz != null));
//        return clazz != null;
    }

    private boolean startEngine(CallEngineType engineType) {
        Log.i(TAG, "startEngine");

        this.voIPEngine = CallEngine.newInstance();
        CallEngineListener engineListener = new CallEngineListener(this.getHandler());
        boolean videoEnabled = true;
        boolean isAr = false;
        if (callSessionImp.getMediaType() == CallMediaType.AUDIO) {
            videoEnabled = false;
        }

        if (callSessionImp.getEngineType() == CallEngineType.ENGINE_TYPE_AR) {
            isAr = true;
        }

        this.voIPEngine.create(context, engineListener, videoEnabled, callSessionImp.getSelfUserId() + "", callSessionImp.getCallId(), isAr);
        //this.voIPEngine.setSubscribeVideoEnabled(false);

//        try {
//            Thread.sleep(800);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        return true;
    }

    public void onArMarkMessage(Message message) {
        ArMarkMessage arMarkMessage = (ArMarkMessage) message.getMessageContent();

        String callid = arMarkMessage.getCallId();
        if (this.voIPEngine != null
                && callSessionImp != null
                && !TextUtils.isEmpty(callSessionImp.getCallId())
                && callSessionImp.getCallId().equals(callid)) {
            this.voIPEngine.showArMark(arMarkMessage.getContent());
        }
    }

    private void stopEngine() {
        Log.i(TAG, "stopEngine");
        HoloCall.needAudio = false;
        if (this.voIPEngine != null) {
            this.voIPEngine.destroy();
            this.voIPEngine = null;
        }

    }

    private long getUserSelfId() {
        return userSelfId;
    }

    public void setUserSelfId(long userSelfId) {
        this.userSelfId = userSelfId;
    }

    public void sendInviteMessage(List<Long> userList, final SignalCallback callback) {
        CallInviteMessage inviteMessage = new CallInviteMessage();
        inviteMessage.setMediaType(this.callSessionImp.getMediaType());
        inviteMessage.setEngineType(this.callSessionImp.getEngineType());
        inviteMessage.setInviteUserIds(userList);
        inviteMessage.setExtra(this.callSessionImp.getExtra());
        inviteMessage.setCallId(this.callSessionImp.getCallId());

        Message e = Message.obtain(Long.valueOf(this.callSessionImp.getTargetId()), this.callSessionImp.getConversationType(), inviteMessage);


        ImLib.SendMessageCallback sendMessageCallback = new ImLib.SendMessageCallback() {
            @Override
            public void onAttached(Message message) {

            }

            @Override
            public void onSuccess(Message message) {
                if (callback != null) {
                    String mediaId = CallManager.this.getMediaIdBySentTime(message.getUpdated());
                    callback.onSuccess(message.getSenderId(), mediaId);
                }
            }

            @Override
            public void onError(Message message, ImLib.ErrorCode errorCode) {
                if (callback != null) {
                    callback.onError();
                }
            }
        };


        HoloMessage message = new HoloMessage();
        message.setMessage(e);
//        message.setCallback(sendMessageCallback);
        message.setAction("CallInviteMessage");
        EventBus.getDefault().post(message);
    }

    private void sendModifyMemberMessage(List<Long> userList, final SignalCallback callback) {
        CallModifyMemberMessage modifyMemberMessage = new CallModifyMemberMessage();
        modifyMemberMessage.setCallId(this.callSessionImp.getCallId());
        modifyMemberMessage.setCaller(this.callSessionImp.getCallerUserId());
        modifyMemberMessage.setInviter(this.callSessionImp.getInviterUserId());
        modifyMemberMessage.setInvitedList(userList);
        modifyMemberMessage.setEngineType(this.callSessionImp.getEngineType());
        modifyMemberMessage.setMediaType(this.callSessionImp.getMediaType());
        modifyMemberMessage.setModifyMemType(CallModifyMemType.MODIFY_MEM_TYPE_ADD);
        modifyMemberMessage.setExtra(this.callSessionImp.getExtra());
        ArrayList pushList = new ArrayList();
        List participantProfileList = this.callSessionImp.getParticipantProfileList();
        Iterator e = participantProfileList.iterator();

        while (e.hasNext()) {
            CallUserProfile id = (CallUserProfile) e.next();
            pushList.add(id.getUserId());
        }

        e = userList.iterator();

        while (e.hasNext()) {
            Long id1 = (Long) e.next();
            CallUserProfile profile = new CallUserProfile();
            profile.setUserId(id1);
            profile.setMediaType(this.callSessionImp.getMediaType());
            profile.setCallStatus(CallStatus.IDLE);
            participantProfileList.add(profile);
            pushList.add(id1);
        }

        modifyMemberMessage.setParticipantList(participantProfileList);


        Message e1 = Message.obtain(this.callSessionImp.getTargetId(), this.callSessionImp.getConversationType(), modifyMemberMessage);


        ImLib.SendMessageCallback sendMessageCallback = new ImLib.SendMessageCallback() {
            @Override
            public void onAttached(Message message) {

            }

            @Override
            public void onSuccess(Message message) {
                if (callback != null) {
                    String mediaId = CallManager.this.getMediaIdBySentTime(message.getUpdated());
                    callback.onSuccess(message.getSenderId(), mediaId);
                }
            }

            @Override
            public void onError(Message message, ImLib.ErrorCode errorCode) {
                if (callback != null) {
                    callback.onError();
                }
            }
        };
        HoloMessage message = new HoloMessage();
        message.setMessage(e1);
//        message.setCallback(sendMessageCallback);
        message.setAction("CallModifyMemberMessage");
        EventBus.getDefault().post(message);
    }

    private void sendRingingMessage(String callId) {
        CallRingingMessage content = new CallRingingMessage();
        content.setCallId(callId);
        Message e = Message.obtain(callSessionImp.getTargetId(), callSessionImp.getConversationType(), content);

        HoloMessage holoMessage = new HoloMessage();
        holoMessage.setMessage(e);
        EventBus.getDefault().post(holoMessage);

    }

    public void sendAcceptMessage(String callId, CallMediaType type, final SignalCallback callback) {
        CallAcceptMessage content = new CallAcceptMessage();
        content.setCallId(callId);
        content.setMediaType(type);

        Message e = Message.obtain(Long.valueOf(this.callSessionImp.getTargetId()), this.callSessionImp.getConversationType(), content);

        ImLib.SendMessageCallback call = new ImLib.SendImageMessageCallback() {
            @Override
            public void onProgress(Message message, double v) {

            }

            @Override
            public void onAttached(Message message) {

            }

            @Override
            public void onSuccess(Message message) {
                if (callback != null) {
                    String mediaId = CallManager.this.getMediaIdBySentTime(message.getUpdated());
                    callback.onSuccess(message.getSenderId(), mediaId);
                }
            }

            @Override
            public void onError(Message message, ImLib.ErrorCode errorCode) {
                if (callback != null) {
                    callback.onError();
                }
            }
        };


        HoloMessage holoMessage = new HoloMessage();
        holoMessage.setMessage(e);
//        holoMessage.setCallback(call);
        holoMessage.setAction("CallAcceptMessage");
        EventBus.getDefault().post(holoMessage);

    }

    private void sendHangupMessage(ConversationType conversationType, long targetId, String callId, CallDisconnectedReason reason, SignalCallback callback) {
        CallHangupMessage content = new CallHangupMessage();
        content.setCallId(callId);
        content.setHangupReason(reason);

        Message e = Message.obtain(Long.valueOf(targetId), conversationType, content);
        String callEnd = "[结束通话]";


        HoloMessage holoMessage = new HoloMessage();
        holoMessage.setMessage(e);
        holoMessage.setAction("CallHangupMessage");
        EventBus.getDefault().post(holoMessage);
    }

    private String getPushData(CallMediaType mediaType, List<String> userIds, String callId) {
        JSONObject jsonObject = new JSONObject();

        try {
            if (mediaType != null) {
                jsonObject.put("mediaType", mediaType.getValue());
            }

            jsonObject.put("callId", callId);
            JSONArray e = new JSONArray();
            if (userIds != null) {
                Iterator var6 = userIds.iterator();

                while (var6.hasNext()) {
                    long userId = (long) var6.next();
                    e.put(userId);
                }

                jsonObject.put("userIdList", e);
            }
        } catch (JSONException var8) {
            var8.printStackTrace();
        }

        return jsonObject.toString();
    }

    private void changeEngineMediaType(long operatorUserId, CallMediaType mediaType) {
        CallUserProfile profile;
        Iterator localVideo1;
        switch (mediaType.getValue()) {
            case 1: // audio
                this.voIPEngine.setVideoEnabled(false);
                this.callSessionImp.setLocalVideo(null);
                localVideo1 = this.callSessionImp.getParticipantProfileList().iterator();

                while (localVideo1.hasNext()) {
                    profile = (CallUserProfile) localVideo1.next();
                    if (profile.getCallStatus().equals(CallStatus.CONNECTED)) {
                        profile.setVideoView(null);
                    }
                }

                if (this.callListener != null) {
                    this.callListener.onMediaTypeChanged(operatorUserId, CallMediaType.AUDIO, (SurfaceView) null);
                }
                break;
            case 2://video
                this.voIPEngine.setVideoEnabled(true);
                if (this.callSessionImp.getLocalVideo() == null) {
                    SurfaceView localVideo = this.setupLocalVideo();
                    this.callSessionImp.setLocalVideo(localVideo);
                    if (this.callListener != null) {
                        this.callListener.onMediaTypeChanged(operatorUserId, CallMediaType.VIDEO, localVideo);
                    }
                }

                localVideo1 = this.callSessionImp.getParticipantProfileList().iterator();

                while (localVideo1.hasNext()) {
                    profile = (CallUserProfile) localVideo1.next();
                    if (profile.getCallStatus().equals(CallStatus.CONNECTED)) {
                        SurfaceView remoteVideo = profile.getVideoView();
                        this.reSetupRemoteVideo(profile.getUserId(), remoteVideo);
                        profile.setVideoView(remoteVideo);
                        if (this.callListener != null) {
                            this.callListener.onMediaTypeChanged(profile.getUserId(), CallMediaType.VIDEO, remoteVideo);
                        }
                    }
                }
        }

        this.callSessionImp.setMediaType(mediaType);
    }

    private void sendChangeMediaTypeMessage(CallMediaType mediaType) {
        CallModifyMediaMessage content = new CallModifyMediaMessage();
        content.setCallId(this.callSessionImp.getCallId());
        content.setMediaType(mediaType);

        Message message = Message.obtain(this.callSessionImp.getTargetId(), this.callSessionImp.getConversationType(), content);

        HoloMessage holoMessage = new HoloMessage();
        holoMessage.setMessage(message);
        EventBus.getDefault().post(holoMessage);

    }

    private void setupTimerTask(final long userId, final int event, int interval) {
        Log.i(TAG, "setupTimerTask : " + userId);
        TimerTask task = new TimerTask() {
            public void run() {
                android.os.Message msg = android.os.Message.obtain();
                msg.what = event;
                msg.obj = userId;
                CallManager.this.getHandler().sendMessage(msg);
            }
        };
        this.timerTasks.put(userId, task);
        this.timer.schedule(task, (long) interval);
    }

    private void cancelTimerTask(long userId) {
        Log.i(TAG, "cancelTimerTask : " + userId);
        if (this.timerTasks.size() > 0) {
            TimerTask task = (TimerTask) this.timerTasks.get(userId);
            if (task != null) {
                task.cancel();
                this.timerTasks.remove(userId);
            }
        }

    }

    private void resetTimer() {
        if (this.timer != null) {
            this.timer.cancel();
            this.timerTasks.clear();
        }

        this.timer = new Timer();
    }

    private void updateCallRongLog() {
    }

    private boolean updateParticipantCallStatus(long userId, CallStatus status) {
        List userStatusList = this.callSessionImp.getParticipantProfileList();
        Iterator var4 = userStatusList.iterator();

        CallUserProfile userStatus;
        do {
            if (!var4.hasNext()) {
                return false;
            }

            userStatus = (CallUserProfile) var4.next();
        } while (userId != (userStatus.getUserId()));

        if (status.equals(CallStatus.IDLE)) {
            userStatusList.remove(userStatus);
            return true;
        } else {
            userStatus.setCallStatus(status);
            return true;
        }
    }

    private void updateParticipantMediaType(long userId, CallMediaType type) {
        List userStatusList = this.callSessionImp.getParticipantProfileList();
        Iterator var4 = userStatusList.iterator();

        CallUserProfile userStatus;
        do {
            if (!var4.hasNext()) {
                return;
            }

            userStatus = (CallUserProfile) var4.next();
        } while (userId != (userStatus.getUserId()));

        userStatus.setMediaType(type);
    }

    private void updateMediaId(long userId, String mediaId) {
        if (userId > 0 && !TextUtils.isEmpty(mediaId) && this.callSessionImp != null) {
            List profileList = this.callSessionImp.getParticipantProfileList();
            Iterator var4 = profileList.iterator();

            CallUserProfile profile;
            do {
                if (!var4.hasNext()) {
                    return;
                }

                profile = (CallUserProfile) var4.next();
            } while (userId != (profile.getUserId()));

            profile.setMediaId(mediaId);
        }
    }

    private void updateParticipantVideo(long userId, SurfaceView surfaceView) {
        List userStatusList = this.callSessionImp.getParticipantProfileList();
        Iterator var4 = userStatusList.iterator();

        CallUserProfile userStatus;
        do {
            if (!var4.hasNext()) {
                return;
            }

            userStatus = (CallUserProfile) var4.next();
        } while (userId != (userStatus.getUserId()));

        userStatus.setVideoView(surfaceView);
    }

    private void joinChannel(final String mediaId) {
        android.os.Message msg = android.os.Message.obtain(CallManager.this.getHandler(), 206);
        Bundle bundle = new Bundle();
        bundle.putString("result", "");
        bundle.putString("mediaId", mediaId);
        msg.setData(bundle);
        msg.sendToTarget();

    }

    private void leaveChannel() {
        Log.i(TAG, "leaveChannel");
        this.voIPEngine.leaveChannel();
        if (this.callSessionImp.getMediaType().equals(CallMediaType.VIDEO)) {
            // this.voIPEngine.stopPreview();
        }

    }

    public CallSessionImp getCallSessionImp() {
        return this.callSessionImp;
    }

    CallSession getCallSession() {
        if (this.callSessionImp == null) {
            return null;
        } else {
            CallSession callSession = new CallSession();
            callSession.setExtra(this.callSessionImp.getExtra());
            callSession.setMediaType(this.callSessionImp.getMediaType());
            callSession.setEngineType(this.callSessionImp.getEngineType());
            callSession.setConversationType(this.callSessionImp.getConversationType());
            callSession.setTargetId(this.callSessionImp.getTargetId());
            callSession.setInviterUserId(this.callSessionImp.getInviterUserId());
            callSession.setSelfUserId(this.callSessionImp.getSelfUserId());
            callSession.setCallId(this.callSessionImp.getCallId());
            callSession.setCallerUserId(this.callSessionImp.getCallerUserId());
            callSession.setActiveTime(this.callSessionImp.getActiveTime());
            callSession.setEndTime(this.callSessionImp.getEndTime());
            callSession.setStartTime(this.callSessionImp.getStartTime());
            ArrayList profileList = new ArrayList();
            profileList.addAll(this.callSessionImp.getParticipantProfileList());
            callSession.setParticipantUserList(profileList);
            return callSession;
        }
    }

    private void initializeCallSessionFromInvite(Message message) {
        CallInviteMessage inviteMessage = (CallInviteMessage) message.getMessageContent();
        this.callSessionImp = new CallSessionImp();
        this.callSessionImp.setCallId(inviteMessage.getCallId());
        this.callSessionImp.setMediaType(inviteMessage.getMediaType());
        this.callSessionImp.setConversationType(message.getConversationType());
        this.callSessionImp.setEngineType(inviteMessage.getEngineType());
        this.callSessionImp.setTargetId(message.getTargetId());
        this.callSessionImp.setCallerUserId(message.getSenderId());
        this.callSessionImp.setInviterUserId(message.getSenderId());
        this.callSessionImp.setSelfUserId(this.getUserSelfId());
        this.callSessionImp.setExtra(inviteMessage.getExtra());
        ArrayList participantProfileList = new ArrayList();
        CallUserProfile profile = new CallUserProfile();
        profile.setUserId(message.getSenderId());
        profile.setCallStatus(CallStatus.IDLE);
        String mediaId = this.getMediaIdBySentTime(message.getUpdated());
        profile.setMediaId(mediaId);
        profile.setMediaType(inviteMessage.getMediaType());
        participantProfileList.add(profile);
        Iterator var6 = inviteMessage.getInviteUserIds().iterator();

        while (var6.hasNext()) {
            long userId = (long) var6.next();
            profile = new CallUserProfile();
            profile.setUserId(userId);
            profile.setMediaType(inviteMessage.getMediaType());
            profile.setCallStatus(CallStatus.IDLE);
            participantProfileList.add(profile);
        }

        this.callSessionImp.setParticipantUserList(participantProfileList);
    }

    private void initializeCallInfoFromModifyMember(Message message) {
        CallModifyMemberMessage modifyMemberMessage = (CallModifyMemberMessage) message.getMessageContent();
        this.callSessionImp = new CallSessionImp();
        this.callSessionImp.setCallId(modifyMemberMessage.getCallId());
        this.callSessionImp.setConversationType(message.getConversationType());
        this.callSessionImp.setTargetId(message.getTargetId());
        this.callSessionImp.setCallerUserId(message.getSenderId());
        this.callSessionImp.setInviterUserId(message.getSenderId());
        this.callSessionImp.setSelfUserId(this.getUserSelfId());
        this.callSessionImp.setParticipantUserList(modifyMemberMessage.getParticipantList());
        this.callSessionImp.setMediaType(modifyMemberMessage.getMediaType());
        this.callSessionImp.setEngineType(modifyMemberMessage.getEngineType());
        this.callSessionImp.setExtra(modifyMemberMessage.getExtra());
    }

    private boolean shouldTerminateCall(long userId) {
        List participantProfileList = this.callSessionImp.getParticipantProfileList();
        return participantProfileList == null || participantProfileList.size() <= 1;
    }

    private SurfaceView setupRemoteVideo(long userId) {
        Log.i(TAG, "setupRemoteVideo, userId = " + userId);
        final SurfaceView remoteVideo = this.voIPEngine.getRemoteSurfaceView(userId + "");

        CallManager.sInstance.runOnUiThread(new Runnable() {
            public void run() {
                remoteVideo.setZOrderOnTop(true);
                remoteVideo.setZOrderMediaOverlay(true);
            }
        });

        String mediaId = this.getMediaIdByUserId(userId);
        if (mediaId != null) {
            // this.voIPEngine.setupRemoteVideo(remoteVideo, mediaId);
        }

        return remoteVideo;
    }

    private SurfaceView reSetupRemoteVideo(long userId, SurfaceView video) {
        if (video == null) {
            video = this.voIPEngine.getRemoteSurfaceView(userId + "");
        }

//        this.voIPEngine.enableVideo();
//        video.setZOrderOnTop(true);
//        video.setZOrderMediaOverlay(true);
//        String mediaId = this.getMediaIdByUserId(userId);
//        if(mediaId != null) {
//            this.voIPEngine.setupRemoteVideo(video, mediaId);
//            this.voIPEngine.setRemoteRenderMode(mediaId, 1);
//        }


        return video;
    }

    private SurfaceView setupLocalVideo() {
//        Log.i(TAG, "setupLocalVideo");
//        this.voIPEngine.setVideoProfile(this.videoProfile);
//        this.voIPEngine.setLocalRenderMode(1);
//        this.voIPEngine.enableVideo();

        SurfaceView localVideo = this.voIPEngine.getLocalSurfaceView();
        // this.voIPEngine.startPreview();
//        this.voIPEngine.setupLocalVideo(localVideo);
//        this.voIPEngine.startPreview();
        return localVideo;
    }

    private String getMediaIdBySentTime(long sentTime) {
        return (sentTime & 2147483647L) + "";
    }

    private String getMediaIdByUserId(long userId) {
        List participantProfileList = this.callSessionImp.getParticipantProfileList();
        Iterator var3 = participantProfileList.iterator();

        CallUserProfile profile;
        do {
            if (!var3.hasNext()) {
                Log.e(TAG, "getMediaIdByUserId : [userId " + userId + "-> mediaId : null]");
                return null;
            }

            profile = (CallUserProfile) var3.next();
        } while (!profile.getUserId().equals(userId));

        return profile.getMediaId();
    }

    private long getUserIdByMediaId(String mediaId) {
        List participantProfileList = this.callSessionImp.getParticipantProfileList();
        Iterator var3 = participantProfileList.iterator();

        CallUserProfile profile;
        do {
            if (!var3.hasNext()) {
                Log.e(TAG, "getUserIdByMediaId : [mediaId " + mediaId + "-> userId : null]");
                return 0;
            }

            profile = (CallUserProfile) var3.next();
        } while (profile.getMediaId() == null || !profile.getMediaId().equals(mediaId));

        return profile.getUserId();
    }

    private CallDisconnectedReason transferRemoteReason(CallDisconnectedReason reason) {
        return reason.equals(CallDisconnectedReason.CANCEL) ? CallDisconnectedReason.REMOTE_CANCEL : (reason.equals(CallDisconnectedReason.REJECT) ? CallDisconnectedReason.REMOTE_REJECT : (reason.equals(CallDisconnectedReason.HANGUP) ? CallDisconnectedReason.REMOTE_HANGUP : (reason.equals(CallDisconnectedReason.BUSY_LINE) ? CallDisconnectedReason.REMOTE_BUSY_LINE : (reason.equals(CallDisconnectedReason.NO_RESPONSE) ? CallDisconnectedReason.REMOTE_NO_RESPONSE : (reason.equals(CallDisconnectedReason.ENGINE_UNSUPPORTED) ? CallDisconnectedReason.REMOTE_ENGINE_UNSUPPORTED : CallDisconnectedReason.NETWORK_ERROR)))));
    }

    void switchCamera() {
        if (this.voIPEngine != null) {
            this.voIPEngine.switchCamera();
        }

    }

    void setEnableLocalVideo(boolean enabled) {
        if (this.voIPEngine != null) {
            this.voIPEngine.setPublishVideoEnabled(enabled);
        }

    }

    void setEnableLocalAudio(boolean enabled) {
        if (this.voIPEngine != null) {
            this.voIPEngine.setMicrophoneMute(!enabled);
        }

    }

    void setEnableRemoteAudio(long userId, boolean enabled) {
        String mediaId = this.getMediaIdByUserId(userId);
//        if(this.voIPEngine != null && mediaId != null) {
//            this.voIPEngine.muteRemoteAudioStream(mediaId, !enabled);
//        }

    }

    void setEnableAllRemoteAudio(boolean enabled) {
//        if(this.voIPEngine != null) {
//            this.voIPEngine.muteAllRemoteAudioStreams(!enabled);
//        }

    }

    void setEnableRemoteVideo(long userId, boolean enabled) {
        String mediaId = this.getMediaIdByUserId(userId);
//        if(this.voIPEngine != null && mediaId != null) {
//            this.voIPEngine.muteRemoteVideoStream(mediaId, !enabled);
//        }

    }

    public void setEnableAllRemoteVideo(boolean enabled) {

        this.voIPEngine = CallEngine.newInstance();

        if (this.voIPEngine != null) {
            this.voIPEngine.setSubscribeVideoEnabled(enabled);
        }

    }

    void setEnableSpeakerphone(boolean enabled) {
        if (this.voIPEngine != null) {
            this.voIPEngine.setSpeakerphoneOn(enabled);
        }

    }

    void setSpeakerPhoneVolume(int level) {
//        if(this.voIPEngine != null) {
//            this.voIPEngine.setSpeakerphoneVolume(level);
//        }

    }

    void switchVideo(long from, long to) {
        //String fromMediaId = this.getMediaIdByUserId(from);
        //String toMediaId = this.getMediaIdByUserId(to);
        if (this.voIPEngine != null) {
            this.voIPEngine.switchCamera();
        }

    }

    protected boolean isAudioCallEnabled(ConversationType type) {
//        try {
//            String e = this.libStub.getVoIPCallInfo();
//            JSONObject jsonObject = new JSONObject(e);
//            int strategy = jsonObject.getInt("strategy");
//            if(strategy == 1) {
//                JSONArray jsonArray = jsonObject.getJSONArray("callEngine");
//                JSONObject object = jsonArray.getJSONObject(0);
//                int engineType = object.optInt("engineType");
//                if(engineType == CallEngineType.ENGINE_TYPE_AGORA.getValue()) {
//                    return true;
//                }
//
//                if(engineType == CallEngineType.ENGINE_TYPE_RONG.getValue() && type.equals(ConversationType.PRIVATE)) {
//                    return true;
//                }
//            }
//        } catch (Exception var8) {
//            Log.e(TAG, "isAudioCallEnabled error!" + var8.getMessage());
//            var8.printStackTrace();
//        }

        return false;
    }

    protected boolean isVideoCallEnabled(ConversationType type) {
//        try {
//            String e = this.libStub.getVoIPCallInfo();
//            JSONObject jsonObject = new JSONObject(e);
//            int strategy = jsonObject.getInt("strategy");
//            if(strategy == 1) {
//                JSONArray jsonArray = jsonObject.getJSONArray("callEngine");
//                JSONObject object = jsonArray.getJSONObject(0);
//                int engineType = object.optInt("engineType");
//                if(engineType == CallEngineType.ENGINE_TYPE_AGORA.getValue()) {
//                    return true;
//                }
//            }
//        } catch (Exception var8) {
//            Log.e(TAG, "isVideoCallEnabled error! " + var8.getMessage());
//            var8.printStackTrace();
//        }

        return true;
    }

    boolean isServerRecordingEnabled() {
//        try {
//            String e = this.libStub.getVoIPCallInfo();
//            JSONObject jsonObject = new JSONObject(e);
//            int strategy = jsonObject.getInt("strategy");
//            if(strategy == 1) {
//                JSONArray jsonArray = jsonObject.getJSONArray("callEngine");
//                JSONObject object = jsonArray.getJSONObject(0);
//                int engineType = object.optInt("engineType");
//                if(engineType == CallEngineType.ENGINE_TYPE_AGORA.getValue()) {
//                    return true;
//                }
//            }
//        } catch (Exception var7) {
//            Log.e(TAG, "isServerRecordingEnabled error! " + var7.getMessage());
//            var7.printStackTrace();
//        }

        return false;
    }

//    public void registerVideoFrameListener(IVideoFrameListener listener) {
//        if(this.voIPEngine != null) {
//            NativeCallObject.getInstance().registerVideoFrameObserver(listener);
//        } else {
//            this.videoFrameListener = listener;
//        }
//
//    }

    public void unregisterVideoFrameListener() {
//        if(this.voIPEngine != null) {
//            NativeCallObject.getInstance().unregisterVideoFrameObserver();
//        }
//
//        this.videoFrameListener = null;
    }

//    public void registerAudioFrameListener(IAudioFrameListener listener) {
//        if(this.voIPEngine != null) {
//            NativeCallObject.getInstance().registerAudioFrameObserver(listener);
//        } else {
//            this.audioFrameListener = listener;
//        }
//
//    }

    public void unregisterAudioFrameListener() {
//        if(this.voIPEngine != null) {
//            NativeCallObject.getInstance().unregisterAudioFrameObserver();
//        }
//
//        this.audioFrameListener = null;
    }

    void setVideoProfile(CallVideoProfile profile) {
        this.videoProfile = profile.getValue();
    }

    private interface SignalCallback {
        void onError();

        void onSuccess(long var1, String var2);
    }

    private class DisconnectingState extends State {
        private DisconnectingState() {
        }

        public void enter() {
            Log.i(CallManager.TAG, "[" + this.getName() + "] enter");
            CallManager.this.leaveChannel();
            CallManager.this.resetTimer();
        }

        public boolean processMessage(android.os.Message msg) {
            Log.i(CallManager.TAG, "[" + this.getName() + "] processMessage : " + msg.what);
            switch (msg.what) {
                case 202:
                case 204:
                case 404:
//                    CallManager.this.stopEngine();
//                    CallManager.this.transitionTo(CallManager.this.mIdleState);
                    EventBus.getDefault().post(new CloseMessage());
                    break;
                default:
                    CallManager.this.stopEngine();
                    CallManager.this.deferMessage(msg);
                    CallManager.this.transitionTo(CallManager.this.mIdleState);
            }

            return true;
        }
    }

    private class ConnectedState extends State {
        private ConnectedState() {
        }

        public void enter() {
            Log.i(CallManager.TAG, "[" + this.getName() + "] enter");
            CallManager.this.callSessionImp.setActiveTime(System.currentTimeMillis());
        }

        public boolean processMessage(android.os.Message msg) {
            Log.i(CallManager.TAG, "[" + this.getName() + "] processMessage : " + msg.what);
            long userId;
            Message message;
            switch (msg.what) {
                case 103:
                    CallManager.this.sendHangupMessage(CallManager.this.callSessionImp.getConversationType(), CallManager.this.callSessionImp.getTargetId(), CallManager.this.callSessionImp.getCallId(), CallDisconnectedReason.HANGUP, (SignalCallback) null);
                    Log.i(CallManager.TAG, "ConnectedState EVENT_HANG_UP callListener = " + CallManager.this.callListener);
                    if (CallManager.this.callListener != null) {
                        CallManager.this.callSessionImp.setEndTime(System.currentTimeMillis());
                        CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), CallDisconnectedReason.HANGUP);
                    }

                    CallManager.this.transitionTo(CallManager.this.mDisconnectingState);
                    break;
                case 104:
                    final ArrayList newUserList = (ArrayList) msg.obj;
                    CallManager.this.sendModifyMemberMessage(newUserList, new SignalCallback() {
                        public void onError() {
                            Iterator var1 = newUserList.iterator();

                            while (var1.hasNext()) {
                                long userId = (long) var1.next();
                                CallManager.this.updateParticipantCallStatus(userId, CallStatus.IDLE);
                            }

                        }

                        public void onSuccess(long userId, String mediaId) {
                            Iterator var3 = newUserList.iterator();

                            while (var3.hasNext()) {
                                Long id = (Long) var3.next();
                                CallManager.this.updateParticipantCallStatus(id, CallStatus.INCOMING);
                                CallManager.this.setupTimerTask(id, 402, '\uea60');
                                if (CallManager.this.callListener != null) {
                                    CallManager.this.callListener.onRemoteUserInvited(id, CallManager.this.callSessionImp.getMediaType());
                                }
                            }

                        }
                    });
                    break;
                case 105:
                    message = (Message) msg.obj;
                    CallInviteMessage inviteMessage = (CallInviteMessage) message.getMessageContent();
                    boolean isInvited = false;
                    Iterator modifyMemberMessage1 = inviteMessage.getInviteUserIds().iterator();

                    while (modifyMemberMessage1.hasNext()) {
                        Long hangupMessage2 = (Long) modifyMemberMessage1.next();
                        if (CallManager.this.callSessionImp.getSelfUserId() == (hangupMessage2)) {
                            isInvited = true;
                            break;
                        }
                    }

                    if (isInvited) {
                        CallManager.this.updateCallRongLog();
                        CallManager.this.sendHangupMessage(message.getConversationType(), message.getTargetId(), inviteMessage.getCallId(), CallDisconnectedReason.BUSY_LINE, (SignalCallback) null);
                    }
                    break;
                case 106:
                    message = (Message) msg.obj;
                    String mediaId2 = CallManager.this.getMediaIdBySentTime(message.getCreated());
                    CallManager.this.updateMediaId(message.getSenderId(), mediaId2);
                    if (CallManager.this.unknownMediaIdList.size() > 0 && CallManager.this.unknownMediaIdList.contains(mediaId2)) {
                        Log.e(CallManager.TAG, "handle cached mediaId : " + mediaId2);
                        CallManager.this.updateParticipantCallStatus(message.getSenderId(), CallStatus.CONNECTED);
                        CallManager.this.cancelTimerTask(message.getSenderId());
                        SurfaceView remoteVideo1 = null;
                        CallMediaType mediaType = CallManager.this.callSessionImp.getMediaType();
                        if (mediaType != null && mediaType.equals(CallMediaType.VIDEO)) {
                            remoteVideo1 = CallManager.this.setupRemoteVideo(message.getSenderId());
                            CallManager.this.updateParticipantVideo(message.getSenderId(), remoteVideo1);
                        }

                        if (CallManager.this.callListener != null) {
                            CallManager.this.callListener.onRemoteUserJoined(message.getSenderId(), mediaType, remoteVideo1);
                        }

                        CallManager.this.unknownMediaIdList.remove(mediaId2);
                    }
                    break;
                case 107:
                    message = (Message) msg.obj;
                    CallModifyMemberMessage modifyMemberMessage = (CallModifyMemberMessage) message.getMessageContent();
                    Iterator hangupMessage1;
                    long reason1;
                    if (modifyMemberMessage.getCallId().equals(CallManager.this.callSessionImp.getCallId())) {
                        hangupMessage1 = modifyMemberMessage.getInvitedList().iterator();

                        while (hangupMessage1.hasNext()) {
                            reason1 = (long) hangupMessage1.next();
                            CallUserProfile changeMediaTypeMessage2 = new CallUserProfile();
                            changeMediaTypeMessage2.setUserId(reason1);
                            changeMediaTypeMessage2.setMediaType(modifyMemberMessage.getMediaType());
                            changeMediaTypeMessage2.setCallStatus(CallStatus.INCOMING);
                            CallManager.this.setupTimerTask(reason1, 402, '\uea60');
                            CallManager.this.callSessionImp.getParticipantProfileList().add(changeMediaTypeMessage2);
                            if (CallManager.this.callListener != null) {
                                CallManager.this.callListener.onRemoteUserInvited(reason1, modifyMemberMessage.getMediaType());
                            }
                        }

                        return true;
                    } else {
                        hangupMessage1 = modifyMemberMessage.getInvitedList().iterator();

                        while (hangupMessage1.hasNext()) {
                            reason1 = (long) hangupMessage1.next();
                            if (CallManager.this.callSessionImp.getSelfUserId() == (reason1)) {
                                CallManager.this.updateCallRongLog();
                                CallManager.this.sendHangupMessage(message.getConversationType(), message.getTargetId(), modifyMemberMessage.getCallId(), CallDisconnectedReason.BUSY_LINE, (SignalCallback) null);
                            }
                        }

                        return true;
                    }
                case 108:
                    userId = (long) msg.obj;
                    CallManager.this.updateParticipantCallStatus(userId, CallStatus.RINGING);
                    break;
                case 109:
                    message = (Message) msg.obj;
                    CallHangupMessage hangupMessage = (CallHangupMessage) message.getMessageContent();
                    CallDisconnectedReason reason = hangupMessage.getHangupReason();
                    reason = CallManager.this.transferRemoteReason(reason);
                    if (hangupMessage.getCallId().equals(CallManager.this.callSessionImp.getCallId())) {
                        userId = message.getSenderId();
                        boolean changeMediaTypeMessage1 = CallManager.this.updateParticipantCallStatus(userId, CallStatus.IDLE);
                        if (changeMediaTypeMessage1) {
                            CallManager.this.cancelTimerTask(userId);
                            if (CallManager.this.shouldTerminateCall(userId)) {
                                if (CallManager.this.callListener != null) {
                                    CallManager.this.callSessionImp.setEndTime(System.currentTimeMillis());
                                    CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), reason);
                                }

                                CallManager.this.transitionTo(CallManager.this.mDisconnectingState);
                            } else if (CallManager.this.callListener != null) {
                                CallManager.this.callListener.onRemoteUserLeft(userId, hangupMessage.getHangupReason());
                            }
                        } else {
                            Log.e(CallManager.TAG, "user : " + userId + " had been deleted when RECEIVED_LEAVE_CHANNEL_ACTION");
                        }
                    }
                    break;
                case 110:
                    message = (Message) msg.obj;
                    CallModifyMediaMessage changeMediaTypeMessage = (CallModifyMediaMessage) message.getMessageContent();
                    if (changeMediaTypeMessage.getCallId().equals(CallManager.this.callSessionImp.getCallId())) {
                        CallMediaType mediaId1 = changeMediaTypeMessage.getMediaType();
                        CallManager.this.changeEngineMediaType(message.getSenderId(), mediaId1);
                    }
                    break;
                case 203:
                    userId = Long.parseLong((String) msg.obj);//CallManager.this.getUserIdByMediaId((String)msg.obj);
                    if (userId > 0) {
                        CallManager.this.updateParticipantCallStatus(userId, CallStatus.CONNECTED);
                        CallManager.this.cancelTimerTask(userId);
                        SurfaceView mediaId = null;
                        CallMediaType remoteVideo = CallManager.this.callSessionImp.getMediaType();
                        if (remoteVideo != null && remoteVideo.equals(CallMediaType.VIDEO)) {
                            mediaId = CallManager.this.setupRemoteVideo(userId);
                            CallManager.this.updateParticipantVideo(userId, mediaId);
                        }

                        if (CallManager.this.callListener != null) {
                            CallManager.this.callListener.onRemoteUserJoined(userId, remoteVideo, mediaId);
                        }
                    } else {
                        Log.e(CallManager.TAG, "can not find userId as " + msg.obj + ", cache it.");
                        CallManager.this.unknownMediaIdList.add((String) msg.obj);
                    }
                    break;
                case 204:
                    userId = Long.parseLong((String) msg.obj); //CallManager.this.getUserIdByMediaId((String)msg.obj);
                    if (userId > 0) {
                        boolean exist = CallManager.this.updateParticipantCallStatus(userId, CallStatus.IDLE);
                        if (exist) {
                            if (CallManager.this.shouldTerminateCall(userId)) {
                                if (CallManager.this.callListener != null) {
                                    CallManager.this.callSessionImp.setEndTime(System.currentTimeMillis());
                                    CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), CallDisconnectedReason.REMOTE_HANGUP);
                                }

                                CallManager.this.transitionTo(CallManager.this.mDisconnectingState);
                            } else if (CallManager.this.callListener != null) {
                                CallManager.this.callListener.onRemoteUserLeft(userId, CallDisconnectedReason.HANGUP);
                            }
                        } else {
                            Log.e(CallManager.TAG, "uid : " + msg.obj + " had been deleted when RECEIVED_HANG_UP_MSG");
                        }

                    } else {
                        Log.e(CallManager.TAG, "uid : " + msg.obj + " had been deleted when RECEIVED_HANG_UP_MSG");
                    }
                    break;
                case 207:
                    CallMediaType type = (CallMediaType) msg.obj;
                    if (!type.equals(CallManager.this.callSessionImp.getMediaType())) {
                        CallManager.this.changeEngineMediaType(CallManager.this.callSessionImp.getSelfUserId(), type);
                        CallManager.this.sendChangeMediaTypeMessage(type);
                    }
                    break;
                case 208:
                    long muteId = CallManager.this.getUserIdByMediaId((String) msg.obj);
                    boolean muted = msg.arg1 != 0;
                    if (CallManager.this.callListener != null) {
                        CallManager.this.callListener.onRemoteCameraDisabled(muteId, muted);
                    }
                    break;
                case 401:
                case 403:
                    if (CallManager.this.callListener != null) {
                        CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), CallDisconnectedReason.NETWORK_ERROR);
                    }

                    CallManager.this.transitionTo(CallManager.this.mDisconnectingState);
                    break;
                case 402:
                    userId = (long) msg.obj;
                    CallManager.this.updateParticipantCallStatus(userId, CallStatus.IDLE);
                    if (CallManager.this.shouldTerminateCall(userId)) {
                        CallManager.this.sendHangupMessage(CallManager.this.callSessionImp.getConversationType(), CallManager.this.callSessionImp.getTargetId(), CallManager.this.callSessionImp.getCallId(), CallDisconnectedReason.REMOTE_NO_RESPONSE, (SignalCallback) null);
                        if (CallManager.this.callListener != null) {
                            CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), CallDisconnectedReason.REMOTE_NO_RESPONSE);
                        }

                        CallManager.this.transitionTo(CallManager.this.mDisconnectingState);
                    } else if (CallManager.this.callListener != null) {
                        CallManager.this.callListener.onRemoteUserLeft(userId, CallDisconnectedReason.NO_RESPONSE);
                    }
            }

            return true;
        }
    }

    private class ConnectingState extends State {
        private ConnectingState() {
        }

        public void enter() {
            Log.i(CallManager.TAG, "[" + this.getName() + "] enter");
        }

        public boolean processMessage(android.os.Message msg) {
            Log.i(CallManager.TAG, "[" + this.getName() + "] processMessage : " + msg.what);
            Message message;
            long userId;
            Bundle bundle;
            switch (msg.what) {
                case 103:
                    Log.i(CallManager.TAG, "ConnectingState EVENT_HANG_UP callListener = " + CallManager.this.callListener);
                    if (CallManager.this.callListener != null) {
                        CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), CallDisconnectedReason.HANGUP);
                    }

                    CallManager.this.updateParticipantCallStatus(CallManager.this.callSessionImp.getSelfUserId(), CallStatus.IDLE);
                    CallManager.this.sendHangupMessage(CallManager.this.callSessionImp.getConversationType(), CallManager.this.callSessionImp.getTargetId(), CallManager.this.callSessionImp.getCallId(), CallDisconnectedReason.HANGUP, (SignalCallback) null);
                    CallManager.this.transitionTo(CallManager.this.mDisconnectingState);
                    break;
                case 104:
                    final ArrayList newUserList = (ArrayList) msg.obj;
                    CallManager.this.sendModifyMemberMessage(newUserList, new SignalCallback() {
                        public void onError() {
                            Iterator var1 = newUserList.iterator();

                            while (var1.hasNext()) {
                                long userId = (long) var1.next();
                                CallManager.this.updateParticipantCallStatus(userId, CallStatus.IDLE);
                                if (CallManager.this.callListener != null) {
                                    CallManager.this.callListener.onRemoteUserLeft(userId, CallDisconnectedReason.NETWORK_ERROR);
                                }
                            }

                        }

                        public void onSuccess(long userId, String mediaId) {
                            Iterator var3 = newUserList.iterator();

                            while (var3.hasNext()) {
                                long id = (long) var3.next();
                                CallManager.this.updateParticipantCallStatus(userId, CallStatus.INCOMING);
                                CallManager.this.setupTimerTask(id, 402, '\uea60');
                            }

                        }
                    });
                    break;
                case 105:
                    message = (Message) msg.obj;
                    CallInviteMessage inviteMessage = (CallInviteMessage) message.getMessageContent();
                    Iterator modifyMemberMessage1 = inviteMessage.getInviteUserIds().iterator();

                    long hangupMessage2;
                    do {
                        if (!modifyMemberMessage1.hasNext()) {
                            return true;
                        }

                        hangupMessage2 = (long) modifyMemberMessage1.next();
                    } while (CallManager.this.callSessionImp.getSelfUserId() != (hangupMessage2));

                    CallManager.this.updateCallRongLog();
                    CallManager.this.sendHangupMessage(message.getConversationType(), message.getTargetId(), inviteMessage.getCallId(), CallDisconnectedReason.BUSY_LINE, (SignalCallback) null);
                    break;
                case 106:
                    message = (Message) msg.obj;
                    String mediaId = CallManager.this.getMediaIdBySentTime(message.getCreated());
                    CallManager.this.updateMediaId(message.getSenderId(), mediaId);
                    break;
                case 107:
                    message = (Message) msg.obj;
                    CallModifyMemberMessage modifyMemberMessage = (CallModifyMemberMessage) message.getMessageContent();
                    Iterator hangupMessage1;
                    long reason1;
                    if (modifyMemberMessage.getCallId().equals(CallManager.this.callSessionImp.getCallId())) {
                        hangupMessage1 = modifyMemberMessage.getInvitedList().iterator();

                        while (hangupMessage1.hasNext()) {
                            reason1 = (long) hangupMessage1.next();
                            CallUserProfile bundle1 = new CallUserProfile();
                            bundle1.setUserId(reason1);
                            bundle1.setMediaType(modifyMemberMessage.getMediaType());
                            bundle1.setCallStatus(CallStatus.INCOMING);
                            CallManager.this.callSessionImp.getParticipantProfileList().add(bundle1);
                            if (CallManager.this.callListener != null) {
                                CallManager.this.callListener.onRemoteUserInvited(reason1, modifyMemberMessage.getMediaType());
                            }
                        }

                        return true;
                    } else {
                        hangupMessage1 = modifyMemberMessage.getInvitedList().iterator();

                        while (hangupMessage1.hasNext()) {
                            reason1 = (long) hangupMessage1.next();
                            if (CallManager.this.callSessionImp.getSelfUserId() == (reason1)) {
                                CallManager.this.updateCallRongLog();
                                CallManager.this.sendHangupMessage(message.getConversationType(), message.getTargetId(), modifyMemberMessage.getCallId(), CallDisconnectedReason.BUSY_LINE, (SignalCallback) null);
                            }
                        }

                        return true;
                    }
                case 108:
                    userId = (long) msg.obj;
                    CallManager.this.updateParticipantCallStatus(userId, CallStatus.RINGING);
                    break;
                case 109:
                    message = (Message) msg.obj;
                    CallHangupMessage hangupMessage = (CallHangupMessage) message.getMessageContent();
                    userId = message.getSenderId();
                    CallDisconnectedReason reason = hangupMessage.getHangupReason();
                    reason = CallManager.this.transferRemoteReason(reason);
                    if (hangupMessage.getCallId().equals(CallManager.this.callSessionImp.getCallId())) {
                        CallManager.this.updateParticipantCallStatus(userId, CallStatus.IDLE);
                        CallManager.this.cancelTimerTask(userId);
                        if (CallManager.this.shouldTerminateCall(userId)) {
                            if (CallManager.this.callListener != null) {
                                CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), reason);
                            }

                            CallManager.this.transitionTo(CallManager.this.mDisconnectingState);
                        } else if (CallManager.this.callListener != null) {
                            CallManager.this.callListener.onRemoteUserLeft(userId, hangupMessage.getHangupReason());
                        }
                    }
                    break;
                case 201:
                    CallManager.this.cancelTimerTask(CallManager.this.callSessionImp.getSelfUserId());
                    CallManager.this.updateParticipantCallStatus(CallManager.this.callSessionImp.getSelfUserId(), CallStatus.CONNECTED);
                    CallMediaType mediaType = CallManager.this.callSessionImp.getMediaType();
                    SurfaceView localVideo = CallManager.this.callSessionImp.getLocalVideo();
                    if (mediaType != null && mediaType.equals(CallMediaType.VIDEO) && localVideo == null) {
                        localVideo = CallManager.this.setupLocalVideo();
                        CallManager.this.callSessionImp.setLocalVideo(localVideo);
                    }

                    if (CallManager.this.callListener != null) {
                        CallManager.this.callListener.onCallConnected(CallManager.this.getCallSession(), localVideo);
                    }

                    CallManager.this.transitionTo(CallManager.this.mConnectedState);
                case 203:
                default:
                    break;
                case 205:
                    bundle = msg.getData();
                    if (CallManager.this.callSessionImp != null) {
                        CallManager.this.updateMediaId(bundle.getLong("userId"), bundle.getString("mediaId"));
                        CallManager.this.joinChannel(bundle.getString("mediaId"));
                    }
                    break;
                case 206:
                    bundle = msg.getData();
                    if (CallManager.this.callSessionImp != null) {
                        CallManager.this.callSessionImp.setDynamicKey(bundle.getString("result"));
//                        CallManager.this.voIPEngine.setVideoProfile(CallManager.this.videoProfile);
                        CallManager.this.voIPEngine.JoinChannel();
                    }
                    break;
                case 401:
                    CallManager.this.updateParticipantCallStatus(CallManager.this.callSessionImp.getSelfUserId(), CallStatus.IDLE);
                    if (CallManager.this.callListener != null) {
                        CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), CallDisconnectedReason.NETWORK_ERROR);
                    }

                    CallManager.this.transitionTo(CallManager.this.mIdleState);
                    break;
                case 402:
                    userId = (long) msg.obj;
                    CallManager.this.updateParticipantCallStatus(userId, CallStatus.IDLE);
                    if (CallManager.this.shouldTerminateCall(userId)) {
                        CallManager.this.sendHangupMessage(CallManager.this.callSessionImp.getConversationType(), CallManager.this.callSessionImp.getTargetId(), CallManager.this.callSessionImp.getCallId(), CallDisconnectedReason.NO_RESPONSE, (SignalCallback) null);
                        if (CallManager.this.callListener != null) {
                            CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), CallDisconnectedReason.NO_RESPONSE);
                        }

                        CallManager.this.transitionTo(CallManager.this.mDisconnectingState);
                    } else if (CallManager.this.callListener != null) {
                        CallManager.this.callListener.onRemoteUserLeft(userId, CallDisconnectedReason.NO_RESPONSE);
                    }
                    break;
                case 403:
                    CallManager.this.updateParticipantCallStatus(CallManager.this.callSessionImp.getSelfUserId(), CallStatus.IDLE);
                    if (CallManager.this.callListener != null) {
                        CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), CallDisconnectedReason.NETWORK_ERROR);
                    }

                    CallManager.this.sendHangupMessage(CallManager.this.callSessionImp.getConversationType(), CallManager.this.callSessionImp.getTargetId(), CallManager.this.callSessionImp.getCallId(), CallDisconnectedReason.NETWORK_ERROR, (SignalCallback) null);
                    CallManager.this.transitionTo(CallManager.this.mIdleState);
            }

            return true;
        }
    }

    private class OutgoingState extends State {
        private OutgoingState() {
        }

        public void enter() {
            Log.i(CallManager.TAG, "[" + this.getName() + "] enter");
            if (!CallManager.this.startEngine(CallManager.this.preferEngineType)) {
                if (CallManager.this.callListener != null) {
                    CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), CallDisconnectedReason.ENGINE_UNSUPPORTED);
                }

                CallManager.this.transitionTo(CallManager.this.mIdleState);
            } else {
                if (CallManager.this.callListener != null) {
                    SurfaceView localVideo = null;
                    if (CallManager.this.callSessionImp.getMediaType().equals(CallMediaType.VIDEO)) {
                        localVideo = CallManager.this.setupLocalVideo();
                        CallManager.this.callSessionImp.setLocalVideo(localVideo);
                    }

                    CallManager.this.callListener.onCallOutgoing(CallManager.this.getCallSession(), localVideo);
                }

                CallManager.this.callSessionImp.setStartTime(System.currentTimeMillis());
            }
        }

        public boolean processMessage(android.os.Message msg) {
            Log.i(CallManager.TAG, "[" + this.getName() + "] processMessage : " + msg.what);
            Message message;
            long userId;
            switch (msg.what) {
                case 103:
                    CallManager.this.sendHangupMessage(CallManager.this.callSessionImp.getConversationType(), CallManager.this.callSessionImp.getTargetId(), CallManager.this.callSessionImp.getCallId(), CallDisconnectedReason.CANCEL, (SignalCallback) null);
                    Log.i(CallManager.TAG, "OutgoingState EVENT_HANG_UP callListener = " + CallManager.this.callListener);
                    if (CallManager.this.callListener != null) {
                        CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), CallDisconnectedReason.CANCEL);
                    }

                    CallManager.this.transitionTo(CallManager.this.mDisconnectingState);
                    break;
                case 104:
                    final ArrayList newUserList = (ArrayList) msg.obj;
                    CallManager.this.sendModifyMemberMessage(newUserList, new SignalCallback() {
                        public void onError() {
                            Iterator var1 = newUserList.iterator();

                            while (var1.hasNext()) {
                                long userId = (long) var1.next();
                                CallManager.this.updateParticipantCallStatus(userId, CallStatus.IDLE);
                                if (CallManager.this.callListener != null) {
                                    CallManager.this.callListener.onRemoteUserLeft(userId, CallDisconnectedReason.NETWORK_ERROR);
                                }
                            }

                        }

                        public void onSuccess(long userId, String mediaId) {
                            Iterator var3 = newUserList.iterator();

                            while (var3.hasNext()) {
                                long id = (long) var3.next();
                                CallManager.this.updateParticipantCallStatus(userId, CallStatus.INCOMING);
                                CallManager.this.setupTimerTask(id, 402, '\uea60');
                            }

                        }
                    });
                    break;
                case 105:
                    message = (Message) msg.obj;
                    CallInviteMessage inviteMessage = (CallInviteMessage) message.getMessageContent();
                    boolean isInvited = false;
                    Iterator modifyMemberMessage1 = inviteMessage.getInviteUserIds().iterator();

                    while (modifyMemberMessage1.hasNext()) {
                        long hangupMessage2 = (long) modifyMemberMessage1.next();
                        if (CallManager.this.callSessionImp.getSelfUserId() == (hangupMessage2)) {
                            isInvited = true;
                            break;
                        }
                    }

                    if (isInvited) {
                        CallManager.this.updateCallRongLog();

                        CallManager.this.sendHangupMessage(message.getConversationType(), message.getTargetId(), inviteMessage.getCallId(), CallDisconnectedReason.BUSY_LINE, (SignalCallback) null);
                    }
                    break;
                case 106:
                    message = (Message) msg.obj;
                    String mediaId = CallManager.this.getMediaIdBySentTime(message.getCreated());
                    CallManager.this.updateMediaId(message.getSenderId(), mediaId);
                    CallManager.this.joinChannel(CallManager.this.getMediaIdByUserId(CallManager.this.callSessionImp.getSelfUserId()));
                    CallManager.this.transitionTo(CallManager.this.mConnectingState);
                    break;
                case 107:
                    message = (Message) msg.obj;
                    CallModifyMemberMessage modifyMemberMessage = (CallModifyMemberMessage) message.getMessageContent();
                    Iterator hangupMessage1;
                    long reason1;
                    if (modifyMemberMessage.getCallId().equals(CallManager.this.callSessionImp.getCallId())) {
                        hangupMessage1 = modifyMemberMessage.getInvitedList().iterator();

                        while (hangupMessage1.hasNext()) {
                            reason1 = (long) hangupMessage1.next();
                            CallUserProfile profile = new CallUserProfile();
                            profile.setUserId(reason1);
                            profile.setMediaType(modifyMemberMessage.getMediaType());
                            profile.setCallStatus(CallStatus.INCOMING);
                            CallManager.this.callSessionImp.getParticipantProfileList().add(profile);
                            if (CallManager.this.callListener != null) {
                                CallManager.this.callListener.onRemoteUserInvited(reason1, modifyMemberMessage.getMediaType());
                            }
                        }

                        return true;
                    } else {
                        hangupMessage1 = modifyMemberMessage.getInvitedList().iterator();

                        while (hangupMessage1.hasNext()) {
                            reason1 = (long) hangupMessage1.next();
                            if (CallManager.this.callSessionImp.getSelfUserId() == (reason1)) {
                                CallManager.this.updateCallRongLog();
                                CallManager.this.sendHangupMessage(message.getConversationType(), message.getTargetId(), modifyMemberMessage.getCallId(), CallDisconnectedReason.BUSY_LINE, (SignalCallback) null);
                            }
                        }

                        return true;
                    }
                case 108:
                    userId = (long) msg.obj;
                    CallManager.this.updateParticipantCallStatus(userId, CallStatus.RINGING);
                    if (CallManager.this.callListener != null) {
                        CallManager.this.callListener.onRemoteUserRinging(userId);
                    }
                    break;
                case 109:
                    message = (Message) msg.obj;
                    CallHangupMessage hangupMessage = (CallHangupMessage) message.getMessageContent();
                    userId = message.getSenderId();
                    CallDisconnectedReason reason = hangupMessage.getHangupReason();
                    reason = CallManager.this.transferRemoteReason(reason);
                    if (hangupMessage.getCallId().equals(CallManager.this.callSessionImp.getCallId())) {
                        CallManager.this.updateParticipantCallStatus(userId, CallStatus.IDLE);
                        CallManager.this.cancelTimerTask(userId);
                        if (CallManager.this.shouldTerminateCall(userId)) {
                            if (CallManager.this.callListener != null) {
                                CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), reason);
                            }

                            CallManager.this.transitionTo(CallManager.this.mDisconnectingState);
                        } else if (CallManager.this.callListener != null) {
                            CallManager.this.callListener.onRemoteUserLeft(userId, hangupMessage.getHangupReason());
                        }
                    }
                    break;
                case 401:
                case 403:
                case 404:
                    CallManager.this.sendHangupMessage(CallManager.this.callSessionImp.getConversationType(), CallManager.this.callSessionImp.getTargetId(), CallManager.this.callSessionImp.getCallId(), CallDisconnectedReason.HANGUP, (SignalCallback) null);
                    CallManager.this.updateParticipantCallStatus(CallManager.this.callSessionImp.getSelfUserId(), CallStatus.IDLE);
                    if (CallManager.this.callListener != null) {
                        if (msg.what == 404) {
                            CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), CallDisconnectedReason.INIT_VIDEO_ERROR);
                        } else {
                            CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), CallDisconnectedReason.NETWORK_ERROR);
                        }
                    }

                    CallManager.this.transitionTo(CallManager.this.mDisconnectingState);
                    break;
                case 402:
                    userId = (long) msg.obj;
                    CallManager.this.updateParticipantCallStatus(userId, CallStatus.IDLE);
                    if (CallManager.this.shouldTerminateCall(userId)) {
                        CallManager.this.sendHangupMessage(CallManager.this.callSessionImp.getConversationType(), CallManager.this.callSessionImp.getTargetId(), CallManager.this.callSessionImp.getCallId(), CallDisconnectedReason.REMOTE_NO_RESPONSE, (SignalCallback) null);
                        if (CallManager.this.callListener != null) {
                            CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), CallDisconnectedReason.REMOTE_NO_RESPONSE);
                        }

                        CallManager.this.transitionTo(CallManager.this.mDisconnectingState);
                    } else if (CallManager.this.callListener != null) {
                        CallManager.this.callListener.onRemoteUserLeft(userId, CallDisconnectedReason.NO_RESPONSE);
                    }
            }

            return true;
        }
    }

    private class IncomingState extends State {
        private IncomingState() {
        }

        public void enter() {
            Log.i(CallManager.TAG, "[" + this.getName() + "] enter");
            if (!CallManager.this.startEngine(CallManager.this.callSessionImp.getEngineType())) {
                if (CallManager.this.callListener != null) {
                    CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), CallDisconnectedReason.ENGINE_UNSUPPORTED);
                }

                CallManager.this.transitionTo(CallManager.this.mIdleState);
            } else {
                CallManager.this.callSessionImp.setStartTime(System.currentTimeMillis());
            }
        }

        public boolean processMessage(android.os.Message msg) {
            Log.i(CallManager.TAG, "[" + this.getName() + "] processMessage : " + msg.what);
            Message message;
            long userId;
            switch (msg.what) {
                case 102:
                    CallMediaType mediaType = CallManager.this.callSessionImp.getMediaType();
                    SurfaceView localVideo = CallManager.this.callSessionImp.getLocalVideo();
                    if (mediaType != null && mediaType.equals(CallMediaType.VIDEO) && localVideo == null) {
                        localVideo = CallManager.this.setupLocalVideo();
                        CallManager.this.callSessionImp.setLocalVideo(localVideo);
                    }

                    CallManager.this.sendAcceptMessage(CallManager.this.callSessionImp.getCallId(), CallManager.this.callSessionImp.getMediaType(), new SignalCallback() {
                        public void onError() {
                            CallManager.this.getHandler().sendEmptyMessage(401);
                        }

                        public void onSuccess(long userId, String mediaId) {
                            android.os.Message msg = android.os.Message.obtain(CallManager.this.getHandler(), 205);
                            Bundle bundle = new Bundle();
                            bundle.putLong("userId", userId);
                            bundle.putString("mediaId", mediaId);
                            msg.setData(bundle);
                            msg.sendToTarget();
                        }
                    });
                    CallManager.this.transitionTo(CallManager.this.mConnectingState);
                    break;
                case 103:
                    CallManager.this.cancelTimerTask(CallManager.this.callSessionImp.getSelfUserId());
                    CallManager.this.sendHangupMessage(CallManager.this.callSessionImp.getConversationType(), CallManager.this.callSessionImp.getTargetId(), CallManager.this.callSessionImp.getCallId(), CallDisconnectedReason.REJECT, (SignalCallback) null);
                    Log.i(CallManager.TAG, "IncomingState EVENT_HANG_UP callListener = " + CallManager.this.callListener);
                    if (CallManager.this.callListener != null) {
                        CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), CallDisconnectedReason.REJECT);
                    }

                    CallManager.this.transitionTo(CallManager.this.mDisconnectingState);
                    break;
                case 105:
                    message = (Message) msg.obj;
                    CallInviteMessage inviteMessage = (CallInviteMessage) message.getMessageContent();
                    boolean isInvited = false;
                    Iterator modifyMemberMessage1 = inviteMessage.getInviteUserIds().iterator();

                    while (modifyMemberMessage1.hasNext()) {
                        long hangupMessage2 = (long) modifyMemberMessage1.next();
                        if (CallManager.this.callSessionImp.getSelfUserId() == (hangupMessage2)) {
                            isInvited = true;
                            break;
                        }
                    }

                    if (isInvited) {
                        CallManager.this.updateCallRongLog();
                        CallManager.this.sendHangupMessage(message.getConversationType(), message.getTargetId(), inviteMessage.getCallId(), CallDisconnectedReason.BUSY_LINE, (SignalCallback) null);
                    }
                    break;
                case 106:
                    message = (Message) msg.obj;
                    String mediaId = CallManager.this.getMediaIdBySentTime(message.getCreated());
                    CallManager.this.updateMediaId(message.getSenderId(), mediaId);
                    if (message.getSenderId() == (CallManager.this.getUserSelfId())) {
                        if (CallManager.this.callListener != null) {
                            CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), CallDisconnectedReason.OTHER_DEVICE_HAD_ACCEPTED);
                        }

                        CallManager.this.updateParticipantCallStatus(CallManager.this.callSessionImp.getSelfUserId(), CallStatus.IDLE);
                        CallManager.this.transitionTo(CallManager.this.mIdleState);
                    }
                    break;
                case 107:
                    message = (Message) msg.obj;
                    CallModifyMemberMessage modifyMemberMessage = (CallModifyMemberMessage) message.getMessageContent();
                    Iterator hangupMessage1;
                    long reason1;
                    if (modifyMemberMessage.getCallId().equals(CallManager.this.callSessionImp.getCallId())) {
                        hangupMessage1 = modifyMemberMessage.getInvitedList().iterator();

                        while (hangupMessage1.hasNext()) {
                            reason1 = (long) hangupMessage1.next();
                            CallUserProfile userId1 = new CallUserProfile();
                            userId1.setUserId(reason1);
                            userId1.setMediaType(modifyMemberMessage.getMediaType());
                            userId1.setCallStatus(CallStatus.INCOMING);
                            CallManager.this.callSessionImp.getParticipantProfileList().add(userId1);
                            if (CallManager.this.callListener != null) {
                                CallManager.this.callListener.onRemoteUserInvited(reason1, modifyMemberMessage.getMediaType());
                            }
                        }

                        return true;
                    } else {
                        hangupMessage1 = modifyMemberMessage.getInvitedList().iterator();

                        while (hangupMessage1.hasNext()) {
                            reason1 = (long) hangupMessage1.next();
                            if (CallManager.this.callSessionImp.getSelfUserId() == (reason1)) {
                                CallManager.this.updateCallRongLog();
                                CallManager.this.sendHangupMessage(message.getConversationType(), message.getTargetId(), modifyMemberMessage.getCallId(), CallDisconnectedReason.BUSY_LINE, (SignalCallback) null);
                            }
                        }

                        return true;
                    }
                case 108:
                    userId = (long) msg.obj;
                    CallManager.this.updateParticipantCallStatus(userId, CallStatus.RINGING);
                    break;
                case 109:
                    message = (Message) msg.obj;
                    CallHangupMessage hangupMessage = (CallHangupMessage) message.getMessageContent();
                    CallDisconnectedReason reason = hangupMessage.getHangupReason();
                    reason = CallManager.this.transferRemoteReason(reason);
                    userId = message.getSenderId();
                    if (hangupMessage.getCallId().equals(CallManager.this.callSessionImp.getCallId())) {
                        CallManager.this.updateParticipantCallStatus(userId, CallStatus.IDLE);
                        if (CallManager.this.shouldTerminateCall(userId)) {
                            if (CallManager.this.callListener != null) {
                                CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), reason);
                            }

                            CallManager.this.transitionTo(CallManager.this.mDisconnectingState);
                        } else if (CallManager.this.callListener != null) {
                            CallManager.this.callListener.onRemoteUserLeft(userId, hangupMessage.getHangupReason());
                        }
                    }
                    break;
                case 401:
                    if (CallManager.this.callListener != null) {
                        CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), CallDisconnectedReason.NETWORK_ERROR);
                    }

                    CallManager.this.updateParticipantCallStatus(CallManager.this.callSessionImp.getSelfUserId(), CallStatus.IDLE);
                    CallManager.this.transitionTo(CallManager.this.mIdleState);
                    break;
                case 402:
                    if (CallManager.this.callListener != null) {
                        CallManager.this.callSessionImp.setEndTime(System.currentTimeMillis());
                        CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), CallDisconnectedReason.NO_RESPONSE);
                    }

                    CallManager.this.sendHangupMessage(CallManager.this.callSessionImp.getConversationType(), CallManager.this.callSessionImp.getTargetId(), CallManager.this.callSessionImp.getCallId(), CallDisconnectedReason.NO_RESPONSE, (SignalCallback) null);
                    CallManager.this.transitionTo(CallManager.this.mIdleState);
            }

            return true;
        }
    }

    private class IdleState extends State {
        private IdleState() {
        }

        public void enter() {
            Log.i(CallManager.TAG, "[" + this.getName() + "] enter, myUserId = " + CallManager.this.getUserSelfId());
            CallManager.this.callSessionImp = null;
        }

        public boolean processMessage(android.os.Message msg) {
            Log.i(CallManager.TAG, "[" + this.getName() + "] processMessage : " + msg.what);
            long myUserId;
            Message message;
            switch (msg.what) {
                case 101:
                    if (!CallManager.this.updateEngineParameters()) {
                        CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), CallDisconnectedReason.ENGINE_UNSUPPORTED);
                    } else {
                        myUserId = CallManager.this.getUserSelfId();
                        CallManager.this.callSessionImp = (CallSessionImp) msg.obj;
//                        CallManager.this.callSessionImp.setEngineType(CallManager.this.preferEngineType);
                        CallManager.this.callSessionImp.setSelfUserId(myUserId);
                        CallManager.this.updateParticipantCallStatus(myUserId, CallStatus.OUTGOING);
                        ArrayList userList = new ArrayList();
                        Iterator message1 = CallManager.this.callSessionImp.getParticipantProfileList().iterator();

                        while (message1.hasNext()) {
                            CallUserProfile inviteMessage1 = (CallUserProfile) message1.next();
                            if (inviteMessage1.getUserId().equals(myUserId)) {
                                CallManager.this.setupTimerTask(myUserId, 402, '\uea60');
                            } else {
                                CallManager.this.setupTimerTask(inviteMessage1.getUserId(), 402, '\uea60');
                                CallManager.this.updateParticipantCallStatus(inviteMessage1.getUserId(), CallStatus.INCOMING);
                                userList.add(inviteMessage1.getUserId());
                            }
                        }

                        CallManager.this.sendInviteMessage(userList, new SignalCallback() {
                            public void onError() {
                                CallManager.this.getHandler().sendEmptyMessage(401);
                            }

                            public void onSuccess(long userId, String mediaId) {
                                CallManager.this.updateMediaId(userId, mediaId);
                            }
                        });
                        CallManager.this.transitionTo(CallManager.this.mOutgoingState);
                    }
                    break;
                case 105:
                    message = (Message) msg.obj;
                    CallInviteMessage inviteMessage = (CallInviteMessage) message.getMessageContent();
                    CallManager.this.initializeCallSessionFromInvite(message);
                    if (CallManager.this.updateEngineParameters() && CallManager.this.checkSupportEngine(inviteMessage.getEngineType())) {
                        boolean isInvited = false;
                        myUserId = CallManager.this.getUserSelfId();
                        Iterator modifyMemberMessage1 = inviteMessage.getInviteUserIds().iterator();

                        while (modifyMemberMessage1.hasNext()) {
                            long invitedSelf1 = (long) modifyMemberMessage1.next();
                            if (myUserId == (invitedSelf1)) {
                                isInvited = true;
                                break;
                            }
                        }

                        if (isInvited) {
                            modifyMemberMessage1 = CallManager.this.callSessionImp.getParticipantProfileList().iterator();

                            while (modifyMemberMessage1.hasNext()) {
                                CallUserProfile invitedSelf2 = (CallUserProfile) modifyMemberMessage1.next();
                                if (!invitedSelf2.getCallStatus().equals(CallStatus.CONNECTED)) {
                                    CallManager.this.setupTimerTask(invitedSelf2.getUserId(), 402, '\uea60');
                                }
                            }

                            CallManager.this.sendRingingMessage(CallManager.this.callSessionImp.getCallId());
                            CallManager.this.transitionTo(CallManager.this.mIncomingState);
                            if (CallManager.receivedCallListener != null) {
                                CallManager.receivedCallListener.onReceivedCall(CallManager.this.getCallSession());
                            }
                        }
                    } else {
                        CallManager.this.sendHangupMessage(message.getConversationType(), message.getTargetId(), inviteMessage.getCallId(), CallDisconnectedReason.ENGINE_UNSUPPORTED, (SignalCallback) null);
                        CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), CallDisconnectedReason.ENGINE_UNSUPPORTED);
                    }
                    break;
                case 107:
                    message = (Message) msg.obj;
                    CallModifyMemberMessage modifyMemberMessage = (CallModifyMemberMessage) message.getMessageContent();
                    if (CallManager.this.updateEngineParameters() && CallManager.this.checkSupportEngine(modifyMemberMessage.getEngineType())) {
                        myUserId = CallManager.this.getUserSelfId();
                        boolean invitedSelf = false;
                        Iterator var9 = modifyMemberMessage.getInvitedList().iterator();

                        while (var9.hasNext()) {
                            long profile = (long) var9.next();
                            if (myUserId == (profile)) {
                                invitedSelf = true;
                                CallManager.this.initializeCallInfoFromModifyMember(message);
                                break;
                            }
                        }

                        if (invitedSelf) {
                            var9 = CallManager.this.callSessionImp.getParticipantProfileList().iterator();

                            while (var9.hasNext()) {
                                CallUserProfile profile1 = (CallUserProfile) var9.next();
                                if (!profile1.getCallStatus().equals(CallStatus.CONNECTED)) {
                                    CallManager.this.setupTimerTask(profile1.getUserId(), 402, '\uea60');
                                }
                            }

                            CallManager.this.sendRingingMessage(CallManager.this.callSessionImp.getCallId());
                            CallManager.this.transitionTo(CallManager.this.mIncomingState);
                            if (CallManager.receivedCallListener != null) {
                                CallManager.receivedCallListener.onReceivedCall(CallManager.this.getCallSession());
                            }
                        }
                    } else {
                        CallManager.this.sendHangupMessage(message.getConversationType(), message.getTargetId(), modifyMemberMessage.getCallId(), CallDisconnectedReason.ENGINE_UNSUPPORTED, (SignalCallback) null);
                        CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), CallDisconnectedReason.ENGINE_UNSUPPORTED);
                    }
            }

            return true;
        }
    }

    private class UnInitState extends State {
        private UnInitState() {
        }

        public void enter() {
            super.enter();
            Log.i(CallManager.TAG, "[" + this.getName() + "] enter");
        }

        public boolean processMessage(android.os.Message msg) {
            Log.i(CallManager.TAG, "[" + this.getName() + "] processMessage : " + msg.what);
            Message message;
            long myUserId;
            boolean isInvited;
            boolean camera;
            boolean audio;
            switch (msg.what) {
                case 101:
                    CallManager.this.deferMessage(msg);
                    CallManager.this.transitionTo(CallManager.this.mIdleState);
                    break;
                case 105:
                    message = (Message) msg.obj;
                    CallInviteMessage inviteMessage2 = (CallInviteMessage) message.getMessageContent();
                    isInvited = false;
                    myUserId = CallManager.this.getUserSelfId();
                    Iterator mediaType2 = inviteMessage2.getInviteUserIds().iterator();

                    while (mediaType2.hasNext()) {
                        long camera1 = (long) mediaType2.next();
                        if (myUserId == (camera1)) {
                            isInvited = true;
                            break;
                        }
                    }

                    if (isInvited) {
                        CallMediaType mediaType3 = inviteMessage2.getMediaType();
                        camera = CallManager.this.context.checkCallingOrSelfPermission("android.permission.CAMERA") == 0;
                        audio = CallManager.this.context.checkCallingOrSelfPermission("android.permission.RECORD_AUDIO") == 0;
                        boolean needCheckPermissions1 = false;
                        Log.i(CallManager.TAG, "camera permission : " + camera + ", audio permission : " + audio);
                        if (mediaType3.equals(CallMediaType.AUDIO) && !audio) {
                            needCheckPermissions1 = true;
                        }

                        if (mediaType3.equals(CallMediaType.VIDEO) && (!camera || !audio)) {
                            needCheckPermissions1 = true;
                        }

                        if (needCheckPermissions1) {
                            CallManager.this.initializeCallSessionFromInvite(message);
                            android.os.Message cachedMsg = android.os.Message.obtain();
                            cachedMsg.what = msg.what;
                            cachedMsg.obj = msg.obj;
                            CallManager.this.callSessionImp.setCachedMsg(cachedMsg);
                            CallManager.this.transitionTo(CallManager.this.mCheckPermissionState);
                        } else {
                            CallManager.this.deferMessage(msg);
                            CallManager.this.transitionTo(CallManager.this.mIdleState);
                        }
                    }
                    break;
                case 107:
                    message = (Message) msg.obj;
                    CallModifyMemberMessage modifyMemberMessage = (CallModifyMemberMessage) message.getMessageContent();
                    myUserId = CallManager.this.getUserSelfId();
                    isInvited = false;
                    Iterator inviteMessage = modifyMemberMessage.getInvitedList().iterator();

                    while (inviteMessage.hasNext()) {
                        long mediaType = (long) inviteMessage.next();
                        if (myUserId == (mediaType)) {
                            isInvited = true;
                            break;
                        }
                    }

                    if (isInvited) {
                        CallMediaType inviteMessage1 = modifyMemberMessage.getMediaType();
                        boolean mediaType1 = CallManager.this.context.checkCallingOrSelfPermission("android.permission.CAMERA") == 0;
                        camera = CallManager.this.context.checkCallingOrSelfPermission("android.permission.RECORD_AUDIO") == 0;
                        audio = false;
                        Log.i(CallManager.TAG, "camera permission : " + mediaType1 + ", audio permission : " + camera);
                        if (inviteMessage1.equals(CallMediaType.AUDIO) && !camera) {
                            audio = true;
                        }

                        if (inviteMessage1.equals(CallMediaType.VIDEO) && (!mediaType1 || !camera)) {
                            audio = true;
                        }

                        if (audio) {
                            CallManager.this.initializeCallInfoFromModifyMember(message);
                            android.os.Message needCheckPermissions = android.os.Message.obtain();
                            needCheckPermissions.what = msg.what;
                            needCheckPermissions.obj = msg.obj;
                            CallManager.this.callSessionImp.setCachedMsg(needCheckPermissions);
                            CallManager.this.transitionTo(CallManager.this.mCheckPermissionState);
                        } else {
                            CallManager.this.deferMessage(msg);
                            CallManager.this.transitionTo(CallManager.this.mIdleState);
                        }
                    }
                    break;
                case 500:
                    if (CallManager.this.checkSupportEngine(CallManager.this.callSessionImp.getEngineType())) {
                        CallManager.this.deferMessage(CallManager.this.callSessionImp.getCachedMsg());
                        CallManager.this.transitionTo(CallManager.this.mIdleState);
                    } else {
                        CallManager.this.sendHangupMessage(CallManager.this.callSessionImp.getConversationType(), CallManager.this.callSessionImp.getTargetId(), CallManager.this.callSessionImp.getCallId(), CallDisconnectedReason.ENGINE_UNSUPPORTED, (SignalCallback) null);
                        if (CallManager.this.callListener != null) {
                            CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), CallDisconnectedReason.ENGINE_UNSUPPORTED);
                        }
                    }
            }

            return super.processMessage(msg);
        }
    }

    private class CheckPermissionState extends State {
        private CheckPermissionState() {
        }

        public void enter() {
            super.enter();
            Log.i(CallManager.TAG, "[" + this.getName() + "] enter");
            if (CallManager.receivedCallListener != null) {
                CallManager.receivedCallListener.onCheckPermission(CallManager.this.getCallSession());
            }

        }

        public boolean processMessage(android.os.Message msg) {
            Log.i(CallManager.TAG, "[" + this.getName() + "] processMessage : " + msg.what);
            Message message;
            boolean isInvited;
            switch (msg.what) {
                case 103:
                case 501:
                    CallManager.this.sendHangupMessage(CallManager.this.callSessionImp.getConversationType(), CallManager.this.callSessionImp.getTargetId(), CallManager.this.callSessionImp.getCallId(), CallDisconnectedReason.REJECT, (SignalCallback) null);
                    if (CallManager.this.callListener != null) {
                        CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), CallDisconnectedReason.REJECT);
                    }

                    CallManager.this.transitionTo(CallManager.this.mUnInitState);
                    break;
                case 105:
                    message = (Message) msg.obj;
                    CallInviteMessage inviteMessage1 = (CallInviteMessage) message.getMessageContent();
                    isInvited = false;
                    Iterator hangupMessage2 = inviteMessage1.getInviteUserIds().iterator();

                    while (hangupMessage2.hasNext()) {
                        long reason1 = (long) hangupMessage2.next();
                        if (CallManager.this.callSessionImp.getSelfUserId() == (reason1)) {
                            isInvited = true;
                            break;
                        }
                    }

                    if (isInvited) {
                        CallManager.this.updateCallRongLog();
                        CallManager.this.sendHangupMessage(message.getConversationType(), message.getTargetId(), inviteMessage1.getCallId(), CallDisconnectedReason.BUSY_LINE, (SignalCallback) null);
                    }
                    break;
                case 107:
                    message = (Message) msg.obj;
                    CallModifyMemberMessage modifyMemberMessage = (CallModifyMemberMessage) message.getMessageContent();
                    isInvited = false;
                    Iterator inviteMessage = modifyMemberMessage.getInvitedList().iterator();

                    while (inviteMessage.hasNext()) {
                        long hangupMessage1 = (long) inviteMessage.next();
                        if (CallManager.this.callSessionImp.getSelfUserId() == (hangupMessage1)) {
                            isInvited = true;
                            break;
                        }
                    }

                    if (isInvited) {
                        CallManager.this.updateCallRongLog();
                        CallManager.this.sendHangupMessage(message.getConversationType(), message.getTargetId(), modifyMemberMessage.getCallId(), CallDisconnectedReason.BUSY_LINE, (SignalCallback) null);
                    }
                    break;
                case 109:
                    message = (Message) msg.obj;
                    CallHangupMessage hangupMessage = (CallHangupMessage) message.getMessageContent();
                    CallDisconnectedReason reason = hangupMessage.getHangupReason();
                    reason = CallManager.this.transferRemoteReason(reason);
                    if (CallManager.this.callListener != null) {
                        CallManager.this.callListener.onCallDisconnected(CallManager.this.getCallSession(), reason);
                    }

                    CallManager.this.transitionTo(CallManager.this.mUnInitState);
                    break;
                case 500:
                    CallManager.this.getHandler().sendEmptyMessage(500);
                    CallManager.this.transitionTo(CallManager.this.mUnInitState);
            }

            return super.processMessage(msg);
        }
    }
}
