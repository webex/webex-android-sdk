package com.cisco.spark.android.whiteboard.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.cisco.spark.android.util.BitmapUtils;
import com.github.benoitdion.ln.Ln;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
public class FileUtilities {

    public static byte[] getByteArray(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, baos);
        return baos.toByteArray();
    }

    public static byte[] readBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();

        byte[] byteBuffer = new byte[1024];
        while (true) {
            int readBytes = inputStream.read(byteBuffer);
            if (readBytes == -1) {
                break;
            }
            arrayOutputStream.write(byteBuffer, 0, readBytes);
        }

        return arrayOutputStream.toByteArray();
    }

    public static Bitmap inputStreamToBitmap(byte[] bytes, final int maxPixels, final BitmapUtils.ScaleProvider.ScaleType scaleType) throws IOException {
        return inputStreamToBitmap(bytes, maxPixels, null, scaleType);
    }

    public static Bitmap inputStreamToBitmap(byte[] bytes, final int maxPixels, Bitmap.Config rgbConfig, final BitmapUtils.ScaleProvider.ScaleType scaleType) throws IOException {

        return inputStreamToBitmap(bytes, new BitmapUtils.ScaleProvider() {
            @Override
            public int getMacroScale(int width, int height) {
                int scale = 1;
                // We want the ^2 scale to result in an image just slightly larger than the final target size.
                while (((width / scale) * (height / scale)) > maxPixels) {
                    scale *= 2;
                }
                return Math.max(1, scale / 2);
            }

            @Override
            public int getMaxPixels() {
                return maxPixels;
            }

            @Override
            public ScaleType getScaleType() {
                return scaleType;
            }
        }, rgbConfig);
    }

    private static Bitmap inputStreamToBitmap(byte[] bytes, BitmapUtils.ScaleProvider scaleProvider, Bitmap.Config rgbConfig) throws IOException {

        Bitmap b;

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);

        BitmapFactory.Options op = new BitmapFactory.Options();
        op.inSampleSize = scaleProvider.getMacroScale(bounds.outWidth, bounds.outHeight);

        if (rgbConfig != null) {
            op.inPreferredConfig = rgbConfig;
        }

        Bitmap startBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, op);

        if (startBitmap == null) {
            return null;
        }

        if (startBitmap.getWidth() == 0 || startBitmap.getHeight() == 0) {
            return null;
        }

        b = BitmapUtils.getScaledBitmap(startBitmap, scaleProvider);
        if (b != null) {
            Ln.d("returning bitmap " + b.getWidth() + "x" + b.getHeight());
            return b;
        }

        return null;
    }
}
