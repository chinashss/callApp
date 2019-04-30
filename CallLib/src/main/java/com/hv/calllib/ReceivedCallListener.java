package com.hv.calllib;

/**
 * Created by yukening on 17/7/18.
 */

public interface ReceivedCallListener {
    void onReceivedCall(CallSession var1);

    void onCheckPermission(CallSession var1);
}
