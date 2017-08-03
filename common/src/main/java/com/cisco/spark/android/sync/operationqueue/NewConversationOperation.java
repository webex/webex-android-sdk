package com.cisco.spark.android.sync.operationqueue;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.events.OperationCompletedEvent;
import com.cisco.spark.android.metrics.SegmentService;
import com.cisco.spark.android.metrics.TimingProvider;
import com.cisco.spark.android.metrics.model.GenericMetric;
import com.cisco.spark.android.metrics.value.ClientMetricField;
import com.cisco.spark.android.metrics.value.ClientMetricNames;
import com.cisco.spark.android.metrics.value.ClientMetricTag;
import com.cisco.spark.android.metrics.value.GenericMetricTagEnums;
import com.cisco.spark.android.model.Conversation;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.model.ParticipantTag;
import com.cisco.spark.android.model.Participants;
import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.model.Team;
import com.cisco.spark.android.model.UserEmailRequest;
import com.cisco.spark.android.model.UserIdentityKey;
import com.cisco.spark.android.sync.ActorRecord;
import com.cisco.spark.android.sync.ActorRecordProvider;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContentProviderOperation;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.sync.queue.ConversationFrontFillTask;
import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.util.LoggingUtils;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.util.UriUtils;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContract.ConversationEntry;
import static com.cisco.spark.android.sync.ConversationContract.ParticipantEntry;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncStateFailureReason;
import static com.cisco.spark.android.sync.ConversationContract.TeamEntry;

public class NewConversationOperation extends PostKmsMessageOperation implements ConversationOperation {

    @Inject
    transient Gson gson;

    @Inject
    transient KeyManager keyManager;

    @Inject
    transient ApiClientProvider apiClientProvider;

    @Inject
    transient ApiTokenProvider apiTokenProvider;

    @Inject
    transient OperationQueue operationQueue;

    @Inject
    transient ActorRecordProvider actorRecordProvider;

    @Inject
    transient EncryptedConversationProcessor conversationProcessor;

    @Inject
    transient EventBus bus;

    @Inject
    transient TimingProvider timingProvider;

    @Inject
    transient SegmentService segmentService;


    private Participants participants = new Participants();
    private String conversationId;
    private boolean hasMessages;
    private String title;
    private String summary;
    private String teamId;
    private Uri teamKROURI;
    private int serverStatusCode;
    private boolean isRoomPreExisting;
    private Conversation result = null;
    private EnumSet<CreateFlags> createFlags = EnumSet.noneOf(CreateFlags.class);

    private transient GenericMetric metric;

    private Team teamResult;

    public enum CreateFlags {
        IS_FIRST_CONVERSATION,
        ONE_ON_ONE,
        PERSIST_WITHOUT_MESSAGES,
        NEW_TEAM,
        CLIENT_SIDE_INVITE,
        ESCALATE_ONE_ON_ONE
    }

    // The key is transient because re-using the same key twice is very bad. For safety, an operation that
    // is persisted and survives across app instances should get a fresh key when it wakes up again.
    private transient KeyObject key;


    // Replaced by CreateFlags, included here to handle deserializing ops from previous versions
    @Deprecated
    @Expose(serialize = false)
    private boolean isFirstConversation;
    @Deprecated
    @Expose(serialize = false)
    private boolean shouldPersistWithoutMessages;
    @Deprecated
    @Expose(serialize = false)
    private boolean serverSideInvite;
    @Deprecated
    @Expose(serialize = false)
    private boolean isTeam;


    @NonNull
    @Override
    public SyncOperationEntry.OperationType getOperationType() {
        return SyncOperationEntry.OperationType.CREATE_CONVERSATION;
    }

    public NewConversationOperation(Injector injector, Collection<String> emailsOrUuids, String title, String summary, @Nullable String teamId, EnumSet<CreateFlags> createFlags) {
        super(injector);
        this.title = title;
        this.summary = summary;
        this.teamId = teamId;
        for (String emailOrUuid : emailsOrUuids) {
            ActorRecord actor = actorRecordProvider.getCached(emailOrUuid);
            if (actor == null) {
                participants.add(new Person(emailOrUuid));
            } else {
                participants.add(new Person(actor));
            }
        }
        conversationId = getOperationId();

        if (createFlags != null)
            getCreateFlags().addAll(createFlags);
    }

