package com.cisco.spark.android.util;

import java.util.concurrent.Executor;

import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Helper class for injecting schedulers in rx in tests
 */
@SuppressWarnings("unused")
public class SchedulerProvider {

    private Scheduler io;
    private Scheduler computation;
    private Scheduler immediate;
    private Scheduler newThread;

    private Scheduler timer;

    private Scheduler mainThread;

    private Scheduler executorOverride;

    public SchedulerProvider() {

        // Built in
        io = Schedulers.io();
        computation = Schedulers.computation();
        immediate = Schedulers.immediate();

        mainThread = AndroidSchedulers.mainThread();

        timer = Schedulers.computation();
        executorOverride = null;
    }

    public void setImmediate(Scheduler immediate) {
        this.immediate = immediate;
    }

    public void setIo(Scheduler io) {
        this.io = io;
    }

    public void setNewThread(Scheduler newThread) {
        this.newThread = newThread;
    }

    public void setComputation(Scheduler computation) {
        this.computation = computation;
    }

    public void setTimer(Scheduler timer) {
        this.timer = timer;
    }

    public void setMainThread(Scheduler mainThread) {
        this.mainThread = mainThread;
    }

    public void setExecutorOverride(Scheduler executorOverride) {
        this.executorOverride = executorOverride;
    }

    public Scheduler computation() {
        return computation;
    }

    public Scheduler immediate() {
        return immediate;
    }

    public Scheduler io() {
        return io;
    }

    public Scheduler newThread() {

        if (newThread != null) {
            return newThread;
        }

        return Schedulers.newThread();
    }

    public Scheduler timer() {
        return timer;
    }

    public Scheduler from(Executor executor) {

        if (executorOverride != null) {
            return executorOverride;
        }

        return Schedulers.from(executor);
    }

    public Scheduler mainThread() {
        return mainThread;
    }
}
