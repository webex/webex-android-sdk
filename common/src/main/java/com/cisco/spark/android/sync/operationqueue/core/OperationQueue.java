 package com.cisco.spark.android.sync.operationqueue.core;

 import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.authenticator.AuthenticatedUserProvider;
import com.cisco.spark.android.authenticator.LogoutEvent;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.flag.FlagOperation;
import com.cisco.spark.android.locus.events.LocusDataCacheChangedEvent;
import com.cisco.spark.android.locus.model.LocusDataCache;
import com.cisco.spark.android.mercury.MercuryClient;
import com.cisco.spark.android.metrics.model.GenericMetric;
import com.cisco.spark.android.model.Comment;
import com.cisco.spark.android.model.ConversationTag;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.model.SearchStringWithModifiers;
import com.cisco.spark.android.model.Verb;
import com.cisco.spark.android.presence.PresenceStatus;
import com.cisco.spark.android.presence.operation.FetchPresenceStatusOperation;
import com.cisco.spark.android.presence.operation.SendPresenceEventOperation;
import com.cisco.spark.android.presence.operation.SubscribePresenceStatusOperation;
import com.cisco.spark.android.reachability.NetworkReachability;
import com.cisco.spark.android.reachability.NetworkReachabilityChangedEvent;
import com.cisco.spark.android.reachability.UIServiceAvailability;
import com.cisco.spark.android.sdk.SdkClient;
import com.cisco.spark.android.stickies.Sticky;
import com.cisco.spark.android.sync.ActorRecord;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ContentDataCacheRecord;
import com.cisco.spark.android.sync.ConversationContract.ActivityEntry;
import com.cisco.spark.android.sync.ConversationContract.ConversationEntry;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry;
import com.cisco.spark.android.sync.ConversationContract.vw_PendingSyncOperations;
import com.cisco.spark.android.sync.operationqueue.ActivityOperation;
import com.cisco.spark.android.sync.operationqueue.AddPersonOperation;
import com.cisco.spark.android.sync.operationqueue.AliasPreloginMetricsUserIdOperation;
import com.cisco.spark.android.sync.operationqueue.AssignRoomAvatarOperation;
import com.cisco.spark.android.sync.operationqueue.AudioMuteOperation;
import com.cisco.spark.android.sync.operationqueue.AudioVolumeOperation;
import com.cisco.spark.android.sync.operationqueue.AvatarUpdateOperation;
import com.cisco.spark.android.sync.operationqueue.CatchUpSyncOperation;
import com.cisco.spark.android.sync.operationqueue.ContentUploadOperation;
import com.cisco.spark.android.sync.operationqueue.CreateKmsResourceOperation;
import com.cisco.spark.android.sync.operationqueue.CustomNotificationsTagOperation;
import com.cisco.spark.android.sync.operationqueue.DeleteActivityOperation;
import com.cisco.spark.android.sync.operationqueue.FeatureToggleOperation;
import com.cisco.spark.android.sync.operationqueue.FetchSpaceUrlOperation;
import com.cisco.spark.android.sync.operationqueue.FetchStickyPackOperation;
import com.cisco.spark.android.sync.operationqueue.FetchUnjoinedTeamRoomsOperation;
import com.cisco.spark.android.sync.operationqueue.GetAvatarUrlsOperation;
import com.cisco.spark.android.sync.operationqueue.GetRetentionPolicyInfoOperation;
import com.cisco.spark.android.sync.operationqueue.IncrementShareCountOperation;
import com.cisco.spark.android.sync.operationqueue.IntegrateContactsOperation;
import com.cisco.spark.android.sync.operationqueue.JoinTeamRoomOperation;
import com.cisco.spark.android.sync.operationqueue.KeyFetchOperation;
import com.cisco.spark.android.sync.operationqueue.MapEventToConversationOperation;
import com.cisco.spark.android.sync.operationqueue.MarkReadOperation;
import com.cisco.spark.android.sync.operationqueue.MoveRoomToTeamOperation;
import com.cisco.spark.android.sync.operationqueue.NewConversationOperation;
import com.cisco.spark.android.sync.operationqueue.NewConversationOperation.CreateFlags;
import com.cisco.spark.android.sync.operationqueue.NewConversationWithRepostedMessagesOperation;
import com.cisco.spark.android.sync.operationqueue.PostCommentOperation;
import com.cisco.spark.android.sync.operationqueue.PostGenericMetricOperation;
import com.cisco.spark.android.sync.operationqueue.PostStickyActivityOperation;
import com.cisco.spark.android.sync.operationqueue.RemoteSearchOperation;
import com.cisco.spark.android.sync.operationqueue.RemoveParticipantOperation;
import com.cisco.spark.android.sync.operationqueue.RemoveRoomAvatarOperation;
import com.cisco.spark.android.sync.operationqueue.SendDtmfOperation;
import com.cisco.spark.android.sync.operationqueue.SetTitleAndSummaryOperation;
import com.cisco.spark.android.sync.operationqueue.SetupSharedKeyWithKmsOperation;
import com.cisco.spark.android.sync.operationqueue.TagOperation;
import com.cisco.spark.android.sync.operationqueue.ToggleActivityOperation;
import com.cisco.spark.android.sync.operationqueue.ToggleParticipantActivityOperation;
import com.cisco.spark.android.sync.operationqueue.TokenRefreshOperation;
import com.cisco.spark.android.sync.operationqueue.UnboundKeyFetchOperation;
import com.cisco.spark.android.sync.operationqueue.UnsetFeatureToggleOperation;
import com.cisco.spark.android.sync.operationqueue.UpdateEncryptionKeyOperation;
import com.cisco.spark.android.sync.operationqueue.UpdateTeamColorOperation;
import com.cisco.spark.android.sync.operationqueue.VideoThumbnailOperation;
import com.cisco.spark.android.sync.queue.AbstractConversationSyncTask;
import com.cisco.spark.android.sync.queue.SyncTaskOperation;
import com.cisco.spark.android.util.Action;
import com.cisco.spark.android.util.CollectionUtils;
import com.cisco.spark.android.util.LoggingLock;
import com.cisco.spark.android.util.SafeAsyncTask;
import com.cisco.spark.android.util.ThrottledAsyncTask;
import com.cisco.spark.android.wdm.Features;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.squareup.leakcanary.RefWatcher;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

