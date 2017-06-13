package com.cisco.spark.android.sync.queue;

import com.cisco.spark.android.authenticator.NotAuthenticatedException;
import com.cisco.spark.android.core.Injector;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.concurrent.Callable;

import javax.inject.Inject;

public abstract class SyncTask implements Callable<Void> {

    @Inject
    ConversationSyncQueue conversationSyncQueue;

    public long timeStarted;
    protected int eventsProcessed;
    private static long nextId;
    protected long taskId = ++nextId;
    public final long timeCreated = System.currentTimeMillis();

    protected SyncTask(Injector injector) {
        injector.inject(this);
    }

    protected abstract boolean execute() throws IOException;

    public final Void call() {
        try {
            conversationSyncQueue.onTaskStart(this);
            execute();
        } catch (NotAuthenticatedException e) {
            Ln.d("Bailing on sync task because we are no longer authenticated. " + this);
        } catch (Throwable e) {
            Ln.e(e, "Task failed! Exception");
        } finally {
            conversationSyncQueue.onTaskComplete(this);
        }
        return null;
    }
}
