// ProcessServiceIAidl.aidl
package com.holoview.aidl;


import com.holoview.aidl.AudioMessage;
// Declare any non-default types here with import statements

interface ProcessServiceIAidl {
    void sendMessage(String json);
    void initCallMessage();
    void onBindSuccess(String packageName,String serviceName);
    void onAudioData(in AudioMessage audio);
}
