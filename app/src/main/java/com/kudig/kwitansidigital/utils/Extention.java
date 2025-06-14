package com.kudig.kwitansidigital.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Environment;
import android.widget.LinearLayout;

import java.io.File;
import java.io.FileOutputStream;

public class Extention {
    private String saveBitmapToStorage(Bitmap bitmap) {
        String filePath = "";
        try {
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Kwitansi Digital");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileName = "print_image.jpg";
            File file = new File(dir, fileName);

            FileOutputStream outputStream = new FileOutputStream(file);
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
            filePath = file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filePath;
    }

    private Bitmap loadBitmapFromView(LinearLayout linearLayout, int width, int height) {
        Bitmap bitmap;
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        linearLayout.draw(canvas);
        return bitmap;
    }
}
