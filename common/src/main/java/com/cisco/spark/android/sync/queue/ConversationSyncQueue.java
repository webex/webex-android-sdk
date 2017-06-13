package com.cisco.spark.android.sync.queue;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Process;

import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.authenticator.LogoutEvent;
import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.events.KmsKeyEvent;
import com.cisco.spark.android.mercury.events.KeyPushEvent;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.util.LoggingLock;
import com.cisco.spark.android.util.ThrottledAsyncTask;
import com.github.benoitdion.ln.Ln;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

import de.greenrobot.event.EventBus;

import static com.cisco.spark.android.sync.ConversationContract.ActivityEntry;
import static com.cisco.spark.android.util.CollectionUtils.containsInstanceOfType;

public class ConversationSyncQueue {

    private static final int PRIORITY_THREADPOOL_SIZE = 5;

    private long highWaterMark = 0;

    private final ContentResolver contentResolver;
    private final ActivitySyncQueue activitySyncQueue;
    private ApplicationController applicationController;
    private final Injector injector;

    protected LoggingLock synclock;
    private Condition idle;
    private boolean isConversationListComplete;

    public ConversationSyncQueue(ContentResolver contentResolver,
                                 EventBus bus,
                                 ActivitySyncQueue activitySyncQueue,
                                 Injector injector) {
        this.contentResolver = contentResolver;
        this.activitySyncQueue = activitySyncQueue;
        this.injector = injector;

        synclock = new LoggingLock(BuildConfig.DEBUG, "ConversationSyncQueue Lock");
        idle = synclock.newCondition();

        priorityTaskExecutor = getPriorityExecutor();

        if (bus != null)
            bus.register(this);
    }

    private PriorityBlockingQueue<SyncTask> taskQ = new PriorityBlockingQueue<SyncTask>(3, new Comparator<SyncTask>() {
        @Override
        public int compare(SyncTask rhs, SyncTask lhs) {
            // Newest priority requests go to the front
            if (rhs.timeCreated > lhs.timeCreated)
                return -1;

            if (rhs.timeCreated < lhs.timeCreated)
                return 1;

            return 0;
        }
    });

    private ExecutorService priorityTaskExecutor;

    private static int nextId = 0;

