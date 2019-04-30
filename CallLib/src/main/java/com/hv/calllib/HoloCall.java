package com.hv.calllib;

import android.content.Context;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.hv.calllib.record.RecordManager;
import com.hv.imlib.model.ConversationType;
import com.hv.imlib.model.MessageContent;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import cn.holo.call.bean.CallUserProfile;
import cn.holo.call.bean.message.CallAcceptMessage;
import cn.holo.call.bean.message.CallHangupMessage;
import cn.holo.call.bean.message.CallInviteMessage;
import cn.holo.call.bean.message.CallModifyMediaMessage;
import cn.holo.call.bean.message.CallModifyMemberMessage;
import cn.holo.call.bean.message.CallRingingMessage;
import cn.holo.call.bean.type.CallEngineType;
import cn.holo.call.bean.type.CallMediaType;
import cn.holo.call.bean.type.CallStatus;

/**
 * Created by admin on 2019/1/29.
 * 正式的入口
 */

public class HoloCall {
    public static final String TAG = "HoloCall";
    private BaseLoaderCallback mLoaderCallback = null;
    public static String routeUrl = "wss://122.225.234.90:8443/groupcall";
    public static boolean needAudio = true;


    private static class HoloCallInstance {
        private static final HoloCall instance = new HoloCall();
    }

    private HoloCall() {
    }

    public static HoloCall getInstance() {
        return HoloCallInstance.instance;
    }

    public void setUserSelfId(long userSelfId) {
        CallSessionImp callSessionImp = CallManager.getInstance().getCallSessionImp();
        if (callSessionImp!=null){
            callSessionImp.setSelfUserId(userSelfId);
        }
    }

    public void init(Context mContext) {
        initOpenCVSDK(mContext);
        //注册消息类型  need
        CallManager.getInstance().init(mContext);
        HoloCallMsgHandler.getInstance();
    }

    private void transferMessage(com.hv.imlib.model.Message msg) {
        MessageContent messageContent = msg.getMessageContent();
        android.os.Message message;
        if (messageContent instanceof CallAcceptMessage) {
            message = android.os.Message.obtain();
            message.what = 106;
            message.obj = msg;
            CallManager.getInstance().sendMessage(message);
        } else if (messageContent instanceof CallInviteMessage) {
//            if ((msg.getUpdated() - msg.getCreated()) > 60 * 1000 ){
//                return;
//            }
            message = android.os.Message.obtain();
            message.what = 105;
            message.obj = msg;
            CallManager.getInstance().sendMessage(message);
        } else if (messageContent instanceof CallRingingMessage) {
            message = android.os.Message.obtain();
            message.what = 108;
            message.obj = msg.getSenderId();
            CallManager.getInstance().sendMessage(message);
        } else if (messageContent instanceof CallHangupMessage) {
            message = android.os.Message.obtain();
            message.what = 109;
            message.obj = msg;
            CallManager.getInstance().sendMessage(message);
        } else if (messageContent instanceof CallModifyMediaMessage) {
            message = android.os.Message.obtain();
            message.what = 110;
            message.obj = msg;
            CallManager.getInstance().sendMessage(message);
        } else if (messageContent instanceof CallModifyMemberMessage) {
            message = android.os.Message.obtain();
            message.what = 107;
            message.obj = msg;
            CallManager.getInstance().sendMessage(message);
        }

    }


