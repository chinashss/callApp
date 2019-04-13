package com.realview.holo.call.activity;

import android.animation.Animator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.alibaba.fastjson.JSON;
import com.holo.tvwidget.DrawingOrderRelativeLayout;
import com.holo.tvwidget.MetroItemFrameLayout;
import com.holo.tvwidget.MetroViewBorderHandler;
import com.holo.tvwidget.MetroViewBorderImpl;
import com.hv.imlib.model.ConversationType;
import com.realview.commonlibrary.server.manager.UserManager;
import com.realview.commonlibrary.server.response.UserInfoGetRes;
import com.realview.holo.call.R;
import com.realview.holo.call.basic.BaseActivity;
import com.realview.holo.call.bean.AudioOrderMessage;
import com.realview.holo.call.bean.Constants;
import com.realview.holo.call.widget.DiscussionAvatarView;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CallNoReplyActivity extends BaseActivity {
    @BindView(R.id.iv_call_reply_phone)
    ImageView ivCallReplyPhone;
    @BindView(R.id.reply_daview)
    DiscussionAvatarView replyDaview;
    @BindView(R.id.iv_video_call_reply)
    ImageView ivVideoCallReply;
    @BindView(R.id.fl_reply_call)
    MetroItemFrameLayout flReplyCall;
    @BindView(R.id.iv_video_reply_quit)
    ImageView ivVideoReplyQuit;
    @BindView(R.id.fl_reply_quit)
    MetroItemFrameLayout flReplyQuit;
    @BindView(R.id.rl_video_reply_view)
    DrawingOrderRelativeLayout rlVideoReplyView;


    private int converstaionType = 0;
    private long roomId = 0L;
    private long userSelfid = 0L;
    private String action;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_reply);
        ButterKnife.bind(this);
        initTVStatusView();


        Intent intentFromApp = getIntent();
        action = intentFromApp.getStringExtra(Constants.CALL_LIST);
        userSelfid = intentFromApp.getLongExtra("userSelfId", 0L);
        converstaionType = intentFromApp.getIntExtra("converstaionType", 0);
        roomId = intentFromApp.getLongExtra("roomId", 0L);

        List<Long> longs = JSON.parseArray(action, Long.class);
        showAvatar(longs);
    }

    private void showAvatar(List<Long> longs) {
        for (int i = 0; i < longs.size(); i++) {
            if (longs.get(i) <= 0) continue;
            ConversationType type = ConversationType.setValue(converstaionType);
            UserManager.instance().getUserInfo(ConversationType.P2P == type ? longs.get(i) : roomId, new UserManager.ResultCallback<UserInfoGetRes.ResultBean>() {
                @Override
                public void onSuccess(final UserInfoGetRes.ResultBean resultBean) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                                replyDaview.addData(resultBean.getPortrait(), (TextUtils.isEmpty(resultBean.getNickname()) ? resultBean.getUsername() : resultBean.getNickname()));

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

    @OnClick({R.id.fl_reply_call, R.id.fl_reply_quit})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.fl_reply_call:
                setResult(Activity.RESULT_OK);
                finish();
                break;
            case R.id.fl_reply_quit:
                setResult(-100);
                finish();
                break;
        }
    }


    private void initTVStatusView() {
        FrameLayout roundedFrameLayout = new FrameLayout(this);

        final MetroViewBorderImpl metroViewBorderImpl = new MetroViewBorderImpl(roundedFrameLayout);
        metroViewBorderImpl.setBackgroundResource(R.drawable.border_color);

        ViewGroup list = findViewById(R.id.rl_video_reply_view);
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
}
