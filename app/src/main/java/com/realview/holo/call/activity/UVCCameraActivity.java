package com.realview.holo.call.activity;

import android.Manifest;
import android.animation.Animator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.holoview.usbcameralib.UVCCameraHelper;
import com.holoview.usbcameralib.utils.FileUtils;
import com.hv.calllib.bean.CaptureImageEvent;
import com.hv.calllib.bean.HoloEvent;
import com.hv.calllib.bean.MajorCommand;
import com.realview.holo.call.FastYUVtoRGB;
import com.realview.holo.call.HoloCallApp;
import com.realview.holo.call.ImageUtil;
import com.realview.holo.call.R;
import com.realview.holo.call.basic.ActivityCollector;
import com.realview.holo.call.basic.BaseActivity;
import com.realview.holo.call.bean.AudioOrderMessage;
import com.realview.holo.call.bean.Constants;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.widget.CameraViewInterface;
import com.serenegiant.usb.widget.UVCCameraTextureView;

import org.evilbinary.tv.widget.BorderEffect;
import org.evilbinary.tv.widget.BorderView;
import org.evilbinary.tv.widget.RoundedFrameLayout;
import org.evilbinary.tv.widget.TvZorderRelativeLayout;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileOutputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class UVCCameraActivity extends BaseActivity implements CameraViewInterface.Callback {
    private static final String TAG = "UVCCameraActivity";

    @BindView(R.id.camera_view)
    UVCCameraTextureView mTextureView;

    @BindView(R.id.mif_switch_camera)
    RoundedFrameLayout mifSwitchCamera;
    @BindView(R.id.mif_take_photo)
    RoundedFrameLayout mifTakePhoto;
    @BindView(R.id.iv_usb_camera_show)
    ImageView ivUsbCameraShow;
    @BindView(R.id.fl_lines)
    ImageView flLines;
    @BindView(R.id.dor_extra_content)
    TvZorderRelativeLayout dorExtraContent;

    private UVCCameraHelper mCameraHelper;
    private CameraViewInterface mUVCCameraView;
    private boolean isRequest;
    private boolean isPreview;


    public boolean inSendingProgress = false;

    private static final int VoiceItem_Home = 0;
    private static final int VoiceItem_TakePhoto = 1;

    private boolean isFinishing = false;
    private boolean isDestroyed = false;

    //private VoiceCmdEngine voiceCmdEngine = null;
    //private UVCCameraActivity.UIHandler uiHandler;


    private Toast mToast;

    private UVCCameraHelper.OnMyDevConnectListener listener = new UVCCameraHelper.OnMyDevConnectListener() {

        @Override
        public void onAttachDev(UsbDevice device) {
            if (mCameraHelper == null || mCameraHelper.getUsbDeviceCount(UVCCameraActivity.this) == 0) {
                showShortMsg("check no usb camera");
                return;
            }
            // request open permission
            if (!isRequest) {
                isRequest = true;
                if (mCameraHelper != null) {
                    mCameraHelper.requestPermission(0, UVCCameraActivity.this);
                }
            }
        }

        @Override
        public void onDettachDev(UsbDevice device) {
            // close camera
            if (isRequest) {
                isRequest = false;
                mCameraHelper.closeCamera();
                showShortMsg(device.getDeviceName() + " is out");
            }
        }

        @Override
        public void onConnectDev(UsbDevice device, boolean isConnected) {
            if (!isConnected) {
                showShortMsg("fail to connect,please check resolution params");
                isPreview = false;
            } else {
                isPreview = true;
                showShortMsg("connecting");
                // initialize seekbar
                // need to wait UVCCamera initialize over
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Looper.prepare();
                        if (mCameraHelper != null && mCameraHelper.isCameraOpened()) {
                            //mSeekBrightness.setProgress(mCameraHelper.getModelValue(UVCCameraHelper.MODE_BRIGHTNESS));
                            //mSeekContrast.setProgress(mCameraHelper.getModelValue(UVCCameraHelper.MODE_CONTRAST));
                        }
                        Looper.loop();
                    }
                }).start();
            }
        }

        @Override
        public void onDisConnectDev(UsbDevice device) {

            showShortMsg("disconnecting");
        }
    };


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioOrderMessage(AudioOrderMessage message) {
        if (message.getType() == 118) {
            //确认
            if (mCameraHelper.isCameraOpened()) {
                TakePhoto();
            }
        } else if (message.getType() == 101 || message.getType() == 119) {
            //取消
            isFinishing = true;
            if (ActivityCollector.isActivityTop(UVCCameraActivity.class, this)) {
                finish();
            }
        } else if (message.getType() == 121) {
            if (ActivityCollector.isActivityTop(UVCCameraActivity.class, this)) {
                finish();
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uvccamera);

        Intent intent = getIntent();
        String launchid = intent.getStringExtra("launchid");
        if (launchid != null) {
            if (launchid.compareTo("holoviewcall") != 0) {
                finish();
            }
        } else {
            finish();
        }
        EventBus.getDefault().register(this);
        ButterKnife.bind(this);
        initView();
        initTVStatusView();
        // step.1 initialize UVCCameraHelper
        mUVCCameraView = (CameraViewInterface) mTextureView;
        mUVCCameraView.setCallback(this);
        mCameraHelper = UVCCameraHelper.getInstance();
        mCameraHelper.setDefaultFrameFormat(UVCCameraHelper.FRAME_FORMAT_YUYV);
        mCameraHelper.setDefaultPreviewSize(640, 480);
        mCameraHelper.initUSBMonitor(this, mUVCCameraView, listener);

        mCameraHelper.setOnPreviewFrameListener(new AbstractUVCCameraHandler.OnPreViewResultListener() {
            @Override
            public void onPreviewResult(byte[] nv21Yuv) {

            }
        });
        RequestPermission();
    }


    private void initTVStatusView() {
        FrameLayout roundedFrameLayout = new FrameLayout(this);
        roundedFrameLayout.setClipChildren(false);

        final BorderView borderView = new BorderView(roundedFrameLayout);
        borderView.setBackgroundResource(R.drawable.border_color);

        ViewGroup list = (ViewGroup) findViewById(R.id.dor_extra_content);
        borderView.attachTo(list);


        borderView.getEffect().addOnFocusChanged(new BorderEffect.FocusListener() {
            @Override
            public void onFocusChanged(View oldFocus, final View newFocus) {
                borderView.getView().setTag(newFocus);

            }
        });
        borderView.getEffect().addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                View t = borderView.getView().findViewWithTag("top");
                if (t != null) {
                    ((ViewGroup) t.getParent()).removeView(t);
                    View of = (View) borderView.getView().getTag(borderView.getView().getId());
                    ((ViewGroup) of).addView(t);

                }

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                View nf = (View) borderView.getView().getTag();
                if (nf != null) {
                    View top = nf.findViewWithTag("top");
                    if (top != null) {
                        ((ViewGroup) top.getParent()).removeView(top);
                        ((ViewGroup) borderView.getView()).addView(top);
                        borderView.getView().setTag(borderView.getView().getId(), nf);

                    }
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

    }

    private void RequestPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            String[] PERMISSIONS_STORAGE = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            int permission = ContextCompat.checkSelfPermission(UVCCameraActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(
                        UVCCameraActivity.this,
                        PERMISSIONS_STORAGE,
                        0
                );
            }
        }
    }

    private void initView() {
        isFinishing = false;
        isDestroyed = false;
    }


    @Override
    public void onSurfaceCreated(CameraViewInterface view, Surface surface) {
        if (!isPreview && mCameraHelper.isCameraOpened()) {
            mCameraHelper.startPreview(mUVCCameraView);
            isPreview = true;
        }
    }

    @Override
    public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {

    }

    @Override
    public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {
        if (isPreview && mCameraHelper.isCameraOpened()) {
            mCameraHelper.stopPreview();
            isPreview = false;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // step.2 register USB event broadcast
        if (mCameraHelper != null) {
            mCameraHelper.registerUSB();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        // step.3 unregister USB event broadcast
        if (mCameraHelper != null) {
            mCameraHelper.unregisterUSB();
        }
    }

    private void destroy() {
        if (isDestroyed) {
            return;
        }

        //  回收
        FileUtils.releaseFile();
        // step.4 release uvc camera resources
        if (mCameraHelper != null) {
            mCameraHelper.release();
        }

        //UnRegisterVoiceCmd();

        isDestroyed = true;
    }

    @Override
    protected void onResume() {
        super.onResume();


    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mToast != null) {
            mToast.cancel();
        }
        this.destroy();
    }

    private void showShortMsg(String msg) {
        if (mToast == null) {
            mToast = Toast.makeText(HoloCallApp.getApp(), msg, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(msg);
        }
        mToast.show();
    }


    private void TakePhoto() {
        Log.d(TAG, "TakePhoto");
        if (mCameraHelper == null || !mCameraHelper.isCameraOpened()) {
            showShortMsg("sorry,camera open failed");
            return;
        }

        mCameraHelper.capturePicture(new AbstractUVCCameraHandler.OnCaptureListener() {
            @Override
            public void onCaputreResult(final byte[] data) {
                inSendingProgress = true;
                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ivUsbCameraShow.setVisibility(View.VISIBLE);
                            FastYUVtoRGB fastYUVtoRGB = new FastYUVtoRGB(UVCCameraActivity.this);
                            bitmap = fastYUVtoRGB.convertYUVtoRGB(data, UVCCameraHelper.getInstance().getPreviewWidth(), UVCCameraHelper.getInstance().getPreviewHeight());
                            ivUsbCameraShow.setImageBitmap(bitmap);
                            imageHandler.sendEmptyMessageDelayed(0, 4000);
                            PostSendPICMessage();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    bitmap.recycle();
                }
            }
        });

    }

    Handler imageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            ivUsbCameraShow.setVisibility(View.GONE);
        }
    };

    Bitmap bitmap;

    private void saveYuv2Jpeg(String path) {
        try {
            File file = new File(path);
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            bitmap.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void PostSendPICMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String srcPath = ImageUtil.getCachePath(UVCCameraActivity.this) + "/" + "uvc-" + System.currentTimeMillis() + UVCCameraHelper.SUFFIX_JPEG;
                File pictureFile = new File(srcPath);
                if (!pictureFile.getParentFile().exists()) {
                    pictureFile.getParentFile().mkdirs();
                }
                saveYuv2Jpeg(srcPath);
                CaptureImageEvent captureImageEvent = new CaptureImageEvent();
                captureImageEvent.setImagepath(srcPath);
                captureImageEvent.setFromeid("0");
                captureImageEvent.setTouid("0");
                Gson gson = new Gson();
                String jsonString = gson.toJson(captureImageEvent);
                HoloEvent hermesEvent = new HoloEvent();
                hermesEvent.setAction("api.camera.capture");
                hermesEvent.setBody(jsonString);
                EventBus.getDefault().postSticky(hermesEvent);
                inSendingProgress = false;
            }
        }).start();
    }

    @OnClick(R.id.mif_switch_camera)
    public void onBack() {
        isFinishing = true;
        finish();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMajorCommand(MajorCommand command) {
        if (command.getData().toLowerCase().equals(Constants.CALLMSG_ID_REMOTE_SWITCH_CAMERA.toLowerCase())) {
            if (HoloCallApp.isActivityTop(UVCCameraActivity.class, this)) {
                onBack();
            }
        }
    }

    @OnClick({R.id.mif_take_photo})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.mif_take_photo:
                TakePhoto();
                break;
        }
    }

}
