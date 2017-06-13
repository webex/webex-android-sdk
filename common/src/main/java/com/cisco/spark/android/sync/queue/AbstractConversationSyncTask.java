package com.cisco.spark.android.sync.queue;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.events.ActivityDecryptedEvent;
import com.cisco.spark.android.metrics.EncryptionDurationMetricManager;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.ActivityObject;
import com.cisco.spark.android.model.ActivityReference;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.model.Team;
import com.cisco.spark.android.sync.ActorRecord;
import com.cisco.spark.android.sync.ActorRecordProvider;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContentProviderOperation;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.ConversationContract.ActivityEntry;
import com.cisco.spark.android.sync.ConversationContract.ParticipantEntry;
import com.cisco.spark.android.sync.ConversationRecord;
import com.cisco.spark.android.sync.Message;
import com.cisco.spark.android.sync.SearchManager;
import com.cisco.spark.android.sync.Tombstone;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.util.SafeAsyncTask;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.nimbusds.jose.JOSEException;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import de.greenrobot.event.EventBus;

import static com.cisco.spark.android.sync.ConversationContract.ConversationEntry;
import static com.cisco.spark.android.sync.ConversationContract.TeamEntry;

/**
 * Abstract class for sync tasks that get passed to the ConversationSyncQueue.
 *
 * This class contains most of the logic for adding sync activity to the database.
 */
public abstract class AbstractConversationSyncTask extends SyncTask {

    @Inject
    KeyManager keyManager;

    @Inject
    EventBus eventBus;

    @Inject
    EncryptionDurationMetricManager encryptionDurationMetricManager;

    @Inject
    ApiTokenProvider apiTokenProvider;

    @Inject
    Gson gson;

    @Inject
    ActorRecordProvider actorRecordProvider;

    @Inject
    SearchManager searchManager;

    @Inject
    Provider<Batch> batchProvider;

    @Inject
    ContentResolver contentResolver;

    @Inject
    OperationQueue operationQueue;

    protected final HashSet<ConversationRecord> updatedConversations = new HashSet<>();
    protected final HashSet<Uri> keysToFetch = new HashSet<>();
    protected final ActivityDecryptedEvent activityDecryptedEvent = new ActivityDecryptedEvent();
    protected final HashMap<String, Long> activityPublishTimes = new HashMap<>();

    /**
     * We work with a distributed system and can't rely on time 100%. Adds a fudge factor on the
     * sinceDate parameters.
     *
     * TODO Is this still necessary? If so is 200ms long enough? Ask the server guys
     */
    protected static final long FUDGE_FACTOR = 200;
    protected static final String MAC_CHECK_FAILURE = "mac check in GCM failed";

    // Constructor is protected. Use the factory classes in ConversationSyncQueue
    protected AbstractConversationSyncTask(Injector injector) {
        super(injector);
    }

    protected void updateContent(Batch batch, Activity activity) {
        if (activity.getContentDataId() == null) {
            Ln.e("Error, content update activity has no content data id.");
            return;
        }
        ContentProviderOperation op = ContentProviderOperation.newUpdate(ActivityEntry.CONTENT_URI)
                .withValue(ActivityEntry.ACTIVITY_DATA.name(), activity.getActivityData(getSelf(), gson))
                .withSelection(ActivityEntry.CONTENT_DATA_ID + "=?", new String[]{activity.getContentDataId()})
                .build();

        batch.add(op);
    }

    protected void updateTeamColor(Batch batch, Activity activity) {
        if (activity.getObject().getId() == null) {
            Ln.e("Error, update team color activity has no id on object");
            return;
        }
        ContentProviderOperation op = ContentProviderOperation.newUpdate(TeamEntry.CONTENT_URI)
                .withValue(TeamEntry.COLOR.name(), ((Team) activity.getObject()).getTeamColor())
                .withSelection(TeamEntry.TEAM_ID + "=?", new String[]{activity.getObject().getId()})
                .build();

        batch.add(op);
    }

