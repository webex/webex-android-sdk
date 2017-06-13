package com.cisco.spark.android.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;

import com.cisco.spark.android.ui.BitmapProvider;

public class CircleTransform implements BitmapProvider.Transformation {
    private int borderColor;
    private int borderWidth;
    private final float density;
    private boolean drawBorder;

    public CircleTransform(Context context, int borderColor, int borderWidth) {
        this(context);
        this.borderColor = borderColor;
        this.borderWidth = borderWidth;
        this.drawBorder = true;
    }

    public CircleTransform(Context context) {
        density = context.getResources().getDisplayMetrics().density;
    }

    @Override
    public Bitmap transform(Bitmap source) {
        int dimen = Math.min(source.getWidth(), source.getHeight());
        int topMargin = (source.getHeight() - dimen) / 2;
        int leftMargin = (source.getWidth() - dimen) / 2;
        int rightMargin = source.getWidth() - leftMargin;
        int bottomMargin = source.getHeight() - topMargin;
        Bitmap result = Bitmap.createBitmap(dimen, dimen, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect srcRect = new Rect(leftMargin, topMargin, rightMargin, bottomMargin);
        final Rect dstRect = new Rect(0, 0, dimen, dimen);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(dimen / 2, dimen / 2, dimen / 2, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(source, srcRect, dstRect, paint);

        if (drawBorder) {
            paint.setColor(borderColor);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(borderWidth * density);
            canvas.drawCircle(dimen / 2, dimen / 2, dimen / 2, paint);
        }

        source.recycle();
        return result;
    }

    @Override
    public String toString() {
        return "circle()";
    }
}
