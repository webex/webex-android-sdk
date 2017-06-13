package com.cisco.spark.android.sync;

import android.annotation.SuppressLint;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.text.TextUtils;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.authenticator.LogoutEvent;
import com.cisco.spark.android.events.ActivityDecryptedEvent;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.model.SearchStringWithModifiers;
import com.cisco.spark.android.util.NameUtils;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.util.ThrottledAsyncTask;
import com.cisco.spark.android.util.UriUtils;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;

import static com.cisco.spark.android.sync.ConversationContract.ActivityEntry;
import static com.cisco.spark.android.sync.ConversationContract.ContentSearchDataEntry;
import static com.cisco.spark.android.sync.ConversationContract.ContentSearchEntry;
import static com.cisco.spark.android.sync.ConversationContract.ConversationSearchDataEntry;
import static com.cisco.spark.android.sync.ConversationContract.ConversationSearchEntry;
import static com.cisco.spark.android.sync.ConversationContract.MessageSearchEntry;
import static com.cisco.spark.android.sync.ConversationContract.vw_Conversation;
import static com.cisco.spark.android.sync.ConversationContract.vw_Participant;

@Singleton
public class SearchManager {
    private final static int MAX_CHUNK_SIZE = 100;
    private final static int MAX_SEARCH_RESULT_LIMIT = 100;
    private final static String MATCH_OPERATOR = ":";
    private final Object databaseLock = new Object();
    private final ApiTokenProvider apiTokenProvider;
    private final ActorRecordProvider actorRecordProvider;
    private final Context context;
    private final Gson gson;
    private final DeviceRegistration deviceRegistration;
    private EventBus bus;
    private final Provider<Batch> batchProvider;
    private Boolean isContentSearchEnabled = null;
    private Boolean togglePrevious;
    private Boolean isMessageSearchEnabled = null;
    private Boolean messageSearchPreviousValue;
    LinkedBlockingQueue<ConversationRecord> conversationsToUpdate = new LinkedBlockingQueue<>();
    LinkedBlockingQueue<MessageSearchReferenceObject> messageActivitesToUpdate = new LinkedBlockingQueue<>();
    LinkedBlockingQueue<MessageSearchReferenceObject> contentActivitesToUpdate = new LinkedBlockingQueue<>();


    private static ArrayList<String> typeList = new ArrayList<>();

    ThrottledAsyncTask updateConversationSearchTask = new ThrottledAsyncTask(1000) {
        @Override
        protected void doInBackground() {
            HashSet<ConversationRecord> conversations = new HashSet<>();
            Batch batch = batchProvider.get();
            synchronized (conversationsToUpdate) {

                // Trying to do too many at once can hog resources
                conversationsToUpdate.drainTo(conversations, MAX_CHUNK_SIZE);

                Ln.d("Updating search index for " + conversations.size() + " conversations");
                for (ConversationRecord conversation : conversations) {

                    if (!apiTokenProvider.isAuthenticated())
                        break;

                    Cursor cursor = null;

                    try {
                        cursor = context.getContentResolver().query(vw_Participant.CONTENT_URI,
                                vw_Participant.DEFAULT_PROJECTION,
                                vw_Participant.CONVERSATION_ID + "=?",
                                new String[]{conversation.getId()}, null);

                        ArrayList<String> participantNames = new ArrayList<String>();
                        ArrayList<String> participantEmails = new ArrayList<String>();
                        String participantEmailsString = null, participantNamesString = null, oneOnOneParticipantUUID = null;
                        Ln.d("SearchManager: Updating " + cursor.getCount() + " participants for conversation: " + conversation.getTitle());

                        while (cursor != null && cursor.moveToNext()) {
                            ActorRecord.ActorKey actorKey = new ActorRecord(cursor).getKey();
                            if (!actorKey.isAuthenticatedUser(apiTokenProvider.getAuthenticatedUser())) {
                                String displayName = cursor.getString(vw_Participant.DISPLAY_NAME.ordinal());
                                participantEmails.add(cursor.getString(vw_Participant.EMAIL.ordinal()));
                                participantNames.add(NameUtils.getShortName(displayName));
                                if (conversation.isOneOnOne())
                                    oneOnOneParticipantUUID = cursor.getString(vw_Participant.ACTOR_UUID.ordinal());
                            }
                        }

                        participantEmailsString = TextUtils.join(" ", participantEmails);
                        participantNamesString = TextUtils.join(ConversationSearchDataEntry.NAME_DELIMITER + " ", participantNames);

                        updateConversationSearchRecord(batch, conversation.getId(),
                                getConversationName(conversation.getId()),
                                participantNamesString,
                                participantEmailsString,
                                conversation.isOneOnOne(),
                                conversation.getPreviewActivityPublishedTime(),
                                conversation.getLocusUrl(),
                                oneOnOneParticipantUUID
                        );
                    } finally {
                        if (cursor != null)
                            cursor.close();
                    }
                }
                if (apiTokenProvider.isAuthenticated())
                    batch.apply();

                if (!conversationsToUpdate.isEmpty()) {
                    updateConversationSearchTask.scheduleExecute();
                }
            }
        }

        @Override
        protected void onException(Exception e) throws RuntimeException {
            Ln.e(e, "Failed updating conversation search index");
        }
    };

