package com.cisco.spark.android.util;

public abstract class Action0 extends ActionN {

    public abstract void call();

    public static final Action0 NO_ACTION = new Action0() {
        @Override
        public void call() {
        }
    };

}