    /**
     * Add one activity to the batch, and modify the conversationrecord as needed. The superclass
     * will have a chance to tweak the conversationrecord before it writes to the conversation
     * table.
     *
     * @param batch The batch
     * @param cr    The (writable) conversation record. This may be null if the current sync task
     *              will not update the conversation; for example the ConversationBackFillTask.
     */
    protected void addConversationActivity(Batch batch, ConversationRecord cr, Activity activity) {
        if (!activity.isValid())
            return;

        // keep track of these for speedy lookups in this task
        activityPublishTimes.put(activity.getId(), activity.getPublished().getTime());
        //Activity json obtained via non-local source is set to false by default. Need to reset it before going into decryption flow
        if (!activity.isEncrypted()) {
            activity.setEncrypted(activity.getEncryptionKeyUrl() != null);
        }
        if (!activity.isEncrypted()) {
            activityDecryptedEvent.addActivity(activity);
        }
        final Uri keyUrl = activity.getEncryptionKeyUrl();
        if (keyUrl != null && activity.getSource() != ActivityEntry.Source.LOCAL) {
            decryptActivity(batch, activity, keyUrl);
        }

        if (cr != null) {
            cr.applyActivity(activity, getSelf());
        }

        if (activity.isAcknowledgeActivity()) {
            handleAcknowledgement(batch, activity, cr);
            return;
        } else if (activity.isDelete()) {
            handleDeleteActivity(batch, activity, cr);
            return;
        } else if (activity.isUpdateContent()) {
            updateContent(batch, activity);
            return;
        } else if (activity.isSetTeamColor()) {
            updateTeamColor(batch, activity);
            return;
        }

        ActorRecord activityActor = ActorRecord.newInstance(activity.getActor());
        activityActor.setTagValidity(false);
        actorRecordProvider.monitor(activityActor, true);
        if (activity.getObject() != null && activity.getObject().isPerson()) {
            ActorRecord objectActor = ActorRecord.newInstance((Person) activity.getObject());
            objectActor.setTagValidity(false);
            actorRecordProvider.monitor(objectActor, false);
        }
        if (activity.isAddParticipant()) {
            handleAddParticipant(batch, activity);
        } else if (activity.isLeaveActivity()) {
            handleLeaveActivity(batch, activity);
        } else if (!activity.shouldWriteToActivityEntry()) {
            //our work is done
            return;
        } else if (cr != null && (activity.isLockConversation() || activity.isUnlockConversation())) {
            updateParticipantModerator(batch, cr.getId(), activity.getActor().getKey(), activity.isLockConversation());
        } else if (cr != null && (activity.isAssignModerator() || activity.isUnassignModerator())) {
            Person p = (Person) activity.getObject();
            updateParticipantModerator(batch, cr.getId(), p.getKey(), activity.isAssignModerator());
        } else if (activity.isCreateConversation()
                && activity.getPublished() != null
                && activity.getPublished().getTime() > 0) {
            // Minus a couple seconds to make sure the create is the first activity
            activity.setPublished(new Date(activity.getPublished().getTime() - 2000));
        } else if (activity.isAddNewTeamConversation()) {
            handleNewTeamConversation(activity);
        }

        String activityData = activity.getActivityData(getSelf(), gson);
        // insert then update. The insert may silently fail due to constraint violation if the row
        // is already there but we need to update the source, the activity data, and in the case
        // of provisional objects the activity id.
        batch.add(ContentProviderOperation.newInsert(ActivityEntry.CONTENT_URI)
                .withValue(ActivityEntry.ACTIVITY_ID.name(), activity.getId())
                .withValue(ActivityEntry.ACTOR_ID.name(), activityActor.getUuidOrEmail())
                .withValue(ActivityEntry.CONVERSATION_ID.name(), activity.getConversationId())
                .withValue(ActivityEntry.ACTIVITY_PUBLISHED_TIME.name(), activity.getPublished().getTime())
                .withValue(ActivityEntry.ACTIVITY_TYPE.name(), activity.getType().ordinal())
                .withValue(ActivityEntry.ACTIVITY_DATA.name(), activityData)
                .withValue(ActivityEntry.SYNC_OPERATION_ID.name(), activity.getClientTempId())
                .withValue(ActivityEntry.SOURCE.name(), activity.getSource().ordinal())
                .withValue(ActivityEntry.CONTENT_DATA_ID.name(), activity.getContentDataId())
                .withValue(ActivityEntry.IS_ENCRYPTED.name(), activity.isEncrypted() ? 1 : 0)
                .withValue(ActivityEntry.IS_MENTION.name(), activity.isSelfMention(apiTokenProvider.getAuthenticatedUser()) ? 1 : 0)
                .withValue(ActivityEntry.ENCRYPTION_KEY_URL.name(), keyUrl != null ? keyUrl.toString() : null)
                .withValue(ActivityEntry.SYNC_STATE.name(),
                        activity.getSource() == ActivityEntry.Source.LOCAL
                                ? ConversationContract.SyncOperationEntry.SyncState.READY.ordinal()
                                : ConversationContract.SyncOperationEntry.SyncState.SUCCEEDED.ordinal())
                .build());

        // Update only if new source is more reliable than previous source
        batch.add(ContentProviderOperation.newUpdate(ActivityEntry.CONTENT_URI)
                .withValue(ActivityEntry.ACTIVITY_ID.name(), activity.getId())                          // include for provisional object use case
                .withValue(ActivityEntry.ACTOR_ID.name(), activityActor.getUuidOrEmail())
                .withValue(ActivityEntry.ACTIVITY_DATA.name(), activityData)
                .withValue(ActivityEntry.CONVERSATION_ID.name(), activity.getConversationId())          // include so the content manager can notify the conversation url
                .withValue(ActivityEntry.SOURCE.name(), activity.getSource().ordinal())
                .withValue(ActivityEntry.ACTIVITY_PUBLISHED_TIME.name(), activity.getPublished().getTime())      // in case our device's clock is off
                .withValue(ActivityEntry.CONTENT_DATA_ID.name(), activity.getContentDataId())           // include for provisional object use case
                .withValue(ActivityEntry.IS_ENCRYPTED.name(), activity.isEncrypted() ? 1 : 0)
                .withValue(ActivityEntry.IS_MENTION.name(), activity.isSelfMention(apiTokenProvider.getAuthenticatedUser()) ? 1 : 0)
                .withValue(ActivityEntry.ENCRYPTION_KEY_URL.name(), keyUrl != null ? keyUrl.toString() : null)
                .withSelection(ActivityEntry.SOURCE + " > ? AND (" + ActivityEntry.ACTIVITY_ID + " =? OR " + ActivityEntry.SYNC_OPERATION_ID + " =?)",
                        new String[]{String.valueOf(activity.getSource().ordinal()),
                                activity.getId(),
                                activity.getClientTempId()}
                ).build());

        if (activity.getClientTempId() != null && !activity.getId().equals(activity.getClientTempId())) {
            batch.add(ContentProviderOperation.newUpdate(ConversationEntry.CONTENT_URI)
                    .withValue(ConversationEntry.LAST_ACTIVITY_ID.name(), activity.getId())
                    .withSelection(ConversationEntry.LAST_ACTIVITY_ID + "=?", new String[]{activity.getClientTempId()})
                    .build());

            if (cr != null && TextUtils.equals(cr.getLastActivityId(), activity.getClientTempId())) {
                cr.setLastActivity(activity.getId());
            }
        }

        if (activity.isCreateConversation()) {
            removeGapsBetween(batch, activity.getConversationId(), -1, activity.getPublished().getTime());
        }
    }

