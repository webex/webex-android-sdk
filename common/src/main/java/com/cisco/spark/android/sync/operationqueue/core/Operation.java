package com.cisco.spark.android.sync.operationqueue.core;

import android.content.ContentResolver;
import android.database.Cursor;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.reachability.NetworkReachability;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.util.Clock;
import com.cisco.spark.android.util.LoggingLock;
import com.cisco.spark.android.util.Strings;
import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

import javax.inject.Inject;
import javax.inject.Provider;

import dagger.Lazy;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncStateFailureReason;
import static com.cisco.spark.android.sync.ConversationContract.vw_PendingSyncOperations;

/**
 * Base class for the serializable Operations that go in the OperationQueue
 */
public abstract class Operation {

    @Inject
    transient Provider<Batch> batchProvider;

    @Inject
    transient NetworkReachability networkReachability;

    @Inject
    transient protected OperationQueue operationQueue;

    @Inject
    transient Ln.Context lnContext;

    @Inject
    transient Lazy<ContentResolver> contentResolver;

    @Inject
    transient Clock clock;

    /**
     * Transient fields are not serialized.
     */
    transient private Condition isEnqueuedCondition;
    transient private Condition isTerminalStateCondition;
    transient Future<SyncState> syncStateFuture;

    transient private HashMap<SyncState, Long> stateDurations;
    transient private long timeLastStateTransition;
    transient private int deferrals, attempts;
    transient private long heldLockFor;
    transient private LoggingLock queueLock;
    transient protected NaturalLog ln;

    /**
     * Serialized fields, persistent across app instances
     */
    private volatile SyncState state = SyncState.UNINITIALIZED;
    private volatile SyncStateFailureReason failureReason = SyncStateFailureReason.UNKNOWN;
    private final String operationId = UUID.randomUUID().toString();
    private String errorMessage = null;
    private ArrayList<String> dependsOn = new ArrayList<>();
    private RetryPolicy retryPolicy = buildRetryPolicy();
    private boolean isDirty;
    protected transient Injector injector;

    public String getJson(Gson gson) {
        queueLock.lock();
        try {
            return gson.toJson(this);
        } catch (Exception e) {
            ln.e(new JsonIOException("Failed serializing operation [" + getOperationType().ordinal() + "]", e));
        } finally {
            queueLock.unlock();
        }

        return null;
    }

    /**
     * isSafeToRemove determines if there is any reason for this operation to go on existing in the
     * queue. It is called by the operation queue as a check before deleting the operation.
     *
     * In the simple case, an operation is safe to remove if it has reached a terminal state.
     *
     * More complex cases exist if this operation is part of a dependency chain. Some operations
     * remain in the queue after they fail, for example a user may 'retry' sending a failed message.
     * If such an operation depends on this one, both should remain in the queue after a failure for
     * the depending operation is to succeed on restart.
     *
     * This function checks for directly or indirectly depending operations that are not safe to
     * remove.
     *
     * Also see OperationQueue.restartOperation. Note that as part of restarting an operation, its
     * depended operations are also restarted.
     *
     * @return True if this operation should be discarded from the queue. It has either succeeded or
     * it has failed and there's no reason to restart it.
     */
    @CallSuper
    public boolean isSafeToRemove() {
        if (!state.isTerminal())
            return false;

        if (state == SyncState.SUCCEEDED)
            return true;

        if (state == SyncState.FAULTED && failureReason == SyncStateFailureReason.CANCELED)
            return true;

        queueLock.lock();
        try {
            cycleCheck();
            List<Operation> ops = operationQueue.getPendingOperations();
            for (Operation op : ops) {
                if (op.dependsOn.contains(getOperationId()) && !op.isSafeToRemove()) {
                    return false;
                }
            }
        } catch (Exception e) {
            Ln.e(e);
        } finally {
            queueLock.unlock();
        }
        return true;
    }

    public Operation(Injector injector) {
        initialize(injector);
    }

    public void initialize(Injector injector) {
        this.injector = injector;
        injector.inject(this);
        stateDurations = new HashMap<>();
        timeLastStateTransition = System.currentTimeMillis();
        queueLock = operationQueue.getQueueLock();
        isEnqueuedCondition = queueLock.newCondition();
        isTerminalStateCondition = queueLock.newCondition();
        ln = Ln.get(lnContext, "Operation");
    }

    // Interface

