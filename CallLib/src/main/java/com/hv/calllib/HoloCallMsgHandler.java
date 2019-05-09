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

    private static class HoloCallMsgHandlerInstance {
        private static final HoloCallMsgHandler instance = new HoloCallMsgHandler();
    }

    private HoloCallMsgHandler() {
    }

    public static HoloCallMsgHandler getInstance() {
        return HoloCallMsgHandlerInstance.instance;
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

    public void handleMessage(com.hv.imlib.model.Message msg) {
        this.transferMessage(msg);
    }
}
