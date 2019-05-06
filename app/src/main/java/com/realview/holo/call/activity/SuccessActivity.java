package com.realview.holo.call.activity;

import android.animation.Animator;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.bumptech.glide.Glide;
import com.holo.tvwidget.MetroViewBorderHandler;
import com.holo.tvwidget.MetroViewBorderImpl;
import com.holoview.usbcameralib.UVCCameraHelper;
import com.hv.calllib.HoloCall;
import com.hv.calllib.bean.CloseMessage;
import com.hv.calllib.bean.MajorCommand;
import com.hv.imlib.model.ConversationType;
import com.hv.imlib.model.message.ImageMessage;
import com.realview.commonlibrary.server.manager.UserManager;
import com.realview.commonlibrary.server.response.UserInfoGetRes;
import com.realview.holo.call.HoloCallApp;
import com.realview.holo.call.R;
import com.realview.holo.call.basic.ActivityCollector;
import com.realview.holo.call.basic.BaseActivity;
import com.realview.holo.call.bean.AudioOrderMessage;
import com.realview.holo.call.bean.Constants;
import com.realview.holo.call.widget.BoardImageView;
import com.realview.holo.call.widget.DiscussionAvatarView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by admin on 2019/2/12.
 */

public class SuccessActivity extends BaseActivity {
    public int timeMills = 0;
    public int timeSecond = 0;
    public int timeHounds = 0;
    @BindView(R.id.tv_video_call_time_countdown)
    TextView tvVideoCallTimeCountdown;
    @BindView(R.id.iv_video_call_image_remote_show)
    BoardImageView ivVideoCallImageRemoteShow;
    @BindView(R.id.task_name)
    TextView tvTaskName;
    @BindView(R.id.task_id)
    TextView tvTaskId;
    @BindView(R.id.daview)
    DiscussionAvatarView daview;
    @BindView(R.id.iv_success_logo)
    ImageView ivSuccessLogo;
    @BindView(R.id.tv_video_status)
    TextView tvVideoStatus;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!(getIntent().getBooleanExtra(Constants.ACTION_CALL_SUCCESS, false))) {
            this.finish();
        }
        setContentView(R.layout.fragment_video_call_success);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        initTVStatusView();
        initUSBCamera();
        startCountdown();
        long taskId = getIntent().getLongExtra(Constants.ACTION_TASK_ID, 0L);
        tvTaskId.setText("工单号：" + taskId);

        String callList = getIntent().getStringExtra(Constants.CALL_LIST);
        if (TextUtils.isEmpty(callList)) {
            return;
        }
        List<Long> longs = JSON.parseArray(callList, Long.class);
        showAvatar(longs);
    }

    private void initUSBCamera() {

    }


    public void showAvatar(List<Long> longs) {
        for (int i = 0; i < longs.size(); i++) {
            if (longs.get(i) <= 0) continue;
            ConversationType type = ConversationType.setValue(getIntent().getIntExtra(Constants.ACTION_CONVERSTAION_TYPE, 0));
            UserManager.instance().getUserInfo(ConversationType.P2P == type ? longs.get(i) : getIntent().getLongExtra(Constants.ACTION_ROOM_ID, 0L), new UserManager.ResultCallback<UserInfoGetRes.ResultBean>() {
                @Override
                public void onSuccess(final UserInfoGetRes.ResultBean resultBean) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            daview.addData(resultBean.getPortrait(), (TextUtils.isEmpty(resultBean.getNickname()) ? resultBean.getUsername() : resultBean.getNickname()));
                        }
                    });


                }

                @Override
                public void onError(String errString) {
                    Log.d("lipengfei", "errString: " + errString);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        stopCountDown();
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
//        closeApp();
    }

    /**
     * 主动性的挂断  会关闭app
     */
    public void closeApp() {
        EventBus.getDefault().post(new CloseMessage());
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioOrderMessage(AudioOrderMessage message) {
        if (message.getType() == 115) {
            //挂断
            closeApp();
        }else if (message.getType()==121){
            if (ActivityCollector.isActivityTop(UVCCameraActivity.class, this)) {
                onSwitchCamera();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onImageReceived(ImageMessage message) {
        ivVideoCallImageRemoteShow.setVisibility(View.VISIBLE);
        Glide.with(this).load(message.getRemoteUri()).into(ivVideoCallImageRemoteShow);
        imageHandler.sendEmptyMessageDelayed(0, 10000);

    }

    Handler imageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            ivVideoCallImageRemoteShow.setVisibility(View.GONE);
        }
    };


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMajorCommand(MajorCommand command) {
        if (command.getData().toLowerCase().equals(Constants.CALLMSG_ID_REMOTE_SWITCH_CAMERA.toLowerCase())) {
            if (HoloCallApp.isActivityTop(SuccessActivity.class, this)) {
                onSwitchCamera();
            }
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    private void initTVStatusView() {
        FrameLayout roundedFrameLayout = new FrameLayout(this);

        final MetroViewBorderImpl metroViewBorderImpl = new MetroViewBorderImpl(roundedFrameLayout);
        metroViewBorderImpl.setBackgroundResource(R.drawable.border_color);

        ViewGroup list = findViewById(R.id.rl_video_call_success_view);
        metroViewBorderImpl.attachTo(list);

        metroViewBorderImpl.getViewBorder().addOnFocusChanged(new MetroViewBorderHandler.FocusListener() {
            @Override
            public void onFocusChanged(View oldFocus, final View newFocus) {
                metroViewBorderImpl.getView().setTag(newFocus);
            }
        });
        metroViewBorderImpl.getViewBorder().addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                View t = metroViewBorderImpl.getView().findViewWithTag("top");
                if (t != null) {
                    ((ViewGroup) t.getParent()).removeView(t);
                    View of = (View) metroViewBorderImpl.getView().getTag(metroViewBorderImpl.getView().getId());
                    ((ViewGroup) of).addView(t);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                View nf = (View) metroViewBorderImpl.getView().getTag();
                if (nf != null) {
                    View top = nf.findViewWithTag("top");
                    if (top != null) {
                        ((ViewGroup) top.getParent()).removeView(top);
                        ((ViewGroup) metroViewBorderImpl.getView()).addView(top);
                        metroViewBorderImpl.getView().setTag(metroViewBorderImpl.getView().getId(), nf);

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

    /**
     * 倒计时开启
     */
    public void startCountdown() {
        handler.postDelayed(countdownRunnable, 1000);
    }

    public void stopCountDown() {
        handler.removeCallbacks(countdownRunnable);
        timeMills = 0;
        timeSecond = 0;
        tvVideoCallTimeCountdown.setText("00:00");

    }

    private Handler handler = new Handler();
    private Runnable countdownRunnable = new Runnable() {
        @Override
        public void run() {
            StringBuilder sb = new StringBuilder();
            timeSecond++;
            if (timeSecond >= 60) {
                timeMills++;
                timeSecond = 0;
            }
            if (timeMills >= 60) {
                timeHounds++;
                timeMills = 0;
            }
            sb.append(timeHounds < 10 ? "0" + timeHounds : timeHounds);
            sb.append(":");
            sb.append(timeMills < 10 ? "0" + timeMills : timeMills);
            sb.append(":");
            sb.append(timeSecond < 10 ? "0" + timeSecond : timeSecond);
            tvVideoCallTimeCountdown.setText(sb.toString());


            if (timeSecond % 3 == 0) {
                tvVideoStatus.setText("正在通话中.");
            } else if (timeSecond % 3 == 1) {
                tvVideoStatus.setText("正在通话中..");
            } else if (timeSecond % 3 == 2) {
                tvVideoStatus.setText("正在通话中...");
            }


            handler.postDelayed(this, 1000);
        }
    };

    @OnClick(R.id.video_call_stop)
    public void stop() {
        closeApp();
    }

    @OnClick(R.id.video_call_camera_change)
    public void onSwitchCamera() {
//        Intent usbcameraIntent = new Intent(this, UVCCameraActivity.class);
//        usbcameraIntent.putExtra("usbcamera", 0);
//        usbcameraIntent.putExtra("launchid", "holoviewcall");
//        startActivity(usbcameraIntent);
        HoloCall.getInstance().switchCamera();

//        if (getUSBCamera()) {
//            boolean ispermiss = UVCCameraHelper.getInstance().requestPermission(0,this);
//            if (!ispermiss){
//                UVCCameraHelper.getInstance().release();
//                Toast.makeText(this, "请检查权限", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//        } else {
//            Toast.makeText(this, "请检查是否插入摄像头", Toast.LENGTH_SHORT).show();
//        }

    }

    UsbManager mUsbManager;

    private boolean getUSBCamera() {
        if (mUsbManager == null) {
            mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        }
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        if (deviceList == null || deviceList.isEmpty()) {
            return false;
        }


        return true;
    }

}