    /**
     * Implementing classes return their type
     *
     * @return a value from the OperationType enum
     */
    @NonNull
    public abstract OperationType getOperationType();

    /**
     * onEnqueue is called once when the Operation is added to the OperationQueue. Implementing
     * classes can use this callback to add outgoing message content to the database for immediate
     * display for example.
     *
     * Called from a worker thread (OperationQueue.workerExecutor).
     */
    @NonNull
    abstract protected SyncState onEnqueue();

    /**
     * onRestart is called when an operation is restarted. Implementing classes should use this to
     * setup anything that needs to be done prior to the operation being restarted.
     *
     * Called from a worker thread (OperationQueue.workerExecutor).
     */
    @NonNull
    protected SyncState onRestart() {
        return SyncState.READY;
    }

    /**
     * doWork is called each time the task (re)tries. Called from a worker thread
     * (OperationQueue.walkerExecutor).
     *
     * @return The operation's desired state after doing the work. See {
     * SyncOperationEntry.SyncState} The operation walker will validate this state against the
     * operation's retry policy and call the operation's setState() with the result.
     */
    @NonNull
    abstract protected SyncState doWork() throws IOException;

    /**
     * Called on every running operation after a new operation is added to the OperationQueue. This
     * is useful for setting up dependent relationships between operations.
     *
     * For example, if an AvatarUpdateOperation is submitted when a another one is still in the
     * queue, the earlier operation will change its own state to FAULTED (CANCELED) so the later one
     * wins.
     *
     * Example 2: If a message is posted in a conversation that has not yet been created on the
     * server, the NewConversationOperation adds itself to the message operation's dependencies.
     *
     * For cases where a new operation should be skipped because identical work is already underway,
     * isOperationRedundant() is preferred because it avoids adding the new operation to the queue.
     */
    protected void onNewOperationEnqueued(Operation newOperation) {
    }

    /**
     * This method is called on every operation in the queue _before_ a new one is added. This
     * method covers the specific case where the new operation is redundant because an existing
     * operation is already doing the work. This function is preferred when the check is safe for
     * the main thread.
     *
     * This function is called as part of creating the new operation. If an operation is 'redundant'
     * then the new operation is junked (will never be enqueued) and the existing one is returned to
     * the caller. This saves cycles and allows the caller to monitor state without doing an
     * additional lookup.
     *
     * To use, override this function and return true if the passed-in operation is redundant.
     *
     * If any operation returns true, the redundancy check ends and no further calls are made.
     *
     * May be called from the main thread.
     *
     * @param newOperation The operation being added
     * @return True if newOperation should be junked because it is redundant
     */
    public boolean isOperationRedundant(Operation newOperation) {
        return false;
    }

    /**
     * Method called instead of doWork() if the operation is in the EXECUTING state. The operation
     * might take the opportunity to check for completion or other bookkeeping.
     *
     * @return The operation's desired state. See doWork()
     */
    @NonNull
    protected SyncState checkProgress() {
        return getState();
    }

    // getters

    /**
     * Get the object's temporary guid for referencing the operation and any activity, conversation,
     * etc. being created by this operation.
     *
     * @return A unique identifier, currently in guid form but not guaranteed.
     */
    @NonNull
    public String getOperationId() {
        return operationId;
    }

    /**
     * Get the operation's state.
     *
     * @return One of { SyncOperationEntry.SyncState SyncState}
     */
    @NonNull
    public final SyncState getState() {
        // Intentionally not locking here. This should always be safe to call from the main thread.
        return state;
    }

    /**
     * Get the operation's failure reason.
     *
     * @return One of { SyncOperationEntry.SyncStateFailureReason SyncState}
     */
    public SyncStateFailureReason getFailureReason() {
        return failureReason == null ? retryPolicy.getFailureReason() : failureReason;
    }

    /**
     * Set the operation's Failure Reason.
     *
     * @param failureReason One of { SyncOperationEntry.SyncStateFailureReason
     *                      SyncStateFailureReason}
     */
    protected void setFailureReason(SyncStateFailureReason failureReason) {
        if (failureReason == null)
            failureReason = SyncStateFailureReason.UNKNOWN;

        if (failureReason == this.failureReason)
            return;

        this.failureReason = failureReason;
        setDirty(true);
    }