import javax.inject.Provider;

import de.greenrobot.event.EventBus;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.DATA;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OPERATION_TYPE;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.OperationType;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.START_TIME;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SYNC_OPERATION_ID;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SYNC_STATE;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState;

/**
 * Manages the queue of operations and provides high-level methods for adding new operations to the
 * queue.
 */
public class OperationQueue {

    private final Context context;
    private Gson gson;
    private Map<String, Operation> pendingOperations = new ConcurrentHashMap<>();
    private LoggingLock lock;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private Condition idle;
    private boolean isInit;
    ThreadPoolExecutor walkerExecutor;
    ExecutorService workerExecutor;
    private final Timer wakeupTimer = new Timer("OperationQueue Wakeup Timer");
    private TimerTask wakeupTimerTask;

    LinkedBlockingQueue<String> operationsToRemoveFromDb = new LinkedBlockingQueue<>();

    public static final long DEFAULT_POLL_INTERVAL = 500;
    private NetworkReachability networkReachability;
    private final Provider<Batch> batchProvider;
    private final Injector injector;
    private RefWatcher refWatcher;
    private final SdkClient sdkClient;
    private final LocusDataCache locusDataCache;

    public OperationQueue(Context context, Gson gson, EventBus bus, AuthenticatedUserProvider authenticatedUserProvider,
                          NetworkReachability networkReachability, Provider<Batch> batchProvider, Injector injector,
                          RefWatcher refWatcher, SdkClient sdkClient, LocusDataCache locusDataCache) {
        this.context = context;
        this.gson = gson;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.networkReachability = networkReachability;
        this.batchProvider = batchProvider;
        this.injector = injector;
        this.refWatcher = refWatcher;
        this.sdkClient = sdkClient;
        this.locusDataCache = locusDataCache;
        lock = new LoggingLock(BuildConfig.DEBUG, "OperationQueue");
        idle = lock.newCondition();
        bus.register(this);
    }

    /////////////
    //
    // Public functions for high-level tasks. Safe to call these from the main thread.
    //

    /**
     * Kick off a task to replace one's own avatar in the cloud with a new one.
     *
     * @param file The new avatar
     */
    public AvatarUpdateOperation postNewAvatar(File file) {
        AvatarUpdateOperation op = new AvatarUpdateOperation(injector, file);
        return (AvatarUpdateOperation) submit(op);
    }

    /**
     * Send a sticky to a conversation.
     *
     * @param conversationId Conversation that contains the ID or temp id if provisional
     * @param sticky         File content, if any. Thumbnails will be generated as needed.
     */

    public ActivityOperation postSticker(String conversationId, Sticky sticky) {
        PostStickyActivityOperation op = new PostStickyActivityOperation(injector, conversationId, sticky);
        submit(op);

        return op;
    }

    /**
     * Request a key from the KMS
     *
     * @param keyUri
     */
    public KeyFetchOperation requestKey(Uri keyUri) {
        if (keyUri == null) {
            return null;
        }
        return requestKeys(Collections.singleton(keyUri));
    }

    public KeyFetchOperation requestKeys(Collection<Uri> keysToFetch) {
        if (keysToFetch.isEmpty())
            return null;

        KeyFetchOperation op = new KeyFetchOperation(injector, keysToFetch);
        return (KeyFetchOperation) submit(op);
    }

    public ActivityOperation postMessage(String conversationId, Comment comment) {
        return (ActivityOperation) submit(new PostCommentOperation(injector, conversationId, comment));
    }

    public Operation fetchSpaceUrl(String conversationId) {
        return submit(new FetchSpaceUrlOperation(injector, conversationId));
    }

    public Operation uploadContent(String conversationId, com.cisco.spark.android.model.File content) {
        return submit(new ContentUploadOperation(injector, conversationId, content));
    }

    public Operation uploadContentToSpaceUrl(Uri spaceUrl, com.cisco.spark.android.model.File content) {
        return submit(new ContentUploadOperation(injector, spaceUrl, content));
    }

    public Operation removeParticipant(String conversationId, ActorRecord.ActorKey actorKey) {
        return submit(new RemoveParticipantOperation(injector, conversationId, actorKey));
    }

    public Operation setParticipantModeratorStatus(String conversationId, ActorRecord.ActorKey actorKey, boolean isModerator) {
        return submit(new ToggleParticipantActivityOperation(injector, conversationId, actorKey, isModerator ? Verb.assignModerator : Verb.unassignModerator, OperationType.ASSIGN_MODERATOR));
    }

    public Operation setConversationTitleAndSummary(String conversationId, String title, String summary) {
        return submit(new SetTitleAndSummaryOperation(injector, conversationId, title, summary));
    }

    public Operation assignRoomAvatar(String conversationId, com.cisco.spark.android.model.File avatar) {
        Operation uploadContentOp = uploadContent(conversationId, avatar);
        Operation assignAvatarOp = new AssignRoomAvatarOperation(injector, conversationId, avatar);
        assignAvatarOp.setDependsOn(uploadContentOp);
        return submit(assignAvatarOp);
    }