    ThrottledAsyncTask updateContentTaskThrottler = new ThrottledAsyncTask(1000) {
        @Override
        protected void doInBackground() {
            HashSet<MessageSearchReferenceObject> activities = new HashSet<>();
            contentActivitesToUpdate.drainTo(activities);
            Batch batch = batchProvider.get();

            Ln.d("Updating search index for " + activities.size() + " content activities");
            for (MessageSearchReferenceObject messageSearchReferenceObject : activities) {
                switch (messageSearchReferenceObject.getType()) {
                    case PHOTO:
                    case FILE:
                    case UPDATE_CONTENT:
                    case WHITEBOARD:
                        String conversationId = messageSearchReferenceObject.getConversationId();
                        DisplayableFileSet displayableFileSet = (DisplayableFileSet) messageSearchReferenceObject.getMessageObject();
                        String actorUUID = (displayableFileSet.getActorKey() != null) ? displayableFileSet.getActorKey().getUuid() : null;
                        for (DisplayableFile file : displayableFileSet.getItems()) {
                            updateContentSearchRecord(batch, conversationId,
                                    file.getName(),
                                    NameUtils.getShortName(messageSearchReferenceObject.getAuthorName()),
                                    file.getMimeType(),
                                    messageSearchReferenceObject.getActivityId(),
                                    messageSearchReferenceObject.getPublishedTime(),
                                    messageSearchReferenceObject.getType(),
                                    getConversationName(conversationId),
                                    actorUUID,
                                    messageSearchReferenceObject.getActivityTempId()
                            );
                        }
                        break;
                }
            }
            if (apiTokenProvider.isAuthenticated())
                batch.apply();
        }

        @Override
        protected void onException(Exception e) throws RuntimeException {
            Ln.e(e, "Failed updating content search index");
        }
    };

    ThrottledAsyncTask updateMessageTaskThrottler = new ThrottledAsyncTask(1000) {
        @Override
        protected void doInBackground() {
            synchronized (updateMessageTaskThrottler) {
                HashSet<MessageSearchReferenceObject> activities = new HashSet<>();

                // Trying to do too many at once can hog resources
                messageActivitesToUpdate.drainTo(activities, MAX_CHUNK_SIZE);

                Batch batch = batchProvider.get();
                Ln.d("Updating search index for " + activities.size() + " searchable messages");
                for (MessageSearchReferenceObject messageSearchReferenceObject : activities) {
                    if (messageSearchReferenceObject.getType() != ActivityEntry.Type.MESSAGE) {
                        continue;
                    }
                    String conversationId = messageSearchReferenceObject.getConversationId();
                    Message message = messageSearchReferenceObject.getMessageObject();
                    String actorUUID = (message.getActorKey() != null) ? message.getActorKey().getUuid() : null;

                    updateMessageSearchRecord(batch,
                            conversationId,
                            message.getText(),
                            NameUtils.getShortName(messageSearchReferenceObject.getAuthorName()),
                            actorUUID,
                            messageSearchReferenceObject.getActivityId(),
                            messageSearchReferenceObject.getPublishedTime(),
                            getConversationName(conversationId),
                            messageSearchReferenceObject.getType(),
                            messageSearchReferenceObject.getActivityTempId(),
                            messageSearchReferenceObject.getFileSharedCount(),
                            getOneOnOneParticipant(conversationId));
                }
                if (apiTokenProvider.isAuthenticated())
                    batch.apply();

                // Reschedule if we still have work to do
                if (!messageActivitesToUpdate.isEmpty())
                    updateMessageTaskThrottler.scheduleExecute();
            }
        }

        @Override
        protected void onException(Exception e) throws RuntimeException {
            Ln.e(e, "Failed updating message search index");
        }
    };

    private String getConversationName(String conversationId) {
        String ret = ConversationContentProviderQueries.getOneValue(context.getContentResolver(),
                vw_Conversation.CONTENT_URI,
                vw_Conversation.CONVERSATION_DISPLAY_NAME.name(),
                vw_Conversation.CONVERSATION_ID + "=?",
                new String[]{conversationId});

        return ret;
    }

    private String getOneOnOneParticipant(String conversationId) {
        String ret = ConversationContentProviderQueries.getOneValue(context.getContentResolver(),
                vw_Conversation.CONTENT_URI,
                vw_Conversation.ONE_ON_ONE_PARTICIPANT.name(),
                vw_Conversation.CONVERSATION_ID + "=?",
                new String[]{conversationId});

        return ret;
    }

    @Inject
    public SearchManager(Gson gson, ApiTokenProvider apiTokenProvider, Context context, ActorRecordProvider actorRecordProvider, DeviceRegistration deviceRegistration, EventBus bus, Provider<Batch> batchProvider) {
        this.gson = gson;
        this.apiTokenProvider = apiTokenProvider;
        this.context = context;
        this.actorRecordProvider = actorRecordProvider;
        this.deviceRegistration = deviceRegistration;
        this.bus = bus;
        this.batchProvider = batchProvider;
        this.bus.register(this);
        ArrayList<String> typeList = new ArrayList<>();
        for (ActivityEntry.Type type : ActivityEntry.Type.values()) {
            if (type.isSearchable() && isContentType(type)) {
                typeList.add(String.valueOf(type.ordinal()));
            }
        }
    }