    private void initOpenCVSDK(Context AppContext) {
        if (mLoaderCallback == null) {
            mLoaderCallback = new BaseLoaderCallback(AppContext) {
                @Override
                public void onManagerConnected(int status) {
                    switch (status) {
                        case LoaderCallbackInterface.SUCCESS: {
                            Log.i(TAG, "OpenCV loaded successfully");
                        }
                        break;
                        default: {
                            super.onManagerConnected(status);
                        }
                        break;
                    }
                }
            };
        }

        if (OpenCVLoader.initDebug()) {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.INIT_FAILED);
        }
    }


    /**
     * 呼出，并且获取CallId，同步请求！
     *
     * @param conversationType
     * @param targetId
     * @param userIds
     * @param mediaType
     * @param callEngineType
     * @param extra
     * @param deviceType
     * @return
     */
    public String startCall(ConversationType conversationType, long targetId, List<Long> userIds, CallMediaType mediaType,
                            CallEngineType callEngineType, String extra, int deviceType,long userSelfId) {

        if (conversationType != null && targetId > 0 && userIds != null && userIds.size() != 0) {
            String callId = makeCallId(conversationType, targetId, userIds);

//            long myId = 1000156016768L;////-----替换

            RecordManager.getInstance().commitCallIds(userSelfId, targetId, callId, deviceType);
            Message message = Message.obtain();
            message.what = 101;
            CallSessionImp callInfo = new CallSessionImp();
            callInfo.setExtra(extra);
            callInfo.setConversationType(conversationType);
            callInfo.setMediaType(mediaType);
            callInfo.setTargetId(targetId);
            callInfo.setCallId(callId);
            callInfo.setCallerUserId(userSelfId);
            callInfo.setInviterUserId(userSelfId);
            callInfo.setEngineType(callEngineType);

            List<CallUserProfile> list = new ArrayList<>();
            CallUserProfile state = new CallUserProfile();
            state.setUserId(userSelfId);
            state.setMediaType(mediaType);
            state.setCallStatus(CallStatus.IDLE);
            list.add(state);

            for (long id : userIds) {
                state = new CallUserProfile();
                state.setUserId(id);
                state.setMediaType(mediaType);
                state.setCallStatus(CallStatus.IDLE);
                list.add(state);
            }

            callInfo.setParticipantUserList(list);
            message.obj = callInfo;
            CallManager.getInstance().sendMessage(message);
            return callId;
        }
        return null;
    }

    /**
     * 接听通话
     *
     * @param callId
     */
    public void acceptCall(String callId) {
        if (TextUtils.isEmpty(callId)) {
            Log.e(TAG, "acceptCall : Illegal Argument.");
        } else {
            android.os.Message message = android.os.Message.obtain();
            message.what = 102;
            message.obj = callId;
            CallManager.getInstance().sendMessage(message);
        }
    }

    /**
     * 挂断通话
     *
     * @param callId
     */
    public void hangUpCall(String callId) {
        if (TextUtils.isEmpty(callId)) {
            Log.e(TAG, "hangUpCall : Illegal Argument.");
        } else {
            android.os.Message message = android.os.Message.obtain();
            message.what = 103;
            message.obj = callId;
            CallManager.getInstance().sendMessage(message);
        }
    }


    /**
     * 设置监听事件
     *
     * @param callListener
     */
    public void setVoIPCallListener(CallListener callListener) {
        CallManager.getInstance().setCallListener(callListener);
    }


    /**
     * 获取通话Session
     *
     * @return
     */
    public CallSession getCallSession() {
        return CallManager.getInstance().getCallSession();
    }


    private String makeCallId(ConversationType conversationType, long targetId, List<Long> userIds) {
        StringBuilder sb = new StringBuilder();
        for (Long userId : userIds) {
            sb.append(userId);
        }
        long time1 = System.currentTimeMillis();
        return ShortMD5(new String[]{"" + conversationType + targetId + sb.toString() + time1});
    }

    private static String ShortMD5(String... args) {
        try {
            StringBuilder sb = new StringBuilder();
            String[] mdInst = args;
            int mds = args.length;

            for (int result = 0; result < mds; ++result) {
                String arg = mdInst[result];
                sb.append(arg);
            }

            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(sb.toString().getBytes());
            byte[] digBytes = messageDigest.digest();
            digBytes = Base64.encode(digBytes, 2);
            String digBase64 = new String(digBytes);
            digBase64 = digBase64.replace("=", "").replace("+", "-").replace("/", "_").replace("\n", "");
            return digBase64;
        } catch (Exception var6) {
            var6.printStackTrace();
            return "";
        }
    }


    public void addParticipants(String callId, List<Long> userIds) {
        if (!TextUtils.isEmpty(callId) && userIds != null && userIds.size() != 0) {
            CallSessionImp callInfo = CallManager.getInstance().getCallSessionImp();
            if (callInfo.getCallId() == null) {
                Log.e(TAG, "addParticipants : Call don\'t start yet.");
            } else if (!callInfo.getCallId().equals(callId)) {
                Log.e(TAG, "addParticipants : callId does not exist.");
            } else {
                android.os.Message message = android.os.Message.obtain();
                message.what = 104;
                message.obj = userIds;
                CallManager.getInstance().sendMessage(message);
            }
        } else {
            Log.e(TAG, "addParticipants : Illegal Argument.");
        }
    }

    public void changeCallMediaType(CallMediaType mediaType) {
        if (mediaType == null) {
            Log.e(TAG, "changeLocalMediaType : Illegal Argument.");
        } else {
            android.os.Message message = android.os.Message.obtain();
            message.what = 207;
            message.obj = mediaType;
            CallManager.getInstance().sendMessage(message);
        }
    }

    public void switchCamera() {
        CallManager.getInstance().switchCamera();
    }

    public void setEnableLocalVideo(boolean enabled) {
        CallManager.getInstance().setEnableLocalVideo(enabled);
    }

    public void setEnableLocalAudio(boolean enabled) {
        CallManager.getInstance().setEnableLocalAudio(enabled);
    }

    public void setEnableAllRemoteVideo(boolean enable) {
        CallManager.getInstance().setEnableAllRemoteVideo(enable);
    }

    public void setEnableSpeakerphone(boolean enabled) {
        CallManager.getInstance().setEnableSpeakerphone(enabled);
    }

    public void setSpeakerPhoneVolume(int level) {
        if (level <= 0) {
            Log.e(TAG, "setSpeakerPhoneVolume : Illegal Argument.");
        } else {
            CallManager.getInstance().setSpeakerPhoneVolume(level);
        }
    }

    public boolean isAudioCallEnabled(ConversationType type) {
        return CallManager.getInstance().isAudioCallEnabled(type);
    }

    public boolean isVideoCallEnabled(ConversationType type) {
        return CallManager.getInstance().isVideoCallEnabled(type);
    }

    public void onPermissionGranted() {
        android.os.Message message = android.os.Message.obtain();
        message.what = 500;
        //CallManager.getInstance().sendMessage(message);
    }

    public void onPermissionDenied() {
        android.os.Message message = android.os.Message.obtain();
        message.what = 501;
        //CallManager.getInstance().sendMessage(message);
    }

//    public void registerVideoFrameListener(IVideoFrameListener listener) {
//        //CallManager.getInstance().registerVideoFrameListener(listener);
//    }

    public void unregisterVideoFrameObserver() {
        CallManager.getInstance().unregisterVideoFrameListener();
    }

//    public void registerAudioFrameListener(IAudioFrameListener listener) {
//        CallManager.getInstance().registerAudioFrameListener(listener);
//    }

    public void unregisterAduioFrameObserver() {
        CallManager.getInstance().unregisterAudioFrameListener();
    }

    public void setVideoProfile(CallCommon.CallVideoProfile profile) {
        CallManager.getInstance().setVideoProfile(profile);
    }
}
