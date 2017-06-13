package com.cisco.spark.android.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import com.cisco.spark.android.core.Application;
import com.cisco.spark.android.util.BitmapUtils.ScaleProvider.ScaleType;
import com.github.benoitdion.ln.Ln;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BitmapUtils {

    // This value varies by device. Typically either 2048 or 4096.
    private static int maxBitmapDim;

    public interface ScaleProvider {
        enum ScaleType {
            DOWNSCALE_ONLY,             //only scale down to getMaxPixels()
            SCALE_BY_SCREEN_DENSITY,    //scale by the device screen density
            ENLARGE_IF_NEEDED           //scale up or down to getMaxPixels()
        }

        // Returns a power of 2 for passing to BitmapFactory.Options.inSampleSize
        int getMacroScale(int width, int height);

        // Returns the max image size in pixels
        int getMaxPixels();

        //Returns how the bitmap should be scaled
        ScaleType getScaleType();
    }

    /**
     * Decode a file to a proportionally scaled bitmap that fits within the given dimensions.
     *
     * @param file
     * @param maxWidth
     * @param maxHeight
     * @return
     * @throws IOException
     */
    public static Bitmap fileToBitmap(File file, final int maxWidth, final int maxHeight, final Bitmap.Config rgbConfig, final ScaleType scaleType) throws IOException {

        return fileToBitmap(file, new ScaleProvider() {
            @Override
            public int getMacroScale(int width, int height) {
                int scale = 1;
                // We want the ^2 scale to result in an image just slightly larger than the final target size.
                while (((width / scale) > maxWidth)
                        || ((height / scale) > maxHeight)) {
                    scale *= 2;
                }
                return Math.max(1, scale / 2);
            }

            @Override
            public int getMaxPixels() {
                return maxHeight * maxWidth;
            }

            @Override
            public ScaleType getScaleType() {
                return scaleType;
            }
        }, rgbConfig);
    }

    /**
     * Decode a file to a proportionally scaled bitmap with a total area of at most maxPixels. Useful for
     * ensuring decent quality regardless of aspect ratio.
     *
     * @param file      The file
     * @param maxPixels Max W x H
     * @return
     * @throws IOException
     */
    public static Bitmap fileToBitmap(File file, final int maxPixels, final ScaleType scaleType) throws IOException {
        return fileToBitmap(file, maxPixels, null, scaleType);
    }

    public static Bitmap fileToBitmap(File file, final int maxPixels, Bitmap.Config rgbConfig, final ScaleType scaleType) throws IOException {

        return fileToBitmap(file, new ScaleProvider() {
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

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getMinimumWidth(), drawable.getMinimumHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public static BitmapFactory.Options getBitmapDims(File bitmapFile) {
        try {
            InputStream in = new FileInputStream(bitmapFile);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, o);
            in.close();
            return o;
        } catch (Exception e) {
            Ln.e(e);
        }
        return null;
    }

    private static Bitmap fileToBitmap(File file, ScaleProvider scaleProvider, Bitmap.Config rgbConfig) throws IOException {
        InputStream in = new FileInputStream(file);
        int scale = 1;
        try {
            BitmapFactory.Options o = getBitmapDims(file);

            if (o.outWidth <= 0 || o.outHeight <= 0) {
                return null;
            }

            scale = scaleProvider.getMacroScale(o.outWidth, o.outHeight);

        } finally {
            if (in != null)
                in.close();
            in = null;
        }

        try {
            in = new FileInputStream(file);

            Bitmap b;
            BitmapFactory.Options o = new BitmapFactory.Options();
            if (scale > 1)
                o.inSampleSize = scale;
            if (rgbConfig != null) {
                o.inPreferredConfig = rgbConfig;
            }

            b = getScaledBitmap(BitmapFactory.decodeStream(in, null, o), scaleProvider);
            if (b != null)
                Ln.d("returning bitmap " + b.getWidth() + "x" + b.getHeight());
            return b;
        } catch (Exception e) {
            Ln.e(e, "Caught exception");
        } finally {
            if (in != null)
                in.close();
        }
        return null;
    }

    public static Bitmap getScaledBitmap(Bitmap b, ScaleProvider scaleProvider) {
        if (b == null)
            return null;

        int height = b.getHeight();
        int width = b.getWidth();
        Application application = Application.getInstance();

        if (scaleProvider.getScaleType() == ScaleType.ENLARGE_IF_NEEDED || width * height > scaleProvider.getMaxPixels()) {
            //scale up/down to max pixel size
            float aspectRatio = (float) width / (float) height;
            height = (int) Math.sqrt(scaleProvider.getMaxPixels() / aspectRatio);
            width = (int) ((float) height * aspectRatio);
        } else if (scaleProvider.getScaleType() == ScaleType.SCALE_BY_SCREEN_DENSITY && application != null) {
            float screenDensity = application.getApplicationContext().getResources().getDisplayMetrics().density;
            height = Math.round(height * screenDensity);
            width = Math.round(width * screenDensity);
        }

        // This value varies by device. Typically either 2048 or 4096.
        if (maxBitmapDim < 2048) {
            maxBitmapDim = new Canvas().getMaximumBitmapHeight();
            if (maxBitmapDim > 4096 || maxBitmapDim < 2048)
                maxBitmapDim = 2048;
        }

        if (maxBitmapDim >= 2048 && Math.max(height, width) > maxBitmapDim) {
            float scaleDown = (float) maxBitmapDim / (float) Math.max(height, width);
            height = (int) ((float) height * scaleDown);
            width = (int) ((float) width * scaleDown);
        }

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(b, width, height, true);

        if (b != scaledBitmap) {
            b.recycle();
            b = scaledBitmap;
        }
        return b;
    }
}
