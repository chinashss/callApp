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
import com.hv.calllib.bean.HoloEvent;
import com.hv.imlib.HoloMessage;
import com.hv.imlib.imservice.event.CaptureImageEvent;
import com.hv.imlib.model.ConversationType;
import com.hv.imlib.model.Message;
import com.hv.imlib.model.message.ImageMessage;
import com.realview.commonlibrary.server.manager.UserManager;
import com.realview.commonlibrary.server.response.UserInfoGetRes;
import com.realview.holo.call.ImageUtil;
import com.realview.holo.call.R;
import com.realview.holo.call.basic.ActivityCollector;
import com.realview.holo.call.basic.BaseActivity;
import com.realview.holo.call.bean.AudioOrderMessage;
import com.realview.holo.call.bean.CloseMessage;
import com.realview.holo.call.bean.Constants;
import com.realview.holo.call.service.CallBackgroundService;
import com.realview.holo.call.widget.DiscussionAvatarView;

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

public class MainActivity extends BaseActivity implements CallListener {
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

    private CallSession mSession;


    private int converstaionType = 0;
    private long roomId = 0L;
    private long userSelfid = 0L;
    private String action;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call_waiting);
        EventBus.getDefault().register(this);
        ButterKnife.bind(this);
        initTVStatusView();


        Intent intentFromApp = getIntent();
        action = intentFromApp.getStringExtra(Constants.CALL_LIST);
        userSelfid = intentFromApp.getLongExtra("userSelfId", 0L);
        converstaionType = intentFromApp.getIntExtra("converstaionType", 0);
        roomId = intentFromApp.getLongExtra("roomId", 0L);
        HoloCall.routeUrl = intentFromApp.getStringExtra("wss");

        bindOrderService();
        mServiceIntent = new Intent(this, CallBackgroundService.class);
        startService(mServiceIntent);
        HoloCall holoCall = HoloCall.getInstance();
        holoCall.init(this);
        holoCall.setVoIPCallListener(this);
        holoCall.setEnableAllRemoteVideo(false);

        if (TextUtils.isEmpty(action)) {
            Toast.makeText(this, "请从Launcher中启动", Toast.LENGTH_SHORT).show();
            finishActvity();
            return;
        }


        List<Long> longs = JSON.parseArray(action, Long.class);
        onCall(longs, userSelfid);
        showAvatar(longs);


    }

//    @Override
//    protected void onNewIntent(Intent intent) {
//        super.onNewIntent(intent);
//        action = intent.getStringExtra(Constants.CALL_LIST);
//        userSelfid = intent.getLongExtra("userSelfId", 0L);
//        converstaionType = intent.getIntExtra("converstaionType", 0);
//        roomId = intent.getLongExtra("roomId", 0L);
//        HoloCall.routeUrl = intent.getStringExtra("wss");
//
//        List<Long> longs = JSON.parseArray(action, Long.class);
//        onCall(longs, userSelfid);
//        showAvatar(longs);
//    }

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


    public void showAvatar(List<Long> longs) {
        if (longs == null) {
            return;
        }
        for (int i = 0; i < longs.size(); i++) {
            if (longs.get(i) <= 0) continue;
            ConversationType type = ConversationType.setValue(converstaionType);
            UserManager.instance().getUserInfo(ConversationType.P2P == type ? longs.get(i) : roomId, new UserManager.ResultCallback<UserInfoGetRes.ResultBean>() {
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

    public void onCall(final List<Long> longs, final long userSelfid) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ConversationType type = ConversationType.setValue(converstaionType);
                CallManager.getInstance().setUserSelfId(userSelfid);

                HoloCall.getInstance().startCall(type, ConversationType.P2P == type ? longs.get(0) : roomId, longs, CallMediaType.VIDEO,
                        CallEngineType.ENGINE_TYPE_NORMAL, null,
                        103, userSelfid);
            }
        }).start();
    }

    @Override
    public void onCallOutgoing(CallSession callSession, SurfaceView surfaceView) {
        this.mSession = callSession;
        startWaiting();
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


    @Override
    public void onCallConnected(CallSession callSession, SurfaceView surfaceView) {
        this.mSession = callSession;
        Log.i("lipengfei", "conn success");
        connVideoCallSuccess();
//        rlVideoCallWaitView.addView(surfaceView);
    }

    private void connVideoCallSuccess() {
        stopRing();
        Intent intent = new Intent(this, SuccessActivity.class);
        intent.putExtra(Constants.ACTION_TASK_ID, roomId);
        intent.putExtra(Constants.ACTION_CALL_SUCCESS, true);
        intent.putExtra(Constants.CALL_LIST, action);
        intent.putExtra(Constants.ACTION_CONVERSTAION_TYPE, converstaionType);
        intent.putExtra(Constants.ACTION_ROOM_ID, roomId);
        startActivity(intent);
    }


    boolean startWaitTo = true;

    public void finishActvity() {
        //主动关闭 并且要让launcher退出登录
        HoloMessage message = new HoloMessage();
        message.setAction("api.audio.unsubscribe");
        EventBus.getDefault().post(message);
        if (mSession != null) {
            startWaitTo = false;
            HoloCall.getInstance().hangUpCall(mSession.getCallId());
        }
        this.finish();
    }

    @Override
    public void onCallDisconnected(CallSession callSession, CallDisconnectedReason callDisconnectedReason) {
        Toast.makeText(MainActivity.this, "视频连接已断开", Toast.LENGTH_SHORT).show();
        ActivityCollector.closeActivity(SuccessActivity.class);
        if (startWaitTo){
            Intent intent = new Intent(this, CallNoReplyActivity.class);
            intent.putExtra(Constants.CALL_LIST, action);
            intent.putExtra("userSelfId", userSelfid);
            intent.putExtra("converstaionType", converstaionType);
            intent.putExtra("roomId", roomId);
            startActivityForResult(intent, Constants.CALL_DIS_CONN);
        }
    }

    @Override
    public void onRemoteUserRinging(long l) {
        String s1 = "";
    }

    @Override
    public void onRemoteUserJoined(long l, CallMediaType callMediaType, SurfaceView surfaceView) {
        Toast.makeText(MainActivity.this, "对方已经接听", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRemoteUserInvited(long l, CallMediaType callMediaType) {
        String s1 = "";
    }

    @Override
    public void onRemoteUserLeft(long l, CallDisconnectedReason callDisconnectedReason) {
        String s1 = "";
    }

    @Override
    public void onMediaTypeChanged(long l, CallMediaType callMediaType, SurfaceView surfaceView) {
        Toast.makeText(MainActivity.this, "对方已经切换为音频聊天", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onError(CallCommon.CallErrorCode callErrorCode) {
        String s1 = "";
    }

    @Override
    public void onRemoteCameraDisabled(long l, boolean b) {
        String s1 = "";
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
                List<Long> longs = JSON.parseArray(action, Long.class);
                onCall(longs, userSelfid);
            } else if (resultCode == -100) {
                //主动关闭 并且要让launcher退出登录
                finishActvity();
            }
        }
    }
}