    private void decryptActivity(Batch batch, Activity activity, Uri keyUrl) {
        KeyObject key;
        key = keyManager.getBoundKey(keyUrl);

        if (key != null && activity.isEncrypted()) {
            try {
                CryptoUtils.decryptActivity(key, activity);
                activityDecryptedEvent.addActivity(activity);
            } catch (Exception e) {
                Ln.w(false, e, "Unable to decrypt - decryption processing failure");
                if (e instanceof IOException && (e.getCause().toString().contains(MAC_CHECK_FAILURE))) {
                    encryptionDurationMetricManager.onError(495, MAC_CHECK_FAILURE);
                } else {
                    encryptionDurationMetricManager.onError(400, e.getMessage());
                    if (e instanceof ParseException && isPlainTextMessageInActivity(activity)) {
                        batch.add(ConversationContentProviderOperation.clearActivityEncryptedFlag(
                                activity.getId(),
                                activity.getActivityData(getSelf(), gson)));
                    }
                }
            }
        } else {
            keysToFetch.add(keyUrl);
        }
    }

    private void handleAddParticipant(final Batch batch, final Activity activity) {
        Person newParticipant = (Person) activity.getObject();
        if (newParticipant == null || newParticipant.getKey() == null)
            return;

        addParticipant(batch, activity.getConversationId(), newParticipant.getKey(), false);
        updateParticipantState(batch, activity.getConversationId(), newParticipant.getKey(), ParticipantEntry.MembershipState.ACTIVE);

        if (newParticipant.getKey().equals(activity.getActor().getKey())) {
            // self add. our work is done, we don't want that shown in the list
            Ln.d("handleAddParticipant: self add. our work is done, we don't want that shown in the list");
        }
    }


