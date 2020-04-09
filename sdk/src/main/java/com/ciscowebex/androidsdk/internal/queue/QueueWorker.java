/*
 * Copyright 2016-2020 Cisco Systems Inc
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

import com.github.benoitdion.ln.Ln;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;

public class QueueWorker {

    private static long nextWorkerThreadIndex;

    private final OperationQueue queue;
    private ExecutorService executor;
    private final Lock lock;

    public QueueWorker(OperationQueue queue) {
        this.queue = queue;
        this.lock = queue.getQueueLock();
    }

    public void shutdown() {
        this.getWorkerService().shutdownNow();
    }

    public void schedule(Operation operation) {
        this.getWorkerService().submit(new WorkTask(operation));
    }

    class WorkTask implements Runnable {

        private final Operation operation;

        WorkTask(Operation operation) {
            this.operation = operation;
        }

        @Override
        public void run() {
            try {
                Ln.v("Starting work task for " + operation);
                if (operation.getState() != Operation.State.EXECUTING) {
                    Ln.d("Unexpected state [%s], aborting work task. %s", operation.getState().name(), operation);
                    return;
                }
                operation.setState(operation.doWork());
            } catch (Throwable t) {
                Throwable cause = t.getCause();
                Ln.w(t, "handleException - %s - %s (%s) [ %s ] ", t.getClass(), t.getMessage(), cause != null ? cause.getMessage() : "unknown", operation);
                operation.setState(Operation.State.FAULTED);
            } finally {
                Ln.v("Finished work task for " + operation);
                if (operation.getState().isDone()) {
                    queue.removeOperation(operation.getOperationId());
                }
            }
        }
    }

    private ExecutorService getWorkerService() {
        if (executor != null && !executor.isShutdown()) {
            return executor;
        }
        lock.lock();
        try {
            if (executor == null || executor.isShutdown()) {
                executor = Executors.newCachedThreadPool(runnable -> {
                    Thread ret = new Thread(runnable);
                    ret.setDaemon(true);
                    ret.setName("OperationWorker" + nextWorkerThreadIndex++);
                    return ret;
                });
            }
            return executor;
        } finally {
            lock.unlock();
        }
    }
}