    public void onEventAsync(ActivityDecryptedEvent event) {
        updateActivitySearch(event.getActivities());
    }

    public void onEvent(LogoutEvent logoutEvent) {
        updateConversationSearchTask.cancelAndWait();
        updateMessageTaskThrottler.cancelAndWait();
        updateContentTaskThrottler.cancelAndWait();

        messageActivitesToUpdate.clear();
        conversationsToUpdate.clear();
        contentActivitesToUpdate.clear();
    }

    public boolean isContentSearchEnabled() {
        togglePrevious = this.isContentSearchEnabled;
        this.isContentSearchEnabled = deviceRegistration.getFeatures().isContentSearchEnabled();
        if (togglePrevious != null && !togglePrevious.equals(this.isContentSearchEnabled) && Boolean.TRUE.equals(this.isContentSearchEnabled))
            checkForContentSearchUpdate();

        return this.isContentSearchEnabled;
    }

    public boolean isMessageSearchEnabled() {
        messageSearchPreviousValue = this.isMessageSearchEnabled;
        this.isMessageSearchEnabled = deviceRegistration.getFeatures().isMessageSearchEnabled();
        if (messageSearchPreviousValue != null && !messageSearchPreviousValue.equals(this.isMessageSearchEnabled) && Boolean.TRUE.equals(this.isMessageSearchEnabled))
            checkForMessageSearchUpdate();
        return this.isMessageSearchEnabled;
    }

    public boolean isContentType(ActivityEntry.Type type) {
        switch (type) {
            case PHOTO:
            case FILE:
            case UPDATE_CONTENT:
            case WHITEBOARD:
                return true;
            default:
                return false;
        }
    }

    public void checkForUpdate() {
        Cursor cursor = context.getContentResolver().query(ConversationSearchEntry.CONTENT_URI, new String[]{"_id"}, null, null, "_id asc limit 1");
        if (cursor != null && cursor.getCount() == 0) {
            updateConversationSearch();
        } else {
            Ln.d("No search index rebuild necessary");
        }
        cursor.close();
        checkForContentSearchUpdate();
        checkForMessageSearchUpdate();
    }

    public void checkForContentSearchUpdate() {
        if (!isContentSearchEnabled()) {
            return;
        }
        Cursor cursor = context.getContentResolver().query(ContentSearchEntry.CONTENT_URI, new String[]{"_id"}, null, null, "_id asc limit 1");
        if (cursor != null && cursor.getCount() == 0) {
            updateContentSearch();
        } else {
            Ln.d("No search index rebuild necessary for Content");
        }
        cursor.close();
    }

    public void checkForMessageSearchUpdate() {
        if (!isMessageSearchEnabled()) {
            return;
        }
        Cursor cursor = context.getContentResolver().query(MessageSearchEntry.CONTENT_URI, new String[]{"_id"}, null, null, "_id asc limit 1");
        if (cursor != null && cursor.getCount() == 0) {
            updateMessageSearch();
        } else {
            Ln.d("No search index rebuild necessary for Messages");
        }
        cursor.close();
    }

    public void updateActivitySearch(Collection<Activity> activities) {
        if (activities != null && !activities.isEmpty()) {
            addActivities(activities);
            if (isContentSearchEnabled() && !contentActivitesToUpdate.isEmpty()) {
                updateContentTaskThrottler.scheduleExecute();
            }
            if (isMessageSearchEnabled() && !messageActivitesToUpdate.isEmpty()) {
                updateMessageTaskThrottler.scheduleExecute();
            }
        }
    }

    public void updateActivitySearch(ActivityEntry.Type type, Message message, String activityId, String conversationId, long publishedTime, String activityTempId) {
        if (isMessageSearchEnabled() || isContentSearchEnabled()) {
            addActivityToSearch(new MessageSearchReferenceObject(activityId, conversationId, message, type, publishedTime, activityTempId));
        }
    }

    public String getAuthorNameFromMessage(Message message) {
        if (message != null && message.getActorKey() != null) {
            ActorRecord actorRecord = actorRecordProvider.get(message.getActorKey());
            if (actorRecord != null) {
                return actorRecord.getDisplayName();
            }
        }
        return null;
    }