    private void handleLeaveActivity(final Batch batch, final Activity activity) {
        //TODO there's some code in here to work around a server bug that leaves out the uuid for the leave object sometimes. Remove when fixed
        Person leftPerson = activity.getActor();
        if (activity.getObject() != null && activity.getObject().isPerson() && !leftPerson.equals(activity.getObject())) {
            leftPerson = (Person) activity.getObject();
        }

        if (leftPerson.getKey() == null) {
            ActorRecord ar = actorRecordProvider.get(leftPerson.getId());
            if (ar != null)
                leftPerson.setUuid(ar.getKey().getUuid());
        }

        updateParticipantState(batch, activity.getConversationId(), leftPerson.getKey(), ParticipantEntry.MembershipState.LEFT);
    }

    private void handleNewTeamConversation(final Activity activity) {
        new SafeAsyncTask<Void>() {
            @Override
            public Void call() throws Exception {
                if (!PushSyncTask.isKnownConversation(contentResolver, activity.getObject().getId())) {
                    IncrementalSyncTask task = conversationSyncQueue.getConversationFrontFillTask(activity.getObject().getId())
                            .withMaxParticipants(0)
                            .withMaxActivities(0);
                    operationQueue.newSyncTask(task);
                }
                return null;
            }
        }.execute();
    }

    /**
     * Set the "Last Active" time of a conversation participant, used for sorting the participant
     * list. The last active time can only move forward, otherwise the update will be silently
     * ignored. The last active time is when the participant last did something where
     * Activity.isNoteworthy().
     *
     * It will also call addAcknowledgement to put the implicit ack into the db.
     */
    protected void setParticipantLastActive(Batch batch, ActorRecord.ActorKey actorKey, Conversation conv, Activity activity) {

        long msTimeLastActive = activity.getPublished().getTime();

        if (msTimeLastActive == 0)
            return;


        ContentProviderOperation op = ContentProviderOperation.newUpdate(ParticipantEntry.CONTENT_URI)
                .withValue(ParticipantEntry.LAST_ACTIVE_TIME.name(), msTimeLastActive)
                .withSelection(ParticipantEntry.CONVERSATION_ID + "=? AND "
                                + ParticipantEntry.ACTOR_UUID + "=? AND "
                                + ParticipantEntry.LAST_ACTIVE_TIME + "<?",
                        new String[]{conv.getId(), actorKey.getUuid(), String.valueOf(msTimeLastActive)}
                )
                .build();

        batch.add(op);

        addAcknowledgement(batch, actorKey, conv.getId(), activity.getId(), msTimeLastActive);
    }

    /**
     * Update the actor's "last ack" and "last ack time" values in the DB. If a later activity is
     * already there this request will be ignored; the timestamp is guaranteed to only move
     * forward.
     */
    protected void handleAcknowledgement(Batch batch, Activity ack, ConversationRecord cr) {
        if (!ack.isAcknowledgeActivity())
            return;

        if (ack.getObject() == null)
            return;

        if (ack.isFromSelf(getSelf())) {
            return;
        }

        String conversationId = ack.getConversationId();
        String ackedActivityId = ack.getObject().getId();

        addAcknowledgement(batch, ack.getActor().getKey(), conversationId, ackedActivityId, ack.getAckTime());
    }