    /**
     * This call is how an operation gets from the PREPARING to the READY state. If an operation
     * returns PREPARING, no further callbacks will be called. In the default implementation an
     * operation returns READY if:
     *
     * - it is not in a terminal state (SyncState.isTerminal())  AND
     *
     * - if the operation requires the network, the device has network connectivity  AND
     *
     * - all depended operations are successful.
     *
     * If a depended operation state is FAULTED, we return FAULTED here with a DEPENDENCY fail
     * code.
     *
     * For RetryPolicy purposes, returning PREPARING here means that no attempts will be recorded;
     * it will not count against the retry limit.
     *
     * Note that if all operations are idle the queue servicer goes to sleep. The servicer wakes up
     * if new operations are enqueued, if the network comes online, or if something else tickles
     * OperationQueue.ensureQueuePolling
     *
     * @return The operation's desired state, which will be validated against the retry policy and
     * set by the servicer
     */
    @CallSuper
    @NonNull
    public SyncState onPrepare() {
        if (getState().isTerminal())
            return getState();

        if (needsNetwork() && !networkReachability.isNetworkConnected()) {
            ln.d("Operation " + this + " needs network. Deferring");
            deferrals++;
            return SyncState.PREPARING;
        }

        if (!getRetryPolicy().shouldStart()) {
            return SyncState.PREPARING;
        }

        for (String dependsOnId : getDependsOn()) {
            Operation dependsOn = operationQueue.get(dependsOnId);

            if (dependsOn == null || dependsOn.getState() == SyncState.SUCCEEDED)
                continue;

            switch (dependsOn.getState()) {
                case FAULTED:
                    if (!getState().isTerminal())
                        setState(dependsOn.getState(), SyncStateFailureReason.DEPENDENCY);
                    ln.d("Ending operation %s because it depends on %s", toString(), dependsOn.toString());
                    return SyncState.FAULTED;
                case UNINITIALIZED:
                case READY:
                case EXECUTING:
                case PREPARING:
                    ln.d("Skipping operation %s because it depends on %s", toString(), dependsOn.toString());
                    deferrals++;
                    return SyncState.PREPARING;
                case SUCCEEDED:
                    continue;
            }
        }

        if (getState() == SyncState.PREPARING)
            return SyncState.READY;

        return getState();
    }

    /**
     * Add an operation that must succeed before this operation can do work. For example, there's no
     * point trying to post activities to a provisional conversation that is not created on the
     * server yet.
     *
     * If the parent operation enters the  { SyncOperationEntry.SyncState#FAULTED FAULTED} state,
     * its dependent operations automatically fault as well.
     *
     * @param dependsOnOperation The operation that must finish before this one starts.
     */
    @CallSuper
    public void setDependsOn(Operation dependsOnOperation) {
        if (dependsOn.contains(dependsOnOperation.getOperationId()))
            return;

        ln.d("setDependsOn: " + this + " depends on " + dependsOnOperation);

        queueLock.lock();
        try {
            dependsOn.add(dependsOnOperation.getOperationId());

            cycleCheck();

            if (getState() == SyncState.READY) {
                setState(SyncState.PREPARING);
            }
        } catch (CycleCheckException e) {
            dependsOn.remove(dependsOnOperation.getOperationId());
            ln.w(e, "Failed setting " + this + " depends on " + dependsOnOperation);
            throw e;
        } finally {
            queueLock.unlock();
        }
    }

    @CallSuper
    public void setDependsOn(List<String> operationIds) {
        ln.d("setDependsOn: " + this + " depends on " + Strings.join(", ", operationIds));

        queueLock.lock();
        try {
            operationIds.removeAll(dependsOn);
            dependsOn.addAll(operationIds);

            cycleCheck();

            if (getState() == SyncState.READY) {
                setState(SyncState.PREPARING);
            }
        } catch (CycleCheckException e) {
            dependsOn.removeAll(operationIds);
            ln.w(e, "Failed setting " + this + " depends on " + Strings.join(", ", operationIds));
            throw e;
        } finally {
            queueLock.unlock();
        }
    }

    /**
     * Get a list of operations that must preceed this operation.
     *
     * @return The operations
     */
    final public List<String> getDependsOn() {
        return Collections.unmodifiableList(dependsOn);
    }

