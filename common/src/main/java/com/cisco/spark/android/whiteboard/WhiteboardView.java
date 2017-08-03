package com.cisco.spark.android.whiteboard;


import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public abstract class WhiteboardView extends FrameLayout {

    private boolean isSharing;

    public WhiteboardView(Context context) {
        super(context);
    }

    public WhiteboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WhiteboardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public WhiteboardView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public abstract void start();

    public boolean isSharing() {
        return isSharing;
    }

    public void setIsSharing(boolean sharing) {
        this.isSharing = sharing;
    }

    public abstract void release();

    public abstract void share(boolean isSharing);

}
