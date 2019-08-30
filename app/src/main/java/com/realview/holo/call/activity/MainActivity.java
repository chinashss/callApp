package com.realview.holo.call.activity;

import android.animation.Animator;
import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.blankj.utilcode.util.AppUtils;
import com.google.gson.Gson;
import com.hv.calllib.HoloCall;
import com.hv.imlib.HoloMessage;
import com.hv.imlib.protocol.http.NaviRes;
import com.realview.commonlibrary.server.manager.CommLib;
import com.realview.holo.call.CallApp;
import com.realview.holo.call.R;
import com.realview.holo.call.basic.ActivityCollector;
import com.realview.holo.call.basic.BaseActivity;
import com.realview.holo.call.bean.CallStateMessage;
import com.realview.holo.call.bean.Constants;
import com.realview.holo.call.service.CallBackgroundService;

import org.evilbinary.tv.widget.BorderEffect;
import org.evilbinary.tv.widget.BorderView;
import org.evilbinary.tv.widget.RoundedFrameLayout;
import org.evilbinary.tv.widget.TvZorderRelativeLayout;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends BaseActivity {
    private static String TAG = "MainActivity";
    @BindView(R.id.iv_call_wait_phone)
    ImageView ivCallWaitPhone;
    @BindView(R.id.iv_video_call_stop_wait)
    ImageView ivVideoCallStopWait;
    @BindView(R.id.stop)
    RoundedFrameLayout stop;
    @BindView(R.id.rl_video_call_wait_view)
    TvZorderRelativeLayout rlVideoCallWaitView;
    @BindView(R.id.tv_app_version)
    TextView tvAppVersion;


    private Intent mServiceIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call_waiting);
        EventBus.getDefault().register(this);
        ButterKnife.bind(this);
        initTVStatusView();
        initData(getIntent());
        CallApp.getInstance().bindOrderService();
        String appVerisonName = AppUtils.getAppVersionName();
        if (appVerisonName != null) {
            tvAppVersion.setText(appVerisonName);
        }
        mServiceIntent = new Intent(this, CallBackgroundService.class);
        startService(mServiceIntent);
        CallApp.getInstance().initListener(this);
        if (CommLib.instance().getNaviRes() == null) {
            Toast.makeText(this, "请从Launcher中启动", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
//        intentFromApp.getExtras().clear();


        CallApp.getInstance().start();

    }



    private void initData(Intent intent) {

        String navi = intent.getStringExtra("navi");

        if (TextUtils.isEmpty(navi)) {
            Toast.makeText(this, "请从Launcher中启动", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        NaviRes naviRes = new Gson().fromJson(navi, NaviRes.class);
        CommLib.instance().setNaviRes(naviRes);


        long userSelfid = intent.getLongExtra("userSelfId", 0L);
        CallApp.getInstance().setUserSelfid(userSelfid);
        String action = intent.getStringExtra(Constants.CALL_LIST);

        List<Long> longs = JSON.parseArray(action, Long.class);
        CallApp.getInstance().addAllRoomUserIds(longs);
        int converstaionType = intent.getIntExtra("converstaionType", 0);
        CallApp.getInstance().setConverstaionType(converstaionType);

        long roomId = intent.getLongExtra("roomId", 0L);
        CallApp.getInstance().setRoomId(roomId);

        HoloCall.routeUrl = intent.getStringExtra("wss");


    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initData(intent);
        CallApp.getInstance().start();
    }

    private void initTVStatusView() {
        FrameLayout roundedFrameLayout = new FrameLayout(this);
        roundedFrameLayout.setClipChildren(false);

        final BorderView borderView = new BorderView(roundedFrameLayout);
        borderView.setBackgroundResource(R.drawable.border_color);

        ViewGroup list = (ViewGroup) findViewById(R.id.rl_video_call_wait_view);
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


    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        stopRing();
        Log.d(TAG, "finishActvity");
        if (mServiceIntent != null) {
            stopService(mServiceIntent);
        }
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        stopCall();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCallMessage(CallStateMessage message) {
        switch (message) {
            case CallOutgoing:
                startWaiting();
                break;
            case CallConnected:
                connVideoCallSuccess();
                break;
            case CallDisconnected:
                Toast.makeText(MainActivity.this, "视频连接已断开", Toast.LENGTH_SHORT).show();
                if (ActivityCollector.isActivityTop(MainActivity.class, this)) {
                    stopCall();
                } else {
                    finish();
                }
                break;
            case RemoteUserJoined:
                Toast.makeText(MainActivity.this, "对方已经接听", Toast.LENGTH_SHORT).show();
                break;
            case MediaTypeChanged:
                Toast.makeText(MainActivity.this, "对方已经切换为音频聊天", Toast.LENGTH_SHORT).show();
                break;
        }
    }


    private void startWaiting() {
        onOutgoingCallRinging();
    }

    private MediaPlayer mMediaPlayer;


    private void onOutgoingCallRinging() {
        mMediaPlayer = MediaPlayer.create(this, R.raw.voip_outgoing_ring);
        mMediaPlayer.setLooping(true);
        mMediaPlayer.start();
    }

    public void stopRing() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer = null;
        }
    }


    private void connVideoCallSuccess() {
        stopRing();
        Intent intent = new Intent(this, SuccessActivity.class);
        intent.putExtra(Constants.ACTION_CALL_SUCCESS, true);
        startActivity(intent);
        finish();
    }


    public void stopCall() {
//        HoloMessage message = new HoloMessage();
//        message.setAction("api.audio.unsubscribe");
//        CallApp.getInstance().sendMessage(message);
        if (CallApp.getInstance().getSession() != null) {
            HoloCall.getInstance().hangUpCall(CallApp.getInstance().getSession().getCallId());
        }
        finish();
    }


    @OnClick(R.id.stop)
    public void onViewClicked() {
        stopCall();
    }

}
