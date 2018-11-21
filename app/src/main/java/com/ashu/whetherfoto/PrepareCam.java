package com.ashu.whetherfoto;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.Camera;
import android.view.Display;
import android.view.Surface;

public class PrepareCam {

    // размеры экрана в первом аргументе
    // размеры превью камеры во втором
    public RectF setPreviewSize(Display display, Camera.Size size) {

        boolean widthIsMax = display.getWidth() > display.getHeight();

        RectF rectDisplay = new RectF();
        RectF rectPreview = new RectF();

        // RectF экрана, соотвествует размерам экрана
        rectDisplay.set(0, 0, display.getWidth(), display.getHeight());

        // RectF первью
        if (widthIsMax) {
            // превью в горизонтальной ориентации
            rectPreview.set(0, 0, size.width, size.height);
        } else {
            // превью в вертикальной ориентации
            rectPreview.set(0, 0, size.height, size.width);
        }

        Matrix matrix = new Matrix();
        // подготовка матрицы преобразования
        //экран будет "втиснут" в превью
        matrix.setRectToRect(rectDisplay, rectPreview,
                Matrix.ScaleToFit.START);
        matrix.invert(matrix);

        // преобразование
        matrix.mapRect(rectPreview);

        return rectPreview;
    }

    //во втором аргументе - насколько повернут экран от нормального положения
    public Integer setCameraDisplayOrientation(int cameraId, int rotation) {

        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result = 0;

        // получаем инфо по камере cameraId
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        // задняя камера
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            result = ((360 - degrees) + info.orientation);
        } else
            // передняя камера
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = ((360 - degrees) - info.orientation);
                result += 360;
            }
        result = result % 360;
        return result;
    }
}