    /**
     * Factory method to deserialize an Operation from the database.
     *
     * @param cursor A cursor for { com.cisco.spark.android.sync.ConversationContract.vw_PendingSyncOperations
     *               vw_PendingSyncOperations} with the default projection
     * @return The deserialized operation
     */
    static Operation fromCursor(Cursor cursor, Gson gson) {
        OperationType type = OperationType.values()[cursor.getInt(vw_PendingSyncOperations.OPERATION_TYPE.ordinal())];
        String data = cursor.getString(vw_PendingSyncOperations.DATA.ordinal());
        if (TextUtils.isEmpty(data)) {
            Ln.e(new JsonSyntaxException("Empty operation data for type " + type));
            return null;
        }

        try {
            Operation ret = gson.fromJson(data, type.operationClass);
            ret.state = SyncState.values()[cursor.getInt(vw_PendingSyncOperations.SYNC_STATE.ordinal())];
            if (ret.state == SyncState.EXECUTING)
                ret.state = SyncState.READY;
            ret.ln = Ln.get(ret.lnContext, "Operation");

            Ln.d("Inflated " + ret + " from persisted operations");
            return ret;
        } catch (JsonSyntaxException e) {
            Ln.e(new JsonSyntaxException("Operation failed to inflate ["
                    + type.ordinal())
                    + "] "
                    + data.substring(0, Math.min(20, data.length() - 1)).replaceAll("[[A-Z][a-z][0-9]]", "x"), e);
            Ln.v("Failed operation data: " + cursor.getString(vw_PendingSyncOperations.DATA.ordinal()));
        }
        return null;
    }

    /**
     * Setter for the operation's state. Only called by the operationqueue.core classes.
     *
     * @param newState one of { SyncOperationEntry.SyncState SyncState}
     * @return the new state
     */
    @NonNull
    final SyncState setState(SyncState newState) {
        if (newState == null) {
            throw new IllegalArgumentException("Operation state cannot be null");
        }

        SyncState oldState = this.state;

        queueLock.lock();
        try {
            oldState = this.state;
            this.state = newState;

            if (newState != SyncState.FAULTED)
                failureReason = null;

            if (newState != oldState || isCanceled()) {
                ln.d("Operation " + this + " " + " <= " + oldState
                        + (newState == SyncState.FAULTED && failureReason != null ? " / " + failureReason : "")
                        + (newState == SyncState.FAULTED && errorMessage != null ? " : " + errorMessage : ""));
            }

            // Optimization to avoid churning when setting state is redundant. Note canceling is special
            // because additional cleanup may be required even if we were already in a faulted state
            if (newState == oldState && !isCanceled()) {
                return newState;
            }

            recordStateTransitionDuration(oldState, newState);

            // Notify the condition if we enqueue OR restart the operation
            if (oldState == SyncState.UNINITIALIZED || (oldState == SyncState.FAULTED && newState != SyncState.FAULTED)) {
                isEnqueuedCondition.signalAll();
            }

            if (getState() == SyncState.FAULTED) {
                for (Operation operation : operationQueue.getPendingOperations()) {
                    if (operation.getDependsOn().contains(getOperationId())) {
                        operation.setErrorMessage("Dependency error: " + getOperationInfo());
                        operation.setState(SyncState.FAULTED, SyncStateFailureReason.DEPENDENCY);
                    }
                }
            }

            if (newState.isIdle())
                operationQueue.signalIdleIfNeeded();

            if (!oldState.isTerminal() && state.isTerminal()) {
                isTerminalStateCondition.signalAll();
            }

            if (oldState != newState || isCanceled()) {
                if (oldState == SyncState.UNINITIALIZED
                        || oldState == SyncState.EXECUTING
                        || newState.isTerminal())

                    setDirty(true);
            }
        } finally {
            queueLock.unlock();

            if (getState() != oldState || failureReason == SyncStateFailureReason.CANCELED)
                onStateChanged(oldState);
        }

        return newState;
    }

    /**
     * Setter for the operation's state.
     *
     * @param state         one of { SyncOperationEntry.SyncState SyncState}
     * @param failureReason one of { SyncOperationEntry.SyncState SyncStateFailureReason}
     * @return the new state
     */
    @NonNull
    final SyncState setState(@NonNull SyncState state, SyncStateFailureReason failureReason) {
        if (state == null) {
            try {
                throw new IllegalArgumentException("State cannot be set to null");
            } catch (Exception e) {
                ln.w(e);
                return state;
            }
        }

        queueLock.lock();
        try {
            if (this.failureReason == failureReason && getState() == state)
                return getState();

            setFailureReason(failureReason);
            return setState(state);
        } finally {
            queueLock.unlock();
        }
    }