    public Operation removeRoomAvatar(String conversationId) {
        return submit(new RemoveRoomAvatarOperation(injector, conversationId));
    }

    public Operation setTeamColor(String teamId, String color, String primaryConversationId) {
        return submit(new UpdateTeamColorOperation(injector, teamId, color, primaryConversationId));
    }

    public Operation markConversationRead(String conversationId) {
        return submit(new MarkReadOperation(injector, conversationId));
    }

    public Operation sendAckForConversationActivityBeforeTime(String conversationId, long time) {
        return submit(new MarkReadOperation(injector, conversationId, time));
    }

    public Operation setConversationMute(String conversationId, boolean mute) {
        return submit(new ToggleActivityOperation(injector, conversationId, mute ? Verb.mute : Verb.unmute, OperationType.CONVERSATION_MUTE));
    }

    public Operation setConversationLocked(String conversationId, boolean lock) {
        return submit(new ToggleActivityOperation(injector, conversationId, lock ? Verb.lock : Verb.unlock, OperationType.CONVERSATION_LOCK));
    }

    public Operation setOneOnOneHidden(String conversationId, boolean hidden) {
        return submit(new ToggleActivityOperation(injector, conversationId, hidden ? Verb.hide : Verb.unhide, OperationType.CONVERSATION_HIDE));
    }

    public Operation setConversationFavorite(String conversationId, boolean favorite) {
        return submit(new ToggleActivityOperation(injector, conversationId, favorite ? Verb.favorite : Verb.unfavorite, OperationType.CONVERSATION_FAVORITE));
    }

    public Operation moveRoomToTeam(String teamId, String conversationId, boolean moveIntoTeam) {
        return submit(new MoveRoomToTeamOperation(injector, conversationId, teamId, moveIntoTeam));
    }

    public Operation refreshUnboundKeys() {
        return submit(new UnboundKeyFetchOperation(injector, KeyManager.defaultUnboundKeyLimit));
    }

    public Operation setDeveloperFeature(@NonNull String key, Object value, @NonNull String... emails) {
        return setFeature(Features.FeatureType.DEVELOPER, key, value, emails);
    }

    public Operation setBooleanUserFeatures(@NonNull Map<String, Boolean> features, @NonNull String... uuids) {
        return setBooleanFeatures(features, Features.FeatureType.USER, uuids);
    }

    public Operation setBooleanDeveloperFeatures(@NonNull Map<String, Boolean> features, @NonNull String... uuids) {
        return setBooleanFeatures(features, Features.FeatureType.DEVELOPER, uuids);
    }

    private Operation setBooleanFeatures(@NonNull Map<String, Boolean> features, Features.FeatureType featureType, @NonNull String... uuids) {
        HashMap<String, String> toggles = new HashMap<>();
        for (String s : features.keySet()) {
            toggles.put(s, String.valueOf(features.get(s)));
        }
        return submit(new FeatureToggleOperation(injector, toggles, featureType, CollectionUtils.asSet(uuids)));
    }

    public Operation setUserFeature(@NonNull String key, Object value, @NonNull String... uuids) {
        return setFeature(Features.FeatureType.USER, key, value, uuids);
    }

    public Operation setFeature(Features.FeatureType featureType, @NonNull String key, Object value, @NonNull String... uuids) {
        HashMap<String, String> toggles = new HashMap<>();
        toggles.put(key, String.valueOf(value));
        return submit(new FeatureToggleOperation(injector, toggles, featureType, CollectionUtils.asSet(uuids)));
    }

    public Operation unsetUserFeature(@NonNull String key, @NonNull String... uuids) {
        return submit(new UnsetFeatureToggleOperation(injector, key, Features.FeatureType.USER, CollectionUtils.asSet(uuids)));
    }

    public Operation mapEventToRoom(@NonNull String eventId, Date endDate, @NonNull String conversationId, boolean cacheOnly) {
        return submit(new MapEventToConversationOperation(injector, eventId, endDate, conversationId, cacheOnly));
    }

    public Operation setUpSharedKey() {
        return submit(new SetupSharedKeyWithKmsOperation(injector));
    }

    public Operation updateConversationKey(@NonNull String conversationId) {
        return submit(new UpdateEncryptionKeyOperation(injector, conversationId));
    }

    public Operation deleteActivity(@NonNull String conversationId, @NonNull String activityId) {
        return submit(new DeleteActivityOperation(injector, conversationId, activityId));
    }

    public Operation ensureVideoThumbnails(@NonNull String activityId) {
        return submit(new VideoThumbnailOperation(injector, activityId));
    }

    public Operation leaveConversation(String conversationId) {
        return submit(new RemoveParticipantOperation(injector, conversationId));
    }

    public Operation fetchUnjoinedTeamConversations() {
        return submit(new FetchUnjoinedTeamRoomsOperation(injector));
    }

    public Operation createKmsResource(String conversationId, Set<String> participantUuids) {
        return submit(new CreateKmsResourceOperation(injector, conversationId, participantUuids));
    }

    public Operation archiveConversation(String conversationId, boolean archive) {
        return submit(new ToggleActivityOperation(injector, conversationId, archive ? Verb.archive : Verb.unarchive, OperationType.CONVERSATION_ARCHIVE));
    }

    public Operation tag(String conversationId, ConversationTag conversationTag) {
        return tag(conversationId, Arrays.asList(conversationTag));
    }

    public Operation tag(String conversationId, List<ConversationTag> conversationTagList) {
        return submit(new TagOperation(injector, conversationId, conversationTagList, true, OperationType.TAG));
    }

    public Operation untag(String conversationId, ConversationTag conversationTag) {
        return untag(conversationId, Arrays.asList(conversationTag));
    }