    public boolean isRoomPreExisting() {
        return isRoomPreExisting;
    }

    @Override
    public void initialize(Injector injector) {
        super.initialize(injector);

        this.metric = GenericMetric.buildOperationalMetric(ClientMetricNames.CLIENT_CREATE_CONVERSATION);

        GenericMetricTagEnums.ConversationTypeMetricTagValue value;

        if (createFlags != null) {
            if (createFlags.contains(CreateFlags.ONE_ON_ONE)) {
                value = GenericMetricTagEnums.ConversationTypeMetricTagValue.CONVERSATION_TYPE_ONE_TO_ONE;
            } else if (!TextUtils.isEmpty(teamId)) {
                value = GenericMetricTagEnums.ConversationTypeMetricTagValue.CONVERSATION_TYPE_TEAM_SPACE;
            } else if (createFlags.contains(CreateFlags.NEW_TEAM)) {
                value = GenericMetricTagEnums.ConversationTypeMetricTagValue.CONVERSATION_TYPE_NEW_TEAM;
            } else {
                value = GenericMetricTagEnums.ConversationTypeMetricTagValue.CONVERSATION_TYPE_GROUP;
            }

            metric.addTag(ClientMetricTag.METRIC_TAG_CONVERSATION_TYPE, value);
        }
    }

    @NonNull
    @Override
    protected SyncState onEnqueue() {
        if (isOneOnOne()) {
            Person otherParticipant = participants.get().get(0);
            String otherEmail = Strings.coalesce(otherParticipant.getEmail(), "");
            String otherUuid = Strings.coalesce(otherParticipant.getUuid(), otherParticipant.getEmail(), "");

            String existing = ConversationContentProviderQueries.getOneValue(getContentResolver(),
                    ConversationEntry.CONVERSATION_ID,
                    ConversationEntry.ONE_ON_ONE_PARTICIPANT + "=? OR " + ConversationEntry.ONE_ON_ONE_PARTICIPANT + "=?",
                    new String[]{otherEmail, otherUuid});
            if (!TextUtils.isEmpty(existing)) {
                Ln.i("Using existing 1:1 conversation " + existing);
                conversationId = existing;

                isRoomPreExisting = true;

                ContentValues cv = new ContentValues();
                cv.put(ConversationEntry.SYNC_OPERATION_ID.name(), getOperationId());

                getContentResolver().update(ConversationEntry.CONTENT_URI,
                        cv,
                        ConversationEntry.CONVERSATION_ID + "=?",
                        new String[]{conversationId});

                return SyncState.SUCCEEDED;
            }
        }

        Batch batch = newBatch();
        ContentProviderOperation.Builder convbuilder = ContentProviderOperation.newInsert(ConversationEntry.CONTENT_URI)
                .withValue(ConversationEntry.CONVERSATION_ID.name(), getOperationId())
                .withValue(ConversationEntry.SYNC_OPERATION_ID.name(), getOperationId())
                .withValue(ConversationEntry.TITLE.name(), title)
                .withValue(ConversationEntry.CONVERSATION_DISPLAY_NAME.name(), title)
                .withValue(ConversationEntry.PARTICIPANT_COUNT.name(), participants.size())
                .withValue(ConversationEntry.SUMMARY.name(), summary);

        if (teamId != null) {
            convbuilder.withValue(ConversationEntry.TEAM_ID.name(), teamId);
        }

        if (isOneOnOne()) {
            ActorRecord actor = actorRecordProvider.get(getOneOnOneParticipant().getId());
            String displayName = getOneOnOneParticipant().getDisplayName();
            if (actor != null)
                displayName = actor.getDisplayName();
            convbuilder = convbuilder
                    .withValue(ConversationEntry.ONE_ON_ONE_PARTICIPANT.name(), getOneOnOneParticipant().getEmail())
                    .withValue(ConversationEntry.CONVERSATION_DISPLAY_NAME.name(), displayName);
        }

        ArrayList<ActorRecord> topParticipants = new ArrayList<>();
        for (Person person : participants.get()) {

            ActorRecord actor = actorRecordProvider.get(person);

            if (topParticipants.size() < 4) {
                ActorRecord topParticipant = actor;
                if (topParticipant == null) {
                    topParticipant = new ActorRecord(person.getKey(), person.getEmail(), person.getDisplayName(), person.getType(), true, null, false);
                }

                topParticipants.remove(topParticipant);

                if (!topParticipant.isAuthenticatedUser(apiTokenProvider.getAuthenticatedUser()))
                    topParticipants.add(topParticipant);
            }

            // Provisional actor, all we have is email addy
            if (actor == null) {
                batch.add(ContentProviderOperation.newInsert(ConversationContract.ActorEntry.CONTENT_URI)
                        .withValue(ConversationContract.ActorEntry.ACTOR_UUID.name(), person.getEmail())
                        .withValue(ConversationContract.ActorEntry.EMAIL.name(), person.getEmail())
                        .build());
                if (!Strings.isEmpty(person.getOrgId())) {
                    batch.add(ContentProviderOperation.newInsert(ConversationContract.OrganizationEntry.CONTENT_URI)
                            .withValue(ConversationContract.OrganizationEntry.ORG_ID.name(), person.getOrgId())
                            .build());
                }
            }

            writeParticipantToTable(batch, person);
        }

        // Authenticated user goes in the participants table too
        writeParticipantToTable(batch, new Person(apiTokenProvider.getAuthenticatedUser()));

        if (topParticipants != null && !topParticipants.isEmpty()) {
            convbuilder = convbuilder.withValue(
                    ConversationEntry.TOP_PARTICIPANTS.name(), gson.toJson(topParticipants)
            );
        }

        batch.add(convbuilder.build());

        batch.apply();
        return SyncState.READY;
    }