    private void handleDeleteActivity(Batch batch, Activity activity, ConversationRecord cr) {
        ActivityObject tombstoneActivity = activity.getObject();
        ActivityReference tombstoneActivityReference = ConversationContentProviderQueries.getActivityReference(getContentResolver(), tombstoneActivity.getId());
        if (tombstoneActivityReference == null) // We don't have the activity in the db yet, we'll get it when we page through history
            return;
        if (tombstoneActivityReference.getType() == ActivityEntry.Type.TOMBSTONE) // The activity is already in tombstone form, nothing to do.
            return;
        Message message = gson.fromJson(tombstoneActivityReference.getData(), tombstoneActivityReference.getType().getSyncClass());
        Tombstone tombstone = new Tombstone(activity.getActor().getKey(), message.getActorKey(), activity.getProvider());

        ContentProviderOperation op = ContentProviderOperation.newUpdate(ActivityEntry.CONTENT_URI)
                .withValue(ActivityEntry.ACTIVITY_TYPE.name(), ActivityEntry.Type.TOMBSTONE.ordinal())
                .withValue(ActivityEntry.ACTIVITY_DATA.name(), gson.toJson(tombstone))
                .withValue(ActivityEntry.CONTENT_DATA_ID.name(), null)
                .withValue(ActivityEntry.ENCRYPTION_KEY_URL.name(), null)
                .withValue(ActivityEntry.IS_ENCRYPTED.name(), 0)
                .withValue(ActivityEntry.IS_MENTION.name(), 0)
                .withSelection(ActivityEntry.ACTIVITY_ID + " = ?", new String[]{tombstoneActivity.getId()})
                .build();

        batch.add(op);

        if (cr.getLastActivityId() != null && cr.getLastActivityId().equals(tombstoneActivity.getId())) {
            ActivityReference ref = ConversationContentProviderQueries.getPreviousPreviewActivity(getContentResolver(), cr.getId(), tombstoneActivity.getId());
            cr.setLastActivity(ref.getActivityId());
            cr.setPreviewActivityPublishedTime(ref.getPublishTime());
        }
        searchManager.deleteActivityFromSearch(tombstoneActivity.getId());
    }

    private void addAcknowledgement(Batch batch, ActorRecord.ActorKey actorKey,
                                    String conversationId, String ackedActivityId, long publishTime) {

        ContentProviderOperation op = ContentProviderOperation.newUpdate(ParticipantEntry.CONTENT_URI)
                .withValue(ParticipantEntry.LASTACK_ACTIVITY_TIME.name(), publishTime)
                .withValue(ParticipantEntry.LASTACK_ACTIVITY_ID.name(), ackedActivityId)
                .withSelection(ParticipantEntry.ACTOR_UUID + "=? AND "
                                + ParticipantEntry.CONVERSATION_ID + "=? AND "
                                + ParticipantEntry.LASTACK_ACTIVITY_TIME + "<?",
                        new String[]{actorKey.getUuid(),
                                conversationId,
                                String.valueOf(publishTime)}
                )
                .build();

        batch.add(op);
    }

    protected boolean addTeam(Batch batch, Team team) {
        // insert then update. The insert may silently fail due to constraint violation if the row
        // is already there but some columns may need to be updated
        batch.add(ContentProviderOperation
                .newInsert(TeamEntry.CONTENT_URI)
                .withValue(TeamEntry.TEAM_ID.name(), team.getId())
                .withValue(TeamEntry.PRIMARY_CONVERSATION_ID.name(), team.getGeneralConversationUuid())
                .withValue(TeamEntry.COLOR.name(), team.getTeamColor())
                .build());

        ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(TeamEntry.CONTENT_URI);

        if (team.getTeamColor() != null) {
            builder.withValue(TeamEntry.COLOR.name(), team.getTeamColor());
        }
        batch.add(builder.withValue(TeamEntry.TEAM_ID.name(), team.getId())
                .withValue(TeamEntry.PRIMARY_CONVERSATION_ID.name(), team.getGeneralConversationUuid())
                .withSelection(TeamEntry.TEAM_ID + " =? ", new String[]{team.getId()})
                .build());

        return true;
    }

