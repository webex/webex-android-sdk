package com.cisco.spark.android.sync.operationqueue.core;

import com.cisco.spark.android.sync.ConversationContract;
import com.github.benoitdion.ln.Ln;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

/**
 * Default retry policy for operations. Each Operation has a retryPolicy member that they can modify
 * as needed. It is persisted as part of the operation. <p/> Operations can have a maximum age and a
 * maximum number of attempts/retries. In addition, operations can configure a timeout for a single
 * attempt and specify a minimum delay between attempts. <p/> The Operations set the parameters, and
 * the Walker task takes care of the rest. <p/> The parameters are strictly enforced, i.e. the first
 * condition to fail wins
 */
public class RetryPolicy {

    // Default values:
    private int maxAttempts = 5;
    private long jobTimeout = TimeUnit.SECONDS.toMillis(60);
    private long attemptTimeout = Long.MAX_VALUE;
    private long retryDelay = TimeUnit.SECONDS.toMillis(10);
    private long initialDelay = 0;
    private long exponentialBackoffMax = 0;

    private final long startTime = System.currentTimeMillis();
    private long attemptStartedTime = startTime;
    private long timeOfNextAttempt;
    private int attemptsLeft = maxAttempts;
    private ConversationContract.SyncOperationEntry.SyncStateFailureReason failureReason;

    protected RetryPolicy() {
    }

    /**
     * Copy constructor, grabs the parameters from the input but times and attempts
     */
    public RetryPolicy(RetryPolicy in) {
        this.withMaxAttempts(in.maxAttempts)
                .withJobTimeout(in.getJobTimeout())
                .withAttemptTimeout(in.attemptTimeout)
                .withRetryDelay(in.retryDelay)
                .withInitialDelay(in.initialDelay);
        timeOfNextAttempt = startTime + initialDelay;
    }

    /**
     * Note : StartTime is not affected by the value of initialDelay
     *
     * @return time the operation was (re)started
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Setters
     */

    /**
     * Set the maximum number of attempts for the operation before faulting. An operation will
     * always be attempted at least once.
     *
     * @param attempts number of attempts
     */
    public RetryPolicy withMaxAttempts(int attempts) {
        this.attemptsLeft = attempts;
        this.maxAttempts = attempts;
        return this;
    }

    /**
     * Maximum age for the job before it faults.
     *
     * @param msTimeout Max age in milliseconds
     */
    public RetryPolicy withJobTimeout(long msTimeout) {
        return withJobTimeout(msTimeout, TimeUnit.MILLISECONDS);
    }

    public RetryPolicy withJobTimeout(long timeout, TimeUnit timeUnit) {
        jobTimeout = timeUnit.toMillis(timeout);
        return this;
    }

    /**
     * Minimum time to wait after a failed attempt before allowing another retry.
     *
     * @param msRetryDelay Delay in milliseconds. If the retry delay would cause the next attempt to
     *                   happen beyond the job timeout the operation faults.
     */
    public RetryPolicy withRetryDelay(long msRetryDelay) {
        return withRetryDelay(msRetryDelay, TimeUnit.MILLISECONDS);
    }

    public RetryPolicy withRetryDelay(long retryDelay, TimeUnit timeUnit) {
        setRetryDelay(retryDelay, timeUnit);
        return this;
    }

    public void setRetryDelay(long retryDelay, TimeUnit timeUnit) {
        this.retryDelay = timeUnit.toMillis(retryDelay);
        exponentialBackoffMax = 0;
    }

    /**
     * Maximum age for a single attempt. Use caution with asyncTasks because we have no way of
     * knowing how much work was done; a retry might cause duplicate work (resulting in duplicate
     * messages etc.)
     *
     * @param msSingleAttemptTimeout Max age in milliseconds of an attempt
     */
    public RetryPolicy withAttemptTimeout(long msSingleAttemptTimeout) {
        return withAttemptTimeout(msSingleAttemptTimeout, TimeUnit.MILLISECONDS);
    }

    public RetryPolicy withAttemptTimeout(long singleAttemptTimeout, TimeUnit timeUnit) {
        this.attemptTimeout = timeUnit.toMillis(singleAttemptTimeout);
        return this;
    }

    /**
     * Initial delay-- the operation will remain in PREPARING or READY state for at least this long
     * before proceeding to EXECUTING.
     */
    public RetryPolicy withInitialDelay(long msInitialDelay) {
        return withInitialDelay(msInitialDelay, TimeUnit.MILLISECONDS);
    }

    public RetryPolicy withInitialDelay(long initialDelay, TimeUnit timeUnit) {
        this.initialDelay = timeUnit.toMillis(initialDelay);
        timeOfNextAttempt = Math.max(timeOfNextAttempt, startTime + initialDelay);
        return this;
    }

    public RetryPolicy withExponentialBackoff() {
        return withExponentialBackoff(
                (Math.abs(new SecureRandom().nextInt()) % TimeUnit.SECONDS.toMillis(9)) + 1000,
                TimeUnit.SECONDS.toMillis(60), TimeUnit.MILLISECONDS);
    }

