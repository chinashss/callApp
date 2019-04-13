/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.realview.holo.call;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by zhenhuiyang on 2017/5/8:上午11:32.
 * Holoview studio.
 */

public class ImageUtil {

    public static String getCachePath( Context context ){
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }

        return "";
    }



    public static class ImageEntity{
        public String sourcePath;
        public String thumbPath;
    }
    public static ImageEntity getImageEntity(Context context, Uri uri) throws IOException {

        if (uri == null || uri.getPath().equals("")){
            return null;
        }
        // String inputFile = UriUtils.getRealPathFromUri(context, uri);
        String inputFile = uri.toString();

        ImageEntity imageEntity = new ImageEntity();

        File imageFileInput = new File(inputFile);

        String fileName=imageFileInput.getName();
        String prefix=fileName.substring(0,fileName.lastIndexOf("."));
        String type = fileName.substring(fileName.lastIndexOf("."));

        String srcPath = getCachePath(context) + "/" + prefix  + type;
        String thumbPath = getCachePath(context) + "/" + prefix + "_thumb" + type;

        //发送图片消息
        File imageFileSource = new File(srcPath);
        File imageFileThumb = new File(thumbPath);

        // 读取图片。
        Bitmap bmpSource = getBitmapFormUri(context, uri);//BitmapFactory.decodeFile(imageFileInput.getAbsolutePath());

        imageFileSource.createNewFile();

        FileOutputStream fosSource = new FileOutputStream(imageFileSource);

        // 保存原图。
        bmpSource.compress(Bitmap.CompressFormat.JPEG, 100, fosSource);

        int destWidth = 240;
        int destHeigh = 400;

        if (destWidth > bmpSource.getWidth()){
            destWidth = bmpSource.getWidth();
        }

        if (destHeigh > bmpSource.getHeight()){
            destHeigh = bmpSource.getHeight();
        }
        // 创建缩略图变换矩阵。
        Matrix m = new Matrix();
        m.setRectToRect(new RectF(0, 0, bmpSource.getWidth(), bmpSource.getHeight()), new RectF(0, 0, destWidth, destHeigh), Matrix.ScaleToFit.CENTER);

        // 生成缩略图。
        Bitmap bmpThumb = Bitmap.createBitmap(bmpSource, 0, 0, bmpSource.getWidth(), bmpSource.getHeight(), m, true);

        imageFileThumb.createNewFile();

        FileOutputStream fosThumb = new FileOutputStream(imageFileThumb);

        // 保存缩略图。
        bmpThumb.compress(Bitmap.CompressFormat.JPEG, 60, fosThumb);


        imageEntity.sourcePath = srcPath;
        imageEntity.thumbPath = thumbPath;

        bmpSource.recycle();
        bmpThumb.recycle();

        return imageEntity;

    }


    public static Bitmap getBitmapFormUri(Context ac, Uri uri) throws FileNotFoundException, IOException {
        InputStream input = ac.getContentResolver().openInputStream(uri);
        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither = true;//optional
        onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();
        int originalWidth = onlyBoundsOptions.outWidth;
        int originalHeight = onlyBoundsOptions.outHeight;
        if ((originalWidth == -1) || (originalHeight == -1))
            return null;
        //图片分辨率以480x800为标准
        float hh = 800f;//这里设置高度为800f
        float ww = 480f;//这里设置宽度为480f
        //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;//be=1表示不缩放
        if (originalWidth > originalHeight && originalWidth > ww) {//如果宽度大的话根据宽度固定大小缩放
            be = (int) (originalWidth / ww);
        } else if (originalWidth < originalHeight && originalHeight > hh) {//如果高度高的话根据宽度固定大小缩放
            be = (int) (originalHeight / hh);
        }
        if (be <= 0)
            be = 1;
        //比例压缩
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = be;//设置缩放比例
        bitmapOptions.inDither = true;//optional
        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
        input = ac.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        input.close();

        return bitmap;//再进行质量压缩
    }
}
