package com.cisco.spark.android.sync.queue;

import android.text.TextUtils;

import com.cisco.spark.android.mercury.events.ConversationActivityEvent;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.util.CollectionUtils;
import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;

@Singleton
public class ActivitySyncQueue {
    private final EventBus bus;
    private LinkedBlockingQueue<Activity> activityQ = new LinkedBlockingQueue<Activity>();

    private boolean enabled = true;
    NaturalLog ln;

    @Inject
    public ActivitySyncQueue(EventBus bus, Ln.Context lnContext) {
        this.bus = bus;
        ln = Ln.get(lnContext);
        bus.register(this);
    }

    public void add(List<Activity> activities) {
        synchronized (activityQ) {
            activityQ.addAll(activities);
        }
        bus.post(new ActivitySyncQueueUpdatedEvent());
    }

    public void add(Activity activity) {
        add(CollectionUtils.asList(activity));
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(ConversationActivityEvent event) {
        if (enabled) {
            Activity activity = event.getActivity();
            ln.d("EventBus - Bus->ActivitySyncQueue: ConversationActivityEvent " + activity);
            add(activity);
        } else {
            ln.w("Ignoring event because ActivitySyncQueue is disabled");
        }
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    public Collection<Conversation> getWork() {
        ArrayList<Activity> activities = new ArrayList<>();

        synchronized (activityQ) {
            activityQ.drainTo(activities);
        }

        HashMap<String, Conversation> ret = new HashMap<>();

        for (Activity activity : activities) {
            if (TextUtils.isEmpty(activity.getConversationId())) {
                ln.w("Activity " + activity.getId() + " has empty conversation id.");
                continue;
            }

            Conversation conversation = null;

            if (activity.getTarget() != null && activity.getTarget() instanceof Conversation) {
                conversation = (Conversation) activity.getTarget();
            } else {
                conversation = new Conversation(activity.getConversationId());
            }

            if (!ret.containsKey(activity.getConversationId())) {
                ret.put(activity.getConversationId(), conversation);
            } else {
                ret.get(activity.getConversationId()).updateWithHeadersFrom(conversation);
            }

            ret.get(activity.getConversationId()).getActivities().addItem(activity);
        }

        return ret.values();
    }

    public static final class ActivitySyncQueueUpdatedEvent {
    }
}

