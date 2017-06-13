package com.cisco.spark.android.util;

import rx.Observer;

public abstract class ObserverAdapter<T> implements Observer<T> {
    @Override
    public void onCompleted() {
    }

    @Override
    public void onError(Throwable throwable) {
    }

    @Override
    public void onNext(T t) {
    }
}