    public Operation untag(String conversationId, List<ConversationTag> conversationTagList) {
        return submit(new TagOperation(injector, conversationId, conversationTagList, false, OperationType.TAG));
    }

    public Operation retrieveRetentionPolicy(String custodianOrgId, Uri retentionUrl, String conversationId, boolean isOneOnOneConversation) {
        return submit(new GetRetentionPolicyInfoOperation(injector, this.context, custodianOrgId, retentionUrl, conversationId, isOneOnOneConversation));
    }

    /**
     * Has special onEnqueue behavior to cater for toggling MESSAGES_ON and MESSAGES_OFF also modifies
     * the MUTED tag
     */
    public Operation untagCustomNotifications(String conversationId, List<ConversationTag> conversationTagList) {
        return submit(new CustomNotificationsTagOperation(injector, conversationId, conversationTagList, false, OperationType.TAG));
    }

    /**
     * Has special onEnqueue behavior to cater for toggling MESSAGES_ON and MESSAGES_OFF also modifies
     * the MUTED tag
     */
    public Operation tagCustomNotifications(String conversationId, List<ConversationTag> conversationTagList) {
        return submit(new CustomNotificationsTagOperation(injector, conversationId, conversationTagList, true, OperationType.TAG_NOTIFICATIONS));
    }

    public Operation searchQuery(String searchString, SearchStringWithModifiers searchStringWithModifiers) {
        return submit(new RemoteSearchOperation(injector, searchString, searchStringWithModifiers));
    }

    public Operation updateStickies() {
        return submit(new FetchStickyPackOperation(injector));
    }

    public Operation joinTeamRoom(String conversationId, String parentTeamId, int participantCount) {
        return submit(new JoinTeamRoomOperation(injector, conversationId, parentTeamId, participantCount));
    }

    public Operation fetchPresenceStatus(String subjectUuid) {
        return submit(new FetchPresenceStatusOperation(injector, subjectUuid));
    }

    public Operation fetchPresenceStatus(List<String> subjectUuids) {
        return submit(new FetchPresenceStatusOperation(injector, subjectUuids));
    }

    public Operation subscribePresenceStatus(String subjectUuid) {
        return submit(new SubscribePresenceStatusOperation(injector, subjectUuid));
    }

    public Operation setContactsIntegrationEnabled(boolean enabled) {
        return submit(new IntegrateContactsOperation(injector, enabled));
    }

    public Operation flagActivity(String activityId, FlagOperation.MetricsData metricsData) {
        return submit(new FlagOperation(injector, activityId, true, metricsData));
    }

    public Operation unflag(String flagId) {
        return submit(new FlagOperation(injector, flagId));
    }

    public Operation unflagActivity(String activityId, FlagOperation.MetricsData metricsData) {
        return submit(new FlagOperation(injector, activityId, false, metricsData));
    }

    public Operation sendPresenceEvent(PresenceStatus status, int ttlInSeconds) {
        return submit(new SendPresenceEventOperation(injector, status, ttlInSeconds));
    }

    public Operation refreshToken(String reason) {
        return submit(new TokenRefreshOperation(injector, reason));
    }

    public Operation getAvatarUrls(String uuid, Action<ContentDataCacheRecord> callback) {
        return submit(new GetAvatarUrlsOperation(injector, uuid, callback));
    }

    public Operation catchUpSync() {
        return submit(new CatchUpSyncOperation(injector));
    }

    public Operation newSyncTask(AbstractConversationSyncTask syncTask) {
        return submit(new SyncTaskOperation(injector, syncTask));
    }

    public Operation getParticipantsForConversation(String id) {
        return submit(new FetchParticipantsOperation(injector, id));
    }

    public Operation getAcksForConversation(String id) {
        return submit(new FetchAcksOperation(injector, id));
    }

    public Operation setAudioMute(String roomId, boolean isMute) {
        return submit(new AudioMuteOperation(injector, roomId, isMute));
    }

    public Operation setAudioVolume(String roomId, int volumeLevel) {
        return submit(new AudioVolumeOperation(injector, roomId, volumeLevel));
    }

    /**
     * Create a new room.
     *
     * @param uuidsOrEmails List of zero or more id's or emails
     * @param title         Can be null
     * @param createFlags   One or more from the @NewConversationOperation.CreateFlags enum
     * @return
     */
    public NewConversationOperation createConversation(Collection<String> uuidsOrEmails, String title, CreateFlags... createFlags) {
        EnumSet<CreateFlags> createFlagses = EnumSet.noneOf(CreateFlags.class);
        createFlagses.addAll(Arrays.asList(createFlags));
        NewConversationOperation op = new NewConversationOperation(injector, uuidsOrEmails, title, null, null, createFlagses);
        return (NewConversationOperation) submit(op);
    }

    public NewConversationOperation createConversation(Collection<String> uuidsOrEmails, String title, EnumSet<CreateFlags> createFlags) {
        return createConversation(uuidsOrEmails, title, createFlags.toArray(new CreateFlags[]{}));
    }

    public NewConversationWithRepostedMessagesOperation createConversationWithRepostedMessagesFromConversation(String conversationIdForMessagesToRepost, Collection<String> uuidsOrEmails, String title, EnumSet<CreateFlags> createFlags) {
        NewConversationOperation op = new NewConversationWithRepostedMessagesOperation(injector, uuidsOrEmails, title, null, null, conversationIdForMessagesToRepost, createFlags);
        return (NewConversationWithRepostedMessagesOperation) submit(op);
    }

    /**
     * Create a new team
     *
     * @param title
     * @param summary
     * @return
     */
    public NewConversationOperation createTeam(String title, String summary, CreateFlags... createFlags) {
        EnumSet<CreateFlags> createFlagses = EnumSet.of(CreateFlags.NEW_TEAM, CreateFlags.PERSIST_WITHOUT_MESSAGES);
        createFlagses.addAll(Arrays.asList(createFlags));

        NewConversationOperation op = new NewConversationOperation(injector, Collections.EMPTY_LIST, title, summary, null, createFlagses);
        return (NewConversationOperation) submit(op);
    }