    public RetryPolicy withExponentialBackoff(long initialRetryDelay, long maxRetryDelay, TimeUnit timeUnit) {
        this.retryDelay = Math.max(1, timeUnit.toMillis(initialRetryDelay));
        this.exponentialBackoffMax = Math.max(this.retryDelay, timeUnit.toMillis(maxRetryDelay));
        return this;
    }

    /**
     * The following functions are called by the queue Walker to manage the states
     */

    public void onWorkStarted() {
        attemptStartedTime = System.currentTimeMillis();
        attemptsLeft--;
        Ln.d(startTime + " : Operation attempt started at " + attemptStartedTime);
    }

    public boolean shouldAbortAttempt() {
        if (System.currentTimeMillis() - attemptStartedTime >= attemptTimeout) {
            Ln.d(startTime + " : Aborting attempt because it's more than " + attemptTimeout + " ms old");
            return true;
        }
        return false;
    }

    public boolean shouldBail() {
        if (System.currentTimeMillis() - startTime >= jobTimeout) {
            Ln.d(startTime + " : Bailing because operation is more than " + jobTimeout + " ms old");
            failureReason = ConversationContract.SyncOperationEntry.SyncStateFailureReason.TIMED_OUT;
            return true;
        }

        if (shouldAbortAttempt() && attemptsLeft <= 0) {
            Ln.d(startTime + " : Bailing because operation is out of retries");
            failureReason = ConversationContract.SyncOperationEntry.SyncStateFailureReason.NO_MORE_RETRIES;
            return true;
        }

        return false;
    }

    public SyncState validateRequestedState(SyncState state) {
        if (state.isTerminal())
            return state;

        if (shouldBail())
            return SyncState.FAULTED;

        if (startTime + initialDelay > System.currentTimeMillis()) {
            return SyncState.PREPARING;
        }

        switch (state) {
            case READY:
                if (timeOfNextAttempt <= System.currentTimeMillis())
                    return onFailedAttempt();
                // else wait politely for the next attempt
                break;
            case EXECUTING:
                if (shouldAbortAttempt())
                    return onFailedAttempt();
                break;
        }
        return state;
    }

    public SyncState onFailedAttempt() {
        Ln.d("onFailedAttempt");
        timeOfNextAttempt = System.currentTimeMillis() + retryDelay;
        attemptStartedTime = timeOfNextAttempt;

        if (exponentialBackoffMax > 0) {
            retryDelay = Math.min(exponentialBackoffMax, retryDelay * 2);
        }

        if ((timeOfNextAttempt - startTime) >= jobTimeout) {
            Ln.d(startTime + " : Operation faulting because job timed out");
            failureReason = ConversationContract.SyncOperationEntry.SyncStateFailureReason.TIMED_OUT;
            return SyncState.FAULTED;
        }
        if (attemptsLeft <= 0) {
            Ln.d(startTime + " : Operation faulting because no retries left");
            failureReason = ConversationContract.SyncOperationEntry.SyncStateFailureReason.NO_MORE_RETRIES;
            return SyncState.FAULTED;
        }
        Ln.d(startTime + " : Next attempt at " + timeOfNextAttempt + " ; " + attemptsLeft + " retries left");

        return SyncState.READY;
    }

    public boolean shouldStart() {
        boolean ret = attemptsLeft > 0 && System.currentTimeMillis() >= timeOfNextAttempt;
        if (!ret)
            Ln.v(this.toString());
        return ret && !shouldBail();
    }

    public void scheduleNow() {
        timeOfNextAttempt = 0;
    }

    public long getJobTimeout() {
        return jobTimeout;
    }

    @Override
    public String toString() {
        long now = System.currentTimeMillis();
        return "attemptsLeft:" + attemptsLeft + " next try in " + (Math.max(0, timeOfNextAttempt - now)) + "ms jobTimeout in " + (Math.max(0, startTime + jobTimeout - now)) + "ms";
    }

    public ConversationContract.SyncOperationEntry.SyncStateFailureReason getFailureReason() {
        return failureReason;
    }

    public long getInitialDelay() {
        return initialDelay;
    }

    public static RetryPolicy newLimitAttemptsPolicy() {
        return newLimitAttemptsPolicy(5);
    }

    public static RetryPolicy newLimitAttemptsPolicy(int numAttempts) {
        RetryPolicy retryPolicy = new RetryPolicy()
                .withMaxAttempts(numAttempts)
                .withRetryDelay(5, TimeUnit.SECONDS)
                .withJobTimeout(24, TimeUnit.HOURS)
                .withAttemptTimeout(24, TimeUnit.HOURS);
        return retryPolicy;
    }

    public static RetryPolicy newJobTimeoutPolicy(long timeout, TimeUnit timeUnit) {
        RetryPolicy retryPolicy = newLimitAttemptsPolicy(20)
                .withJobTimeout(timeout, timeUnit);
        return retryPolicy;
    }

    public long getRetryDelay() {
        return retryDelay;
    }

    public long getExponentialBackoffMax() {
        return exponentialBackoffMax;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public long getTimeOfNextAttempt() {
        return timeOfNextAttempt;
    }

    public long getAttemptTimeout() {
        return attemptTimeout;
    }

    public long getAttemptStartedTime() {
        return attemptStartedTime;
    }

    public int getAttemptsLeft() {
        return attemptsLeft;
    }

}