    public void addActivityToSearch(MessageSearchReferenceObject messageSearchReferenceObject) {
        switch (messageSearchReferenceObject.getType()) {
            case MESSAGE:
                if (!messageActivitesToUpdate.contains(messageSearchReferenceObject)) {
                    messageActivitesToUpdate.add(messageSearchReferenceObject);
                }
                break;
            case PHOTO:
            case FILE:
            case UPDATE_CONTENT:
            case WHITEBOARD:
                if (!contentActivitesToUpdate.contains(messageSearchReferenceObject)) {
                    contentActivitesToUpdate.add(messageSearchReferenceObject);
                }
                //Content type activities containing a message during share goes into messages bucket
                if (messageSearchReferenceObject.getMessageObject().getText() != null && !messageActivitesToUpdate.contains(messageSearchReferenceObject)) {
                    MessageSearchReferenceObject messageSearchReferenceObjectCopy = messageSearchReferenceObject.clone();
                    messageSearchReferenceObjectCopy.setType(ActivityEntry.Type.MESSAGE);
                    messageActivitesToUpdate.add(messageSearchReferenceObjectCopy);
                }
                break;
        }
    }

    public void updateConversationSearch(Collection<ConversationRecord> conversations, Collection<Activity> activities) {
        if (conversations != null && !conversations.isEmpty()) {
            conversationsToUpdate.addAll(conversations);
            updateConversationSearchTask.scheduleExecute();
        }
        if (activities != null && !activities.isEmpty()) {
            updateActivitySearch(activities);
        }
    }

    private void addActivities(Collection<Activity> activities) {
        if (isContentSearchEnabled() || isMessageSearchEnabled()) {
            for (Activity activity : activities) {
                if (activity.getType().isSearchable() && !activity.isEncrypted()) {
                    addActivityToSearch(new MessageSearchReferenceObject(activity));
                }
            }
        }
    }

    public void deleteConversationSearchEntry(String conversationId) {
        context.getContentResolver().delete(ConversationSearchEntry.CONTENT_URI, ConversationSearchEntry.suggest_intent_data.name() + " = ?", new String[]{conversationId});
        if (isContentSearchEnabled()) {
            context.getContentResolver().delete(ContentSearchEntry.CONTENT_URI, ContentSearchEntry.suggest_intent_data.name() + " = ?", new String[]{conversationId});
        }
        if (isMessageSearchEnabled()) {
            context.getContentResolver().delete(MessageSearchEntry.CONTENT_URI, MessageSearchEntry.suggest_intent_data.name() + " = ?", new String[]{conversationId});
        }
        Ln.d("FTS record deleted for " + conversationId);
    }

    //TODO: Set tokenchars for fts tokenizer
    private boolean searchStringContainsSpecialCharacters(String searchString) {
        return (!TextUtils.isEmpty(searchString) && ((searchString.trim().contains(" ") || searchString.trim().contains("-") || searchString.trim().contains("@") || searchString.trim().contains("+")) || searchString.trim().contains(".")));
    }

    private void setSelectionArgs(String[] selectionArgs, List<String> messageSelectionArgs, List<String> contentSelectionArgs, String searchString, SearchStringWithModifiers searchStringWithModifiers) {
        if ((searchStringWithModifiers != null && !searchStringWithModifiers.usesModifiers()) && searchStringContainsSpecialCharacters(searchString)) {
            // Search for phrase
            selectionArgs[0] = "\"" + searchString + "*\"";
            contentSelectionArgs.add(selectionArgs[0]);
            messageSelectionArgs.add(selectionArgs[0]);
        } else {
            // Search for prefix
            if (searchStringWithModifiers != null && searchStringWithModifiers.usesModifiers()) {
                selectionArgs[0] = searchStringWithModifiers.getSelectionCriteriaForConversationSearch();
                List<String> selectionArgsForContentSearchWithModifiers = searchStringWithModifiers.getSelectionArgsForContentSearchWithModifiers();
                if (!selectionArgsForContentSearchWithModifiers.isEmpty()) {
                    contentSelectionArgs.addAll(selectionArgsForContentSearchWithModifiers);
                }
                List<String> selectionArgsForMessageSearchWithModifiers = searchStringWithModifiers.getSelectionArgsForMessageSearchWithModifiers();
                if (!selectionArgsForMessageSearchWithModifiers.isEmpty()) {
                    messageSelectionArgs.addAll(selectionArgsForMessageSearchWithModifiers);
                }
            } else {
                if (!TextUtils.isEmpty(searchString)) {
                    selectionArgs[0] = ConversationSearchEntry.CONVERSATION_NAME + ":" + searchString + "* OR " +
                            ConversationSearchEntry.PARTICIPANT_EMAILS + ":" + searchString + " OR " +
                            ConversationSearchEntry.PARTICIPANT_NAMES + ":" + searchString;
                    contentSelectionArgs.add(ContentSearchEntry.CONTENT_TITLE + ":" + searchString + "* OR " +
                            ContentSearchEntry.CONTENT_TYPE + ":" + searchString + "* OR " +
                            ContentSearchEntry.CONTENT_SHARED_BY + ":" + searchString + "*");
                    messageSelectionArgs.add(MessageSearchEntry.MESSAGE_TEXT + ":" + searchString + "* OR " +
                            MessageSearchEntry.MESSAGE_POSTED_BY + ":" + searchString + "*");
                }
            }
        }
    }

