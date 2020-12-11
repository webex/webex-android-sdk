/*
 * Copyright 2016-2021 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscowebex.androidsdk.internal.queue;

import android.os.Handler;
import android.os.HandlerThread;

import com.github.benoitdion.ln.Ln;

import java.util.UUID;

public class BackgroundQueue implements Queue {

    private Handler handler;

    public BackgroundQueue() {
        this(null, true);
    }

    public BackgroundQueue(String name, boolean daemon) {
        HandlerThread thread = new HandlerThread("Background-" + (name == null ? UUID.randomUUID().toString() : name));
        thread.setDaemon(daemon);
        thread.start();
        handler = new Handler(thread.getLooper());
        handler.getLooper().getThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler(name, daemon));
    }

    public void run(Runnable runnable) {
        handler.post(runnable);
    }

    public void yield() {
    }

    public Queue underlying() {
        return this;
    }

    class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler{
        String name;
        boolean daemon;

        public UncaughtExceptionHandler(String name, boolean daemon) {
            this.name = name;
            this.daemon = daemon;
        }

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            Ln.e(e, "UncaughtException-" + t.getName());
            if (t.getName().contains("Serial")){
                Queue.serial.yield();
            }
            t = new HandlerThread("Background-" + (name == null ? UUID.randomUUID().toString() : name));
            t.setDaemon(daemon);
            t.start();
            handler = new Handler(((HandlerThread)t).getLooper());
            handler.getLooper().getThread().setUncaughtExceptionHandler(this);
        }
    }
}
