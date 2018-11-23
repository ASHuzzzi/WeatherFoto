package com.ashu.whetherfoto;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.media.MediaScannerConnection;
import android.support.media.ExifInterface;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private SurfaceView surfaceView;
    private Camera camera;
    private File photoFile;

    private int iCameraId;

    public static final int iNumberOfRequest = 23401;

    private BitmapRotate BitmapRotate;
    private PrepareCam PrepareCam;
    private NetworkCheck NetworkCheck;

    private Handler handlerMainActivity;
    private Runnable runnableMainActivity;
    private boolean flagThreadIsRun = false; //переменная для проверики запущен ли поток или нет

    String TAG = "WEATHER";
    WeatherAPI.ApiInterface api;
    private String wheather;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //восстанавливаем состояние при повороте
        if (savedInstanceState != null) {
            iCameraId = savedInstanceState.getInt(
                    getResources().getString(R.string.savedInstanceStateKey), 0);
        } else {
            iCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.surfaceView);

        PrepareCam = new PrepareCam();

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

                // определяем насколько повернут экран от нормального положения во втором аргументе
                int result = PrepareCam.setCameraDisplayOrientation(
                        iCameraId,
                        getWindowManager().getDefaultDisplay().getRotation());
                camera.setDisplayOrientation(result);

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
                        String stFotoCounter ="A_" + timeStamp + ".jpg";

                        // получаем путь к папке во внутренней памяти
                        File sdPath = Environment
                                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                        // добавляем свой каталог к пути
                        sdPath = new File(sdPath.getAbsolutePath() + "/" + "WhetherFoto");
                        // создаем каталог
                        if (!sdPath.exists()) {
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
                            BitmapRotate = new BitmapRotate();
                            realImage = BitmapRotate.rotate(realImage, rotation, iCameraId);
                            //realImage = rotate(realImage, rotation, iCameraId);
                            if (realImage != null) {
                                realImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                            } else {
                                Toast.makeText(
                                        getApplicationContext(),
                                        getResources().getString(R.string.toastNotSaveFoto),
                                        Toast.LENGTH_SHORT).show();
                            }
                            fos.flush();
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

                        ExifInterface exif;
                        try {
                            exif = new ExifInterface(sdPath + "/" + stFotoCounter);
                            exif.setAttribute(ExifInterface.TAG_MAKE,
                                    String.valueOf(wheather));

                                exif.saveAttributes();

                        } catch (IOException e) {
                            Log.e("PictureActivity", e.getLocalizedMessage());
                        }

                        //Для того, чтобы файл был видет в ФМ.
                        File imageFile = photoFile;
                        MediaScannerConnection.scanFile(
                                getApplicationContext(),
                                new String[] { imageFile.getPath() },
                                new String[] { "image/jpeg" },
                                null);
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
                if (iCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    iCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
                } else {
                    iCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                }

                //возобновляем показ
                camera = Camera.open(iCameraId);


                int result = PrepareCam.setCameraDisplayOrientation(
                        iCameraId,
                        getWindowManager().getDefaultDisplay().getRotation());
                camera.setDisplayOrientation(result);

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

            if (canCam != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, iNumberOfRequest);
            }
        }

        //хэндлер для потока runnableMainActivity
        handlerMainActivity = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                String result_check = bundle.getString(
                        getResources().getString(R.string.resultCheckKey));


                if (result_check != null && result_check.equals("true")) {
                    wheather = bundle.getString(
                            "wheather");
                    Toast.makeText(
                            getApplicationContext(),
                            wheather,
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(
                            getApplicationContext(),
                            getResources().getString(R.string.toastNetworkCheckNo),
                            Toast.LENGTH_SHORT).show();
                }
                flagThreadIsRun = false;
            }
        };

        //поток запускаемый при создании экрана (запуск происходит из onStart)
        runnableMainActivity = new Runnable() {
            @Override
            public void run() {

                flagThreadIsRun = true;
                NetworkCheck = new NetworkCheck(getApplicationContext());
                boolean resultCheck = NetworkCheck.checkInternet();
                final Bundle bundle = new Bundle();
                if (resultCheck) {
                    bundle.putString(
                            getResources().getString(R.string.resultCheckKey),
                            String.valueOf(true));

                    api = WeatherAPI.getClient().create(WeatherAPI.ApiInterface.class);
                    Double lat = 59.9387;
                    Double lng = 30.3162;
                    String units = "metric";
                    String key = WeatherAPI.KEY;

                    // get weather for today
                    Call<WeatherDay> callToday = api.getToday(lat, lng, units, key);
                    callToday.enqueue(new Callback<WeatherDay>() {
                        @Override
                        public void onResponse(Call<WeatherDay> call, Response<WeatherDay> response) {
                            Log.e(TAG, "onResponse");
                            WeatherDay data = response.body();

                            if (response.isSuccessful()) {

                                bundle.putString(
                                        "wheather",
                                        data.getCity() + " " + data.getTempWithDegree());
                                Message msg = handlerMainActivity.obtainMessage();
                                msg.setData(bundle);
                                handlerMainActivity.sendMessage(msg);
                            }
                        }

                        @Override
                        public void onFailure(Call<WeatherDay> call, Throwable t) {
                            Log.e(TAG, "onFailure");
                            Log.e(TAG, t.toString());
                        }
                    });
                } else {
                    bundle.putString(
                            getResources().getString(R.string.resultCheckKey),
                            String.valueOf(false));
                    Message msg = handlerMainActivity.obtainMessage();
                    msg.setData(bundle);
                    handlerMainActivity.sendMessage(msg);
                }

            }
        };


    }


    //запоминаем выбранную камеру
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(getResources().getString(R.string.savedInstanceStateKey), iCameraId);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!flagThreadIsRun) {
            Thread threadMainActivity = new Thread(runnableMainActivity);
            threadMainActivity.setDaemon(true);
            threadMainActivity.start();
        } else {
            Toast.makeText(
                    getApplicationContext(),
                    getResources().getString(R.string.toastThreadIsRuning),
                    Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        camera = Camera.open(iCameraId);

        RectF rectPreview = PrepareCam.setPreviewSize(
                getWindowManager().getDefaultDisplay(),
                camera.getParameters().getPreviewSize()
        );

        // установка размеров surface из получившегося преобразования
        surfaceView.getLayoutParams().height = (int) (rectPreview.bottom);
        surfaceView.getLayoutParams().width = (int) (rectPreview.right);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }
}
