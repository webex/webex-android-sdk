package com.cisco.spark.android.util;

import android.content.Context;
import android.graphics.*;

import com.cisco.spark.android.ui.BitmapProvider;

import java.util.*;

// Inspired by stackoverflow.com/questions/15384837/android-round-a-layouts-background-image-only-top-or-bottom-corners
// Variation of RoundedCornerTransform
public class RoundedTopCornerTransform implements BitmapProvider.Transformation {
    private final int radius;
    private int borderColor;
    private final float density;
    private boolean drawBorder;

    public RoundedTopCornerTransform(Context context, int radius, int borderColor) {
        this(context, radius);
        this.borderColor = borderColor;
        this.drawBorder = true;
    }

    public RoundedTopCornerTransform(Context context, int radius) {
        this.radius = radius;
        density = context.getResources().getDisplayMetrics().density;
    }

    @Override
    public Bitmap transform(Bitmap source) {
        Bitmap result = Bitmap.createBitmap(source.getWidth(), source
                .getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final float roundPx = radius * density;
        final Rect rect = new Rect(0, 0, source.getWidth(), source.getHeight() - (int) roundPx);
        final Rect bottomRect = new Rect(0, (int) roundPx, source.getWidth(), source.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRect(bottomRect, paint);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(source, rect, rect, paint);
        canvas.drawBitmap(source, bottomRect, bottomRect, paint);
        source.recycle();
        if (drawBorder) {
            paint.setColor(borderColor);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1 * density);
            canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        }
        return result;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "rounded_corner_%d_%d()", radius, borderColor);
    }
}