    @SuppressLint("Recycle")
    public SearchResultsCursorWithCount query(String searchString, SearchStringWithModifiers searchStringWithModifiers) {
        String[] selectionArgs = new String[1];
        List<String> contentSelectionArgs = new ArrayList<>();
        List<String> messageSelectionArgs = new ArrayList<>();
        ArrayList<Cursor> cursorList = new ArrayList<>();
        SearchResultsCursorWithCount searchResultsCursorWithCount = new SearchResultsCursorWithCount();
        String limit = "LIMIT " + MAX_SEARCH_RESULT_LIMIT;
        String orderBy = ConversationSearchEntry.suggest_intent_extra_data.name() + " ASC, " + ConversationSearchEntry.LAST_ACTIVE_TIME.name() + " DESC " + limit;
        Cursor cursor;
        setSelectionArgs(selectionArgs, messageSelectionArgs, contentSelectionArgs, searchString, searchStringWithModifiers);
        if (!TextUtils.isEmpty(selectionArgs[0])) {
            cursor = context.getContentResolver().query(
                    ConversationSearchEntry.CONTENT_URI,
                    ConversationSearchEntry.PROJECTION_WITH_SNIPPET,
                    "ConversationSearchEntry.CONVERSATION_NAME !=0 AND " + ConversationSearchEntry.TABLE_NAME + " MATCH ?",
                    selectionArgs,
                    orderBy);
            searchResultsCursorWithCount.setConversationCountInCursor(cursor.getCount());
            cursorList.add(cursor);
        }

        String messageSelectionCriteria = searchStringWithModifiers.usesModifiers() ? searchStringWithModifiers.getSelectionCriteriaForMessageSearchWithModifiers() : "";
        if (isMessageSearchEnabled() && (!messageSelectionArgs.isEmpty() || !messageSelectionCriteria.isEmpty())) {
            orderBy = MessageSearchEntry.MESSAGE_PUBLISHED_TIME.name() + " DESC " + limit;
            cursor = context.getContentResolver().query(
                    MessageSearchEntry.CONTENT_URI,
                    MessageSearchEntry.DEFAULT_PROJECTION,
                    getMessageSearchEntrySelection(messageSelectionCriteria, messageSelectionArgs, searchStringWithModifiers),
                    (messageSelectionArgs.isEmpty() ? null : messageSelectionArgs.toArray(new String[messageSelectionArgs.size()])),
                    orderBy);
            searchResultsCursorWithCount.setMessageCountInCursor(cursor.getCount());
            cursorList.add(cursor);
        }

        String contentSelectionCriteria = searchStringWithModifiers.usesModifiers() ? searchStringWithModifiers.getSelectionCriteriaForContentSearchWithModifiers() : "";
        if (isContentSearchEnabled() && !contentSelectionArgs.isEmpty()) {
            orderBy = ContentSearchEntry.CONTENT_PUBLISHED_TIME.name() + " DESC " + limit;
            cursor = context.getContentResolver().query(
                    ContentSearchEntry.CONTENT_URI,
                    ContentSearchEntry.DEFAULT_PROJECTION,
                    getContentSearchEntrySelection(contentSelectionCriteria, contentSelectionArgs, searchStringWithModifiers),
                    (contentSelectionArgs.isEmpty() ? null : contentSelectionArgs.toArray(new String[contentSelectionArgs.size()])),
                    orderBy);
            searchResultsCursorWithCount.setContentCountInCursor(cursor.getCount());
            cursorList.add(cursor);
        }

        searchResultsCursorWithCount.setCursor(new MergeCursor(cursorList.toArray(new Cursor[cursorList.size()])));
        return searchResultsCursorWithCount;
    }

    private String getContentSearchEntrySelection(String contentSelectionCriteria, List<String> contentSelectionArgs, SearchStringWithModifiers searchStringWithModifiers) {
        StringBuilder contentSearchQueryBuilder = new StringBuilder(ContentSearchEntry.CONTENT_TITLE + " !=0");
        if (!TextUtils.isEmpty(contentSelectionCriteria)) {
            contentSearchQueryBuilder.append(" AND " + contentSelectionCriteria);
        }
        if (!contentSelectionArgs.isEmpty() && (!searchStringWithModifiers.usesModifiers() || contentSelectionArgs.toString().contains(MATCH_OPERATOR))) {
            contentSearchQueryBuilder.append(" AND " + ContentSearchEntry.TABLE_NAME + " MATCH ? ");
        }
        return contentSearchQueryBuilder.toString();
    }

    private String getMessageSearchEntrySelection(String messageSelectionCriteria, List<String> messageSelectionArgs, SearchStringWithModifiers searchStringWithModifiers) {
        StringBuilder messageSearchQueryBuilder = new StringBuilder(MessageSearchEntry.MESSAGE_TEXT + " !=0");
        if (!TextUtils.isEmpty(messageSelectionCriteria)) {
            messageSearchQueryBuilder.append(" AND " + messageSelectionCriteria);
        }
        if (!messageSelectionArgs.isEmpty() && (!searchStringWithModifiers.usesModifiers() || messageSelectionArgs.toString().contains(MATCH_OPERATOR))) {
            messageSearchQueryBuilder.append(" AND " + MessageSearchEntry.TABLE_NAME + " MATCH ? ");
        }
        return messageSearchQueryBuilder.toString();
    }

