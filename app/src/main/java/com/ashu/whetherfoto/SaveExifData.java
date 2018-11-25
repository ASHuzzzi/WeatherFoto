package com.ashu.whetherfoto;

import android.support.media.ExifInterface;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class SaveExifData {
    public void saveExifData(File sdPath, String stFotoCounter, String stWeatherResult){
        ExifInterface exif;
        try {
            exif = new ExifInterface(sdPath + "/" + stFotoCounter);
            exif.setAttribute(ExifInterface.TAG_MAKE,
                    String.valueOf(stWeatherResult));

            exif.saveAttributes();

        } catch (IOException e) {
            Log.e("PictureActivity", e.getLocalizedMessage());
        }


    }
}