    private void writeParticipantToTable(Batch batch, Person person) {
        batch.add(ContentProviderOperation.newInsert(ParticipantEntry.CONTENT_URI)
                .withValue(ParticipantEntry.CONVERSATION_ID.name(), conversationId)
                .withValue(ParticipantEntry.ACTOR_UUID.name(), person.getId())
                .build()
        );
    }

    private void applyKeyToConversation(KeyObject key) {
        if (key != null) {
            // new conversation : default key and conversation key (for title) are the same at this point

            ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(ConversationEntry.CONTENT_URI)
                    .withSelection(ConversationEntry.SYNC_OPERATION_ID.name() + "=?", new String[]{getOperationId()})
                    .withValue(ConversationEntry.IS_TITLE_ENCRYPTED.name(), String.valueOf(0))
                    .withValue(ConversationEntry.TITLE_ENCRYPTION_KEY_URL.name(), key.getKeyUrl().toString())
                    .withValue(ConversationEntry.DEFAULT_ENCRYPTION_KEY_URL.name(), key.getKeyUrl().toString());

            Batch batch = newBatch();
            batch.add(builder.build());
            batch.add(ConversationContentProviderOperation.insertEncryptionKey(key.getKeyUrl().toString(), key.getKey(), UriUtils.toString(key.getKeyId()), 0));
            batch.apply();
        }
    }

