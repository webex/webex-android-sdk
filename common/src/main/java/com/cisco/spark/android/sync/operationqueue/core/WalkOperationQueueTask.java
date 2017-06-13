package com.cisco.spark.android.sync.operationqueue.core;

import com.cisco.spark.android.reachability.NetworkReachability;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.util.LoggingLock;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import retrofit.RetrofitError;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

/**
 * Task that walks the OperationQueue and apply the operations. This task loops every 500ms by
 * default until all operations are idle (terminal state or PREPARING)
 */
public class WalkOperationQueueTask implements Callable<Void> {
    private final OperationQueue operationQueue;
    private final LoggingLock lock;

    protected WalkOperationQueueTask(OperationQueue operationQueue, NetworkReachability networkReachability) {
        this.operationQueue = operationQueue;
        this.lock = operationQueue.getQueueLock();
    }

    @Override
    public Void call() throws Exception {
        do {
            long startTime = System.currentTimeMillis();
            walkQueue();
            long duration = System.currentTimeMillis() - startTime;

            // Delay starting the next WalkOperationQueueTask for pollrate
            Thread.sleep(Math.max(0, operationQueue.getPollRate() - duration));
        } while (!operationQueue.isIdle());

        operationQueue.ensureQueuePolling(false);

        return null;
    }

    private void walkQueue() {
        List<Operation> operations = operationQueue.getPendingOperations();

        for (Operation operation : operations) {

            RetryPolicy retryPolicy = operation.getRetryPolicy();
            SyncState oldState = operation.getState();

            if (oldState.isTerminal()) {
                operationQueue.persist(operation);
                continue;
            }

            long timeLocked = 0;
            lock.lock();
            try {
                timeLocked = System.currentTimeMillis();
                if (!operation.getState().isTerminal() && retryPolicy.shouldBail()) {
                    operation.setErrorMessage("Operation Timed Out");
                    operation.setState(SyncState.FAULTED);
                }

                switch (operation.getState()) {
                    case PREPARING:
                        // fall through
                    case READY:
                        operation.setState(operation.onPrepare());
                        if (operation.getState() != SyncState.READY)
                            continue;

                        if (retryPolicy.shouldStart()) {
                            retryPolicy.onWorkStarted();
                            operation.setState(SyncState.EXECUTING);
                            Future<SyncState> futureSyncState = operationQueue.getWorkerService().submit(new OperationWorkTask(operation));
                            operation.syncStateFuture = futureSyncState;
                        }
                        break;
                    case EXECUTING:
                        if (operation.syncStateFuture == null)
                            operation.setState(operation.checkProgress());

                        if (operation.getState() == SyncState.EXECUTING && retryPolicy.shouldAbortAttempt()) {
                            if (operation.syncStateFuture != null && !operation.syncStateFuture.isDone()) {
                                operation.syncStateFuture.cancel(true);
                            }
                            operation.syncStateFuture = null;
                            operation.setState(SyncState.READY);
                        }
                        break;
                    default:
                        continue;

                }
            } catch (Exception e) {
                handleException(operation, e);
            } finally {
                try {
                    operation.setState(retryPolicy.validateRequestedState(operation.getState()));
                } catch (Throwable t) {
                    Ln.w(t);
                }
                lock.unlock();
                Ln.v("Checked operation " + operation);
                operation.heldLockFor(System.currentTimeMillis() - timeLocked);

                if (operation.getState().isTerminal() || oldState != operation.getState())
                    operationQueue.persist(operation);
            }
        }
        return;
    }

    class OperationWorkTask implements Callable<SyncState> {
        private final Operation operation;

        public OperationWorkTask(Operation operation) {
            this.operation = operation;
        }

        @Override
        public SyncState call() {
            try {
                Ln.v("Starting work task for " + operation);
                if (operation.getState() != SyncState.EXECUTING) {
                    Ln.i("Unexpected state [%s], aborting work task. %s", operation.getState().name(), operation);
                    return null;
                }

                SyncState syncState = operation.doWork();

                lock.lock();
                try {
                    if (operation.getState().isTerminal()) {
                        Ln.i("Operation in unexpected terminal state after doWork finished. Ignoring doWork result " + syncState + " for " + operation);
                    } else {
                        operation.setState(operation.getRetryPolicy().validateRequestedState(syncState));
                    }

                    operationQueue.persist(operation);
                    return operation.getState();
                } finally {
                    operation.syncStateFuture = null;
                    lock.unlock();
                }
            } catch (Throwable t) {
                handleException(operation, t);
            } finally {
                onFinally();
            }
            return operation.getState();
        }

        protected void onFinally() throws RuntimeException {
            Ln.v("Finished work task for " + operation);
        }
    }

    private void handleException(Operation operation, Throwable exception) {
        try {
            throw exception;
        } catch (RetrofitError e) {
            Ln.i(e, "Network error.");
            if (!operation.getState().isTerminal())
                operation.setState(operation.getRetryPolicy().validateRequestedState(SyncState.READY));
        } catch (IOException e) {
            Ln.i(e, "Network error.");
            if (!operation.getState().isTerminal())
                operation.setState(operation.getRetryPolicy().validateRequestedState(SyncState.READY));
        } catch (InterruptedException e) {
            Ln.i(e);
            if (!operation.getState().isTerminal())
                operation.setState(operation.getRetryPolicy().validateRequestedState(SyncState.READY));
        } catch (Operation.NotReadyException e) {
            Ln.i(e);
            if (!operation.getState().isTerminal())
                operation.setState(operation.getRetryPolicy().validateRequestedState(SyncState.READY));
        } catch (Throwable e) {
            Ln.e(e, "Operation  " + operation + " faulted");
            if (!operation.getState().isTerminal())
                operation.setState(SyncState.FAULTED, ConversationContract.SyncOperationEntry.SyncStateFailureReason.EXCEPTION);
        }
    }
}
