package com.cisco.spark.android.sync;

import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.LruCache;

import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.authenticator.LogoutEvent;
import com.cisco.spark.android.client.UserClient;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.model.ParticipantTag;
import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.model.User;
import com.cisco.spark.android.util.Action;
import com.cisco.spark.android.util.LoggingLock;
import com.cisco.spark.android.util.NameUtils;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.util.ThrottledAsyncTask;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
import retrofit2.Response;

/**
 * {@link ActorRecordProvider} is used to load and store {@link ActorRecord} from and to the
 * database.
 *
 * Initialize the provider with {@link ActorRecordProvider#DEFAULT_CAPACITY} {@link ActorRecord} at
 * startup. The other ones are loaded on demand using {@link ActorRecordProvider#get(com.cisco.spark.android.sync.ActorRecord.ActorKey)}
 * synchronously or {@link ActorRecordProvider#callWithActor(ActorRecord.ActorKey, Action)}
 * asynchronously.
 *
 * New items can be monitored using {@link ActorRecordProvider#monitor(ActorRecord, boolean)} and
 * synced to the persistence store using {@link com.cisco.spark.android.sync.ActorRecordProvider#sync()}.
 */
@Singleton
public class ActorRecordProvider {
    public static final int DEFAULT_CAPACITY = 1000;
    private final Context context;
    private final LruCache<ActorRecord.ActorKey, ActorRecord> actors = new LruCache<>(DEFAULT_CAPACITY);
    private final ConcurrentHashMap<String, ActorRecord.ActorKey> keysByEmail = new ConcurrentHashMap<>(DEFAULT_CAPACITY);
    private final ConcurrentHashMap<com.cisco.spark.android.sync.ActorRecord.ActorKey, ActorRecord> updateList = new ConcurrentHashMap<>();
    private final Provider<Batch> batchProvider;
    private final LoggingLock lock = new LoggingLock(BuildConfig.DEBUG, "ActorRecordProvider Lock");
    private ThrottledLoadActorsTask loadActorsTask = new ThrottledLoadActorsTask();

    public final static Comparator<ActorRecord> actorComparator =
            new Comparator<ActorRecord>() {
                @Override
                public int compare(ActorRecord lhs, ActorRecord rhs) {
                    return NameUtils.getShortName(lhs.getDisplayName()).compareTo(NameUtils.getShortName(rhs.getDisplayName()));
                }
            };

    @Inject
    public ActorRecordProvider(Context context, EventBus bus, Provider<Batch> batchProvider) {
        this.batchProvider = batchProvider;
        bus.register(this);
        this.context = context;
    }

    /**
     * Execute action for the actor referenced by the email or uuid string
     *
     * @param emailOrUuid Reference to the actor.
     * @param action      Action to execute with the actor.
     * @return Whether the action was executed synchronously.
     */
    public boolean callWithActor(String emailOrUuid, Action<ActorRecord> action) {
        ActorRecord ret = getCached(emailOrUuid);
        if (ret != null) {
            action.call(ret);
            return true;
        }

        loadActorsTask.add(new SingleActorRecordRequest(emailOrUuid, action));
        return false;
    }

    /**
     * Execute action for the actor referenced by key.
     *
     * @param key    Reference to the actor.
     * @param action Action to execute with the actor.
     * @return Whether the action was executed synchronously.
     */
    public boolean callWithActor(ActorRecord.ActorKey key, Action<ActorRecord> action) {
        if (action == null || key == null) {
            return true;
        }
        ActorRecord actor = actors.get(key);
        if (actor == null) {
            loadActorsTask.add(new SingleActorRecordRequest(key, action));
            return false;
        } else {
            action.call(actor);
            return true;
        }
    }

    /**
     * Get an actor, querying the DB synchronously if needed
     *
     * @return The actor record
     */
    public ActorRecord get(ActorRecord.ActorKey key) {
        if (key == null) {
            throw new IllegalArgumentException("ActorKey is null.");
        }
        return queryActor(key);
    }

    /**
     * Get an actor, querying the DB synchronously if needed
     *
     * @return The actor record
     */
    public ActorRecord get(String emailOrUuid) {
        if (TextUtils.isEmpty(emailOrUuid))
            return null;

        ActorRecord ret = getCached(emailOrUuid);

        if (ret != null)
            return ret;

        return queryActor(emailOrUuid);
    }

    public ActorRecord get(Person person) {
        if (person == null) {
            throw new IllegalArgumentException("Person is null.");
        }
        if (person.getKey() != null)
            return get(person.getKey());

        return get(person.getId());
    }