    /**
     * Grab the list of participants from the conversation and make sure they are in the DB
     *
     * @return true if the participant list was brought up to date
     */
    protected boolean addConversationParticipants(Batch batch, Conversation conv) {
        List<Person> serverParticipants = conv.getParticipants().getItems();

        if (serverParticipants == null || serverParticipants.isEmpty())
            return false;

        HashMap<ActorRecord.ActorKey, ParticipantEntry.MembershipState> localParticipants
                = ConversationContentProviderQueries.getConversationParticipants(getContentResolver(), conv.getId());

        for (Person person : serverParticipants) {
            boolean isModerator = (person.getRoomProperties() == null) ? false : person.getRoomProperties().isModerator();
            if (!localParticipants.containsKey(person.getKey())) {
                addParticipant(batch, conv, person.getKey(), isModerator);
            } else if (localParticipants.get(person.getKey()) != ParticipantEntry.MembershipState.ACTIVE) {
                updateParticipantState(batch, conv, person.getKey(),
                        ParticipantEntry.MembershipState.ACTIVE);
                updateParticipantModerator(batch, conv, person.getKey(),
                        isModerator);
            }

            localParticipants.remove(person.getKey());
            ActorRecord record = new ActorRecord(person);
            actorRecordProvider.monitor(record, false);
        }

        //Remaining participants have left the conv
        if (resultsIncludeCompleteParticipants(conv)) {
            for (ActorRecord.ActorKey leftperson : localParticipants.keySet()) {
                updateParticipantState(batch, conv, leftperson, ParticipantEntry.MembershipState.LEFT);
            }
        }
        return true;
    }

    /**
     * Add a participant to a conversation
     */
    protected void addParticipant(Batch batch, Conversation conv, ActorRecord.ActorKey actorKey, boolean isModerator) {
        addParticipant(batch, conv.getId(), actorKey, isModerator);
    }

    /**
     * Add a participant to a conversation
     */
    protected void addParticipant(Batch batch, String conversationId, ActorRecord.ActorKey actorKey, boolean isModerator) {
        ContentProviderOperation op = ContentProviderOperation
                .newInsert(ParticipantEntry.CONTENT_URI)
                .withValue(ParticipantEntry.CONVERSATION_ID.name(), conversationId)
                .withValue(ParticipantEntry.ACTOR_UUID.name(), actorKey.getUuid())
                .withValue(ParticipantEntry.MEMBERSHIP_STATE.name(),
                        ParticipantEntry.MembershipState.ACTIVE.ordinal())
                .withValue(ParticipantEntry.IS_MODERATOR.name(), isModerator)
                .build();

        batch.add(op);
    }

    /**
     * Set the participant state
     *
     * @param state Value from Participant.State enum
     */
    protected void updateParticipantState(Batch batch, Conversation conv, ActorRecord.ActorKey actorKey,
                                          ParticipantEntry.MembershipState state) {

        updateParticipantState(batch, conv.getId(), actorKey, state);
    }

    /**
     * Set the participant state
     *
     * @param state Value from Participant.State enum
     */
    protected void updateParticipantState(Batch batch, String convId, ActorRecord.ActorKey actorKey,
                                          ParticipantEntry.MembershipState state) {

        ContentProviderOperation op = ContentProviderOperation.newUpdate(ParticipantEntry.CONTENT_URI)
                .withValue(ParticipantEntry.MEMBERSHIP_STATE.name(), state.ordinal())
                .withSelection(ParticipantEntry.CONVERSATION_ID + "=? AND "
                                + ParticipantEntry.ACTOR_UUID + "=?",
                        new String[]{convId, actorKey.getUuid()}
                )
                .build();

        batch.add(op);

        if (state == ParticipantEntry.MembershipState.LEFT) {
            op = ContentProviderOperation.newUpdate(ParticipantEntry.CONTENT_URI)
                    .withValue(ParticipantEntry.IS_MODERATOR.name(), 0)
                    .withSelection(ParticipantEntry.CONVERSATION_ID + "=? AND "
                                    + ParticipantEntry.ACTOR_UUID + "=? AND "
                                    + ParticipantEntry.IS_MODERATOR + "=1 ",
                            new String[]{convId, actorKey.getUuid()})
                    .build();
            batch.add(op);
        }
    }

    /**
     * Set the participant maderator flag
     *
     * @param isModerator Moderator Flag
     */
    protected void updateParticipantModerator(Batch batch, Conversation conv, ActorRecord.ActorKey actorKey,
                                              boolean isModerator) {

        updateParticipantModerator(batch, conv.getId(), actorKey, isModerator);
    }

