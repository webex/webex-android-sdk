package com.cisco.spark.android.sync.operationqueue;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Comment;
import com.cisco.spark.android.sync.ActorRecordProvider;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.Message;
import com.cisco.spark.android.util.NameUtils;
import com.google.gson.Gson;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;

import javax.inject.Inject;

public class NewConversationWithRepostedMessagesOperation extends NewConversationOperation {
    public static final int MAX_REPOST_CHARS = 10000;
    public static final String REPOSTED_MESSAGE_CONTENT_KEY = "REPOSTED_MESSAGE_CONTENT";
    public static final String REPOSTED_MESSAGE_TEXT_KEY = "REPOSTED_MESSAGE_TEXT";
    private String repostedMessagesContent;
    private String repostedMessagesText;
    private String conversationIdForReposts;

    @Inject transient Context context;

    public NewConversationWithRepostedMessagesOperation(Injector injector, Collection<String> emailsOrUuids, String title, String summary, @Nullable String teamId, String conversationIdForReposts, EnumSet<CreateFlags> createFlags) {
        super(injector, emailsOrUuids, title, summary, teamId, createFlags);
        this.conversationIdForReposts = conversationIdForReposts;
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState onEnqueue() {
        // This StringBuilder will contain every message, starting with the most recent one and ending with the oldest one until the capacity is reached or
        // We add all the messages. Each message will be represented as a string, and then reversed so that in the end we can reverse the entire string builder
        // which will place the newest messages at the end of the string. This is done so that if we reach capacity we lose older messages rather than the
        // newest ones

        Cursor cursor = ConversationContentProviderQueries.getMax50RecentMessagesFromConversation(getContentResolver(), conversationIdForReposts);


        HashMap<String, String> messageRepostData = buildRepostMessage(cursor, gson, actorRecordProvider, context);
        repostedMessagesContent = messageRepostData.get(REPOSTED_MESSAGE_CONTENT_KEY);
        repostedMessagesText = messageRepostData.get(REPOSTED_MESSAGE_TEXT_KEY);

        Comment comment = new Comment(repostedMessagesText);
        comment.setContent(repostedMessagesContent); //TODO: Re-enable when rich text is enabled across clients
        operationQueue.postMessage(getConversationId(), comment).setDependsOn(this);

        return super.onEnqueue();
    }

    @NonNull
    @Override
    public ConversationContract.SyncOperationEntry.OperationType getOperationType() {
        return ConversationContract.SyncOperationEntry.OperationType.CREATE_CONVERSATION_WITH_REPOSTS;
    }

    public static HashMap<String, String> buildRepostMessage(Cursor cursor, Gson gson, ActorRecordProvider actorRecordProvider, Context context) {
        StringBuilder repostedMessageContentBuilder = new StringBuilder(MAX_REPOST_CHARS);  // Includes HTML tags
        StringBuilder repostedMessageTextBuilder = new StringBuilder(MAX_REPOST_CHARS);     // Plain text, no tags

        if (cursor != null) {
            while (cursor.moveToNext()) {
                ConversationContract.ActivityEntry.Type type = ConversationContract.ActivityEntry.Type.values()[cursor.getInt(ConversationContract.ActivityEntry.ACTIVITY_TYPE.ordinal())];
                Message message = gson.fromJson(cursor.getString(ConversationContract.ActivityEntry.ACTIVITY_DATA.ordinal()), type.getSyncClass());
                String timestampText = buildRepostMessageTimestampText(context, cursor.getLong(ConversationContract.ActivityEntry.ACTIVITY_PUBLISHED_TIME.ordinal()));

                String messageTextString =
                        NameUtils.getShortName(actorRecordProvider.get(message.getActorKey()).getDisplayName()) +
                        " - " +
                        timestampText +
                        "\n\n" +
                        message.getText();

                String messageContentString =
                        "<b>" +
                        NameUtils.getShortName(actorRecordProvider.get(message.getActorKey()).getDisplayName()) +
                        "</b> - <i>" +
                        timestampText +
                        "</i>\n\n" +
                        message.getText();


                // Check the content length not the text length since the tags will add more characters
                if (repostedMessageContentBuilder.length() + messageContentString.length() < repostedMessageContentBuilder.capacity()) {
                    if (!cursor.isFirst()) {
                        messageTextString += "\n\n";
                        messageContentString += "\n\n";
                    }
                    // Reverse the string so we can reverse the entire StringBuilder at the end (because we want the most recent messages at the end of the string)
                    // Do it this way so that if we go over the capacity we will lose the oldest messages rather than the newest
                    repostedMessageContentBuilder.append(new StringBuffer(messageContentString).reverse().toString());
                    repostedMessageTextBuilder.append(new StringBuffer(messageTextString).reverse().toString());
                } else {
                    break;
                }
            }
            cursor.close();
        }

        HashMap<String, String> ret = new HashMap(2);
        ret.put(REPOSTED_MESSAGE_TEXT_KEY, repostedMessageTextBuilder.reverse().toString());
        ret.put(REPOSTED_MESSAGE_CONTENT_KEY, repostedMessageContentBuilder.reverse().toString());

        return ret;
    }

    public static String buildRepostMessageTimestampText(Context context, long timestamp) {
        return DateUtils.formatDateTime(context, timestamp, DateUtils.FORMAT_SHOW_TIME) + " " + DateUtils.formatDateTime(context, timestamp, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR);
    }
}
