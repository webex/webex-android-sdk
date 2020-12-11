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

import android.support.annotation.NonNull;
import com.github.benoitdion.ln.Ln;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SerialQueue implements Queue {

    public enum Mode {
        MAIN, BACKGROUND
    }

    public static abstract class SerialOperation implements Runnable {

        private SerialQueue queue;

        private void setQueue(SerialQueue queue) {
            this.queue = queue;
        }

        protected void done() {
            this.queue.yield();
        }

    }

    private static class PausableThreadPoolExecutor extends ThreadPoolExecutor {
        private boolean isPaused;
        private ReentrantLock pauseLock = new ReentrantLock();
        private Condition unpaused = pauseLock.newCondition();

        private PausableThreadPoolExecutor(String uuid) {
            super(1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingDeque<>(), r -> {
                Thread ret = new Thread(r);
                ret.setDaemon(true);
                ret.setName("SerialQueue-Waiting-" + uuid);
                return ret;
            });
        }

        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t, r);
            pauseLock.lock();
            try {
                while (isPaused) unpaused.await();
            } catch (InterruptedException ie) {
                t.interrupt();
            } finally {
                pauseLock.unlock();
            }
        }

        public void pause() {
            pauseLock.lock();
            try {
                isPaused = true;
            } finally {
                pauseLock.unlock();
            }
        }

        public void resume() {
            pauseLock.lock();
            try {
                isPaused = false;
                unpaused.signalAll();
            } finally {
                pauseLock.unlock();
            }
        }
    }

    private final SerialQueue.Mode type;

    private Queue queue;

    private final PausableThreadPoolExecutor picker;

    public SerialQueue(@NonNull Mode type) {
        String id = UUID.randomUUID().toString();
        this.type = type;
        this.picker = new PausableThreadPoolExecutor(id);
        if (type == Mode.MAIN) {
            queue = new MainQueue();
        } else {
            queue = new BackgroundQueue("Serial-" + id, true);
        }
    }

    public void run(Runnable task) {
        Ln.d("SerialQueue pause: %s, size: %s, task: %s", picker.isPaused, picker.getQueue().size(), task);
        picker.execute(() -> {
            Ln.d("SerialQueue execute task: %s, pause: %s, size: %s", task, picker.isPaused, picker.getQueue().size());
            picker.pause();
            queue.run(task);
        });
    }

    public void run(SerialOperation operation) {
        operation.setQueue(this);
        run((Runnable) operation);
    }

    public void yield() {
        picker.resume();
    }

    public Queue underlying() {
        return queue;
    }

}