    /**
     * Remove an operation from the queue and from persistent storage, even if
     * !operation.isSafeToRemove()
     */
    final public void cancel() {
        queueLock.lock();
        try {
            if (isCanceled())
                return;

            setDirty(true);

            if (getState() != SyncState.SUCCEEDED) {
                setState(SyncState.FAULTED, SyncStateFailureReason.CANCELED);
                if (syncStateFuture != null)
                    syncStateFuture.cancel(false);
            }
        } finally {
            queueLock.unlock();
            operationQueue.ensureQueuePolling();
        }
    }

    /**
     * Override onStateChanged to implement any special handling for example to post metrics if the
     * operation fails. Guaranteed to only be called if getState() != oldState
     *
     * @param oldState The state we are moving from
     */
    protected void onStateChanged(SyncState oldState) {
    }

    /**
     * This function keeps track of how long the operation has spent in each state for logging and
     * (eventually) metrics
     */
    private void recordStateTransitionDuration(SyncState oldState, SyncState newState) {
        Long existingDuration = stateDurations.get(oldState);
        long now = System.currentTimeMillis();
        long timeToAdd = now - timeLastStateTransition;
        if (existingDuration == null) {
            stateDurations.put(oldState, timeToAdd);
        } else {
            stateDurations.put(oldState, existingDuration + timeToAdd);
        }
        timeLastStateTransition = now;

        if (newState == SyncState.EXECUTING)
            attempts++;

        if (newState == SyncState.FAULTED && getFailureReason() == SyncStateFailureReason.CANCELED) {
            // don't log these, too chatty
        } else if (newState.isTerminal()) {
            // TODO this should also be a metric
            StringBuilder sb = new StringBuilder(toString())
                    .append(" ")
                    .append(" Duration: ");

            for (SyncState iterstate : SyncState.values()) {
                if (!stateDurations.containsKey(iterstate))
                    continue;

                sb.append(iterstate.name())
                        .append(":")
                        .append(stateDurations.get(iterstate))
                        .append("ms ");
            }

            if (deferrals > 0) {
                sb
                        .append(": ")
                        .append(deferrals)
                        .append(" deferrals ");
            }
            if (attempts > 1) {
                sb
                        .append(": ")
                        .append(attempts)
                        .append(" attempts ");
            }
            if (heldLockFor > 1000) {
                sb
                        .append(": locked ")
                        .append(heldLockFor)
                        .append("ms");
            }
            Ln.get(lnContext, "OperationSummary").i(sb.toString());
        }
    }

    public long getTotalDuration() {
        long ret = 0;
        for (SyncState state : SyncState.values()) {
            if (!state.isTerminal() && stateDurations.containsKey(state)) {
                ret += stateDurations.get(state);
            }
        }
        return ret;
    }

    public void heldLockFor(long l) {
        heldLockFor += l;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        ln.d("Error: " + errorMessage + " " + this);
        this.errorMessage = errorMessage;
    }

    public static Comparator<Operation> ascendingStartTimeComparator = new Comparator<Operation>() {

        @Override
        public int compare(Operation lhs, Operation rhs) {
            if (lhs.getDependsOn().contains(rhs.getOperationId()))
                return 1;

            if (rhs.getDependsOn().contains(lhs.getOperationId()))
                return -1;

            if (lhs.getRetryPolicy().getStartTime() == rhs.getRetryPolicy().getStartTime())
                return 0;

            return lhs.getRetryPolicy().getStartTime() > rhs.getRetryPolicy().getStartTime() ? 1 : -1;
        }
    };

    /**
     * Block until the operation is successfully enqueued.
     *
     * @param msTimeout max time to wait, ms
     * @return the operation state
     */
    @NonNull
    public SyncState waitUntilEnqueued(long msTimeout) {
        queueLock.lock();
        try {
            // UNINITIALIZED is the normal case but FAULTED ops can be restarted
            if (getState() == SyncState.UNINITIALIZED || getState() == SyncState.FAULTED) {
                boolean succeeded = isEnqueuedCondition.await(msTimeout, TimeUnit.MILLISECONDS);
                if (!succeeded) {
                    ln.e("FAILED waiting for operation " + this + " to enqueue");
                }
                return getState();
            }
        } catch (InterruptedException e) {
            ln.w(e, "Wait interrupted.");
        } finally {
            queueLock.unlock();
        }
        return getState();
    }