    @NonNull
    @Override
    protected SyncState doWork() throws IOException {
        if (!hasMessages && !getCreateFlags().contains(CreateFlags.PERSIST_WITHOUT_MESSAGES)) {
            Ln.d("Skipping doWork because this is we have no messages that should not be persisted. " + this);
            return SyncState.READY;
        }

        if (key == null || TextUtils.isEmpty(key.getKey())) {
            key = keyManager.getCachedUnboundKey();
        }

        if (key == null || TextUtils.isEmpty(key.getKey())) {
            return SyncState.PREPARING;
        }

        Response response = null;
        try {
            if (isOneOnOne() && oneOnOneExists()) {
                return SyncState.SUCCEEDED;
            }

            applyKeyToConversation(key);

            Conversation.Builder conversationBuilder = new Conversation.Builder()
                    .setCreator(new Person(apiTokenProvider.getAuthenticatedUser()))
                    .setEncryptionKeyUrl(key.getKeyUrl())
                    .setIsEncrypted(key.getKey() == null)
                    .setClientTempId(getOperationId());

            List<Person> participantsWithIds = resolvePersonUuids(participants.get());
            List<String> participantIds = new ArrayList<>();
            for (Person participantWithId : participantsWithIds) {
                participantIds.add(participantWithId.getId());
            }

            // If we're attempting to post a team conversation and are missing the team's KRO URI, bail out and wait for a retry
            if (teamId != null) {
                teamKROURI = getTeamKro();
                if (teamKROURI == null) {
                    Ln.i("Trying to create a team conversation but missing the team's KRO URI, bailing out and waiting for a retry");
                    return SyncState.READY;
                } else {
                    participantIds.add(teamKROURI.toString());
                }
            }

            String kmsMessage = conversationProcessor.createNewResource(participantIds, Collections.singletonList(key.getKeyId()));

            conversationBuilder.setEncryptedKmsMessage(kmsMessage);
            conversationBuilder.setIsOneOnOne(isOneOnOne());

            for (Person participant : participantsWithIds) {
                conversationBuilder.addParticipant(participant);
            }

            if (Strings.notEmpty(title)) {
                conversationBuilder.setTitle(title);
            }

            if (!TextUtils.isEmpty(summary)) {
                conversationBuilder.setSummary(summary);
            }

            if (teamId != null) {
                conversationBuilder.setTeamShellWithTeamId(teamId);
            }

            Conversation conv = conversationBuilder.build();
            try {
                conv.encrypt(key);
            } catch (IOException e) {
                Ln.e(true, e, "Failed to encrypt title and/or summary, getting rid of the potentially bad key and preparing for a retry");
                key = null;
                return SyncState.READY;
            }

            // If teamId != null, it's a conversation within a team. If isTeam == true, it is a team
            if (teamId != null && !isTeam()) {
                response = apiClientProvider.getConversationClient().postTeamConversation(teamId, conv).execute();
                if (response.isSuccessful()) {
                    result = (Conversation) response.body();
                } else {
                    Ln.w("Failed posting team conversation " + LoggingUtils.toString(response));
                }
            } else if (isTeam()) {
                response = apiClientProvider.getConversationClient().postTeam(conv).execute();
                if (response.isSuccessful()) {
                    teamResult = (Team) response.body();
                    try {
                        teamResult.decrypt(key);
                    } catch (Exception e) {
                        Ln.w("Failed decrypting new team object");
                    }
                    persistTeam(teamResult);
                    result = teamResult.getConversations().getItems().get(0);
                } else {
                    Ln.w("Failed posting new team " + LoggingUtils.toString(response));
                }
            } else {
                response = apiClientProvider.getConversationClient().postConversation(conv).execute();
                if (response.isSuccessful()) {
                    result = (Conversation) response.body();
                } else {
                    Ln.w("Failed posting new conversation " + LoggingUtils.toString(response));
                }
            }

            metric.addNetworkStatus(response);
            metric.addField(ClientMetricField.METRIC_FIELD_PERCIEVED_DURATION, timingProvider.get(getOperationId()).finish());

            if (result != null && !TextUtils.equals(getOperationId(), result.getId())) {
                try {
                    result.decrypt(key);
                } catch (Exception e) {
                    Ln.e(e, "Failed decrypting a newly created conversation");
                }

                conversationId = result.getId();
                persistConversation(result);

                return SyncState.SUCCEEDED;
            }

            if (response.code() >= 400 && response.code() < 500) {
                Ln.w("Failed creating new conversation; " + LoggingUtils.toString(response));
                if (response.code() == 400) {
                    String message = CryptoUtils.getKmsErrorMessage(response, gson, keyManager.getSharedKeyAsJWK());
                    if (!TextUtils.isEmpty(message)) {
                        setErrorMessage(message);
                        Ln.d("KMS ERROR: " + message);
                    }
                }
                serverStatusCode = response.code();
                // CANCELED because otherwise it won't be cleaned up
                setFailureReason(SyncStateFailureReason.CANCELED);
                return SyncState.FAULTED;
            }
        } finally {

            if (isTeam()) {
                GenericMetric metric = GenericMetric.buildBehavioralMetric(ClientMetricNames.ONBOARDING_CREATED_TEAM)
                        .withNetworkTraits(response);
                metric.addField(ClientMetricField.METRIC_FIELD_TEAM_NAME_WORD_COUNT, title.split("\\s+").length);
                metric.addField(ClientMetricField.METRIC_FIELD_TEAM_NAME_CHARACTER_COUNT, title.replaceAll("\\s+", "").length());
            }

            postSegmentMetrics(response);
        }
        return SyncState.READY;
    }

    private boolean isTeam() {
        return getCreateFlags().contains(CreateFlags.NEW_TEAM);
    }

