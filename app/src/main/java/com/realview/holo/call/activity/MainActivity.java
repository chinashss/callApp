package com.realview.holo.call.activity;

import android.animation.Animator;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.telecom.Call;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.blankj.utilcode.util.AppUtils;
import com.google.gson.Gson;
import com.holo.tvwidget.DrawingOrderRelativeLayout;
import com.holo.tvwidget.MetroItemFrameLayout;
import com.holo.tvwidget.MetroViewBorderHandler;
import com.holo.tvwidget.MetroViewBorderImpl;
import com.holoview.aidl.ProcessServiceIAidl;
import com.hv.calllib.CallCommon;
import com.hv.calllib.CallListener;
import com.hv.calllib.CallManager;
import com.hv.calllib.CallSession;
import com.hv.calllib.HoloCall;
import com.hv.calllib.bean.CloseMessage;
import com.hv.calllib.bean.HoloEvent;
import com.hv.imlib.HoloMessage;
import com.hv.imlib.imservice.event.CaptureImageEvent;
import com.hv.imlib.model.ConversationType;
import com.hv.imlib.model.Message;
import com.hv.imlib.model.message.ImageMessage;
import com.hv.imlib.protocol.http.NaviRes;
import com.realview.commonlibrary.server.manager.CommLib;
import com.realview.commonlibrary.server.manager.UserManager;
import com.realview.commonlibrary.server.response.UserInfoGetRes;
import com.realview.holo.call.CallApp;
import com.realview.holo.call.ImageUtil;
import com.realview.holo.call.R;
import com.realview.holo.call.basic.ActivityCollector;
import com.realview.holo.call.basic.BaseActivity;
import com.realview.holo.call.bean.AudioOrderMessage;
import com.realview.holo.call.bean.CallStateMessage;
import com.realview.holo.call.bean.Constants;
import com.realview.holo.call.service.CallBackgroundService;
import com.realview.holo.call.widget.DiscussionAvatarView;
import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.beta.UpgradeInfo;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.holo.call.bean.type.CallDisconnectedReason;
import cn.holo.call.bean.type.CallEngineType;
import cn.holo.call.bean.type.CallMediaType;

public class MainActivity extends BaseActivity {
    private static String TAG = "MainActivity";
    @BindView(R.id.iv_call_wait_phone)
    ImageView ivCallWaitPhone;
    @BindView(R.id.daview)
    DiscussionAvatarView daview;
    @BindView(R.id.iv_video_call_stop_wait)
    ImageView ivVideoCallStopWait;
    @BindView(R.id.stop)
    MetroItemFrameLayout stop;
    @BindView(R.id.rl_video_call_wait_view)
    DrawingOrderRelativeLayout rlVideoCallWaitView;