    /**
     * Block until the operation reaches a terminal state (succeeded / faulted) or timeout.
     *
     * @param msTimeout max time to wait, ms
     * @return the operation state, might not be terminal if we timed out.
     */
    @NonNull
    public SyncState waitForTerminalState(long msTimeout) {
        queueLock.lock();
        try {
            if (!getState().isTerminal()) {
                boolean succeeded = isTerminalStateCondition.await(msTimeout, TimeUnit.MILLISECONDS);
                if (!succeeded) {
                    ln.e("FAILED waiting for operation " + this + " to complete (timeout %s ms)", msTimeout);
                }
                return getState();
            }
        } catch (InterruptedException e) {
            ln.w(e, "Wait interrupted.");
        } finally {
            queueLock.unlock();
        }
        return getState();
    }

    protected String getOperationInfo() {
        return getOperationType() + " " + getState() + (getState() == SyncState.FAULTED ? "/" + getFailureReason() : "");
    }

    public String toString() {
        return operationId + " " + getOperationInfo();
    }

    protected Batch newBatch() {
        return batchProvider.get();
    }

    protected ContentResolver getContentResolver() {
        return contentResolver.get();
    }

    /**
     * @return True if the operation requires a network to complete. This is usually the case but
     * can be overridden.
     *
     * For RetryPolicy purposes, returning false here does not count as an attempt against the retry
     * limit.
     */
    public boolean needsNetwork() {
        return true;
    }

    public boolean shouldPersist() {
        return requiresAuth();
    }

    public boolean requiresAuth() {
        return true;
    }

    @NonNull
    public final RetryPolicy getRetryPolicy() {
        if (retryPolicy == null)
            retryPolicy = buildRetryPolicy();

        return retryPolicy;
    }

    public final void setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    public final void resetRetryPolicy() {
        this.retryPolicy = null;
    }

    /**
     * Override this method in operations to customize the retry policy
     *
     * @return A new RetryPolicy
     */
    @NonNull
    public abstract RetryPolicy buildRetryPolicy();

    /**
     * NotReadyException is thrown by an operation if in the process of executing it discovers it
     * cannot continue until some other condition happens. It is the responsibility of the throwing
     * operation to ensure any new depended operations are enqueued.
     *
     * It likely indicates a timing problem because prerequisites of onPrepare() have already been
     * satisfied.
     */
    public static class NotReadyException extends RuntimeException {
        public NotReadyException(String detailMessage) {
            super(detailMessage);
        }
    }

    // Check dependsOn for dependency cycles
    private void cycleCheck() {
        queueLock.lock();
        try {
            cycleCheck(getOperationId());
        } finally {
            queueLock.unlock();
        }
    }

    private void cycleCheck(String rootOperationId) {
        for (String dependsOnId : dependsOn) {
            Operation dependsOnOp = operationQueue.get(dependsOnId);

            if (TextUtils.equals(dependsOnId, rootOperationId))
                throw new CycleCheckException(dependsOnId, rootOperationId);

            if (dependsOnOp != null)
                dependsOnOp.cycleCheck(rootOperationId);
        }
    }

    public class CycleCheckException extends RuntimeException {

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

    final public boolean isCanceled() {
        return getState() == SyncState.FAULTED && getFailureReason() == SyncStateFailureReason.CANCELED;
    }

    final public boolean isSucceeded() {
        return getState() == SyncState.SUCCEEDED;
    }

    boolean isDirty() {
        return isDirty;
    }

    void setDirty(boolean dirty) {
        if (this.isDirty != dirty)
            this.isDirty = dirty;
    }

    final public long getMsUntilStateChange() {
        long ret = Long.MAX_VALUE;

        SyncState state = getState();
        if (state.isPreExecute()) {
            ret = Math.max(0, retryPolicy.getTimeOfNextAttempt() - clock.now());
        } else if (state == SyncState.EXECUTING) {
            ret = retryPolicy.getAttemptStartedTime() + retryPolicy.getAttemptTimeout() - clock.now();
        }

        if (ret == 0 && onPrepare() == SyncState.PREPARING) {
            ret = Long.MAX_VALUE;
        }

        return ret;
    }
}