    /**
     * It's possible for the user to create a duplicate 1:1 via search or avatar tapping during
     * initial sync if we have not received the existing one yet. This function detects that case
     * and patches it up.
     * <p/>
     * To fix, set the provisional conv's ID to the real one and do a FrontFill.
     *
     * @return true if we verify that the 1:1 conversation exists on the server
     */
    private boolean oneOnOneExists() throws IOException {
        if (!isOneOnOne())
            return false;

        retrofit2.Response<Conversation> response = apiClientProvider.getConversationClient().getOneOnOneConversation(getOneOnOneParticipant().getId()).execute();
        if (response.isSuccessful()) {
            Conversation conv = response.body();
            conversationId = conv.getId();
            Ln.i(this + " : One-on-One conversation already exists: " + conversationId);
            persistConversation(conv);
            return true;
        }

        return false;
    }

    /**
     * Ensure the Persons in the list are well-formed
     * @param persons
     * @return a new List of Person objects with UUID's
     */
    private List<Person> resolvePersonUuids(List<Person> persons) throws IOException {
        ArrayList<Person> ret = new ArrayList<>();
        ArrayList<UserEmailRequest> toRequest = new ArrayList<>();

        for (Person p : persons) {
            if (TextUtils.isEmpty(p.getUuid())) {
                toRequest.add(new UserEmailRequest(p.getEmail()));
            } else {
                ret.add(p);
            }
        }

        metric.addField(ClientMetricField.METRIC_FIELD_UUID_COUNT, ret.size());
        metric.addField(ClientMetricField.METRIC_FIELD_EMAIL_COUNT, toRequest.size());

        if (!toRequest.isEmpty()) {
            retrofit2.Response<Map<String, UserIdentityKey>> response = apiClientProvider.getUserClient().getOrCreateUserID(apiTokenProvider.getAuthenticatedUser().getConversationAuthorizationHeader(), toRequest).execute();
            if (response.isSuccessful()) {
                Batch batch = newBatch();

                for (String email : response.body().keySet()) {
                    UserIdentityKey userIdentityKey = response.body().get(email);
                    if (userIdentityKey.getId() == null) {
                        Ln.v("Failed getting or creating UUID for " + email);
                        continue;
                    }

                    Person person = new Person(new ActorRecord.ActorKey(userIdentityKey.getId()));
                    person.setEmail(email);
                    if (!userIdentityKey.isUserExists())
                        person.getTags().add(ParticipantTag.NOT_SIGNED_UP);

                    ret.add(person);

                    ContentProviderOperation op = ContentProviderOperation.newInsert(ConversationContract.ActorEntry.CONTENT_URI)
                            .withValue(ConversationContract.ActorEntry.ACTOR_UUID.name(), person.getId())
                            .withValue(ConversationContract.ActorEntry.EMAIL.name(), email)
                            .withValue(ConversationContract.ActorEntry.ENTITLEMENT_SQUARED.name(), userIdentityKey.isUserExists() ? 1 : 0)
                            .build();
                    batch.add(op);
                    if (!Strings.isEmpty(person.getOrgId())) {
                        op = ContentProviderOperation.newInsert(ConversationContract.OrganizationEntry.CONTENT_URI)
                                .withValue(ConversationContract.OrganizationEntry.ORG_ID.name(), person.getOrgId())
                                .build();
                        batch.add(op);
                    }
                }
                batch.apply();
            }
        }

        return ret;
    }

    @NonNull
    @Override
    public SyncState onPrepare() {
        SyncState ret = super.onPrepare();

        if (ret != SyncState.READY)
            return ret;

        if (!hasMessages && !getCreateFlags().contains(CreateFlags.PERSIST_WITHOUT_MESSAGES))
            return SyncState.PREPARING;

        if (keyManager.getUnboundKeyCount() == 0) {
            setDependsOn(operationQueue.refreshUnboundKeys());
            return SyncState.PREPARING;
        }
        return SyncState.READY;
    }

