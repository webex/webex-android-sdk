package com.ciscospark.androidsdk.utils.log;

import android.util.Log;

import com.github.benoitdion.ln.Ln;

/**
 * Created with IntelliJ IDEA.
 * User: kt
 * Date: 29/11/2017
 * Time: 00:47
 */

public class MediaLog {

    public static int outputLog(int priority, String tag, String msg) {
        String message = "<" + tag + ">" + msg;
        if (priority == Log.WARN) {
            Ln.w(message);
        } else if (priority == Log.ERROR) {
            Ln.e(message);
        } else if (priority == Log.DEBUG) {
            Ln.d(message);
        } else if (priority == Log.INFO) {
            Ln.i(message);
        } else if (priority == Log.VERBOSE) {
            Ln.v(message);
        } else {
            Ln.e(message);
        }
        return 0;
    }

}