    /**
     * Set the participant maderator flag
     *
     * @param isModerator Moderator Flag
     */
    protected void updateParticipantModerator(Batch batch, String convId, ActorRecord.ActorKey actorKey,
                                              boolean isModerator) {

        ContentProviderOperation op = ContentProviderOperation.newUpdate(ParticipantEntry.CONTENT_URI)
                .withValue(ParticipantEntry.IS_MODERATOR.name(), isModerator)
                .withSelection(ParticipantEntry.CONVERSATION_ID + "=? AND "
                                + ParticipantEntry.ACTOR_UUID + "=?",
                        new String[]{convId, actorKey.getUuid()}
                )
                .build();

        batch.add(op);
    }

    protected void addConversationMetadata(Batch batch, ConversationRecord cr) {
        if (cr.getTitleKeyUrl() != null) {
            KeyObject key = keyManager.getBoundKey(cr.getTitleKeyUrl());
            if (key != null && !TextUtils.isEmpty(key.getKey())) {
                try {
                    cr.decryptTitleAndSummary(new KeyObject(cr.getTitleKeyUrl(), key.getKey(), key.getKeyId()));
                    cr.setAreTitleAndSummaryEncrypted(false);
                } catch (IOException | ParseException e) {
                    Ln.d(e, "Failed decrypting title and/or summary");
                }

            } else {
                //TODO do we need keysToFetch anymore?
                keysToFetch.add(cr.getTitleKeyUrl());
                cr.setAreTitleAndSummaryEncrypted(true);
            }
        }

        if (cr.getAvatarEncryptionKeyUrl() != null) {
            KeyObject key = keyManager.getBoundKey(cr.getAvatarEncryptionKeyUrl());
            if (key != null && !TextUtils.isEmpty(key.getKey())) {
                try {
                    cr.decryptAvatarScr(key);
                    cr.setIsAvatarEncrypted(false);
                } catch (IOException | ParseException | JOSEException e) {
                    Ln.d(e, "Failed decrypting conversation avatar");
                }
            } else {
                keysToFetch.add(cr.getAvatarEncryptionKeyUrl());
                cr.setIsAvatarEncrypted(true);
            }
        }

        if (cr.getDefaultEncryptionKeyUrl() != null && keyManager.getBoundKey(cr.getDefaultEncryptionKeyUrl()) == null) {
            keysToFetch.add(cr.getDefaultEncryptionKeyUrl());
        }

        // Make sure the conversation is there. Fail duplicate insert silently.
        if (!cr.isBuiltFromCursor())
            batch.add(cr.getInsertOperation().build());
        cr.addUpdateOperations(batch, getYieldAllowed());
        updatedConversations.add(cr);
    }

    /**
     * Adds a BACKFILL_GAP or FORWARDFILL_GAP row for the given conversation to the batch. A
     * BACKFILL_GAP row indicates data may be missing between time mstime and the publish time of
     * the first row after mstime (inclusive) and a FORWARDFILL_GAP row indicates data may be
     * missing between mstime and the publish time of the last row before mstime (inclusive).
     *
     * @param conversationId conversation to add the row to
     * @param mstime         epoch time for the gap row
     * @param gapType        type of gap row to add
     * @return the GUID (ACTIVITY_ID) of the gap row at mstime.
     */
    protected String addGapRowAtTime(Batch batch, String conversationId, long mstime, ActivityEntry.Type gapType) {

        Ln.d("Adding " + gapType.name() + " row at " + conversationId + " : " + mstime);

        //cheap way of ensuring that we don't have two gaps at the same place in the convo. There
        // are currently no plans to require UUID types in activity_id column
        String newActivityId = conversationId + "_GAP_" + mstime;

        ContentProviderOperation op = ContentProviderOperation
                .newInsert(ActivityEntry.CONTENT_URI)
                .withValue(ActivityEntry.ACTIVITY_ID.name(), newActivityId)
                .withValue(ActivityEntry.CONVERSATION_ID.name(), conversationId)
                .withValue(ActivityEntry.ACTIVITY_TYPE.name(),
                        gapType.ordinal())
                .withValue(ActivityEntry.ACTIVITY_PUBLISHED_TIME.name(), mstime)
                .withValue(ActivityEntry.SOURCE.name(), ActivityEntry.Source.LOCAL.ordinal())
                .withYieldAllowed(getYieldAllowed())
                .build();

        batch.add(op);

        return newActivityId;
    }