    /**
     * Create a new team room, open to all team members
     *
     * @param title
     * @param teamId
     */
    public NewConversationOperation createTeamConversation(String title, String teamId, CreateFlags... createFlags) {
        EnumSet<CreateFlags> createFlagses = EnumSet.of(CreateFlags.PERSIST_WITHOUT_MESSAGES);
        createFlagses.addAll(Arrays.asList(createFlags));

        NewConversationOperation op = new NewConversationOperation(injector, Collections.EMPTY_LIST, title, null, teamId, createFlagses);
        return (NewConversationOperation) submit(op);
    }

    public SendDtmfOperation sendDtmf(String tones) {
        SendDtmfOperation op = new SendDtmfOperation(injector, tones);
        return (SendDtmfOperation) submit(op);
    }

    public IncrementShareCountOperation incrementShareCount(String conversationId) {
        IncrementShareCountOperation op = new IncrementShareCountOperation(injector, conversationId);
        return (IncrementShareCountOperation) submit(op);
    }

    /**
     * Add a person to a room
     *
     * @param conversationId the room
     * @param person         the person
     */
    public AddPersonOperation addParticipant(String conversationId, Person person) {
        return (AddPersonOperation) submit(new AddPersonOperation(injector, conversationId, person, false));
    }

    /**
     * Add a person to a team
     *
     * @param teamId the team
     * @param person         the person
     */
    public AddPersonOperation addTeamMember(String teamId, Person person) {
        return (AddPersonOperation) submit(new AddPersonOperation(injector, teamId, person, true));
    }

    /**
     * Checks an operation for redundancy and hands it off to be enqueue executor
     *
     * @param op The new operation
     * @return The new operation if it was enqueued, or an existing operation if the new operation
     * is deemed redundant, or null if it cannot be enqueued.
     */
    public Operation submit(Operation op) {

        if (!sdkClient.operationEnabled(op)) {
            Ln.w("Discarding operation because operation is not enabled " + op);
            return null;
        }

        // Checking for registered state here to work around an issue where operations can get submitted
        // after logout (markRead)
        if (op.requiresAuth() && !authenticatedUserProvider.isAuthenticated()) {
            Ln.w("Discarding operation because we are logged out " + op);
            return null;
        }

        Operation existingOp = isOperationRedundant(op);
        if (existingOp != null) {
            Ln.d("Discarding redundant operation: " + op + " because of " + existingOp);
            ensureQueuePolling();
            return existingOp;
        }

        configureRetryPolicy(op);
        try {
            getWorkerService().submit(new OperationEnqueueTask(this, op));
            pendingOperations.put(op.getOperationId(), op);
        } catch (Exception e) {
            Ln.w("Failed submitting to operation queue " + op);
        }
        return op;
    }

    private Operation isOperationRedundant(Operation newOp) {
        // Here we make a copy of the list rather than iterating over it directly. This way
        // we don't have to synchronize.
        ArrayList<Operation> copy = new ArrayList<>();
        try {
            copy.addAll(pendingOperations.values());
        } catch (Exception e) {
            // addAll can throw if there's a collision in the pendingOperations map. Fall back to using
            // the lock and try again.
            lock.lock();
            try {
                copy.clear();
                copy.addAll(pendingOperations.values());
            } catch (Exception e1) {
                Ln.w(e, "Failed checking for redundancy " + newOp);
                return newOp;
            } finally {
                lock.unlock();
            }
        }

        for (Operation existingOp : copy) {
            try {
                if (existingOp.getState().isTerminal())
                    continue;

                if (existingOp.getRetryPolicy().shouldBail())
                    continue;

                if (existingOp.isOperationRedundant(newOp)) {
                    return existingOp;
                }
            } catch (Throwable t) {
                Ln.w(t, "Failed redundancy check " + newOp.getOperationId() + " against " + existingOp.getOperationId() + " " + existingOp.getOperationType());
            }
        }
        return null;
    }

    public Operation postGenericMetric(GenericMetric metric) {
        if (authenticatedUserProvider.isAuthenticated()) {
            return submit(new PostGenericMetricOperation(injector, metric));
        } else {
            return submit(new PostGenericMetricOperation(injector, metric, true));
        }
    }

    public Operation postGenericMetricNoAuth(GenericMetric metric) {
        return submit(new PostGenericMetricOperation(injector, metric, true));
    }

    public Operation postAliasUser(String preloginId) {
        return submit(new AliasPreloginMetricsUserIdOperation(injector, preloginId));
    }

    ////////////
    //
    // Queue management
    //

