package com.kudig.kwitansidigital.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.view.View;
import android.widget.FrameLayout;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PDFGenerator {
    public static void generatePDF(Context context, View view, String fileName) {
        Document document = new Document();

        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(path, fileName + ".pdf");
            FileOutputStream outputStream = new FileOutputStream(file);

            PdfWriter writer = PdfWriter.getInstance(document, outputStream);

            document.open();

            // Convert the layout XML to a Bitmap
            Bitmap bitmap = viewToBitmap(context, view);

            int documentWidth = (int) (document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin());
            int documentHeight = (int) (document.getPageSize().getHeight() - document.topMargin() - document.bottomMargin());
            float scaleX = (float) documentWidth / bitmap.getWidth();
            float scaleY = (float) documentHeight / bitmap.getHeight();
            float scale = Math.min(scaleX, scaleY);


            // Scale the Bitmap
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap,
                    (int) (bitmap.getWidth() * scale),
                    (int) (bitmap.getHeight() * scale),
                    true);

            // Add the scaled Bitmap to the PDF document
            com.itextpdf.text.Image image = com.itextpdf.text.Image.getInstance(bitmapToByteArray(scaledBitmap));
            image.setAlignment(Element.ALIGN_CENTER);
            document.add(image);

            // ...

        } catch (DocumentException | IOException e) {
            e.printStackTrace();
        }

    }

    private static Bitmap viewToBitmap(Context context, View view) {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        view.setLayoutParams(layoutParams);
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        view.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Drawable backgroundDrawable = view.getBackground();
        if (backgroundDrawable != null) {
            backgroundDrawable.draw(canvas);
        } else {
            canvas.drawColor(Color.WHITE);
        }
        view.draw(canvas);
        return bitmap;

    }

    private static byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }
}

