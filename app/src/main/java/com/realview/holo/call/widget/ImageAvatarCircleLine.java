package com.realview.holo.call.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

/*** TODO: document your custom view class.*/
public class ImageAvatarCircleLine extends View {
    //大圆属性
    private boolean mGradientAble = false;//颜色渐变
    private int mBigCircleRadius=0;//大圆半径
    private int mBigCircleColor;//大圆颜色
    private boolean mRotationFunction = false;//是否显示小圈转动
    private boolean mBigHollow = false;//是否空心
    private int mPaintSize = 4;//大圆画笔大小

    // 小圆属性
    private int mSmallCircleColor;//小圆颜色
    private int mSmallCircleRadius;//小圆半径
    private boolean mSmallHollow = false;//是否空心
    private int mSmallPaintSize = 2;//小圆画笔大小
    private Paint mSmallCirclePaint;//小圆画笔
    private Paint mBigCirclePaint;//大圆画笔
    private int mBigCircleX, mBigCircleY, mSmallCircleX, mSmallCircleY;
    private boolean mRunSmallBallLogic = true;
    private int mRunDegree = 0;
    private int mTimeSleep = 10;
    private int mRunDegreeAdd = 2;
    private final int UPDATE_VIEW_MSG = 1;
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void dispatchMessage(Message msg) {
            super.dispatchMessage(msg);
            if (msg.what == UPDATE_VIEW_MSG) {
                calculatCenterPoint();
            }
        }
    };

    /*** 计算圆点中心*/
    private void calculatCenterPoint() {
        double cos = Math.cos(mRunDegree * Math.PI / 180);
        double sin = Math.sin(mRunDegree * Math.PI / 180);
        mSmallCircleX = (int) (mBigCircleX + mBigCircleRadius * cos);
        mSmallCircleY = (int) (mBigCircleY - mBigCircleRadius * sin);
        mRunDegree += mRunDegreeAdd;
        invalidate();
        mHandler.sendEmptyMessageDelayed(UPDATE_VIEW_MSG, mTimeSleep);
    }

    public ImageAvatarCircleLine(Context context) {
        super(context);
        init(null, 0);
    }

    public ImageAvatarCircleLine(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImageAvatarCircleLine(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributesfinal
        mBigCircleColor = Color.parseColor("#003d66");
        mBigHollow = true;//是否空心
        mPaintSize = 2;//大圆画笔大小

        // 小圆属性
        mSmallCircleColor = Color.WHITE;// 小圆颜色
        mSmallCircleRadius = 4;
        mSmallHollow = false;
        mSmallPaintSize = 2;
        mSmallCirclePaint = new Paint();
        mBigCirclePaint = new Paint();
        setPaint();
    }

    private void logicSmallBall() {
        mRunSmallBallLogic = true;
        new Thread() {
            @Override
            public void run() {
                super.run();
                while (mRunSmallBallLogic) {
                    mSmallCircleX = (int) (mBigCircleX + (mBigCircleRadius + mPaintSize / 2f) * Math.cos(mRunDegree));
                    mSmallCircleY = (int) (mBigCircleY - (mBigCircleRadius + mPaintSize / 2f) * Math.sin(mRunDegree));
                    mRunDegree++;
                    try {
                        Thread.sleep(mTimeSleep);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mHandler.sendEmptyMessage(1);
                }
            }
        }.start();
        mHandler.removeMessages(1);
        mHandler.sendEmptyMessageDelayed(1, mTimeSleep);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    //设置画笔
    public void setPaint() {
        mSmallCirclePaint.setAntiAlias(true);//去锯齿
        mSmallCirclePaint.setColor(mSmallCircleColor);
        mSmallCirclePaint.setStrokeWidth(mSmallPaintSize);
        mBigCirclePaint.setAntiAlias(true);
        mBigCirclePaint.setColor(mBigCircleColor);
        mBigCirclePaint.setStrokeWidth(mPaintSize);
        if (mBigHollow) {
            mBigCirclePaint.setStyle(Paint.Style.STROKE);
        } else {
            mBigCirclePaint.setStyle(Paint.Style.FILL);
        }
        if (mSmallHollow) {
            mSmallCirclePaint.setStyle(Paint.Style.STROKE);
        } else {
            mSmallCirclePaint.setStyle(Paint.Style.FILL);
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.restore();
        canvas.drawCircle(mBigCircleX, mBigCircleY, mBigCircleRadius, mBigCirclePaint);
        if (mRotationFunction) {
            canvas.drawCircle(mSmallCircleX, mSmallCircleY, mSmallCircleRadius, mSmallCirclePaint);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            mBigCircleX = this.getWidth() / 2;
            mBigCircleY = this.getHeight() / 2;
            if (mBigCircleRadius == 0) {
                mBigCircleRadius = this.getWidth() / 2 - 2 * mPaintSize;
            }
            if (mRotationFunction) {//显示旋转小球
                mHandler.removeMessages(UPDATE_VIEW_MSG);
                calculatCenterPoint();
            }
            invalidate();
        }
    }

    public void setmRunDegreeAdd(int mRunDegreeAdd) {
        this.mRunDegreeAdd = mRunDegreeAdd;
    }

    public void setmTimeSleep(int mTimeSleep) {
        this.mTimeSleep = mTimeSleep;
    }


    public void setmGradientAble(boolean mGradientAble) {
        this.mGradientAble = mGradientAble;
    }

    public void setmBigCircleRadius(int mBigCircleRadius) {
        this.mBigCircleRadius = mBigCircleRadius;
    }

    public void setmBigCircleColor(int mBigCircleColor) {
        this.mBigCircleColor = mBigCircleColor;
        mBigCirclePaint.setColor(mBigCircleColor);
    }

    public void setmRotationFunction(boolean mRotationFunction) {
        this.mRotationFunction = mRotationFunction;
        invalidate();
    }

    public void setmBigHollow(boolean mBigHollow) {
        this.mBigHollow = mBigHollow;
    }

    public void setmPaintSize(int mPaintSize) {
        this.mPaintSize = mPaintSize;
        mBigCirclePaint.setStrokeWidth(mPaintSize);
    }

    public void setmSmallCircleColor(int mSmallCircleColor) {
        this.mSmallCircleColor = mSmallCircleColor;
        mSmallCirclePaint.setColor(mSmallCircleColor);
    }

    public void setmSmallCircleRadius(int mSmallCircleRadius) {
        this.mSmallCircleRadius = mSmallCircleRadius;
    }

    public void setmSmallHollow(boolean mSmallHollow) {
        this.mSmallHollow = mSmallHollow;
    }

    public void setmSmallPaintSize(int mSmallPaintSize) {
        this.mSmallPaintSize = mSmallPaintSize;
        mSmallCirclePaint.setStrokeWidth(mSmallPaintSize);
    }

}


