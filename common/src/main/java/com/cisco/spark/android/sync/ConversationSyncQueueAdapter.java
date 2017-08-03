package com.cisco.spark.android.sync;

import android.content.Context;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.locus.events.ResetEvent;
import com.cisco.spark.android.mercury.MercuryClient;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.sync.operationqueue.FetchMentionsOperation;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.sync.queue.ConversationSyncQueue;
import com.cisco.spark.android.sync.queue.SyncFlagsOperation;
import com.cisco.spark.android.util.SafeAsyncTask;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.github.benoitdion.ln.Ln;

import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;

public class ConversationSyncQueueAdapter {
    private final Context context;
    private final Settings settings;
    private final OperationQueue operationQueue;
    private KeyManager keyManager;
    private final Injector injector;
    private final DeviceRegistration deviceRegistration;
    private MercuryClient.WebSocketStatusCodes lastResetReason;
    private long lastResetTimestamp;

    public ConversationSyncQueueAdapter(Context context, Settings settings, OperationQueue operationQueue, EventBus bus, KeyManager keyManager, Injector injector, DeviceRegistration deviceRegistration) {
        this.context = context;
        this.settings = settings;
        this.operationQueue = operationQueue;
        this.keyManager = keyManager;
        this.injector = injector;
        this.deviceRegistration = deviceRegistration;
        bus.register(this);
    }

    public void register() {
        if (lastResetReason == MercuryClient.WebSocketStatusCodes.CLOSE_GOING_AWAY && (System.currentTimeMillis() - lastResetTimestamp < TimeUnit.MINUTES.toMillis(5))) {
            Ln.i("Recovering from recent graceful shutdown. No catch-up needed");
            lastResetTimestamp = 0;
            lastResetReason = null;
            return;
        }

        // This starts negotiating the shared key if we don't have one yet
        keyManager.getSharedKeyWithKMS();

        if (!deviceRegistration.getFeatures().isBufferedMercuryEnabled() || ConversationSyncQueue.getHighWaterMark(context.getContentResolver()) == 0) {
            Ln.i("Submitting catchup task");
            operationQueue.catchUpSync();
        }

        Ln.i("Submitting fetch Mentions task (if necessary)");
        if (!settings.hasLoadedMentions())
            operationQueue.submit(new FetchMentionsOperation(injector));

        Ln.i("Submitting fetch Flags task");
        operationQueue.submit(new SyncFlagsOperation(injector));
    }

    public void clear() {
        lastResetTimestamp = 0;
        new SafeAsyncTask<Void>() {
            @Override
            public Void call() throws Exception {
                context.getContentResolver().applyBatch(ConversationContract.CONTENT_AUTHORITY,
                        ConversationContentProviderOperation.emptyDatabase());
                return null;
            }
        }.execute();
    }

    public void onEvent(ResetEvent event) {
        lastResetReason = event.getCode();
        lastResetTimestamp = System.currentTimeMillis();
    }
}
