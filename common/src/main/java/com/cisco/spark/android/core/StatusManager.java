package com.cisco.spark.android.core;

import android.content.ContentResolver;

import com.cisco.spark.android.authenticator.NotAuthenticatedException;
import com.cisco.spark.android.mercury.MercuryEventType;
import com.cisco.spark.android.mercury.TypingEvent;
import com.cisco.spark.android.sync.ActorRecord;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.util.ObserverAdapter;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.github.benoitdion.ln.Ln;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.Subscription;

@Singleton
public class StatusManager implements Component {
    private static final int DEFAULT_TYPING_TIMEOUT = 5000;
    private final int fudgeFactor;
    private final ApiClientProvider apiClientProvider;
    private final DeviceRegistration deviceRegistration;
    private final ContentResolver contentResolver;
    private int typingTimeout;
    private Map<String, Map<ActorRecord.ActorKey, TypingInfo>> typing = new ConcurrentHashMap<String, Map<ActorRecord.ActorKey, TypingInfo>>();
    private Map<String, Long> lastUpdates = new ConcurrentHashMap<String, Long>();
    private Subscription subscription;
    private String observedConversation;
    private WeakReference<Typist> typist;
    private boolean isKeyboardUp;

    @Inject
    public StatusManager(ApiClientProvider apiClientProvider, DeviceRegistration deviceRegistration, EventBus bus, ContentResolver contentResolver) {
        this(apiClientProvider, deviceRegistration, bus, contentResolver, DEFAULT_TYPING_TIMEOUT);
    }

    public StatusManager(ApiClientProvider apiClientProvider, DeviceRegistration deviceRegistration, EventBus bus, ContentResolver contentResolver, int typingTimeout) {
        this.apiClientProvider = apiClientProvider;
        this.deviceRegistration = deviceRegistration;
        this.contentResolver = contentResolver;
        bus.register(this);
        this.typingTimeout = typingTimeout;
        fudgeFactor = (int) (typingTimeout * .5);
    }

    public void setApplicationController(ApplicationController applicationController) {
        applicationController.register(this);
    }

    public void onEventAsync(TypingEvent typingEvent) {
        String conversationId = typingEvent.getConversationId();
        if (!typing.containsKey(conversationId)) {
            typing.put(conversationId, new ConcurrentHashMap<ActorRecord.ActorKey, TypingInfo>());
        }
        ActorRecord.ActorKey actorKey = typingEvent.getActor().getKey();
        Map<ActorRecord.ActorKey, TypingInfo> actorRecordWithTypingInfo = typing.get(conversationId);
        if (actorRecordWithTypingInfo != null && actorKey != null) {
            if (typingEvent.getEventType() == MercuryEventType.START_TYPING) {
                if (!actorRecordWithTypingInfo.containsKey(actorKey)) {
                    actorRecordWithTypingInfo.put(actorKey, new TypingInfo(actorKey));
                    contentResolver.notifyChange(ConversationContract.ConversationEntry.getConversationActivitiesUri(conversationId), null, false);
                } else {
                    TypingInfo typingInfo = actorRecordWithTypingInfo.get(actorKey);
                    if (typingInfo != null) {
                        typingInfo.time = new Date().getTime();
                    }
                }
            } else if (typingEvent.getEventType() == MercuryEventType.STOP_TYPING_EVENT) {
                if (actorRecordWithTypingInfo.containsKey(actorKey)) {
                    actorRecordWithTypingInfo.remove(actorKey);
                    contentResolver.notifyChange(ConversationContract.ConversationEntry.getConversationActivitiesUri(conversationId), null, false);
                }
            }
        }
    }

    public int getTypingTimeout() {
        return typingTimeout + fudgeFactor;
    }

    private void prune() {
        for (Map.Entry<String, Map<ActorRecord.ActorKey, TypingInfo>> typingInfos : typing.entrySet()) {
            Set<ActorRecord.ActorKey> keysToRemove = new HashSet<ActorRecord.ActorKey>();
            boolean notifyContentResolver = false;
            for (TypingInfo typingInfo : typingInfos.getValue().values()) {
                if (!typingInfo.stillTyping()) {
                    notifyContentResolver = true;
                    keysToRemove.add(typingInfo.actorKey);
                }
            }
            for (ActorRecord.ActorKey key : keysToRemove) {
                typingInfos.getValue().remove(key);
            }
            if (notifyContentResolver) {
                contentResolver.notifyChange(ConversationContract.ConversationEntry.getConversationActivitiesUri(typingInfos.getKey()), null, false);
            }
        }
    }

    private void updateTypistStatus() {
        if (typist != null) {
            Typist currentTypist = typist.get();
            if (currentTypist != null) {
                try {
                    sendTypingUpdate(observedConversation, currentTypist.isTyping());
                } catch (Exception ex) {
                    Ln.w(ex);
                }
            }
        }
    }

    public boolean isTyping(String conversationId, ActorRecord.ActorKey actorKey) {
        if (conversationId != null && actorKey != null) {
            Map<ActorRecord.ActorKey, TypingInfo> convoMap = typing.get(conversationId);
            if (convoMap != null) {
                TypingInfo typingInfo = convoMap.get(actorKey);
                if (typingInfo != null) return typingInfo.stillTyping();
            }
        }
        return false;
    }

    public void observe(String conversationId, Typist typist) {
        this.observedConversation = conversationId;
        this.typist = new WeakReference<>(typist);
    }

    public void sendTypingUpdate(String conversationId, boolean typing) {
        if (conversationId == null)
            return;

        try {
            if (typing) {
                long now = new Date().getTime();
                if (!lastUpdates.containsKey(conversationId) || now - lastUpdates.get(conversationId) > typingTimeout) {
                    apiClientProvider.getConversationClient().postTyping(new TypingEvent(conversationId, true)).enqueue(ApiClientProvider.NULL_CALLBACK);
                    lastUpdates.put(conversationId, now);
                }
            } else {
                if (lastUpdates.containsKey(conversationId)) {
                    apiClientProvider.getConversationClient().postTyping(new TypingEvent(conversationId, false)).enqueue(ApiClientProvider.NULL_CALLBACK);
                    lastUpdates.remove(conversationId);
                }
            }
        } catch (NotAuthenticatedException e) {
            // Just ignore
        }
    }

    @Override
    public boolean shouldStart() {
        return true;
    }

    @Override
    public void start() {
        stop();
        subscription = Observable.interval(typingTimeout, TimeUnit.MILLISECONDS).subscribe(new ObserverAdapter<Long>() {
            @Override
            public void onNext(Long aLong) {
                prune();
                updateTypistStatus();
            }
        });
    }

    @Override
    public void stop() {
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }
    }

    private class TypingInfo {
        public ActorRecord.ActorKey actorKey;
        private long time;

        public TypingInfo(ActorRecord.ActorKey actorKey) {
            this.time = new Date().getTime();
            this.actorKey = actorKey;
        }

        private boolean stillTyping() {
            return new Date().getTime() - this.time < typingTimeout + fudgeFactor;
        }
    }

    public interface Typist {
        boolean isTyping();
    }

    public void setIsKeyboardUp(boolean status) {
        isKeyboardUp = status;
    }

    public boolean isKeyboardUp() {
        return isKeyboardUp;
    }
}
