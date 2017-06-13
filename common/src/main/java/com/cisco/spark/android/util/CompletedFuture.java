package com.cisco.spark.android.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class CompletedFuture<T> implements Future<T> {
    private final T object;

    public CompletedFuture(T object) {
        this.object = object;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws ExecutionException {
        return get();
    }

    @Override
    public T get() throws ExecutionException {
        return object;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }
}
