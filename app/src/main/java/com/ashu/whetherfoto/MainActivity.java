package com.ashu.whetherfoto;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity{

    private SurfaceView surfaceView;
    private Camera camera;
    private File photoFile;

    private int iCameraId;

    public static final int iNumberOfRequest = 23401;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //восстанавливаем состояние при повороте
        if(savedInstanceState != null){
            iCameraId = savedInstanceState.getInt("camNum", 0);
        }else{
            iCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.surfaceView);

        final SurfaceHolder holder = surfaceView.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    camera.setPreviewDisplay(holder);
                    camera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                camera.stopPreview();
                setCameraDisplayOrientation(iCameraId);
                try {
                    camera.setPreviewDisplay(holder);
                    camera.startPreview();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

        ImageButton ibtnTakePicture = findViewById(R.id.ibtnTakePicture);
        ibtnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                camera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {

                        //берем время в миллисекундах
                        String timeStamp = String.valueOf(
                                TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));

                        //Получаем имя файла из времени и расширения
                        String stFotoCounter = timeStamp + ".jpg";

                        // получаем путь к папке во внутренней памяти
                        File sdPath = Environment
                                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                        // добавляем свой каталог к пути
                        sdPath = new File(sdPath.getAbsolutePath() + "/" + "WhetherFoto");
                        // создаем каталог
                        if (!sdPath.exists()){
                            boolean isDirectoryCreated = sdPath.mkdir();
                            //если не получилось, то пишем в каталог по умолчанию
                            if (!isDirectoryCreated) {
                                sdPath = Environment
                                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                            }
                        }

                        photoFile = new File(sdPath, stFotoCounter);


                        try {
                            FileOutputStream fos = new FileOutputStream(photoFile);

                            Bitmap realImage = BitmapFactory.decodeByteArray(data, 0, data.length);

                            //берем угол поворота телефона
                            int rotation = getWindowManager().getDefaultDisplay().getRotation();

                            //поворачиваем изобажение
                            realImage = rotate(realImage, rotation, iCameraId);
                            if (realImage != null) {
                                realImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                            }
                            fos.close();

                            //обновляем экран после снимка
                            try {
                                camera.setPreviewDisplay(holder);
                                camera.startPreview();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        ImageButton ibtnChangeCam = findViewById(R.id.ibtnChangeCam);
        ibtnChangeCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //прекращаем показ и освобождаем камеру
                camera.stopPreview();
                camera.release();

                //меняем камеру
                if (iCameraId ==  Camera.CameraInfo.CAMERA_FACING_BACK){
                    iCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
                }else {
                    iCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                }

                //возобновляем показ
                camera = Camera.open(iCameraId);
                setCameraDisplayOrientation(iCameraId);
                try {
                    camera.setPreviewDisplay(holder);
                    camera.startPreview();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        //Разрешения на чтение/запись для андроида старше версии М.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int canRead = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            int canWrite = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int canCam = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);


            if (canRead != PackageManager.PERMISSION_GRANTED || canWrite != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE}, iNumberOfRequest);
            }

            if (canCam != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.CAMERA}, iNumberOfRequest);
            }
        }
    }


    //запоминаем выбранную камеру
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("camNum", iCameraId);
    }


    @Override
    protected void onResume() {
        super.onResume();
        camera = Camera.open(iCameraId);
        setPreviewSize();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (camera != null){
            camera.release();
            camera = null;
        }
    }

    private void setPreviewSize() {

        // получаем размеры экрана
        Display display = getWindowManager().getDefaultDisplay();
        boolean widthIsMax = display.getWidth() > display.getHeight();

        // определяем размеры превью камеры
        Size size = camera.getParameters().getPreviewSize();

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

        // установка размеров surface из получившегося преобразования
        surfaceView.getLayoutParams().height = (int) (rectPreview.bottom);
        surfaceView.getLayoutParams().width = (int) (rectPreview.right);
    }

    private void setCameraDisplayOrientation(int cameraId) {
        // определяем насколько повернут экран от нормального положения
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
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
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        // задняя камера
        if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
            result = ((360 - degrees) + info.orientation);
        } else
            // передняя камера
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                result = ((360 - degrees) - info.orientation);
                result += 360;
            }
        result = result % 360;
        camera.setDisplayOrientation(result);
    }

    public static Bitmap rotate(Bitmap bitmap, int orientation, int cameraId) {
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