    /**
     * Given a collection of emails, return a vw_Conversation cursor containing conversations shared
     * by all of those users.
     *
     * @param emails The list of people
     * @return The vw_Conversation cursor, or null if there are no rooms in common.
     */

    public Cursor querySuggestedConversations(Collection<String> emails) {
        long t = System.currentTimeMillis();
        HashSet<String> convIds = new HashSet<>();
        for (String email : emails) {
            List<String> convIdsForEmail = ConversationContentProviderQueries.getConversationsByParticipant(context.getContentResolver(), email);
            if (convIds.isEmpty()) {
                convIds.addAll(convIdsForEmail);
                continue;
            }
            convIds.retainAll(convIdsForEmail);
            if (convIds.isEmpty())
                break;
        }
        Cursor ret = getConversationsCursor(convIds);

        t = System.currentTimeMillis() - t;
        if (t > 1000) {
            Ln.w(new Exception("Perf Warning: querySuggestedConversations took " + t + "ms"));
        }
        return ret;
    }

    /**
     * Get a vw_Conversation cursor with one row for each conversation ID that exists.
     *
     * @param conversationIds set of Conversation ID's to search for.
     * @return the cursor
     */
    private Cursor getConversationsCursor(HashSet<String> conversationIds) {
        if (conversationIds.isEmpty())
            return null;

        StringBuilder builder = new StringBuilder();

        builder.append(vw_Conversation.CONVERSATION_ID.name());
        builder.append(" IN (");
        List<String> params = Collections.nCopies(conversationIds.size(), "?");
        builder.append(Strings.join(",", params));
        builder.append(")");

        return context.getContentResolver().query(vw_Conversation.CONTENT_URI,
                vw_Conversation.DEFAULT_PROJECTION,
                builder.toString(),
                conversationIds.toArray(new String[]{}),
                null);
    }

    private void updateConversationSearchRecord(Batch batch, String conversationId,
                                                String conversationTitle,
                                                String participantNames,
                                                String participantEmails,
                                                boolean isOneOnOne,
                                                long lastActiveTime,
                                                Uri locusUrl,
                                                String oneOnOneParticipantUUID) {

        ContentValues values = new ContentValues();
        if (participantNames != null) {
            values.put(ConversationSearchDataEntry.PARTICIPANT_NAMES.name(), participantNames);
        }
        if (participantEmails != null) {
            values.put(ConversationSearchDataEntry.PARTICIPANT_EMAILS.name(), participantEmails);
        }

        values.put(ConversationSearchDataEntry.suggest_text_1.name(), conversationTitle);
        values.put(ConversationSearchDataEntry.suggest_intent_data.name(), conversationId);
        values.put(ConversationSearchDataEntry.CONVERSATION_NAME.name(), conversationTitle);
        values.put(ConversationSearchDataEntry.suggest_intent_extra_data.name(), isOneOnOne ? ConversationSearchDataEntry.TYPE_ONE_ON_ONE : ConversationSearchDataEntry.TYPE_GROUP);
        values.put(ConversationSearchDataEntry.LOCUS_URL.name(), UriUtils.toString(locusUrl));
        values.put(ConversationSearchDataEntry.ONE_ON_ONE_PARTICIPANT_UUID.name(), oneOnOneParticipantUUID);

        if (lastActiveTime > 0) {
            values.put(ConversationSearchDataEntry.LAST_ACTIVE_TIME.name(), lastActiveTime);
        }

        batch.add(ContentProviderOperation.newInsert(ConversationSearchDataEntry.CONTENT_URI)
                .withValues(values)
                .build());

        batch.add(ContentProviderOperation.newUpdate(ConversationSearchDataEntry.CONTENT_URI)
                .withValues(values)
                .withSelection(ConversationSearchDataEntry.suggest_intent_data + "=?", new String[]{conversationId})
                .build());

        Ln.d("Added conversation search entry for " + conversationTitle + " / " + conversationId);
    }