    private ProcessServiceIAidl mProcessAidl;
    private Intent mServiceIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call_waiting);
        EventBus.getDefault().register(this);
        ButterKnife.bind(this);
        initTVStatusView();
        initData();
        bindOrderService();
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
        showAvatar();

    }

    private void initData() {
        Intent intentFromApp = getIntent();

        String navi = intentFromApp.getStringExtra("navi");

        if (TextUtils.isEmpty(navi)) {
            Toast.makeText(this, "请从Launcher中启动", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        NaviRes naviRes = new Gson().fromJson(navi, NaviRes.class);
        CommLib.instance().setNaviRes(naviRes);


        long userSelfid = intentFromApp.getLongExtra("userSelfId", 0L);
        CallApp.getInstance().setUserSelfid(userSelfid);
        String action = intentFromApp.getStringExtra(Constants.CALL_LIST);
        List<Long> longs = JSON.parseArray(action, Long.class);
        CallApp.getInstance().addAllRoomUserIds(longs);
        int converstaionType = intentFromApp.getIntExtra("converstaionType", 0);
        CallApp.getInstance().setConverstaionType(converstaionType);

        long roomId = intentFromApp.getLongExtra("roomId", 0L);
        CallApp.getInstance().setRoomId(roomId);





        HoloCall.routeUrl = intentFromApp.getStringExtra("wss");


    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initData();
        CallApp.getInstance().start();
        showAvatar();
    }

    private void initTVStatusView() {
        FrameLayout roundedFrameLayout = new FrameLayout(this);

        final MetroViewBorderImpl metroViewBorderImpl = new MetroViewBorderImpl(roundedFrameLayout);
        metroViewBorderImpl.setBackgroundResource(R.drawable.border_color);

        ViewGroup list = findViewById(R.id.rl_video_call_wait_view);
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


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAudioOrderMessage(AudioOrderMessage message) {
        if (message.getType() == 115) {
            //挂断
            finishActvity();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        unbindService(conn);
        stopRing();
        Log.d(TAG, "finishActvity");
        if (mServiceIntent != null) {
            stopService(mServiceIntent);
        }
    }


    public void showAvatar() {
        for (int i = 0; i < CallApp.getInstance().getRoomUserIds().size(); i++) {
            if (CallApp.getInstance().getRoomUserIds().get(i) <= 0) continue;
            ConversationType type = ConversationType.setValue(CallApp.getInstance().getConverstaionType());
            UserManager.instance().getUserInfo(ConversationType.P2P == type ? CallApp.getInstance().getRoomUserIds().get(i) : CallApp.getInstance().getRoomId(), new UserManager.ResultCallback<UserInfoGetRes.ResultBean>() {
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


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCallMessage(HoloMessage message) {
        try {
            String jsonString = JSON.toJSONString(message, SerializerFeature.WriteMapNullValue);
            Log.i("TAG", jsonString);
            if (mProcessAidl != null) {
                mProcessAidl.sendMessage(jsonString);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    ServiceConnection conn = new ServiceConnection() {//这个最重要，用于连接Service
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i("MainActivity.conn", "@@ onServiceConnected name=" + name);
            ProcessServiceIAidl aidl = ProcessServiceIAidl.Stub.asInterface(service);
            mProcessAidl = aidl;
            try {
                mProcessAidl.initCallMessage();
                mProcessAidl.onBindSuccess("com.realview.holo.call", "com.realview.holo.call.service.CallBackgroundService");
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            try {
                service.linkToDeath(deathRecipient, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i("MainActivity.conn", "@@ onServiceDisconnected name=" + name);
        }
    };
    //注册一个死亡代理，监测连接状态
    IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            Log.i("MainActivity", "@@ binderDied " + (mProcessAidl == null));
            if (mProcessAidl == null) {
                return;
            }
            mProcessAidl.asBinder().unlinkToDeath(deathRecipient, 0);
            mProcessAidl = null;
            //重新绑定
            bindOrderService();
        }
    };

    private void bindOrderService() {
        Intent intent = new Intent("com.holoview.hololauncher.BackgroundService");
        intent.setPackage("com.holoview.hololauncher");
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishActvity();
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
                ActivityCollector.closeActivity(SuccessActivity.class);
//        System.exit(0);
//        if (startWaitTo) {
//            Intent intent = new Intent(this, CallNoReplyActivity.class);
//            intent.putExtra(Constants.CALL_LIST, action);
//            intent.putExtra("userSelfId", userSelfid);
//            intent.putExtra("converstaionType", converstaionType);
//            intent.putExtra("roomId", roomId);
//            startActivityForResult(intent, Constants.CALL_DIS_CONN);
//        }
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
    }


    boolean startWaitTo = true;

    public void finishActvity() {
        //主动关闭 并且要让launcher退出登录
        HoloMessage message = new HoloMessage();
        message.setAction("api.audio.unsubscribe");
        EventBus.getDefault().post(message);
        if (CallApp.getInstance().getSession() != null) {
            startWaitTo = false;
            HoloCall.getInstance().hangUpCall(CallApp.getInstance().getSession().getCallId());
        }
        this.finish();
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
            Message e = Message.obtain(Long.valueOf(CallManager.getInstance().getCallSessionImp().getTargetId()), CallManager.getInstance().getCallSessionImp().getConversationType(), imageMessage);
            HoloMessage holoMessage = new HoloMessage();
            holoMessage.setAction("ImageMessage");
            holoMessage.setMessage(e);
            String json = JSON.toJSONString(holoMessage);
            mProcessAidl.sendMessage(json);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    @OnClick(R.id.stop)
    public void onViewClicked() {
        finishActvity();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCloseMessage(CloseMessage message) {
        finishActvity();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.CALL_DIS_CONN) {
            if (resultCode == Activity.RESULT_OK) {
//                List<Long> longs = JSON.parseArray(action, Long.class);
//                onCall(longs, userSelfid);
            } else if (resultCode == -100) {
                //主动关闭 并且要让launcher退出登录
                finishActvity();
            }
        }
    }
}