    private void persistTeam(Team team) {
        Batch batch = newBatch();
        ContentProviderOperation op = ContentProviderOperation
                .newInsert(TeamEntry.CONTENT_URI)
                .withValue(TeamEntry.TEAM_ID.name(), team.getId())
                .withValue(TeamEntry.COLOR.name(), team.getTeamColor())
                .withValue(TeamEntry.PRIMARY_CONVERSATION_ID.name(), team.getGeneralConversationUuid())
                .build();
        batch.add(op);

        op = ContentProviderOperation
                .newUpdate(TeamEntry.CONTENT_URI)
                .withValue(TeamEntry.COLOR.name(), team.getTeamColor())
                .withValue(TeamEntry.PRIMARY_CONVERSATION_ID.name(), team.getGeneralConversationUuid())
                .withSelection(TeamEntry.TEAM_ID.name() + "=?", new String[]{team.getId()})
                .build();
        batch.add(op);

        op = ContentProviderOperation
                .newUpdate(ConversationEntry.CONTENT_URI)
                .withValue(ConversationEntry.TEAM_ID.name(), team.getId())
                .withSelection(ConversationEntry.CONVERSATION_ID + "=? OR " + ConversationEntry.CONVERSATION_ID + "=?", new String[]{getOperationId(), team.getGeneralConversationUuid()})
                .build();
        batch.add(op);

        batch.apply();
    }

    private void persistConversation(final Conversation conversation) {

        ln.i(this + " : Persisting conversation " + conversation);

        conversation.setClientTempId(getOperationId());
        new ConversationFrontFillTask(injector, conversation).execute();

        Batch batch = newBatch();

        // Add a database write to be absolutely sure that the real conversation id is persisted
        // correctly in the database, because once the operation is complete we don't have
        // a good way to map the provisionalId from this operation to the real conversation id
        ContentProviderOperation persistOperation = ContentProviderOperation
                .newUpdate(ConversationEntry.CONTENT_URI)
                .withValue(ConversationEntry.CONVERSATION_ID.name(), conversation.getId())
                .withSelection(ConversationEntry.CONVERSATION_ID + "=? OR " + ConversationEntry.CONVERSATION_ID + "=?", new String[]{getOperationId(), conversation.getId()})
                .build();
        batch.add(persistOperation);

        if (!conversation.getId().equals(getOperationId())) {
            ContentProviderOperation op = ContentProviderOperation
                    .newDelete(ParticipantEntry.CONTENT_URI)
                    .withSelection(ParticipantEntry.CONVERSATION_ID + "=?", new String[]{getOperationId()})
                    .build();
            batch.add(op);
        }
        batch.apply();
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
        if (!(newOperation instanceof ConversationOperation)) {
            return;
        }

        // If the new operation is creating a 1:1 with the same participant they must have abandoned the first attempt
        // and are trying again. Cancel the first one.
        if (newOperation.getOperationType() == SyncOperationEntry.OperationType.CREATE_CONVERSATION
                && isOneOnOne()
                && getOneOnOneParticipant().equals(((NewConversationOperation) newOperation).getOneOnOneParticipant())) {
            Ln.i("Duplicate 1:1; canceling");
            cancel();
            return;
        }

        // If the new operation implements the ConversationOperation interface, and its getConversation()
        // returns the one we're creating, make it depend on us.
        //
        // A few of the operations have extra handling but we get the dependency for free with the
        // ConversationOperation interface.

        String newOpConvId = ((ConversationOperation) newOperation).getConversationId();

        if (!TextUtils.equals(newOpConvId, getConversationId()) && !TextUtils.equals(newOpConvId, getOperationId()))
            return;

        switch (newOperation.getOperationType()) {
            case MESSAGE:
            case MESSAGE_WITH_CONTENT:
                hasMessages = true;
                break;
            case CONVERSATION_TITLE_AND_SUMMARY:
                SetTitleAndSummaryOperation setTitleAndSummaryOperation = (SetTitleAndSummaryOperation) newOperation;
                title = setTitleAndSummaryOperation.getTitle();
                summary = setTitleAndSummaryOperation.getSummary();
                if (getState().isPreExecute())
                    newOperation.cancel();
                break;
            case MARK_CONVERSATION_READ:
                if (!getState().isTerminal())
                    newOperation.cancel();
                break;
        }

        if (!getState().isTerminal() && !newOperation.getState().isTerminal())
            newOperation.setDependsOn(this);

    }

    public boolean isOneOnOne() {
        return getCreateFlags().contains(CreateFlags.ONE_ON_ONE);
    }

    public Person getOneOnOneParticipant() {
        if (isOneOnOne()) {
            return participants.get().get(0);
        }
        return null;
    }