    public void updateConversationSearch() {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(vw_Conversation.CONTENT_URI, vw_Conversation.DEFAULT_PROJECTION, null, null, null);

            while (cursor != null && cursor.moveToNext()) {
                ConversationRecord record = ConversationRecord.buildFromCursor(cursor, gson, null);
                conversationsToUpdate.add(record);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        Ln.d("Rebuilding search index for " + conversationsToUpdate.size() + " conversations");
        updateConversationSearchTask.scheduleExecute();
    }

    public void deleteActivityFromSearch(String activityId) {
        if (isMessageSearchEnabled()) {
            synchronized (databaseLock) {
                context.getContentResolver().delete(MessageSearchEntry.CONTENT_URI, MessageSearchEntry.ACTIVITY_ID.name() + " = ?", new String[]{activityId});
                Ln.d("Deleted FTS MessageSearchEntry for " + activityId);
            }
        }
    }

    public void updateContentSearch() {
        if (!isContentSearchEnabled()) {
            return;
        }

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(ActivityEntry.CONTENT_URI, ActivityEntry.DEFAULT_PROJECTION, ActivityEntry.IS_ENCRYPTED + " =0"
                    + " AND " + ActivityEntry.ACTIVITY_TYPE + " IN (" + Strings.join(",", typeList) + ")", null, null);

            while (cursor != null && cursor.moveToNext()) {
                final ActivityEntry.Type type = ActivityEntry.Type.values()[cursor.getInt(ActivityEntry.ACTIVITY_TYPE.ordinal())];
                final boolean isEncrypted = cursor.getInt(ActivityEntry.IS_ENCRYPTED.ordinal()) == 1;
                if (type.isSearchable() && !isEncrypted) {
                    contentActivitesToUpdate.add(new MessageSearchReferenceObject(cursor));
                }
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        Ln.d("Rebuilding search index for " + contentActivitesToUpdate.size() + " activities");
        updateContentTaskThrottler.scheduleExecute();
    }

    private void updateContentSearchRecord(Batch batch, String conversationId, String contentTitle, String contentAuthor, String contentType, String activityId, long contentPublishedTime, ActivityEntry.Type type, String conversationName, String actorUUID, String activityTempId) {
        if (!isContentSearchEnabled()) {
            return;
        }
        ContentValues values = new ContentValues();
        if (contentTitle != null) {
            values.put(ContentSearchDataEntry.CONTENT_TITLE.name(), contentTitle);
            values.put(ContentSearchDataEntry.suggest_text_1.name(), contentTitle);
        }
        values.put(ContentSearchDataEntry.suggest_intent_data.name(), conversationId);
        values.put(ContentSearchDataEntry.ACTIVITY_ID.name(), activityId);
        values.put(ContentSearchDataEntry.CONTENT_SHARED_BY.name(), contentAuthor);
        values.put(ContentSearchDataEntry.CONTENT_TYPE.name(), contentType);
        values.put(ContentSearchDataEntry.CONTENT_PUBLISHED_TIME.name(), contentPublishedTime);
        values.put(ContentSearchDataEntry.suggest_intent_extra_data.name(), type.name());
        values.put(ContentSearchDataEntry.CONVERSATION_NAME.name(), conversationName);
        values.put(ContentSearchDataEntry.CONTENT_POSTED_BY_UUID.name(), actorUUID);

        //delete any provisional content results already stored for this activity
        if (!activityId.equals(activityTempId)) {
            batch.add(ConversationContentProviderOperation.deleteContentSearchActivityWithTempID(activityTempId));
        }

        batch.add(ContentProviderOperation.newInsert(ConversationContract.ContentSearchDataEntry.CONTENT_URI)
                .withValues(values)
                .build());

        batch.add(ContentProviderOperation.newUpdate(ConversationContract.ContentSearchDataEntry.CONTENT_URI)
                .withValues(values)
                .withSelection(ContentSearchEntry.ACTIVITY_ID + "=?", new String[]{activityId})
                .build());
    }

    public void updateMessageSearch() {
        if (!isMessageSearchEnabled()) {
            return;
        }

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(ActivityEntry.CONTENT_URI, ActivityEntry.DEFAULT_PROJECTION, ActivityEntry.IS_ENCRYPTED + " =0"
                    + " AND " + ActivityEntry.ACTIVITY_TYPE + " =" + String.valueOf(ActivityEntry.Type.MESSAGE.ordinal()), null, null);

            while (cursor != null && cursor.moveToNext()) {
                messageActivitesToUpdate.add(new MessageSearchReferenceObject(cursor));
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        Ln.d("Rebuilding search index for " + messageActivitesToUpdate.size() + " messages");
        updateMessageTaskThrottler.scheduleExecute();
    }

    private void updateMessageSearchRecord(Batch batch, String conversationId, String messageText, String postedBy, String actorUUId, String activityId, long messagePublishedTime, String conversationName, ActivityEntry.Type type, String tempId, int fileSharedCount, String oneOneOneParticipant) {
        if (!isMessageSearchEnabled()) {
            return;
        }

        //delete any provisional message results already stored for this activity
        if (!activityId.equals(tempId)) {
            batch.add(ConversationContentProviderOperation.deleteMessageSearchActivityWithTempID(tempId));
        }

        batch.add(ConversationContentProviderOperation.insertMessageSearchActivity(conversationId,
                messageText,
                postedBy,
                actorUUId,
                activityId,
                messagePublishedTime,
                conversationName,
                type,
                fileSharedCount,
                oneOneOneParticipant));

        batch.add(ConversationContentProviderOperation.updateMessageSearchActivity(activityId,
                conversationId,
                messageText,
                postedBy,
                actorUUId,
                messagePublishedTime,
                conversationName,
                type,
                fileSharedCount,
                oneOneOneParticipant));


    }

    public class MessageSearchReferenceObject implements Cloneable {
        private String activityId;
        private String conversationId;
        private Message messageObject;
        private long publishedTime;
        private String authorName;
        private ActivityEntry.Type type;
        private String activityTempId;
        private int fileSharedCount = 0;

        public String getActivityTempId() {
            return activityTempId;
        }

        public String getAuthorName() {
            return authorName;
        }

        public void setAuthorName(String authorName) {
            this.authorName = authorName;
        }

        public ActivityEntry.Type getType() {
            return type;
        }

        public void setType(ActivityEntry.Type type) {
            this.type = type;
        }

        public long getPublishedTime() {
            return publishedTime;
        }

        public void setPublishedTime(long publishedTime) {
            this.publishedTime = publishedTime;
        }

        public int getFileSharedCount() {
            return fileSharedCount;
        }

        public void setFileSharedCount(int fileSharedCount) {
            this.fileSharedCount = fileSharedCount;
        }


        public MessageSearchReferenceObject(String activityId, String conversationId, Message messageObject, ActivityEntry.Type type, long publishedTime, String activityTempId) {
            this.activityId = activityId;
            this.conversationId = conversationId;
            this.messageObject = messageObject;
            this.authorName = getAuthorNameFromMessage(messageObject);
            this.type = type;
            this.publishedTime = publishedTime;
            this.activityTempId = activityTempId;
            if (messageObject != null && isContentType(type)) {
                this.fileSharedCount = ((DisplayableFileSet) messageObject).size();
            }
        }

        public MessageSearchReferenceObject(Activity activity) {
            this.activityId = activity.getId();
            this.conversationId = activity.getConversationId();
            switch (activity.getType()) {
                case PHOTO:
                case FILE:
                case UPDATE_CONTENT:
                case WHITEBOARD:
                    DisplayableFileSet displayableFileSet = DisplayableFileSet.fromActivity(activity);
                    this.messageObject = displayableFileSet;
                    this.fileSharedCount = (displayableFileSet != null) ? displayableFileSet.size() : 0;
                    break;
                case MESSAGE:
                    this.messageObject = new Message(activity);
                    break;
            }
            this.type = activity.getType();
            Person person = activity.getActor();
            if (person != null) {
                this.authorName = person.getDisplayName();
            }
            this.publishedTime = activity.getPublished().getTime();
            this.activityTempId = activity.getClientTempId();

        }

        public MessageSearchReferenceObject(Cursor cursor) {
            ActivityEntry.Type type = ActivityEntry.Type.values()[cursor.getInt(ActivityEntry.ACTIVITY_TYPE.ordinal())];
            if (isContentType(type)) {
                DisplayableFileSet displayableFileSet = (DisplayableFileSet) gson.fromJson(cursor.getString(ActivityEntry.ACTIVITY_DATA.ordinal()), type.getSyncClass());
                this.messageObject = displayableFileSet;
                this.fileSharedCount = (displayableFileSet != null) ? displayableFileSet.size() : 0;
            } else if (type == ActivityEntry.Type.MESSAGE) {
                this.messageObject = gson.fromJson(cursor.getString(ActivityEntry.ACTIVITY_DATA.ordinal()), type.getSyncClass());
            }
            this.type = type;
            this.activityId = cursor.getString(ActivityEntry.ACTIVITY_ID.ordinal());
            this.conversationId = cursor.getString(ActivityEntry.CONVERSATION_ID.ordinal());
            this.authorName = getAuthorNameFromMessage(this.messageObject);
            this.publishedTime = cursor.getLong(ActivityEntry.ACTIVITY_PUBLISHED_TIME.ordinal());
            this.activityTempId = cursor.getString(ActivityEntry.SYNC_OPERATION_ID.ordinal());
        }

        public MessageSearchReferenceObject clone() {
            try {
                return (MessageSearchReferenceObject) super.clone();
            } catch (CloneNotSupportedException e) {
                Ln.e(false, e, "Unable to clone MessageSearchReferenceObject");
            }
            return null;
        }


        public String getActivityId() {
            return activityId;
        }

        public void setActivityId(String activityId) {
            this.activityId = activityId;
        }

        public String getConversationId() {
            return conversationId;
        }

        public void setConversationId(String conversationId) {
            this.conversationId = conversationId;
        }

        public Message getMessageObject() {
            return messageObject;
        }

        public void setMessageObject(Message messageObject) {
            this.messageObject = messageObject;
        }
    }

    public class SearchResultsCursorWithCount {

        private Cursor cursor;
        private int conversationCountInCursor;
        private int contentCountInCursor;
        private int messageCountInCursor;

        public SearchResultsCursorWithCount() {
            this.cursor = null;
            this.conversationCountInCursor = 0;
            this.contentCountInCursor = 0;
            this.messageCountInCursor = 0;
        }

        public void setCursor(Cursor cursor) {
            this.cursor = cursor;
        }

        public void setConversationCountInCursor(int conversationCountInCursor) {
            this.conversationCountInCursor = conversationCountInCursor;
        }

        public void setContentCountInCursor(int contentCountInCursor) {
            this.contentCountInCursor = contentCountInCursor;
        }

        public void setMessageCountInCursor(int messageCountInCursor) {
            this.messageCountInCursor = messageCountInCursor;
        }

        public Cursor getCursor() {
            return cursor;
        }

        public int getConversationCountInCursor() {
            return conversationCountInCursor;
        }

        public int getMessageCountInCursor() {
            return messageCountInCursor;
        }

        public int getContentCountInCursor() {
            return contentCountInCursor;
        }


    }
}