    protected ExecutorService getPriorityExecutor() {
        synclock.lock();
        try {
            if (priorityTaskExecutor == null || priorityTaskExecutor.isShutdown()) {
                priorityTaskExecutor = Executors.newFixedThreadPool(PRIORITY_THREADPOOL_SIZE, new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable runnable) {
                        Thread ret = new Thread(runnable);
                        ret.setDaemon(true);
                        ret.setName("Priority task executor " + (nextId++));
                        return ret;
                    }
                });
            }
            return priorityTaskExecutor;
        } finally {
            synclock.unlock();
        }
    }

    /**
     * Submit a high-priority task to the queue. Newer high-priority tasks are handled before older
     * ones. Intended for use in response to some intentional user action.
     *
     * @param task the task to submit
     */
    public Future<Void> submitPriorityTask(SyncTask task) {
        synclock.lock();
        try {
            /**
             * {@link PushSyncTask} consumes all the work on the ActivitySyncQueue. One on the task queue is enough.
             */
            if (!(task instanceof PushSyncTask && containsInstanceOfType(taskQ, PushSyncTask.class))) {
                taskQ.add(task);
                pendingTasks.add(task);
            }

            if (taskQ.size() > 10) {
                Ln.w("WARNING the priority queue has " + taskQ.size() + " entries, probably too many.");
                //TODO This can be caused by a user rapidly scooting around the UI doing lots of things.
                //     Should we remove old tasks from the queue since the user has probably moved on to something else?
            }

            Ln.v("Submitting priority task " + task + " queue size " + taskQ.size());

            return getPriorityExecutor().submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    ArrayList<SyncTask> tasksToExecute = new ArrayList<SyncTask>();
                    synclock.lock();
                    try {
                        while (!taskQ.isEmpty()) {
                            tasksToExecute.add(taskQ.take());
                        }
                    } finally {
                        synclock.unlock();
                    }

                    for (SyncTask syncTask : tasksToExecute) {
                        //TODO handle failures and retries if needed
                        syncTask.call();
                    }
                    return null;
                }
            });
        } finally {
            synclock.unlock();
        }
    }

    public void clear() {
        synclock.lock();
        try {
            incomingKeys.clear();
            if (priorityTaskExecutor != null) {
                priorityTaskExecutor.shutdownNow();
                priorityTaskExecutor = null;
            }
            highWaterMark = 0;
            isConversationListComplete = false;
            pendingTasks.clear();
            idle.signalAll();
        } finally {
            synclock.unlock();
        }
    }

    public boolean isConversationListComplete() {
        return isConversationListComplete;
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventBackgroundThread(ActivitySyncQueue.ActivitySyncQueueUpdatedEvent event) {
        submitPriorityTask(getPushSyncTask());
    }

    LinkedBlockingQueue<KeyObject> incomingKeys = new LinkedBlockingQueue<>();
    ThrottledAsyncTask keyEventThrottler = new ThrottledAsyncTask() {
        @Override
        protected void doInBackground() {
            HashSet<KeyObject> keySet = new HashSet<>();
            incomingKeys.drainTo(keySet);
            if (!keySet.isEmpty())
                submitPriorityTask(getKeyPushEventTask(new ArrayList<>(keySet)));
        }
    };

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public synchronized void onEvent(KmsKeyEvent event) {
        Ln.d("Received keys via kmsMessage");
        if (event.getKeys() == null || event.getKeys().isEmpty()) {
            return;
        }
        incomingKeys.addAll(event.getKeys());
        keyEventThrottler.scheduleExecute();
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public synchronized void onEvent(KeyPushEvent event) {
        if (event.getKeys() == null || event.getKeys().isEmpty())
            return;

        Ln.d("KeyPushEvent with " + event.getKeys().size() + " keys");
        incomingKeys.addAll(event.getKeys());
        keyEventThrottler.scheduleExecute();
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public synchronized void onEvent(LogoutEvent event) {
        clear();
    }

    /*********
     *
     * TASKS
     *
     */

    /**
     * Push Sync Task : Handle some activities from the incoming queue (GCM/Mercury)
     *
     * @return the task
     */
    public IncrementalSyncTask getPushSyncTask() {
        IncrementalSyncTask ret = new PushSyncTask(injector, activitySyncQueue)
                .withMaxConversations(IncrementalSyncTask.MAX_CONVERSATIONS)
                .withMaxActivities(20);
        return ret;
    }

    /**
     * NewActivityTask : Write a new outgoing activity to the database. Used for outgoing.
     */
    public NewActivityTask getNewActivityTask(Activity activity) {
        return new NewActivityTask(injector, activity);
    }

    /**
     * Conversation Frontfill Task : Get the latest activities for a particular conversation Used
     * when joining an existing one.
     *
     * @return the task
     */
    public ConversationFrontFillTask getConversationFrontFillTask(String conversationId) {
        IncrementalSyncTask ret = new ConversationFrontFillTask(injector, conversationId)
                .withMaxParticipants(IncrementalSyncTask.MAX_PARTICIPANTS)
                .withMaxActivities(50)
                .withSinceTime(0);
        return (ConversationFrontFillTask) ret;
    }

    public ConversationFrontFillTask getConversationFrontFillWithoutParticipantsTask(String conversationId) {
        IncrementalSyncTask ret = new ConversationFrontFillTask(injector, conversationId)
                .withMaxParticipants(0)
                .withMaxActivities(50)
                .withSinceTime(0);
        return (ConversationFrontFillTask) ret;
    }

    /**
     * When a new key arrives, we launch one of these tasks to decrypt any existing
     *
     * @param keys List of keys
     */
    public KeyPushEventTask getKeyPushEventTask(List<KeyObject> keys) {
        return new KeyPushEventTask(injector, keys);
    }

    /*
     * Task tracking
     * FIXME: shouldn't be static
     */
    private static ArrayList<SyncTask> pendingTasks = new ArrayList<>();

    protected void onTaskStart(SyncTask task) {
        Ln.i("Starting task " + task);
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        task.timeStarted = System.currentTimeMillis();
    }

    protected void onTaskComplete(SyncTask task) {
        Ln.i("Task Completed! " + task + " processed " + task.eventsProcessed + " activities in " + (System.currentTimeMillis() - task.timeStarted) + "ms. Requested at " + task.timeCreated);
        synclock.lock();
        try {
            pendingTasks.remove(task);

            if (!isSyncBusy()) {
                idle.signalAll();
            }
        } finally {
            synclock.unlock();
        }
    }

    /**
     * ***** <p/> Utility functions
     */
    public long getHighWaterMark() {
        synclock.lock();
        try {
            if (highWaterMark > 0)
                return highWaterMark;
        } finally {
            synclock.unlock();
        }

        setHighWaterMark(getHighWaterMark(contentResolver));

        return highWaterMark;
    }

    public static long getHighWaterMark(ContentResolver contentResolver) {
        if (contentResolver == null)
            return 0;

        Cursor cursor = null;
        try {
            cursor = contentResolver.query(ActivityEntry.CONTENT_URI,
                    new String[]{ActivityEntry.ACTIVITY_PUBLISHED_TIME.name()},
                    ActivityEntry.SOURCE + "=?",
                    new String[]{String.valueOf(ActivityEntry.Source.SYNC.ordinal())},
                    ActivityEntry.ACTIVITY_PUBLISHED_TIME + " DESC LIMIT 1");
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }

    /**
     * Set the high water mark, stored in memory. This function will only move the HWM forward. If
     * the value passed in is lower than the existing HWM we do nothing.
     */
    public void setHighWaterMark(long highWaterMark) {
        synclock.lock();
        try {
            if (applicationController != null && !applicationController.isRegistered())
                return;

            if (highWaterMark > this.highWaterMark) {
                this.highWaterMark = highWaterMark;
                Ln.i("Sync hwm is now " + highWaterMark);
            }
        } finally {
            synclock.unlock();
        }
    }

    public static boolean isSyncBusy() {
        return !pendingTasks.isEmpty();
    }

    public void pause() {
        throw new RuntimeException("Not implemented, for testing only");
    }

    public void resume() {
        throw new RuntimeException("Not implemented, for testing only");
    }

    public boolean waitUntilIdle(long time, TimeUnit unit) {
        Ln.d("Starting wait for idle : " + unit.toMillis(time));
        boolean ret = false;
        synclock.lock();
        try {
            if (!isSyncBusy()) {
                return true;
            }
            ret = idle.await(time, unit);
        } catch (InterruptedException e) {
            Ln.w(e);
        } finally {
            synclock.unlock();
            Ln.d("Done waiting for idle : " + ret);
        }
        return ret;
    }

    public void setApplicationController(ApplicationController applicationController) {
        this.applicationController = applicationController;
    }


    public static class ConversationListCompletedEvent {
    }
}
