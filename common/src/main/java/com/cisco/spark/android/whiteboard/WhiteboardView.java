package com.cisco.spark.android.whiteboard;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import java.util.Locale;

public abstract class WhiteboardView extends FrameLayout {

    protected static final String JS_BRIDGE_HOST = "WhiteboardBridgeHost";

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

    public void post(final String message) {
        this.post(new Runnable() {
            @Override
            public void run() {
                sendMsgtoJS(message);
            }
    });
}

    public boolean isSharing() {
        return isSharing;
    }

    public void setIsSharing(boolean sharing) {
        this.isSharing = sharing;
    }

    public abstract void release();

    public abstract void share(boolean isSharing);

    protected void changeBoardSize() {
        String javascript = String.format(Locale.getDefault(), "screen = { availHeight: %2$d, availLeft: 0, availTop: 0, " +
                        "availWidth: %1$d, colorDepth: 32, deviceHeight: %2$d, " +
                        "deviceWidth: %1$d, height: %2$d, pixelDepth: 32, " +
                        "width: %1$d, deviceWidth: %1$d, deviceHeight: %2$d };",
                getWidth(), getHeight());

        sendMsgtoJS(javascript);
    }

    protected void sendMsgtoJS(String javascript) {
        // remove the abstract key word and left empty because
        // The NativeWhiteboardView need to derived this class but it makes no sence to
        // have this method, while the SquaredWhiteboardView still override it
    };

}
