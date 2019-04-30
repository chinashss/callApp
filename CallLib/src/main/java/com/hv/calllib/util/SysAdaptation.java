package com.hv.calllib.util;

/**
 * Created by liuhongyu on 2017/7/26.
 */

import android.media.AudioManager;
import java.io.IOException;

public class SysAdaptation {

    public static class MIUIUtils {

        private static final String KEY_MIUI_VERSION_CODE      = "ro.miui.ui.version.code";
        private static final String KEY_MIUI_VERSION_NAME      = "ro.miui.ui.version.name";
        private static final String KEY_MIUI_INTERNAL_STORAGE  = "ro.miui.internal.storage";

        public static boolean isMIUI() {
            try {
                final BuildProperties prop = BuildProperties.newInstance();
                return prop.getProperty(KEY_MIUI_VERSION_CODE, null) != null
                        || prop.getProperty(KEY_MIUI_VERSION_NAME, null) != null
                        || prop.getProperty(KEY_MIUI_INTERNAL_STORAGE, null) != null;
            } catch (final IOException e) {
                return false;
            }
        }
    }

    // 做适配用，未来根据不通终端决定是返回什么参数
    public static int GetAudioManageMode() {
        int mode = AudioManager.MODE_IN_COMMUNICATION;

        if( MIUIUtils.isMIUI() ) {
            mode = AudioManager.MODE_IN_COMMUNICATION;//MODE_IN_CALL  MODE_RINGTONE MODE_NORMAL
        }

        return mode;
    }

}