    /**
     * This adds a BACKFILL_GAP to the DB just before the activity passed as a parameter
     *
     * @param batch    - DB Batch to add this insert to
     * @param activity - Activity that the gap should be placed before. Note that this activity
     *                 should never be a gap and if a gap activity is passed to this function no new
     *                 gaps will be added
     */
    protected String addGapRowBeforeActivity(@NonNull Batch batch, @NonNull Activity activity) {
        if (activity.isCreateConversation() || activity.getType() == ActivityEntry.Type.BACKFILL_GAP || activity.getType() == ActivityEntry.Type.FORWARDFILL_GAP)
            return null;

        return addGapRowAtTime(batch, activity.getConversationId(), activity.getPublished().getTime() - 1, ActivityEntry.Type.BACKFILL_GAP);
    }

    /**
     * This adds a FORWARDFILL_GAP just after the activity passed as a parameter
     *
     * @param batch    - DB Batch to add this insert to
     * @param activity - Activity that the gap should be placed after. Note that this activity
     *                 should never be a gap and if a gap activity is passed to this function no new
     *                 gaps will be added
     */
    protected String addGapRowAfterActivity(@NonNull Batch batch, @NonNull Activity activity) {
        if (activity.getType() == ActivityEntry.Type.BACKFILL_GAP || activity.getType() == ActivityEntry.Type.FORWARDFILL_GAP)
            return null;

        return addGapRowAtTime(batch, activity.getConversationId(), activity.getPublished().getTime() + 1, ActivityEntry.Type.FORWARDFILL_GAP);
    }

    /**
     * This adds a FORWARDFILL_GAP just after the activity passed as a parameter and a BACKFILL_GAP
     * just before it
     */
    protected void addGapRowsAroundActivity(@NonNull Batch batch, @NonNull Activity activity) {
        addGapRowBeforeActivity(batch, activity);
        addGapRowAfterActivity(batch, activity);
    }

    /**
     * Remove BACKFILL_GAP and FORWARDFILL_GAP rows for a conversation between times msMin and
     * msMax, including msMin.
     */
    protected void removeGapsBetween(Batch batch, String conversationId, long msMintime, long msMaxtime) {

        Ln.d("Removing gaps for conv " + conversationId + " " + msMintime + "-" + msMaxtime);
        ContentProviderOperation op = ContentProviderOperation
                .newDelete(ActivityEntry.CONTENT_URI)
                .withSelection(ActivityEntry.CONVERSATION_ID + "=? AND "
                                + ActivityEntry.ACTIVITY_PUBLISHED_TIME + ">? AND "
                                + ActivityEntry.ACTIVITY_PUBLISHED_TIME + "<=? AND ("
                                + ActivityEntry.ACTIVITY_TYPE + "=? OR "
                                + ActivityEntry.ACTIVITY_TYPE + "=?)",
                        new String[]{conversationId,
                                String.valueOf(msMintime),
                                String.valueOf(msMaxtime),
                                String.valueOf(ActivityEntry.Type.BACKFILL_GAP.ordinal()),
                                String.valueOf(ActivityEntry.Type.FORWARDFILL_GAP.ordinal())}
                )
                .withYieldAllowed(getYieldAllowed())
                .build();

        batch.add(op);
    }

    public void postEventsToBus() {
        if (!activityDecryptedEvent.getActivities().isEmpty())
            eventBus.post(activityDecryptedEvent);
    }

    protected AuthenticatedUser getSelf() {
        return apiTokenProvider.getAuthenticatedUser();
    }

    protected boolean resultsIncludeCompleteParticipants(Conversation conv) {
        return false;
    }

    public boolean getYieldAllowed() {
        return true;
    }

    public boolean isPlainTextMessageInActivity(Activity activity) {
        if (!activity.isPostOrAddComment()) {
            return false;
        }
        boolean isPlainText = CryptoUtils.isPlainTextMessage(activity.getObject().getDisplayName());
        if (isPlainText) {
            activity.setEncrypted(false);
        }
        return isPlainText;
    }

    protected void sendEncryptionMetrics() {
        encryptionDurationMetricManager.enqueueReport();
    }

    public ContentResolver getContentResolver() {
        return contentResolver;
    }
}
