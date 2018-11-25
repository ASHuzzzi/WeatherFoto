package com.ashu.whetherfoto;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;

public class SaveNewFoto {

    private Boolean flagResult;
    private Context context;

    public SaveNewFoto(Context context) {
        this.context = context;
    }

    public Boolean saveNewFoto(
            byte[] data, int rotation, int iCameraId, String stWeatherResult, String stAppName){
        //берем время в миллисекундах
        String timeStamp = String.valueOf(
                TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));

        //Получаем имя файла из времени и расширения
        String stFotoCounter = timeStamp + ".jpg";

        // получаем путь к папке во внутренней памяти
        File sdPath = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        // добавляем свой каталог к пути
        sdPath = new File(
                sdPath.getAbsolutePath() + "/" + stAppName);
        // создаем каталог
        if (!sdPath.exists()) {
            boolean isDirectoryCreated = sdPath.mkdir();
            //если не получилось, то пишем в каталог по умолчанию
            if (!isDirectoryCreated) {
                sdPath = Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            }
        }

        File photoFile = new File(sdPath, stFotoCounter);

        try {
            FileOutputStream fos = new FileOutputStream(photoFile);

            Bitmap realImage = BitmapFactory.decodeByteArray(data, 0, data.length);

            //поворачиваем изобажение
            com.ashu.whetherfoto.BitmapRotate bitmapRotate = new BitmapRotate();
            realImage = bitmapRotate.rotate(realImage, rotation, iCameraId);
            if (realImage != null) {
                realImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                flagResult = true;
                if (stWeatherResult.length() > 1){
                    SaveExifData saveExifData = new SaveExifData();
                    saveExifData.saveExifData(sdPath, stFotoCounter, stWeatherResult);
                }

            } else {
                flagResult = false;
            }
            fos.flush();
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        //Для того, чтобы файл был виден в ФМ.
        MediaScannerConnection.scanFile(
                context,
                new String[] { photoFile.getPath() },
                new String[] { "image/jpeg" },
                null);

        return flagResult;
    }
}
