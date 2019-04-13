package com.realview.holo.call.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageButton;

import com.realview.holo.call.R;


/**
 * Created by zhenhuiyang on 2017/5/3:下午1:47.
 * Holoview studio.
 */

@SuppressLint("AppCompatCustomView")
public class MicImageButton extends ImageButton {

    private boolean micShow = false;

    private Bitmap bitmap;

    public MicImageButton(Context context) {
        super(context, null);
    }

    public MicImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.microphone_small);

    }
    public MicImageButton(Context context, AttributeSet attrs, int RId) {
        super(context, attrs);
        bitmap = BitmapFactory.decodeResource(getResources(), RId);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub

        if (micShow) {
            // 图片顶部居中显示
            int destLeft = ( this.getWidth() - 150 ) / 2;
            int destTop =  ( this.getHeight() - 150 ) / 2;
            int destRight = this.getWidth() - destLeft;
            int destBottom = this.getHeight() - destTop;

           // canvas.drawBitmap(bitmap, x, y, null);
            canvas.drawBitmap(bitmap,new Rect(0,0,bitmap.getWidth(),bitmap.getHeight()),
                    new Rect(destLeft, destTop, destRight, destBottom), null);
        }
        super.onDraw(canvas);
    }

    public void setMicShow(boolean micShow){
        this.micShow = micShow;
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }
        System.gc();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

    }
}
