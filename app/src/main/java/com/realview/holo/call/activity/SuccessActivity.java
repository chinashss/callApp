package com.realview.holo.call.activity;

import android.animation.Animator;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.telecom.Call;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.bumptech.glide.Glide;
import com.hv.calllib.CallManager;
import com.hv.calllib.HoloCall;
import com.hv.calllib.bean.HoloEvent;
import com.hv.calllib.bean.MajorCommand;
import com.hv.imlib.HoloMessage;
import com.hv.imlib.imservice.event.CaptureImageEvent;
import com.hv.imlib.model.message.ImageMessage;
import com.hv.imlib.model.message.TextMessage;
import com.realview.holo.call.CallApp;
import com.realview.holo.call.HoloCallApp;
import com.realview.holo.call.ImageUtil;
import com.realview.holo.call.R;
import com.realview.holo.call.basic.ActivityCollector;
import com.realview.holo.call.basic.BaseActivity;
import com.realview.holo.call.bean.AudioOrderMessage;
import com.realview.holo.call.bean.CallStateMessage;
import com.realview.holo.call.bean.Constants;
import com.realview.holo.call.widget.BoardImageView;

import org.evilbinary.tv.widget.BorderEffect;
import org.evilbinary.tv.widget.BorderView;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.Date;

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
    @BindView(R.id.iv_success_logo)
    ImageView ivSuccessLogo;
    @BindView(R.id.tv_video_status)
    TextView tvVideoStatus;
    @BindView(R.id.content)
    FrameLayout content;
    @BindView(R.id.tv_message_text)
    TextView tvMessageText;
    @BindView(R.id.tv_show_preview_tips)
    TextView tvShowPreviewTips;
    @BindView(R.id.iv_show_preview_tips)
    ImageView ivShowPreviewTips;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!(getIntent().getBooleanExtra(Constants.ACTION_CALL_SUCCESS, false))) {
            this.finish();
            return;
        }
        setContentView(R.layout.fragment_video_call_success);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        initTVStatusView();
        initUSBCamera();
        startCountdown();
        if (CallApp.getInstance().getSurfaceView() != null) {
            content.addView(CallApp.getInstance().getSurfaceView());
        }
    }

    private void initUSBCamera() {

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
        Toast.makeText(this, "视频连接已断开", Toast.LENGTH_SHORT).show();
//        HoloMessage message = new HoloMessage();
//        message.setAction("api.audio.unsubscribe");
//        CallApp.getInstance().sendMessage(message);
        if (CallApp.getInstance().getSession() != null) {
            HoloCall.getInstance().hangUpCall(CallApp.getInstance().getSession().getCallId());
        }
        finish();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCallMessage(CallStateMessage message) {
        switch (message) {
            case CallDisconnected:
                if (ActivityCollector.isActivityTop(UVCCameraActivity.class, this)) {
                    ActivityCollector.closeActivity(UVCCameraActivity.class);
                }
                closeApp();
                break;
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioOrderMessage(AudioOrderMessage message) {
        if (message.getType() == 115) {
            //挂断
            closeApp();
        } else if (message.getType() == 121) {
            if (ActivityCollector.isActivityTop(UVCCameraActivity.class, this)) {
                onSwitchCamera();
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSendCaptureImageEvent(HoloEvent event) {
        if (!event.getAction().equals("api.camera.capture")) {
            return;
        }
        Toast.makeText(this, event.getBody(), Toast.LENGTH_SHORT).show();
        try {
            String body = event.getBody();
            CaptureImageEvent captureImageEvent = JSON.parseObject(body, CaptureImageEvent.class);
            String imagePath = captureImageEvent.getImagepath();
            ImageMessage imageMessage = new ImageMessage();
            ImageUtil.ImageEntity entity = ImageUtil.getImageEntity(this, Uri.parse("file://" + imagePath));
            imageMessage.setLocalUri(Uri.parse("file://" + entity.sourcePath));
            imageMessage.setmLocalThumUri(Uri.parse("file://" + entity.thumbPath));
            com.hv.imlib.model.Message e = com.hv.imlib.model.Message.obtain(Long.valueOf(CallManager.getInstance().getCallSessionImp().getTargetId()), CallManager.getInstance().getCallSessionImp().getConversationType(), imageMessage);
            HoloMessage holoMessage = new HoloMessage();
            holoMessage.setAction("ImageMessage");
            holoMessage.setMessage(e);
            CallApp.getInstance().sendMessage(holoMessage);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onImageReceived(ImageMessage message) {
        ivVideoCallImageRemoteShow.setVisibility(View.VISIBLE);
        Glide.with(this).load(message.getRemoteUri()).into(ivVideoCallImageRemoteShow);
        imageHandler.sendEmptyMessageDelayed(0, 10000);

    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTextReceived(TextMessage message) {
        StringBuilder sb = new StringBuilder();
        sb.append(message.getUserInfo().getUsername().trim());
        sb.append("  ");
        SimpleDateFormat df = new SimpleDateFormat("MM月dd日  HH:mm:ss");//设置日期格式
        sb.append(df.format(new Date()));
        sb.append("\n");
        int length = sb.length();
        sb.append(message.getContent().trim());
        SpannableStringBuilder style = new SpannableStringBuilder(sb.toString());
        style.setSpan(new ForegroundColorSpan(Color.parseColor("#00BAFF")), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        style.setSpan(new RelativeSizeSpan(1.5f), length, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        style.setSpan(new ForegroundColorSpan(Color.parseColor("#FFFFFF")), length, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvMessageText.setText(style);
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
        roundedFrameLayout.setClipChildren(false);

        final BorderView borderView = new BorderView(roundedFrameLayout);
        borderView.setBackgroundResource(R.drawable.border_color);

        ViewGroup list = (ViewGroup) findViewById(R.id.rl_video_call_success_view);
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

    /**
     * 倒计时开启
     */
    public void startCountdown() {
        handler.postDelayed(countdownRunnable, 1000);
    }

    public void stopCountDown() {
        handler.removeCallbacks(countdownRunnable);
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
        Intent usbcameraIntent = new Intent(this, UVCCameraActivity.class);
        usbcameraIntent.putExtra("usbcamera", 0);
        usbcameraIntent.putExtra("launchid", "holoviewcall");
        startActivity(usbcameraIntent);
//        HoloCall.getInstance().switchCamera();
    }

    @OnClick(R.id.fl_show_preview)
    public void onPreview() {
        if (content.getVisibility() == View.VISIBLE) {
            content.setVisibility(View.INVISIBLE);
            CallApp.getInstance().getSurfaceView().setVisibility(View.GONE);
            ivShowPreviewTips.setImageResource(R.mipmap.ic_open_video_preview);
        } else {
            content.setVisibility(View.VISIBLE);
            CallApp.getInstance().getSurfaceView().setVisibility(View.VISIBLE);
            ivShowPreviewTips.setImageResource(R.mipmap.ic_close_video_preview);
        }
    }
}
