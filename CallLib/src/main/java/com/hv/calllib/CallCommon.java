package com.hv.calllib;

/**
 * Created by yukening on 17/7/18.
 */




public class CallCommon {
    public CallCommon() {
    }

    public static enum ServerRecordingErrorCode {
        SUCCESS(0),
        FAIL(1),
        INVALID_ARGUMENT(2),
        NOT_READY(3),
        NOT_IN_CALL(4),
        NOT_INITIALIZED(7),
        TIME_OUT(10);

        private int value;

        private ServerRecordingErrorCode(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

        public static CallCommon.ServerRecordingErrorCode valueOf(int value) {
            CallCommon.ServerRecordingErrorCode[] var1 = values();
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                CallCommon.ServerRecordingErrorCode v = var1[var3];
                if(v.value == value) {
                    return v;
                }
            }

            return null;
        }
    }

    public static enum CallVideoProfile {
        VIDEO_PROFILE_240P(20),
        VIDEO_PROFILE_360P(30),
        VIDEO_PROFILE_480P(40),
        VIDEO_PROFILE_720P(50);

        private int value;

        private CallVideoProfile(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

        public static CallCommon.CallVideoProfile valueOf(int value) {
            CallCommon.CallVideoProfile[] var1 = values();
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                CallCommon.CallVideoProfile v = var1[var3];
                if(v.value == value) {
                    return v;
                }
            }

            return null;
        }
    }

    public static enum CallPermission {
        PERMISSION_AUDIO,
        PERMISSION_CAMERA,
        PERMISSION_AUDIO_AND_CAMERA;

        private CallPermission() {
        }
    }




    public static enum CallErrorCode {
        ENGINE_ERROR(1),
        SIGNAL_ERROR(2);

        private int value;

        private CallErrorCode(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

        public static CallCommon.CallErrorCode valueOf(int value) {
            CallCommon.CallErrorCode[] var1 = values();
            int var2 = var1.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                CallCommon.CallErrorCode v = var1[var3];
                if(v.value == value) {
                    return v;
                }
            }

            return null;
        }
    }

}
