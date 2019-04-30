package com.hv.calllib;

import android.view.SurfaceView;

import cn.holo.call.bean.type.CallDisconnectedReason;
import cn.holo.call.bean.type.CallMediaType;

/**
 * Created by yukening on 17/7/18.
 */

public interface CallListener {
    void onCallOutgoing(CallSession var1, SurfaceView var2);

    void onCallConnected(CallSession var1, SurfaceView var2);

    void onCallDisconnected(CallSession var1, CallDisconnectedReason var2);

    void onRemoteUserRinging(long var1);

    void onRemoteUserJoined(long var1, CallMediaType var2, SurfaceView var3);

    void onRemoteUserInvited(long var1, CallMediaType var2);

    void onRemoteUserLeft(long var1, CallDisconnectedReason var2);

    void onMediaTypeChanged(long var1, CallMediaType var2, SurfaceView var3);

    void onError(CallCommon.CallErrorCode var1);

    void onRemoteCameraDisabled(long var1, boolean var2);
}
