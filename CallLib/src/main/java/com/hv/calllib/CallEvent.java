package com.hv.calllib;

/**
 * Created by yukening on 17/7/18.
 */

public class CallEvent {
    public static final int EVENT_INVITE = 101;
    public static final int EVENT_ACCEPT = 102;
    public static final int EVENT_HANG_UP = 103;
    public static final int EVENT_MODIFY_MEMBER = 104;
    public static final int EVENT_RECEIVED_INVITE_MSG = 105;
    public static final int EVENT_RECEIVED_ACCEPT_MSG = 106;
    public static final int EVENT_RECEIVED_MODIFY_MEMBER_MSG = 107;
    public static final int EVENT_RECEIVED_RING_MSG = 108;
    public static final int EVENT_RECEIVED_HANG_UP_MSG = 109;
    public static final int EVENT_RECEIVED_MODIFY_MEDIA_TYPE_MSG = 110;
    public static final int EVENT_ON_JOIN_CHANNEL_ACTION = 201;
    public static final int EVENT_ON_LEAVE_CHANNEL_ACTION = 202;
    public static final int EVENT_RECEIVED_JOIN_CHANNEL_ACTION = 203;
    public static final int EVENT_RECEIVED_LEAVE_CHANNEL_ACTION = 204;
    public static final int EVENT_GET_VOIP_KEY_ACTION = 205;
    public static final int EVENT_JOIN_CHANNEL_ACTION = 206;
    public static final int EVENT_CHANGE_MEDIA_TYPE = 207;
    public static final int EVENT_USER_MUTE_VIDEO = 208;
    public static final int EVENT_SIGNAL_ERROR = 401;
    public static final int EVENT_TIMEOUT = 402;
    public static final int EVENT_ENGINE_ERROR = 403;
    public static final int EVENT_INIT_VIDEO_ERROR = 404;
    public static final int EVENT_ON_PERMISSION_GRANTED = 500;
    public static final int EVENT_ON_PERMISSION_DENIED = 501;

    public CallEvent() {
    }
}
