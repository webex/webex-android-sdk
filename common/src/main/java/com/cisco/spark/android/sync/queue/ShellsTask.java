package com.cisco.spark.android.sync.queue;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.sync.ConversationContract.ActivityEntry.Source;
import com.cisco.spark.android.util.Action;

import java.io.IOException;
import java.util.Collections;

import rx.Observable;

/**
 * FastShellsTask - Task to grab a smallish subset of data on startup
 */
public class ShellsTask extends IncrementalSyncTask {

    public ShellsTask(Injector injector) {
        super(injector);
        withMaxActivities(1);
    }

    @Override
    protected java.util.List<Conversation> getLeftConversations() {
        return Collections.emptyList();
    }

    // If we requested all the conversations, it's OK to set the HWM based on the complete results.
    // Otherwise return 0 so the HWM is not affected.
    @Override
    protected long getTaskHighWaterMark() {
        return getMaxConversations() == MAX_CONVERSATIONS ? lastActivityPublishTime : 0;
    }

    @Override
    protected boolean shouldNotify() {
        return false;
    }

    protected Observable<Conversation> getConversations() throws IOException {

        Action<Conversation> transform = new Action<Conversation>() {
            @Override
            public void call(Conversation conv) {
                if (conv.getActivities() != null) {
                    Source source = Source.SHELL;

                    // If we're in 'full shells' mode we can set the hwm
                    if (getMaxConversations() == MAX_CONVERSATIONS)
                        source = Source.SYNC_PARTIAL;

                    // We need to set the source for the activities to "SHELL"
                    for (Activity activity : conv.getActivities().getItems()) {
                        activity.setSource(source);
                    }
                }
            }
        };

        // If 'full shells', we're setting the HWM so we should get all the acks. Otherwise just get our own
        String ackFilter = getMaxConversations() == MAX_CONVERSATIONS ? "all" : "myack";

        return apiClientProvider.getConversationClient().getShells(
                getMaxConversations(),
                getMaxParticipants(),
                Math.max(0, sinceTime - FUDGE_FACTOR),
                ackFilter)
                .flatMap(new ConversationStreamObservableMapper(gson, transform));
    }

    @Override
    public boolean getYieldAllowed() {
        // Don't yield for the small shell fetch with limited participants, just the bigger fetches.
        return getMaxParticipants() == MAX_PARTICIPANTS;
    }
}
