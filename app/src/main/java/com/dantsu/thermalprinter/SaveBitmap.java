package com.dantsu.thermalprinter;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class SaveBitmap {

    public static void  saveBitmapToPictures(Context context, Bitmap bitmap, String fileName) {
        OutputStream outputStream = null;

        try {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 and above
                outputStream = context.getContentResolver().openOutputStream(
                        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY));
            } else {
                // For older Android versions
                String filePath = Environment.getExternalStorageDirectory() + "/Pictures/MyAppBitmaps";
                File directory = new File(filePath);
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                File file = new File(directory, fileName);
                outputStream = new FileOutputStream(file);
            }

            if (outputStream != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.flush();
                outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            // Close the stream in case of an exception
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}