package com.hv.calllib;

import com.hv.imlib.ImLib;
import com.hv.imlib.imservice.ModuleManager;
import com.hv.imlib.model.Message;
import com.hv.imlib.model.MessageContent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cn.holo.call.bean.message.ArMarkMessage;
import cn.holo.call.bean.message.CallAcceptMessage;
import cn.holo.call.bean.message.CallHangupMessage;
import cn.holo.call.bean.message.CallInviteMessage;
import cn.holo.call.bean.message.CallModifyMediaMessage;
import cn.holo.call.bean.message.CallModifyMemberMessage;
import cn.holo.call.bean.message.CallRingingMessage;

/**
 * Created by admin on 2019/1/30.
 */

public class HoloCallMsgHandler {
    private List<Class<? extends MessageContent>> mMessages;
    private List<Message> mInviteMessages;
    private List<Message> mHangupMessages;

    private static class HoloCallMsgHandlerInstance {
        private static final HoloCallMsgHandler instance = new HoloCallMsgHandler();
    }

    private HoloCallMsgHandler() {
        mMessages = new ArrayList<>();
        mInviteMessages = new ArrayList<>();
        mHangupMessages = new ArrayList<>();
        initHandler();
    }

    public List<Class<? extends MessageContent>> getmMessages() {
        return mMessages;
    }

    public void setmMessages(List<Class<? extends MessageContent>> mMessages) {
        this.mMessages = mMessages;
    }

    private void initHandler() {
        ModuleManager.addMessageRouter(new ModuleManager.MessageRouter() {
            public boolean onReceived(Message msg, int left, boolean offline, int cmdLeft) {
                if (msg.getMessageContent() instanceof ArMarkMessage){
                    CallManager.getInstance().onArMarkMessage(msg);
                    return false;
                }else {
                    if (getDeltaTime(msg.getUpdated()) < 60000L) {
                        handleMessage(msg, left, offline, cmdLeft);
                    }
                    return mMessages.contains(msg.getMessageContent().getClass());
                }
            }
        });
        ModuleManager.addConnectivityStateChangedListener(new ModuleManager.ConnectivityStateChangedListener() {
            public void onChanged(ImLib.ConnectionStatusListener.ConnectionStatus state) {
                if(state != null && state.equals(ImLib.ConnectionStatusListener.ConnectionStatus.KICKED_OFFLINE_BY_OTHER_CLIENT)) {
                    CallManager.getInstance().getHandler().sendEmptyMessage(401);
                }
            }
        });

    }

    public static HoloCallMsgHandler getInstance() {
        return HoloCallMsgHandlerInstance.instance;
    }


    private boolean storeOfflineVoIPMessage(com.hv.imlib.model.Message msg) {
        MessageContent messageContent = msg.getMessageContent();
        if (!(messageContent instanceof CallInviteMessage) && !(messageContent instanceof CallModifyMediaMessage)) {
            if (messageContent instanceof CallHangupMessage) {
                mHangupMessages.add(msg);
                return true;
            } else {
                return false;
            }
        } else {
            mInviteMessages.add(msg);
            return true;
        }
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

    public void handleMessage(com.hv.imlib.model.Message msg, int left, boolean offline, int cmdLeft) {
        if (cmdLeft != 0 && offline) {
            this.storeOfflineVoIPMessage(msg);
        } else if (mInviteMessages.size() == 0) {
            if (cmdLeft > 0 && this.storeOfflineVoIPMessage(msg)) {
                return;
            }
            this.transferMessage(msg);
        } else {
            MessageContent messageContent = msg.getMessageContent();
            if (!(messageContent instanceof CallInviteMessage) && !(messageContent instanceof CallModifyMediaMessage)) {
                if (messageContent instanceof CallHangupMessage) {
                    mHangupMessages.add(msg);
                }
            } else {
                mInviteMessages.add(msg);
            }

            for (Message message : mInviteMessages) {
                if (mHangupMessages.size() == 0) {
                    transferMessage(message);
                    break;
                }

                String callId;
                if (message.getMessageContent() instanceof CallInviteMessage) {
                    CallInviteMessage index = (CallInviteMessage) message.getMessageContent();
                    callId = index.getCallId();
                } else {
                    CallModifyMediaMessage var12 = (CallModifyMediaMessage) message.getMessageContent();
                    callId = var12.getCallId();
                }

                int var13 = -1;

                for (int i = 0; i < mHangupMessages.size(); ++i) {
                    CallHangupMessage hangupMessage = (CallHangupMessage) mHangupMessages.get(i).getMessageContent();
                    if (callId.equals(hangupMessage.getCallId())) {
                        var13 = i;
                        break;
                    }
                }

                if (var13 == -1) {
                    this.transferMessage(message);
                    break;
                }
            }

            mHangupMessages.clear();
            mInviteMessages.clear();
        }

    }


    public long getDeltaTime(long sentTime) {
        long deltaTime = ImLib.instance().getDeltaTime();
        long normalTime = System.currentTimeMillis() - deltaTime;
        return normalTime - sentTime;
    }
}
