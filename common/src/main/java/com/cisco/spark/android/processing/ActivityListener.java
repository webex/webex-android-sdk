package com.cisco.spark.android.processing;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.events.ActivityDecryptedEvent;
import com.cisco.spark.android.mercury.AlertType;
import com.cisco.spark.android.mercury.events.ConversationActivityEvent;
import com.cisco.spark.android.model.Activity;
import com.github.benoitdion.ln.Ln;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.greenrobot.event.EventBus;

/**
 * Listen on the bus for {@link ActivityDecryptedEvent} events and
 * delegate the processing to the registered {@link ActivityProcessor}'s. Each {@link
 * ActivityProcessor} generates a command that {@link ActivityListener} executes.
 */
public class ActivityListener {
    private final Set<ActivityProcessor> processors = new HashSet<ActivityProcessor>();
    private final Object processorsLock = new Object();
    private final LinkedList<String> seenActivityIds = new LinkedList<String>();
    private final ConcurrentHashMap<String, AlertType> alertTypes = new ConcurrentHashMap<>();
    private long timeFirstActivityPublished;

    public ActivityListener(EventBus bus) {
        if (bus == null)
            throw new NullPointerException("Bus is null");
        bus.register(this);
        Ln.i("Registered for bus events on bus " + bus);
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventAsync(ActivityDecryptedEvent event) {
        List<Activity> activities = event.getActivities();
        if (activities == null)
            return;

        publish(activities);
    }

    /*
     * To avoid spamming on initial sync, use the first GCM/Mercury event publish time as the start
     * time; activities published before that time will not be published to processors.
     * This event sink is just to get the start time.
     */
    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(ConversationActivityEvent event) {
        if (timeFirstActivityPublished == 0) {
            if (event.getActivity() != null && event.getActivity().getSource().isPushNotification()) {
                timeFirstActivityPublished = event.getActivity().getPublished().getTime();

                // minus a couple seconds fudge factor
                timeFirstActivityPublished -= 2000;

                Ln.i("Activity Listener publishing activities as of " + timeFirstActivityPublished);
            } else {
                Ln.i("Ignoring ConversationActivityEvent " + event.getActivity());
            }
        }
    }

    private void verboseLog(String s, Activity activity) {
        if (activity != null) {
            Ln.v("[%s] - %s - actorId: %s alertType: %s, type: %s, verb: %s, source: %s, published: %s", activity.getId(), s,
                    activity.getActor() != null ? activity.getActor().getId() : "", activity.getAlertType(), activity.getVerb() != null ? activity.getType() : "",
                    activity.getVerb(), activity.getSource(), activity.getPublished() != null ? activity.getPublished().getTime() : "");
        }
    }

    private void publish(List<Activity> activities) {
        if (activities.isEmpty())
            return;

        Ln.v("Publishing " + activities.size() + " activities");
        for (Activity activity : activities) {
            if (!activity.getSource().isPushNotification()) {
                if (timeFirstActivityPublished == 0) {
                    verboseLog("Not ready yet. ignore", activity);
                    continue;
                }

                // Don't push old events. Push notifications are never old
                if (timeFirstActivityPublished > activity.getPublished().getTime()) {
                    verboseLog("Not publishing stale activity", activity);
                    continue;
                }
            }

            if (activity.isEncrypted()) {
                verboseLog("Not publishing activity because it is encrypted", activity);
                continue;
            }
            if (alertTypes.containsKey(activity.getId())) {
                activity.setAlertType(alertTypes.get(activity.getId()));
                verboseLog("Escalate alertType", activity);
            }

            if (add(activity.getId())) {
                if (!TextUtils.equals(activity.getClientTempId(), activity.getId())
                        && activity.getClientTempId() != null
                        && !add(activity.getClientTempId())) {
                    Ln.v("Not publishing activity because we already did this one by clientTempId " + activity);
                    continue;
                }
            } else {
                Ln.v("Not publishing activity because we already did this one " + activity);
                if (activity.getClientTempId() != null)
                    add(activity.getClientTempId());
                continue;
            }

            verboseLog("Processing activity", activity);

            synchronized (processorsLock) {
                for (ActivityProcessor processor : processors) {
                    Ln.d("ActivityListener, process activity with " + processor.toString());
                    try {
                        ActivityProcessorCommand command = processor.processActivity(activity);
                        if (command != null) {
                            Ln.d("Running command: %s", command.description());
                            command.execute();
                        }
                    } catch (Exception e) {
                        Ln.e(e, "Failed publishing activity " + activity.getId() + " to listener");
                    }
                }
            }
        }
    }

    public void register(ActivityProcessor processor) {
        if (processor == null)
            return;

        synchronized (processorsLock) {
            processors.add(processor);
        }
    }

    public void setActivityMetadata(@NonNull String activityId, @NonNull AlertType alertType, boolean isEscalation) {
        if (isEscalation) {
            synchronized (seenActivityIds) {
                seenActivityIds.remove(activityId);
            }
        }
        alertTypes.put(activityId, alertType);
    }

    /**
     * Adds the activityId to the cache
     *
     * @param activityId
     * @return True if the activityId was added. False if it existed already
     */
    private boolean add(String activityId) {
        synchronized (seenActivityIds) {
            if (seenActivityIds.contains(activityId))
                return false;

            // Add new ones to the front of the list since they are more likely to be searched on.
            seenActivityIds.addFirst(activityId);
            if (seenActivityIds.size() > 1000)
                seenActivityIds.removeLast();

            return true;
        }
    }
}
