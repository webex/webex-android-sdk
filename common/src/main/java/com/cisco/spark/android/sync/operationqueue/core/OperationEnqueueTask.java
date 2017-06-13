package com.cisco.spark.android.sync.operationqueue.core;

import com.cisco.spark.android.util.LoggingLock;
import com.github.benoitdion.ln.Ln;

import java.util.List;
import java.util.concurrent.Callable;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

/**
 * Adds Operations to the Operation Queue including whatever work is needed to display locally.
 * <p/>
 * The high-level methods in {@link OperationQueue} for pushing operations on to the queue are safe
 * to call from the main thread. This task asynchronously enqueues new operations and tickles the
 * {@link WalkOperationQueueTask} to walk the queue.
 */
public class OperationEnqueueTask implements Callable<Void> {

    protected Operation operation;
    private LoggingLock lock;

    protected OperationQueue operationQueue;

    protected OperationEnqueueTask(OperationQueue operationQueue, Operation operation) {
        this.operationQueue = operationQueue;
        this.operation = operation;
        this.lock = operationQueue.getQueueLock();
    }

    /**
     * execute() is called from the enqueue executor. This function sets up the PendingOperation and
     * kicks off a HandlePendingOperationsTask which will walk the Operation queue from the
     * beginning.
     */
    @Override
    public Void call() {
        try {
            if (operation.getState() != SyncState.UNINITIALIZED)
                throw new IllegalStateException("Operation " + operation + " is already initialized.");

            // Add the pending activity or whatever to the database for UI representation, if needed
            // Preferably outside any locks
            SyncState state = operation.onEnqueue();

            lock.lock();
            try {
                if (state == SyncState.READY)
                    state = SyncState.PREPARING;

                operation.setState(state);

                // See if any existing tasks care about this new one
                List<Operation> ops = operationQueue.getPendingOperations();
                for (Operation op : ops) {
                    try {
                        if (op.getOperationId().equals(operation.getOperationId()))
                            continue;

                        if (op.getState().isTerminal())
                            continue;

                        op.onNewOperationEnqueued(operation);

                        if (operation.getState().isTerminal())
                            break;
                    } catch (Throwable e) {
                        Ln.w(e, "Enqueue Failed checking " + operation.getOperationId() + " against " + op.getOperationId() + " (" + op.getOperationType() + ")");
                    }
                }

                if (!operation.getState().isTerminal()) {
                    // Add our row to the SyncOperationEntry table
                    operationQueue.persist(operation);
                    operationQueue.ensureQueuePolling();
                }
            } finally {
                lock.unlock();
            }
        } catch (Throwable e) {
            Ln.e(e, "Failed handling new operation");
            operation.setState(SyncState.FAULTED);
        }

        return null;
    }

}

