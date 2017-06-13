package com.cisco.spark.android.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.support.annotation.CheckResult;
import android.support.annotation.ColorRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.TypedValue;
import android.view.MenuItem;
import android.widget.ProgressBar;

import com.cisco.spark.android.R;
import com.cisco.spark.android.log.Lns;
import com.cisco.spark.android.ui.BitmapProvider;
import com.cisco.spark.android.util.BitmapUtils.ScaleProvider.ScaleType;
import com.github.benoitdion.ln.Ln;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Locale;

public class ImageUtils {
    public static final int THUMBNAIL_VIEW_ID = 123212321;
    public static final int OUTGOING_THUMBNAIL_MAX_PIXELS = 640 * 480;
    private static final String TEMP_THUMBNAIL_DIR = "thumbs";

    @CheckResult
    public static boolean writeBitmap(File outfile, Bitmap thumbnail) {
        if (outfile == null) {
            Ln.e("Null file passed to writeBitmap()");
            return false;
        }
        if (thumbnail == null) {
            Ln.e("Null thumbnail passed to writeBitmap()");
            return false;
        }
        try {
            if (!outfile.exists()) {
                if (!outfile.createNewFile()) {
                    Ln.e("create file failed, it might already exist");
                    return false;
                }
            }

            OutputStream out = new FileOutputStream(outfile);

            thumbnail.compress(Bitmap.CompressFormat.PNG, 75, out);
            out.close();

        } catch (FileNotFoundException e) {
            Ln.e("Failed writing bitmap to file", e);
            return false;
        } catch (IOException e) {
            Ln.e("Failed writing bitmap to file", e);
            return false;
        }

        return true;
    }

    public static File getTmpFile(Context context, String name, String ext) throws IOException {
        File tmpDir = FileUtils.getCacheDir(context); // context being the Activity pointer
        tmpDir.mkdirs();
        return File.createTempFile(name, ext, tmpDir);
    }

    public static File getLocalTmpFile(File srcFile) {
        File tmpFile = new File(srcFile + ".tmp");
        if (tmpFile.exists())
            tmpFile.delete();
        return tmpFile;
    }

    /**
     * Rotate an image for display.
     */
    public static File correctRotation(File file, int compression) {
        int width = 0, height = 0;
        try {
            Matrix matrix = getRotation(file);
            if (matrix == null || matrix.isIdentity()) {
                return file;
            }

            Bitmap original = BitmapUtils.fileToBitmap(file, BitmapProvider.LARGE_DEFAULT_PIXELSIZE, ScaleType.DOWNSCALE_ONLY);
            if (original == null) {
                return file;
            }

            Lns.content().i("Rotating image...");
            Bitmap.CompressFormat format;
            String extension = "." + MimeUtils.getExtension(file.getAbsolutePath());
            String png = ".PNG";
            if (extension.toUpperCase(Locale.getDefault()).equals(png)) {
                format = Bitmap.CompressFormat.PNG;
            } else {
                format = Bitmap.CompressFormat.JPEG;
            }

            width = original.getWidth();
            height = original.getHeight();

            Bitmap bitmap = Bitmap.createBitmap(original, 0, 0, width, height, matrix, true);

            File rotatedImage = getLocalTmpFile(file);
            OutputStream out = new FileOutputStream(rotatedImage.getAbsolutePath());
            bitmap.compress(format, compression, out);
            bitmap.recycle();
            original.recycle();
            System.gc();
            out.close();
            file.delete();
            if (!rotatedImage.renameTo(file)) {
                Lns.content().i("Failed to rename rotated file. Reverting to copy.");
                FileUtils.copyFile(rotatedImage, file);
            }
        } catch (Throwable tr) {
            long fileSize = 0;
            if (file != null)
                fileSize = file.length();
            Ln.e(true, tr, "Error generating rotated bitmap from file '%s' (WxH=%dx%d, size=%d)",
                    (file == null ? "null" : file.getAbsolutePath()), width, height, fileSize);
            return file;
        }

        return file;
    }

