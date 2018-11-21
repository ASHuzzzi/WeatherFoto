package com.ashu.whetherfoto;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.view.Surface;

public class BitmapRotate {

    public Bitmap rotate(Bitmap bitmap, int orientation, int cameraId) {
        Matrix mtx = new Matrix();

        switch (orientation) {
            case Surface.ROTATION_0:
                //вертикальная ориентация, обычная
                if (cameraId == 1){
                    mtx.setRotate(-90);
                    mtx.preScale(1,-1); //делеам зеркальное отображение
                }else {
                    mtx.setRotate(90);
                }

                break;
            case Surface.ROTATION_90:

                if (cameraId == 1){
                    mtx.preScale(-1,1); //делеам зеркальное отображение
                }
                break;
            case Surface.ROTATION_180:
                //вертикальная, верх ногами, если поддерживается телефоном
                if (cameraId == 1){
                    mtx.setRotate(90);
                    mtx.preScale(-1,1); //делеам зеркальное отображение
                }else {
                    mtx.setRotate(-90);
                }

                break;
            case Surface.ROTATION_270:
                //горизонтальная, камера справа
                if (cameraId == 1){
                    mtx.setRotate(-180);
                    mtx.preScale(-1,1); //делеам зеркальное отображение
                }else {
                    mtx.setRotate(180);
                }
                break;
        }

        try {
            return Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mtx, true);
        }
        catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }
}