    public String getConversationId() {
        return conversationId;
    }

    @Override
    public boolean needsNetwork() {
        return getState() != SyncState.FAULTED && getState() != SyncState.UNINITIALIZED;
    }

    @Override
    protected void onStateChanged(SyncState oldState) {
        if (getState() == SyncState.FAULTED) {
            bus.post(new OperationCompletedEvent(this));

            if (isSafeToRemove()) {
                // Roll back the DB
                getContentResolver().delete(Uri.withAppendedPath(ConversationEntry.CONTENT_URI, getOperationId()), null, null);

                // Cancel any operations that depend on this conv
                for (Operation op : operationQueue.getPendingOperations()) {
                    if (op instanceof ConversationOperation && ((ConversationOperation) op).getConversationId().equals(getConversationId())) {
                        op.cancel();
                    }
                }
            }
        }

        if (getState().isTerminal()) {
            bus.post(new NewConversationOperationCompletedEvent(this));

            operationQueue.postGenericMetric(metric);
        }
    }

    @Override
    public boolean isSafeToRemove() {
        if (!super.isSafeToRemove())
            return false;

        // remove succeeded ops regardless; they're already persisted
        if (getState() == SyncState.SUCCEEDED)
            return true;

        //failed
        if (getFailureReason() == SyncStateFailureReason.CANCELED)
            return true;

        return !getCreateFlags().contains(CreateFlags.PERSIST_WITHOUT_MESSAGES);
    }

    /**
     * @return The fully-formed conversation returned by the server, or null if the operation is
     * still in progress
     */
    public Conversation getResult() {
        return result;
    }

    public Team getTeamResult() {
        return teamResult;
    }

    public int getServerStatusCode() {
        return serverStatusCode;
    }

    public static class NewConversationOperationCompletedEvent {
        public NewConversationOperation operation;

        NewConversationOperationCompletedEvent(NewConversationOperation op) {
            this.operation = op;
        }
    }

    private Uri getTeamKro() {
        if (!TextUtils.isEmpty(teamId) && teamKROURI == null) {
            teamKROURI = KeyManager.getTeamKroUri(getContentResolver(), apiClientProvider, teamId);
        }
        return teamKROURI;
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy(3)
                .withAttemptTimeout(1, TimeUnit.MINUTES)
                .withJobTimeout(Long.MAX_VALUE);
    }

    private EnumSet<CreateFlags> getCreateFlags() {
        if (createFlags == null) {
            createFlags = EnumSet.noneOf(CreateFlags.class);
            if (isFirstConversation)
                createFlags.add(CreateFlags.IS_FIRST_CONVERSATION);
            if (!serverSideInvite)
                createFlags.add(CreateFlags.CLIENT_SIDE_INVITE);
            if (shouldPersistWithoutMessages)
                createFlags.add(CreateFlags.PERSIST_WITHOUT_MESSAGES);
            if (isTeam)
                createFlags.add(CreateFlags.NEW_TEAM);
        }

        return createFlags;
    }

    @Override
    public boolean shouldPersist() {
        if (isOneOnOne() && !hasMessages)
            return false;

        return super.shouldPersist();
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        if (newOperation.getOperationType() != getOperationType())
            return false;

        // Handle the case where we already have an operation to create this 1:1

        if (!getCreateFlags().contains(CreateFlags.ONE_ON_ONE))
            return false;

        NewConversationOperation newNewConvOperation = ((NewConversationOperation) newOperation);

        if (!newNewConvOperation.getCreateFlags().contains(CreateFlags.ONE_ON_ONE))
            return false;

        if (getOneOnOneParticipant() == null || !getOneOnOneParticipant().equals(newNewConvOperation.getOneOnOneParticipant()))
            return false;

        return true;
    }


    private void postSegmentMetrics(Response response) {
        SegmentService.PropertiesBuilder propertiesBuilder = new SegmentService.PropertiesBuilder()
                .setNetworkResponse(response);

        if (isTeam()) {
            segmentService.reportMetric(SegmentService.CREATED_TEAM_EVENT, propertiesBuilder.build());
        } else {
            // are we creating team space
            propertiesBuilder.setSpaceIsTeam(!TextUtils.isEmpty(teamId));
            segmentService.reportMetric(SegmentService.CREATED_SPACE_EVENT, propertiesBuilder.build());
        }
    }

}
