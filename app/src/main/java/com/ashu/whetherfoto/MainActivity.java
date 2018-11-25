package com.ashu.whetherfoto;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import com.ashu.whetherfoto.Weather.WeatherTake;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private SurfaceView surfaceView;
    private Camera camera;

    private int iCameraId;

    public static final int iNumberOfRequest = 23401;

    private PrepareCam PrepareCam;
    private NetworkCheck NetworkCheck;
    private WeatherTake WeatherTake;
    private SaveNewFoto SaveNewFoto;

    private Handler handlerMainActivity;
    private Runnable runnableMainActivity;
    private boolean flagThreadIsRun = false; //переменная для проверики запущен ли поток или нет

    private String stWeatherResult;

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

        stWeatherResult= " ";

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

                        //обновляем экран после снимка
                        try {
                            camera.setPreviewDisplay(holder);
                            camera.startPreview();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        //берем угол поворота телефона
                        int rotation = getWindowManager().getDefaultDisplay().getRotation();

                        SaveNewFoto = new SaveNewFoto(getApplicationContext());
                        boolean bResult = SaveNewFoto.saveNewFoto(
                                data,rotation, iCameraId, stWeatherResult, getResources().getString(R.string.app_name));

                        if (!bResult) {
                            Toast.makeText(
                                    getApplicationContext(),
                                    getResources().getString(R.string.toastNotSaveFoto),
                                    Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(
                            getApplicationContext(),
                            getResources().getString(R.string.toastWeatherLoad),
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
                    WeatherTake = new WeatherTake();
                    stWeatherResult = WeatherTake.waetherTake();

                    //если данные по погоде получены, то записываем их в переменную
                    if(stWeatherResult.length() > 1){
                        bundle.putString(
                                getResources().getString(R.string.resultCheckKey),
                                String.valueOf(true));

                        bundle.putString(
                                getResources().getString(R.string.resultWheatherKey),
                                stWeatherResult);
                        Message msg = handlerMainActivity.obtainMessage();
                        msg.setData(bundle);
                        handlerMainActivity.sendMessage(msg);
                    }
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

        //проверяем запущен ли поток
        if (!flagThreadIsRun) {
            Thread threadMainActivity = new Thread(runnableMainActivity);
            threadMainActivity.setDaemon(true);
            threadMainActivity.start();
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
