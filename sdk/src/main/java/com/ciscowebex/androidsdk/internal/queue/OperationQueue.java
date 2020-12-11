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
import android.text.TextUtils;
import com.github.benoitdion.ln.Ln;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class OperationQueue {

    private static final long DEFAULT_POLL_INTERVAL = 500;
    private final Map<String, Operation> operations = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private ThreadPoolExecutor walkerExecutor;
    private QueueWorker queueWorker;

    public OperationQueue() {
        this.queueWorker = new QueueWorker(this);
    }

    @NonNull
    public QueueWorker getQueueWorker() {
        return queueWorker;
    }

    @NonNull
    public Lock getQueueLock() {
        return lock;
    }

    public void shutdown() {
        Ln.i("Shutdown OperationQueue");
        lock.lock();
        try {
            getQueueWorker().shutdown();
            getWalkerExecutor().shutdownNow();
            for (Operation op : getOperations()) {
                if (!op.getState().isDone()) {
                    Ln.d("NOT DONE : " + op);
                }
            }
            operations.clear();
        } finally {
            lock.unlock();
        }
    }

    @NonNull
    public List<Operation> getOperations() {
        lock.lock();
        try {
            List<Operation> ret = new ArrayList<>(operations.values());
            Collections.sort(ret, Operation.ascendingStartTimeComparator);
            return ret;
        } finally {
            lock.unlock();
        }
    }

    public Operation getOperation(@NonNull String operationId) {
        if (TextUtils.isEmpty(operationId)) {
            return null;
        }
        Operation ret = operations.get(operationId);
        if (ret != null) {
            return ret;
        }
        lock.lock();
        try {
            return operations.get(operationId);
        } finally {
            lock.unlock();
        }
    }

    public void submit(@NonNull Operation operation) {
        operation.setQueue(this);
        lock.lock();
        try {
            operations.put(operation.getOperationId(), operation);
            Operation.State state = operation.getState();
            if (state.isDone()) {
                this.removeOperation(operation.getOperationId());
            }
            else {
                this.walk();
            }
        } finally {
            lock.unlock();
        }
    }

    void walk() {
        getWalkerExecutor().submit(new WalkTask(this));
    }

    void removeOperation(String operationId) {
        lock.lock();
        try {
            this.operations.remove(operationId);
        }
        finally {
            lock.unlock();
        }
    }

    private boolean hasReadyTask() {
        lock.lock();
        try {
            for (Operation op : operations.values()) {
                if (op.getState() == Operation.State.READY) {
                    return true;
                }
            }
        } finally {
            lock.unlock();
        }
        Ln.i("OperationQueue idle");
        return false;
    }

    private ExecutorService getWalkerExecutor() {
        if (walkerExecutor != null && !walkerExecutor.isShutdown()) {
            return walkerExecutor;
        }
        lock.lock();
        try {
            if (walkerExecutor == null || walkerExecutor.isShutdown()) {
                ThreadFactory threadFactory = runnable -> {
                    Thread ret = new Thread(runnable);
                    ret.setDaemon(true);
                    ret.setName("QueueWalker");
                    return ret;
                };
                walkerExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1), threadFactory);
                walkerExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
            }
            return walkerExecutor;
        } finally {
            lock.unlock();
        }
    }

    private static class WalkTask implements Runnable {

        private final OperationQueue queue;
        private final Lock lock;

        WalkTask(OperationQueue queue) {
            this.queue = queue;
            this.lock = queue.getQueueLock();
        }

        @Override
        public void run() {
            do {
                long startTime = System.currentTimeMillis();
                List<Operation> operations = this.queue.getOperations();
                Ln.v("WalkQueue operation queue has %d pending operations", operations.size());
                for (Operation operation : operations) {
                    Ln.v("WalkQueue test operation: %s", operation);
                    if (operation.getState().isDone()) {
                        Ln.v("WalkQueue operation state was finished: %s", operation);
                        this.queue.removeOperation(operation.getOperationId());
                        continue;
                    }
                    lock.lock();
                    try {
                        Operation.State currentState = operation.getState();
                        Operation.State newState = currentState;
                        if (currentState == Operation.State.WAITING || currentState == Operation.State.READY) {
                            List<String> dependencie = operation.getDependsOn();
                            if (!dependencie.isEmpty()) {
                                newState = checkDependencies(dependencie);
                            }
                            Ln.v("WalkQueue operation: %s oldState: %s newState: %s", operation, currentState, newState);
                            operation.setState(newState);
                            if (operation.getState() == Operation.State.READY) {
                                Ln.v("WalkQueue operation: %s should start set state EXECUTING schedule work", operation);
                                operation.setState(Operation.State.EXECUTING);
                                queue.getQueueWorker().schedule(operation);
                            }
                        }
                    } catch (Exception e) {
                        Ln.e("WalkQueue @ exception condition: %s - %s [ Operation: %s ]", e.getClass(), e.getMessage(), operation);
                        if (!operation.getState().isDone()) {
                            operation.setState(Operation.State.FAULTED);
                        }
                    } finally {
                        lock.unlock();
                        if (operation.getState().isDone()) {
                            Ln.v("WalkQueue operation state was finished: %s", operation);
                            this.queue.removeOperation(operation.getOperationId());
                        }
                    }
                }
                long duration = System.currentTimeMillis() - startTime;
                try {
                    Thread.sleep(Math.max(0, DEFAULT_POLL_INTERVAL - duration));
                } catch (InterruptedException ignored) {
                }
            } while (queue.hasReadyTask());
        }

        private Operation.State checkDependencies(List<String> dependencie) {
            for (String dependsOnId : dependencie) {
                Operation dependsOn = this.queue.getOperation(dependsOnId);
                if (dependsOn != null && dependsOn.getState() != Operation.State.SUCCEEDED) {
                    Operation.State dependsOnState = dependsOn.getState();
                    if (dependsOnState == Operation.State.FAULTED) {
                        return Operation.State.FAULTED;
                    }
                    else if (dependsOnState == Operation.State.WAITING || dependsOnState == Operation.State.READY || dependsOnState == Operation.State.EXECUTING) {
                        return Operation.State.WAITING;
                    }
                }
            }
            return Operation.State.READY;
        }
    }

}