    public ActorRecord getCached(String emailOrUuid) {
        if (emailOrUuid == null)
            return null;

        ActorRecord ret = null;

        ActorRecord.ActorKey keyByEmail = keysByEmail.get(emailOrUuid);

        if (keyByEmail != null)
            ret = getCached(keyByEmail);

        if (ret == null)
            ret = getCached(new ActorRecord.ActorKey(emailOrUuid));

        return ret;
    }

    public ActorRecord getCached(ActorRecord.ActorKey key) {
        if (key == null) {
            return null;
        }
        return actors.get(key);
    }

    private ActorRecord queryActor(ActorRecord.ActorKey key) {
        ActorRecord actor = actors.get(key);
        if (actor == null) {
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver()
                        .query(Uri.withAppendedPath(ConversationContract.ActorEntry.CONTENT_URI, key.toString()),
                                ConversationContract.ActorEntry.DEFAULT_PROJECTION, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    actor = new ActorRecord(cursor);
                    actors.put(actor.getKey(), actor);
                    if (actor.getEmail() != null)
                        keysByEmail.put(actor.getEmail(), actor.getKey());
                } else {
                    Ln.w("No actor for key: %s", key);
                }
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }
        return actor;
    }

    private ActorRecord queryActor(String emailOrUuid) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver()
                    .query(ConversationContract.ActorEntry.CONTENT_URI,
                            ConversationContract.ActorEntry.DEFAULT_PROJECTION,
                            ConversationContract.ActorEntry.EMAIL + "=? OR " + ConversationContract.ActorEntry.ACTOR_UUID + "=?",
                            new String[]{emailOrUuid, emailOrUuid},
                            null);
            if (cursor != null && cursor.moveToFirst()) {
                ActorRecord actor = new ActorRecord(cursor);
                actors.put(actor.getKey(), actor);
                keysByEmail.put(emailOrUuid, actor.getKey());
                return actor;
            } else {
                Ln.w("No actor for email/uuid");
                Ln.v("No actor for email/uuid " + emailOrUuid);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * Execute action when all the actor have been loaded.
     *
     * @param emailsOrUuids References to the actor to load.
     * @param action        Action to execute.
     * @return Whether the action was executed synchronously.
     */
    public boolean loadActors(Collection<String> emailsOrUuids, Action<List<ActorRecord>> action) {
        if (action == null || emailsOrUuids == null) {
            return true;
        }

        lock.lock();
        try {
            for (String emailOrUuid : emailsOrUuids) {
                if (getCached(emailOrUuid) == null) {
                    loadActorsTask.add(new MultiActorRecordRequest(emailsOrUuids, action));
                    return false;
                }
            }
        } finally {
            lock.unlock();
        }
        action.call(null);
        return true;
    }

    /**
     * Execute action when all the actor have been loaded.
     *
     * @param actorKeys References to the actor to load.
     * @param action    Action to execute.
     * @return Whether the action was executed synchronously.
     */
    public boolean loadActorsFromKeys(Collection<ActorRecord.ActorKey> actorKeys, Action<List<ActorRecord>> action) {
        if (action == null || actorKeys == null) {
            return true;
        }

        List<String> emailsOrUuids = new ArrayList<>();
        List<ActorRecord> cachedActors = new ArrayList<>();
        boolean ret = true;

        for (ActorRecord.ActorKey key : actorKeys) {
            if (key == null)
                continue;

            emailsOrUuids.add(key.getUuid());
            ActorRecord actorRecord = getCached(key);
            if (actorRecord == null) {
                ret = false;
            } else {
                cachedActors.add(actorRecord);
            }
        }

        if (emailsOrUuids.isEmpty())
            return false;

        if (!ret) {
            loadActorsTask.add(new MultiActorRecordByKeyRequest(actorKeys, action));
            return false;
        }
        action.call(cachedActors);
        return true;
    }

    public void monitor(ActorRecord actor, boolean checkEntitled) {
        if (actor.getKey() == null) {
            actor = get(actor.getEmail());
            if (actor == null || actor.getKey() == null) {
                return;
            }
        }

        // Only insert fully-formed actors into the cache
        if (TextUtils.isEmpty(actor.getDisplayName())) {
            return;
        }

        // Always monitor newer records except when they're missing information.
        lock.lock();
        try {

            // check if actor for this activity contains not entitled tag...if so, remove it
            // (getting an activity for this actor implies that they are entitled)
            if (checkEntitled) {
                ActorRecord oldRecord = get(actor.getKey());
                if (oldRecord != null && !oldRecord.isSquaredEntitled()) {
                    oldRecord.getTags().remove(ParticipantTag.ENTITLEMENT_NO_SQUARED);
                    actor.setTags(oldRecord.getTags());
                    actor.setTagValidity(true);
                    updateList.put(actor.getKey(), actor);
                    return;
                }
            }

            ActorRecord existingActor = updateList.get(actor.getKey());

            // Maintain actor type, if the new actor object has a type and the existing one doesn't, set it on the existing actor and vice versa
            // If the new actor has no type and the existing actor has no type, check the actors list and get the type from there
            if (existingActor != null && existingActor.getType() == null && actor.getType() != null) {
                existingActor.setType(actor.getType());
            } else if (existingActor != null && existingActor.getType() != null && actor.getType() == null) {
                actor.setType(existingActor.getType());
            } else if (actors.get(actor.getKey()) != null && actors.get(actor.getKey()).getType() != null && actor.getType() == null) {
                actor.setType(actors.get(actor.getKey()).getType());
            }

            if (existingActor != null && existingActor.hasValidTags()) { // Do not override if the actor already in cache has tags.
                if (existingActor.getOrgId() == null) {
                    existingActor.setOrgId(actor.getOrgId());
                }
                return;
            }

            if (existingActor == null || (!existingActor.hasValidTags() && actor.hasValidTags())) {
                Ln.d("Actor updated: " + actor.getDisplayName() + " " + actor.isSquaredEntitled() + " " + actor.hasValidTags());
                updateList.remove(actor.getKey());
                ActorRecord oldRecord = actors.get(actor.getKey());
                if (oldRecord != null && oldRecord.hasValidTags() && !actor.hasValidTags()) {
                    actor.setTags(oldRecord.getTags());
                    actor.setTagValidity(true);
                    Ln.d("Actor updated - propagated tags to new actor record: " + actor.getDisplayName() + " " + actor.isSquaredEntitled() + " " + actor.hasValidTags());
                }
                actors.put(actor.getKey(), actor);
                if (actor.getEmail() != null)
                    keysByEmail.put(actor.getEmail(), actor.getKey());
                updateList.put(actor.getKey(), actor);
            }
        } finally {
            lock.unlock();
        }
    }

    public void sync() throws RemoteException, OperationApplicationException {
        Batch batch = batchProvider.get();
        ActorRecord actorRecord;
        lock.lock();
        try {
            while (!updateList.isEmpty()) {
                actorRecord = updateList.remove(updateList.keys().nextElement());
                if (actorRecord != null) {
                    actorRecord.addInsertUpdateContentProviderOperation(batch);
                    if (batch.size() > 100) {
                        batch.apply();
                        batch = batchProvider.get();
                    }
                }
            }
        } finally {
            lock.unlock();
        }
        batch.apply();
    }

    public void sync(Batch batch) {
        lock.lock();
        try {
            for (ActorRecord actorRecord : updateList.values()) {
                actorRecord.addInsertUpdateContentProviderOperation(batch);
            }
            updateList.clear();
        } finally {
            lock.unlock();
        }
    }

    public Set<String> emailsToUserIds(ApiClientProvider apiClientProvider, ApiTokenProvider apiTokenProvider, @NonNull Set<String> emails) {
        Set<String> userIds = new HashSet<>(emails.size());
        UserClient userClient = apiClientProvider.getUserClient();
        for (String email : emails) {
            try {
                ActorRecord.ActorKey key = keysByEmail.get(email);
                if (key == null) {
                    ActorRecord ar = get(email);
                    if (ar != null)
                        key = ar.getKey();
                }

                if (key != null)
                    userIds.add(key.getUuid());
                else {
                    Response<User> resp = userClient.getUserID(apiTokenProvider.getAuthenticatedUser().getConversationAuthorizationHeader(), email).execute();
                    if (resp.isSuccessful()) {
                        userIds.add(resp.body().getId());
                    } else if (resp.code() == 404) {
                        Ln.d("Unable to get UUID for user (404): %s", email);
                    } else {
                        Ln.w("Failed requesting user id " + resp.code());
                        return null;
                    }
                }
            } catch (IOException ex) {
                Ln.e(ex, "Failed getting user ID's");
                return null;
            }
        }
        return userIds;
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(LogoutEvent logoutEvent) {
        Ln.d("Bus->ActorRecordProvider: LogoutEvent");
        lock.lock();
        try {
            actors.evictAll();
            updateList.clear();
        } finally {
            lock.unlock();
        }
    }

    public List<ActorRecord> sortActors(List<String> emailsOrUuids) {
        List<ActorRecord> actors = new ArrayList<ActorRecord>();
        for (String emailOrUuid : emailsOrUuids) {
            ActorRecord actor = getCached(emailOrUuid);
            if (actor == null && Strings.isEmailAddress(emailOrUuid)) {
                // Default to a person type
                actor = new ActorRecord(new ActorRecord.ActorKey(emailOrUuid), emailOrUuid, NameUtils.getLocalPartFromEmail(emailOrUuid), Person.PERSON, false, null, false);
            }

            // A null actor will occur if there is nothing cached and what you recieved is not an email
            if (actor != null) {
                actors.add(actor);
            }

        }
        Collections.sort(actors, actorComparator);
        return actors;
    }

    public List<ActorRecord> sortKeysFromCache(List<ActorRecord.ActorKey> keys) {
        List<ActorRecord> actors = new ArrayList<>();
        for (ActorRecord.ActorKey key : keys) {
            ActorRecord actor = getCached(key);
            if (actor != null)
                actors.add(actor);
        }
        Collections.sort(actors, actorComparator);
        return actors;
    }

    private interface ActorRecordRequest {
        void processQuery();

        void callbackWithResults();
    }

    private class SingleActorRecordRequest implements ActorRecordRequest {
        private String emailOrUuid;
        private Action<ActorRecord> action;
        private ActorRecord result;

        public SingleActorRecordRequest(String emailOrUuid, Action<ActorRecord> action) {
            this.emailOrUuid = emailOrUuid;
            this.action = action;
        }

        public SingleActorRecordRequest(ActorRecord.ActorKey key, Action<ActorRecord> action) {
            this(key == null ? null : key.getUuid(), action);
        }

        @Override
        public void processQuery() {
            if (TextUtils.isEmpty(emailOrUuid))
                return;

            result = getCached(emailOrUuid);
            if (result == null)
                result = queryActor(emailOrUuid);
        }

        @Override
        public void callbackWithResults() {
            if (action != null)
                action.call(result);
        }
    }

    private class MultiActorRecordRequest implements ActorRecordRequest {
        protected List<String> emailsOrUuids = new ArrayList<>();
        protected Action<List<ActorRecord>> action;
        protected List<ActorRecord> result = new ArrayList<>();

        public MultiActorRecordRequest(Collection<String> emailsOrUuids, Action<List<ActorRecord>> action) {
            if (emailsOrUuids != null)
                this.emailsOrUuids.addAll(emailsOrUuids);
            this.action = action;
        }

        @Override
        public void processQuery() {
            for (String emailOrUuid : emailsOrUuids) {
                ActorRecord record = getCached(emailOrUuid);
                if (record == null)
                    record = queryActor(emailOrUuid);

                if (record != null)
                    result.add(record);
            }
        }

        @Override
        public void callbackWithResults() {
            if (action != null)
                action.call(result);
        }
    }

    private class MultiActorRecordByKeyRequest extends MultiActorRecordRequest {
        private List<ActorRecord.ActorKey> actorKeys = new ArrayList<>();

        public MultiActorRecordByKeyRequest(Collection<ActorRecord.ActorKey> actorKeys, Action<List<ActorRecord>> action) {
            super(null, action);
            if (actorKeys != null)
                this.actorKeys.addAll(actorKeys);
        }

        @Override
        public void processQuery() {
            for (ActorRecord.ActorKey actorKey : actorKeys) {
                ActorRecord record = getCached(actorKey);
                if (record == null)
                    record = queryActor(actorKey);

                if (record != null)
                    result.add(record);
            }
        }
    }

    private class ThrottledLoadActorsTask extends ThrottledAsyncTask {

        private LinkedBlockingQueue<ActorRecordRequest> actorRecordRequests = new LinkedBlockingQueue<>();
        private LinkedBlockingQueue<ActorRecordRequest> actorRecordRequestsCompleted = new LinkedBlockingQueue<>();

        public ThrottledLoadActorsTask() {
            //run every 500ms at most
            super(500);
        }

        public void add(ActorRecordRequest request) {
            actorRecordRequests.add(request);
            scheduleExecute();
        }

        @Override
        protected void doInBackground() {
            while (!actorRecordRequests.isEmpty()) {
                ActorRecordRequest request = actorRecordRequests.poll();
                try {
                    request.processQuery();
                } catch (Throwable t) {
                    Ln.i(t, "Failed querying actors");
                }
                actorRecordRequestsCompleted.add(request);
            }
        }

        @Override
        protected void onSuccess() {
            Ln.d("processing " + actorRecordRequestsCompleted.size() + " ActorRecordRequest results");
            while (!actorRecordRequestsCompleted.isEmpty()) {
                ActorRecordRequest request = actorRecordRequestsCompleted.poll();
                try {
                    request.callbackWithResults();
                } catch (Throwable t) {
                    Ln.i(t, "Failed calling back with actor results");
                }
            }
        }

        @Override
        protected void onException(Exception e) throws RuntimeException {
            Ln.e(false, e);
        }
    }
}
