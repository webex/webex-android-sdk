package com.cisco.spark.android.whiteboard.renderer;

import android.view.MotionEvent;

import com.github.benoitdion.ln.Ln;
import com.wacom.ink.rendering.EGLRenderingContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class WILLFrontBuffer {

    private Method clearFrontBufferTouchesMethod;
    private Method updateFrontBufferTouchesMethod;

    WILLFrontBuffer() {
        try {
            clearFrontBufferTouchesMethod = EGLRenderingContext.class.getMethod("clearFrontBufferTouches");
            updateFrontBufferTouchesMethod = EGLRenderingContext.class.getMethod("updateFrontBufferTouches", MotionEvent.class);
        } catch (NoSuchMethodException e) {
            // This is fine
        }
    }

    public void clearFrontBufferTouches() {
        if (clearFrontBufferTouchesMethod != null) {
            try {
                clearFrontBufferTouchesMethod.invoke(null);
            } catch (IllegalAccessException e) {
                Ln.e(e);
            } catch (InvocationTargetException e) {
                Ln.e(e);
            }
        }
    }

    public void updateFrontBufferTouches(MotionEvent event) {
        if (updateFrontBufferTouchesMethod != null) {
            try {
                updateFrontBufferTouchesMethod.invoke(null, event);
            } catch (IllegalAccessException e) {
                Ln.e(e);
            } catch (InvocationTargetException e) {
                Ln.e(e);
            }
        }
    }
}
