package org.webrtc;


interface UsbCameraSession {
    void stop();

    public interface Events {
        void onCameraOpening();

        void onCameraError(UsbCameraSession session, String error);

        void onCameraDisconnected(UsbCameraSession var1);

        void onCameraClosed(UsbCameraSession var1);

        void onByteBufferFrameCaptured(UsbCameraSession var1, byte[] data, int width, int height, int rotation, long timestamp);

        void onTextureFrameCaptured(UsbCameraSession session, int width, int height, int oesTextureId, float[] transformMatrix, int rotation, long timestamp);
    }

    public interface CreateSessionCallback {
        void onDone(UsbCameraSession var1);

        void onFailure(UsbCameraSession.FailureType var1, String var2);
    }

    public static enum FailureType {
        ERROR,
        DISCONNECTED;

        private FailureType() {
        }
    }
}
