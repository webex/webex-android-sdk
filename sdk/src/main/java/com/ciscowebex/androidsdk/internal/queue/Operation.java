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
import java.util.concurrent.locks.Lock;


public abstract class Operation {

    public enum State {
        WAITING, READY, EXECUTING, FAULTED, SUCCEEDED;

        public boolean isDone() {
            return this == FAULTED || this == SUCCEEDED;
        }
    }

    public static Comparator<Operation> ascendingStartTimeComparator = new Comparator<Operation>() {

        @Override
        public int compare(Operation lhs, Operation rhs) {
            return Long.compare(lhs.startTime, rhs.startTime);
        }
    };

    private final String operationId = UUID.randomUUID().toString();
    private volatile State state = State.READY;
    private List<String> dependsOn = new ArrayList<>();
    private long startTime = System.currentTimeMillis();
    private Lock queueLock;
    private OperationQueue queue;

    @NonNull
    public final String getOperationId() {
        return operationId;
    }

    @NonNull
    public final List<String> getDependsOn() {
        return Collections.unmodifiableList(dependsOn);
    }

    public final boolean setDependsOn(@NonNull Operation dependsOnOperation) {
        if (dependsOn.contains(dependsOnOperation.getOperationId())) {
            return false;
        }
        Ln.d("setDependsOn: " + this + " depends on " + dependsOnOperation);
        queueLock.lock();
        try {
            dependsOn.add(dependsOnOperation.getOperationId());
            cycleCheck();
            if (!dependsOnOperation.getState().isDone()) {
                setState(State.WAITING);
            }
            return true;
        } catch (CycleCheckException e) {
            dependsOn.remove(dependsOnOperation.getOperationId());
            Ln.w(e, "Failed setting " + this + " depends on " + dependsOnOperation);
            return false;
        } finally {
            queueLock.unlock();
        }
    }

    @NonNull
    public final State getState() {
        return state;
    }

    @NonNull
    protected final void setState(@NonNull State state) {
        State old = this.state;
        queueLock.lock();
        try {
            old = this.state;
            this.state = state;
            if (state != old && state.isDone()) {
                for (Operation operation : queue.getOperations()) {
                    if (operation.getDependsOn().contains(getOperationId())) {
                        if (state == State.FAULTED) {
                            operation.setState(Operation.State.FAULTED);
                        }
                        queue.walk();
                    }
                }
            }
        } finally {
            queueLock.unlock();
            if (state != old) {
                onStateChanged(old);
            }
        }
    }

    final void setQueue(@NonNull OperationQueue queue) {
        this.queue = queue;
        this.queueLock = queue.getQueueLock();
    }

    public String toString() {
        return operationId + "@" + getState();
    }

    protected void onStateChanged(@NonNull State oldState) {
    }

    @NonNull
    abstract protected State doWork();

    private void cycleCheck() throws CycleCheckException {
        queueLock.lock();
        try {
            cycleCheck(getOperationId());
        } finally {
            queueLock.unlock();
        }
    }

    private void cycleCheck(String rootOperationId) throws CycleCheckException {
        for (String dependsOnId : dependsOn) {
            Operation dependsOnOp = queue.getOperation(dependsOnId);
            if (TextUtils.equals(dependsOnId, rootOperationId)) {
                throw new CycleCheckException(dependsOnId, rootOperationId);
            }
            if (dependsOnOp != null) {
                dependsOnOp.cycleCheck(rootOperationId);
            }
        }
    }

    private static class CycleCheckException extends RuntimeException {
        private static final long serialVersionUID = 3734254880311311692L;
        String a;
        String b;
        CycleCheckException(String a, String b) {
            this.a = a;
            this.b = b;
        }
        public String toString() {
            return "Cycle dependency between operations " + a + " and " + b;
        }
    }

}

