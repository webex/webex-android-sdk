package com.cisco.spark.android.util;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class LifoBlockingDeque<E> extends LinkedBlockingDeque<E> {

    private static final long serialVersionUID = -329298389238L;

    @Override
    public boolean offer(E e) {
        return super.offerFirst(e);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        return super.offerFirst(e, timeout, unit);
    }

    @Override
    public boolean add(E e) {
        return super.offerFirst(e);
    }

    @Override
    public void put(E e) throws InterruptedException {
        super.putFirst(e);
    }
}