    /**
     * Get the queued operations
     *
     * @return A sorted list of pending operations
     */
    public List<Operation> getPendingOperations() {
        init();

        lock.lock();
        try {
            List<Operation> ret = new ArrayList<Operation>(pendingOperations.values());
            Collections.sort(ret, Operation.ascendingStartTimeComparator);
            return ret;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Write an operation to the queue and to persistent storage
     *
     * @param operation
     */
    void persist(Operation operation) {
        if (operation.getOperationType().operationClass == null) {
            throw new IllegalArgumentException("Operation must have a class in the OperationType enum");
        }

        updateCache(operation);

        if (!operation.shouldPersist() || !operation.isDirty())
            return;

        operation.setDirty(false);

        try {
            Ln.d("Persisting operation: %s", operation);

            Batch batch = injector.getObjectGraph().get(Batch.class);
            ContentProviderOperation op;

            SyncState stateToPersist = operation.getState();

            // Don't persist executing state to the DB
            if (stateToPersist == SyncState.EXECUTING)
                stateToPersist = SyncState.READY;

            String serializedOperation = operation.getJson(gson);
            if (TextUtils.isEmpty(serializedOperation)) {
                Ln.e("Operation serialization failed");
                return;
            }

            switch (stateToPersist) {
                case UNINITIALIZED:
                    Ln.w("Cannot persist operation in state UNINITIALIZED");
                    return;
                case PREPARING:
                case READY:
                case EXECUTING:
                case FAULTED:
                    //insert then update
                    op = ContentProviderOperation.newInsert(SyncOperationEntry.CONTENT_URI)
                            .withValue(SYNC_OPERATION_ID.name(), operation.getOperationId())
                            .withValue(SYNC_STATE.name(), stateToPersist.ordinal())
                            .withValue(START_TIME.name(), operation.getRetryPolicy().getStartTime())
                            .withValue(OPERATION_TYPE.name(), operation.getOperationType().ordinal())
                            .build();
                    batch.add(op);

                    op = ContentProviderOperation.newUpdate(SyncOperationEntry.CONTENT_URI)
                            .withValue(SYNC_STATE.name(), stateToPersist.ordinal())
                            .withValue(DATA.name(), serializedOperation)
                            .withSelection(SYNC_OPERATION_ID + "=?", new String[]{operation.getOperationId()})
                            .build();
                    batch.add(op);
                    break;
                case SUCCEEDED:
                    op = ContentProviderOperation.newDelete(Uri.withAppendedPath(SyncOperationEntry.CONTENT_URI, operation.getOperationId()))
                            .build();
                    batch.add(op);
                    break;
            }

            if (operation.isSafeToRemove()) {
                op = ContentProviderOperation.newDelete(Uri.withAppendedPath(SyncOperationEntry.CONTENT_URI, operation.getOperationId()))
                        .build();
                batch.add(op);
            }

            // If any activities have this operation's id, update its state for the ui.
            op = ContentProviderOperation.newUpdate(ActivityEntry.CONTENT_URI)
                    .withValue(ActivityEntry.SYNC_STATE.name(), operation.getState().ordinal())
                    .withSelection(ActivityEntry.SYNC_OPERATION_ID + "=?", new String[]{String.valueOf(operation.getOperationId())})
                    .build();

            batch.add(op);

            batch.apply();

        } catch (Throwable e) {
            Ln.e(e, "Failed persisting operation " + operation);
        }
    }

    private void updateCache(Operation operation) {
        if (operation.getState() == SyncState.UNINITIALIZED) {
            return;
        }

        try {
            if (operation.isSafeToRemove()) {
                pendingOperations.remove(operation.getOperationId());
                refWatcher.watch(operation);
            } else {
                pendingOperations.put(operation.getOperationId(), operation);
            }
        } catch (Throwable t) {
            Ln.w(t, "updateCache failed. Removing operation " + operation.getOperationId() + " " + operation.getOperationType());
        }
    }

    /**
     * Remove an operation from the queue and from persistent storage, even if
     * !operation.isSafeToRemove()
     *
     * @param operationId
     */
    public Operation cancelOperation(final String operationId) {
        Operation op = get(operationId);
        if (op != null)
            op.cancel();
        return op;
    }

    protected void init() {
        if (isInit)
            return;

        Cursor c = null;

        lock.lock();
        try {
            if (isInit)
                return;

            Ln.i("Initializing Operation Queue");

            c = getContentResolver().query(vw_PendingSyncOperations.CONTENT_URI,
                    vw_PendingSyncOperations.DEFAULT_PROJECTION,
                    null, null, null);

            while (c != null && c.moveToNext()) {
                String id = null;
                try {
                    id = c.getString(vw_PendingSyncOperations.SYNC_OPERATION_ID.ordinal());
                    if (pendingOperations.containsKey(id))
                        continue;

                    Operation operation = Operation.fromCursor(c, gson);
                    if (operation != null && operation.shouldPersist()) {
                        operation.initialize(injector);
                        pendingOperations.put(operation.getOperationId(), operation);
                    } else if (operation != null) {
                        removeOperation(id);
                    }
                } catch (Exception e) {
                    Ln.w(e, "Failed initializing OperationQueue");
                    if (id != null) {
                        removeOperation(id);
                    }
                }
            }
            isInit = true;
            Ln.i("Initialized : Ensure queue polling");
            ensureQueuePolling();
        } finally {
            lock.unlock();
            if (c != null)
                c.close();
        }
    }

    /**
     * The executor for the operation walker has a single thread, a backlog queue size of 1, and
     * quietly discards tasks that would overflow the queue.
     * <p/>
     * Walker tasks are all the same (not parameterized) so there's no point in letting the work
     * queue grow beyond 1. The single thread ensures that at most one WalkOperationQueueTask is
     * running at a given time.
     *
     * @return The executor for WalkOperationQueueTasks
     */
    private ExecutorService getWalkerService() {
        // Avoid locking in the happy case
        if (walkerExecutor != null && !walkerExecutor.isShutdown())
            return walkerExecutor;

        lock.lock();
        try {
            if (walkerExecutor == null || walkerExecutor.isShutdown()) {

                ThreadFactory threadFactory = new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable runnable) {
                        Thread ret = new Thread(runnable);
                        ret.setDaemon(true);
                        ret.setName("OperationWalker");
                        return ret;
                    }
                };

                walkerExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<Runnable>(1),
                        threadFactory);

                walkerExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
            }
            return walkerExecutor;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Enqueueing operations must happen as quickly as possible since UI updates are gating on them.
     * Therefore we use an unbounded thread pool. Enqueueing should be very fast so the pool should
     * never be very large with the possible exception of startup activity.
     * <p/>
     * update: Operations' doWork() calls now happen on this executor as well. The default AsyncTask
     * thread pool proved too small and that caused trouble in tests, especially when one thread is
     * waiting for another one to finish.
     */
    private static long nextWorkerThreadIndex;

    ExecutorService getWorkerService() {
        // Avoid locking in the happy case
        if (workerExecutor != null && !workerExecutor.isShutdown())
            return workerExecutor;

        lock.lock();
        try {
            if (workerExecutor == null || workerExecutor.isShutdown()) {
                workerExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable runnable) {
                        Thread ret = new Thread(runnable);
                        ret.setDaemon(true);
                        ret.setName("OperationWorker" + nextWorkerThreadIndex++);
                        return ret;
                    }
                });
            }
            return workerExecutor;
        } finally {
            lock.unlock();
        }
    }

    // Package private : Lock shared with core classes only
    LoggingLock getQueueLock() {
        return lock;
    }

    private ContentResolver getContentResolver() {
        return context.getContentResolver();
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(NetworkReachabilityChangedEvent event) {
        if (event.isConnected() && !pendingOperations.isEmpty()) {
            Ln.i("Network restored, tickling OperationQueue Walker");
            ensureQueuePolling();
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(UIServiceAvailability.UIServiceAvailabilityEvent event) {
        if (event.isServiceAvailable() && !pendingOperations.isEmpty()) {
            Ln.i("Service availability restored, tickling OperationQueue Walker");
            ensureQueuePolling();
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(LocusDataCacheChangedEvent event) {
        if (!pendingOperations.isEmpty() && !locusDataCache.isInCall()) {
            Ln.i("LocusDataCacheChangedEvent " + event.getLocusChange() + " tickling OperationQueue Walker");
            ensureQueuePolling();
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(MercuryClient.MercuryConnectedEvent event) {
        Ln.i("Mercury connected, catching up");
        catchUpSync();
    }

    void signalIdleIfNeeded() {

        if (isIdle()) {
            // Persist usually happens in the context of the queue walker, this takes care of
            // async operations without waking up the walker if it's gone idle.
            Ln.d("Signaling idle");
            idle.signalAll();

            // Tickle the walker to persist the change
            ensureQueuePolling();
        }
    }

    public Operation get(String operationId) {
        if (TextUtils.isEmpty(operationId))
            return null;

        lock.lock();
        try {
            init();
            return pendingOperations.get(operationId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Helpers for integration tests
     */

    public boolean isIdle() {
        lock.lock();
        try {
            if (!isInit) {
                Ln.d("OperationQueue is idle because it is not initialized");
                return true;
            }

            for (Operation op : pendingOperations.values()) {
                try {
                    // If the operation is not idle we are definitely not idle yet
                    if (!op.getState().isIdle())
                        return false;

                    // If we are idle in the PREPARING state, call onPrepare to see if we can transition to another state
                    if (op.getState() == SyncState.PREPARING && op.onPrepare() != SyncState.PREPARING) {
                        return false;
                    }
                } catch (Throwable t) {
                    Ln.w("Failed checking isIdle: " + op.getOperationId() + " " + op.getOperationType());
                }
            }
        } finally {
            lock.unlock();
        }
        Ln.d("OperationQueue idle");
        return true;
    }

    public boolean waitUntilIdle(long time, TimeUnit unit) {
        Ln.d("Starting wait for idle : " + unit.toMillis(time));
        boolean ret = false;
        lock.lock();
        try {
            if (isIdle()) {
                ret = true;
                return ret;
            }
            ret = idle.await(time, unit);
        } catch (InterruptedException e) {
            Ln.w(e);
        } finally {
            lock.unlock();
            Ln.d("Done waiting for idle : " + ret);
        }
        if (!ret) {
            for (Operation op : getPendingOperations()) {
                if (!op.getState().isTerminal()) {
                    Ln.d("NOT IDLE : " + op);
                }
            }
        }
        return ret;
    }

    public long getMsUntilNextWork() {
        long ret = Long.MAX_VALUE;
        for (Operation operation : getPendingOperations()) {
            ret = Math.min(ret, operation.getMsUntilStateChange());
            if (ret == 0)
                break;
        }
        return Math.max(0, ret);
    }

    /**
     * Handy for tests or when Operation.waitUntilEnqueued() is not practical
     *
     * @return true if all operations are enqueued
     */
    public boolean waitUntilAllEnqueued(long time, TimeUnit unit) {
        if (isIdle()) {
            return true;
        }

        Ln.d("Starting wait for all enqueued : " + unit.toMillis(time));

        long endTime = System.currentTimeMillis() + unit.toMillis(time);
        for (Operation op : pendingOperations.values()) {
            if (op.getState() != SyncState.UNINITIALIZED)
                continue;

            long timeToWait = endTime - System.currentTimeMillis();
            if (timeToWait <= 0) {
                Ln.d("Failed waiting for all operations to enqueue");
                return false;
            }
            op.waitUntilEnqueued(timeToWait);
        }
        Ln.d("All operations enqueued, wait successful");
        return true;
    }

    /**
     * Clears the OperationQueue from memory. Will be re-hydrated from persistent storage
     */
    public void clear() {
        Ln.i("Clearing OperationQueue");
        lock.lock();
        try {
            getWorkerService().shutdownNow();
            getWalkerService().shutdownNow();
            pendingOperations.clear();
            isInit = false;
            waitUntilIdle(5, TimeUnit.SECONDS);
            Ln.d("Signaling idle");
            idle.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Clears and shuts down the operation queue. Any in-progress op's will be canceled.
     */
    public void shutDown() {
        if (!isInit)
            return;

        lock.lock();
        try {
            for (Operation op : getPendingOperations()) {
                try {
                    if (!op.getState().isTerminal())
                        op.cancel();
                } catch (Throwable t) {
                    Ln.w("Failed canceling on shutdown: " + op.getOperationId() + " " + op.getOperationType());
                }
            }
            clear();
        } finally {
            lock.unlock();
        }

        new SafeAsyncTask<Void>() {
            @Override
            public Void call() throws Exception {
                context.getContentResolver().delete(SyncOperationEntry.CONTENT_URI, null, null);
                return null;
            }
        }.execute();
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEvent(LogoutEvent event) {
        shutDown();
    }

    protected void ensureQueuePolling(boolean immediate) {
        if (wakeupTimerTask != null)
            wakeupTimerTask.cancel();

        if (immediate) {
            getWalkerService().submit(new WalkOperationQueueTask(OperationQueue.this, networkReachability));
        } else {
            long msDelay = getMsUntilNextWork();
            if (msDelay == 0)
                ensureQueuePolling(true);
            else if (msDelay < TimeUnit.MINUTES.toMillis(5)) {
                wakeupTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        ensureQueuePolling(true);
                    }
                };
                wakeupTimer.schedule(wakeupTimerTask, Math.max(0, msDelay));
                Ln.d("Scheduling OperationQueue wakeup in " + Math.max(0, msDelay) + "ms");
            } else {
                Ln.d("No work to do, quiescing until something wakes us up");
            }
        }
    }

    protected void ensureQueuePolling() {
        ensureQueuePolling(true);
    }

    // overridden in some tests
    public long getPollRate() {
        return DEFAULT_POLL_INTERVAL;
    }

    /**
     * Restart an operation that has faulted. The operation's onRestart callback will be triggered.
     * Any depended operations will also be restarted.
     * <p/>
     * Asynchronous, safe to call from the main thread.
     * <p/>
     * see Operation.onRestart()
     *
     * @param operationId The ID of the operation to restart
     * @return The restarted operation
     */
    public Operation restartOperation(final String operationId) {
        Operation ret = get(operationId);
        Ln.d("Restart operation " + ret);
        if (ret != null) {
            lock.lock();
            try {
                getWorkerService().submit(new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        restartOperationSynchronous(operationId);
                        return null;
                    }
                });
            } catch (Exception e) {
                Ln.w(e, "Failed restarting operation " + ret);
            } finally {
                lock.unlock();
            }
        }
        return ret;
    }

    private void restartOperationSynchronous(String operationId) {
        init();

        Operation op = get(operationId);
        if (op == null || op.getState() != SyncState.FAULTED) {
            return;
        }

        Ln.d("OperationQueue: Restarting Operation - " + op.getOperationId());

        for (String dependantOpId : op.getDependsOn()) {
            restartOperationSynchronous(dependantOpId);
        }

        lock.lock();
        try {
            configureRetryPolicy(op);
            op.setState(op.onRestart());
        } finally {
            lock.unlock();
        }
        ensureQueuePolling();
    }

    private Operation removeOperation(final String operationId) {
        Operation ret = pendingOperations.remove(operationId);
        operationsToRemoveFromDb.add(operationId);
        removeOperationTaskThrottled.scheduleExecute();
        refWatcher.watch(ret);
        return ret;
    }

    ThrottledAsyncTask removeOperationTaskThrottled = new ThrottledAsyncTask() {
        @Override
        protected void doInBackground() {
            ArrayList<String> toRemove = new ArrayList<>();
            operationsToRemoveFromDb.drainTo(toRemove);

            Batch batch = batchProvider.get();

            for (String operationId : toRemove) {
                try {
                    ContentProviderOperation op = ContentProviderOperation
                            .newDelete(ActivityEntry.CONTENT_URI)
                            .withSelection(ActivityEntry.SYNC_OPERATION_ID + "=? AND " + ActivityEntry.ACTIVITY_ID + "=" + ActivityEntry.SYNC_OPERATION_ID,
                                    new String[]{operationId}).build();
                    batch.add(op);

                    op = ContentProviderOperation
                            .newDelete(SyncOperationEntry.CONTENT_URI)
                            .withSelection(SYNC_OPERATION_ID + "=?", new String[]{operationId})
                            .build();
                    batch.add(op);

                    op = ContentProviderOperation.newUpdate(ConversationEntry.CONTENT_URI)
                            .withSelection(ConversationEntry.LAST_ACTIVITY_ID + "=?", new String[]{operationId})
                                    // TODO this can leave the Last Activity field blank in the room list
                            .withValue(ConversationEntry.LAST_ACTIVITY_ID.name(), null)
                            .build();
                    batch.add(op);
                } catch (Exception ex) {
                    Ln.d(ex, "Error removing operation.");
                }
            }
            batch.apply();
        }
    };

    public boolean isUninitialized() {
        if (!pendingOperations.isEmpty()) {
            Ln.d("Error non empty operation queue!");
        }

        return pendingOperations.isEmpty();
    }

    // provides a hook for tests that need to customize retrypolicy
    protected RetryPolicy configureRetryPolicy(Operation operation) {
        RetryPolicy retryPolicy = operation.buildRetryPolicy();
        operation.setRetryPolicy(retryPolicy);
        return retryPolicy;
    }

    //For Testing
    public HashMap<OperationType, RetryPolicy> getRetryPolicyMap() {
        throw new RuntimeException("Not Implemented");
    }

    public void setPollRate(long pollRate) {
        throw new RuntimeException("Not Implemented");
    }

    public void reload() {
        throw new RuntimeException("Not implemented");
    }

    public Operation submitTestOperation(Operation op) {
        throw new RuntimeException("Not implemented");
    }

}
