package com.realview.holo.call.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by admin on 2019/1/14.
 */

public class BoardImageView extends ImageView {
    private int color =Color.BLACK;
    public BoardImageView(Context context) {
        super(context);
    }

    public BoardImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BoardImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //获取控件需要重新绘制的区域
        Rect rect=canvas.getClipBounds();
        rect.bottom--;
        rect.right--;
        Paint paint=new Paint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6);
        canvas.drawRect(rect, paint);
    }

    public void setColor(int color) {
        this.color = color;
        invalidate();
    }
}