    /**
     * Handle all the various cases to correctly rotate an image file for display
     */
    private static Matrix getRotation(File file) {
        Matrix matrix = new Matrix();

        try {
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0);
            int height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0);

            switch (orientation) {
                case ExifInterface.ORIENTATION_NORMAL:
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.setTranslate(width, height);
                    matrix.setRotate(180);
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.setTranslate(width, 0);
                    matrix.setRotate(90);
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.setTranslate(0, height);
                    matrix.setRotate(-90);
                    break;
            }
            switch (orientation) {
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.setTranslate(width, 0);
                    matrix.setScale(-1.0f, 1.0f);
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    matrix.setTranslate(height, 0);
                    matrix.setScale(-1.0f, 1.0f);
            }
        } catch (Throwable tr) {
            Ln.e(tr);
        }
        return matrix;
    }


    public static com.cisco.spark.android.model.Image getThumbnailTempFile(File outFile, Uri content, MimeUtils.ContentType contentType) {
        FileOutputStream fos = null;
        try {
            File file = new File(new URI(content.toString()));
            Bitmap thumb;
            if (contentType == MimeUtils.ContentType.VIDEO) {
                thumb = ThumbnailUtils.createVideoThumbnail(file.getAbsolutePath(), 0);
                if (thumb == null) {
                    Ln.w("Failed creating bitmap for video file");
                    return null;
                }
                thumb = BitmapUtils.getScaledBitmap(thumb, new BitmapUtils.ScaleProvider() {
                    @Override
                    public int getMacroScale(int width, int height) {
                        return 1; // it's already in memory, no need to pre-scale
                    }

                    @Override
                    public int getMaxPixels() {
                        return BitmapProvider.BitmapType.VIDEO_THUMBNAIL.maxPixels;
                    }

                    @Override
                    public ScaleType getScaleType() {
                        return ScaleType.DOWNSCALE_ONLY;
                    }
                });
            } else {
                thumb = BitmapUtils.fileToBitmap(file, OUTGOING_THUMBNAIL_MAX_PIXELS, ScaleType.DOWNSCALE_ONLY);
            }
            if (thumb != null) {
                fos = new FileOutputStream(outFile);
                thumb.compress(Bitmap.CompressFormat.PNG, 80, fos);
                int width = thumb.getWidth();
                int height = thumb.getHeight();
                return new com.cisco.spark.android.model.Image(Uri.fromFile(outFile), width, height, true);
            }
        } catch (Exception e) {
            Ln.e(e, "Failed getting temp file for thumbnail");
        } finally {
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
                Ln.e(e);
            }
        }
        return null;
    }

    public static void dumpExifData(File file) {
        try {
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0);
            int height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0);
            Ln.d("EXIF dump for: %s", file.getAbsolutePath());
            Ln.d("  orientation: %s (%d)", getOrientationString(orientation), orientation);
            Ln.d("        width: %d", width);
            Ln.d("       height: %d", height);
        } catch (Throwable throwable) {
            Ln.e(throwable, "Error getting EXIF data");
        }
    }

    private static String getOrientationString(int orientation) {
        String result;
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                result = "ExifInterface.ORIENTATION_NORMAL";
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                result = "ExifInterface.ORIENTATION_ROTATE_90";
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                result = "ExifInterface.ORIENTATION_ROTATE_180";
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                result = "ExifInterface.ORIENTATION_ROTATE_270";
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                result = "ExifInterface.ORIENTATION_FLIP_HORIZONTAL";
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                result = "ExifInterface.ORIENTATION_FLIP_VERTICAL";
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                result = "ExifInterface.ORIENTATION_TRANSPOSE";
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                result = "ExifInterface.ORIENTATION_TRANSVERSE";
                break;
            case ExifInterface.ORIENTATION_UNDEFINED:
                result = "ExifInterface.UNDEFINED";
                break;
            default:
                result = "Unknown";
        }
        return result;
    }

    public static void setTintCompat(Context context, MenuItem view, int tint) {
        Drawable drawable = DrawableCompat.wrap(view.getIcon());
        DrawableCompat.setTint(drawable.mutate(), context.getResources().getColor(tint));
        view.setIcon(drawable);
    }

    public static void setTintCompat(Context context, FloatingActionButton view, int tint) {
        Drawable drawable = DrawableCompat.wrap(view.getDrawable());
        DrawableCompat.setTint(drawable.mutate(), context.getResources().getColor(tint));
        view.setImageDrawable(drawable);
    }

    public static void setTintCompat(Context context, ProgressBar view, int tint) {
        Drawable drawable = DrawableCompat.wrap(view.getIndeterminateDrawable());
        DrawableCompat.setTint(drawable.mutate(), context.getResources().getColor(tint));
        view.setIndeterminateDrawable(drawable);
    }

    public static StateListDrawable generateCustomButton(Context context, /* Can't annotate this one */
                                                         int resourceId, @ColorRes int colourUpId,
                                                         @ColorRes int colourDownId, @ColorRes int colourSelectedId,
                                                         @ColorRes int colourSelectedDownId) {

        GradientDrawable down, up, disabled, selected, selectedDown; // Start these off as null

        Resources res = context.getResources();

        try {
            down = (GradientDrawable) Drawable.createFromXml(res, res.getXml(resourceId));
            up = (GradientDrawable) Drawable.createFromXml(res, res.getXml(resourceId));
            disabled = (GradientDrawable) Drawable.createFromXml(res, res.getXml(resourceId));
            selected = (GradientDrawable) Drawable.createFromXml(res, res.getXml(resourceId));
            selectedDown = (GradientDrawable) Drawable.createFromXml(res, res.getXml(resourceId));
        } catch (Exception e) {
            Ln.e("Failed at loading images, reverting to defaults");
            down = new GradientDrawable();
            up = new GradientDrawable();
            disabled = new GradientDrawable();
            selected = new GradientDrawable();
            selectedDown = new GradientDrawable();
        }

        int colourUp = ContextCompat.getColor(context, colourUpId);
        up.setColor(colourUp);

        int colourDown = ContextCompat.getColor(context, colourDownId);
        down.setColor(colourDown);

        int colourDisabled = ContextCompat.getColor(context, R.color.gray_dark_2);
        disabled.setColor(colourDisabled);

        StateListDrawable states = new StateListDrawable();

        if (colourSelectedDownId != -1) {
            int colourSelectedDown = ContextCompat.getColor(context, colourSelectedDownId);
            selectedDown.setColor(colourSelectedDown);
            states.addState(new int[]{android.R.attr.state_selected, android.R.attr.state_pressed}, selectedDown);
        }

        if (colourSelectedId != -1) {
            int colourSelected = ContextCompat.getColor(context, colourSelectedId);
            selected.setColor(colourSelected);
            states.addState(new int[]{android.R.attr.state_selected}, selected);
        }

        states.addState(new int[]{android.R.attr.state_pressed}, down);
        states.addState(new int[]{android.R.attr.state_focused}, up);
        states.addState(new int[]{-android.R.attr.state_enabled}, disabled);
        states.addState(new int[]{android.R.attr.state_enabled}, up);

        return states;
    }

    public static StateListDrawable generateCircleButton(Context context, @ColorRes int upColour,
                                                         @ColorRes int downColour, @ColorRes int selectedColour,
                                                         @ColorRes int selectedDownColour) {
        return generateCustomButton(context, R.drawable.shape_circle_gradient_custom_colors,
                upColour, downColour, selectedColour, selectedDownColour);
    }

    public static StateListDrawable generateCircleButton(Context context, @ColorRes int buttonColor) {
        return generateCircleButton(context, buttonColor, buttonColor, buttonColor, buttonColor);
    }

    public static int dpToPx(Resources res, float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, res.getDisplayMetrics());
    }
}
