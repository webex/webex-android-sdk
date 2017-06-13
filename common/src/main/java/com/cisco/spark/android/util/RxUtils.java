package com.cisco.spark.android.util;

import com.github.benoitdion.ln.Ln;

import rx.functions.Action1;

public class RxUtils {

    public static Action1<Throwable> onError = new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) {
            Ln.e(throwable);
        }
    };
}
