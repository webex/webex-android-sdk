package com.cisco.spark.android.util;

public abstract class Action<T> extends ActionN {

    public static final Action<Void> NO_ACTION = new Action<Void>() {
        @Override
        public void call(Void param) {
        }
    };

    public abstract void call(T item);

}
