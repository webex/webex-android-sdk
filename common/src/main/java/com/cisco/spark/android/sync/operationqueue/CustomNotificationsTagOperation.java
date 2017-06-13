package com.cisco.spark.android.sync.operationqueue;

import android.content.ContentProviderOperation;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.ConversationTag;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry;
import com.cisco.spark.android.util.TriStateUtil;
import com.github.benoitdion.ln.Ln;

import java.util.List;

public class CustomNotificationsTagOperation extends TagOperation {

    public CustomNotificationsTagOperation(Injector injector, String conversationId, List<ConversationTag> conversationTagList, boolean tag, SyncOperationEntry.OperationType operationType) {
        super(injector, conversationId, conversationTagList, tag, operationType);
    }


    @NonNull
    @Override
    protected SyncOperationEntry.SyncState onEnqueue() {

        Bundle conversationValues = ConversationContentProviderQueries.getConversationById(getContentResolver(), getConversationId());

        if (conversationValues != null) {

            boolean newMutedValue = Integer.valueOf(conversationValues.getString(ConversationContract.ConversationEntry.MUTED.name())) == 1;
            Boolean newMessagesValue = null;
            Boolean newMentionsValue = null;

            boolean messagesDirty = false;
            boolean mentionsDirty = false;

            if (isTag()) {
                if (conversationTagList.contains(ConversationTag.MESSAGE_NOTIFICATIONS_OFF)) {
                    if (!conversationTagList.contains(ConversationTag.MUTED)) {
                        newMutedValue = true;
                    }
                    newMessagesValue = false;
                    messagesDirty = true;

                } else if (conversationTagList.contains(ConversationTag.MESSAGE_NOTIFICATIONS_ON)) {
                    newMutedValue = false;
                    newMessagesValue = true;
                    messagesDirty = true;
                }
                if (conversationTagList.contains(ConversationTag.MENTION_NOTIFICATIONS_OFF)) {
                    newMentionsValue = false;
                    mentionsDirty = true;
                } else if (conversationTagList.contains(ConversationTag.MENTION_NOTIFICATIONS_ON)) {
                    newMentionsValue = true;
                    mentionsDirty = true;
                }


            } else {

                if (conversationTagList.contains(ConversationTag.MESSAGE_NOTIFICATIONS_OFF) || conversationTagList.contains(ConversationTag.MESSAGE_NOTIFICATIONS_ON)) {
                    newMessagesValue = null;
                    messagesDirty = true;
                    newMutedValue = false;

                }
                if (conversationTagList.contains(ConversationTag.MENTION_NOTIFICATIONS_OFF) || conversationTagList.contains(ConversationTag.MENTION_NOTIFICATIONS_ON)) {
                    newMentionsValue = null;
                    mentionsDirty = true;
                }
            }

            // Convert tristate Boolean value to Integer representation used in database
            final Integer withNewMessageValue = TriStateUtil.integerValueOf(newMessagesValue);
            final Integer withNewMentionValue = TriStateUtil.integerValueOf(newMentionsValue);
            final Integer withNewMutedValue = newMutedValue ? 1 : 0;

            final ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(ConversationContract.ConversationEntry.CONTENT_URI)
                    .withValue(ConversationContract.ConversationEntry.MUTED.name(), withNewMutedValue)
                    .withSelection(ConversationContract.ConversationEntry.CONVERSATION_ID + "=?", new String[]{getConversationId()});

            if (messagesDirty) {
                builder.withValue(ConversationContract.ConversationEntry.MESSAGE_NOTIFICATIONS.name(), withNewMessageValue);
            }
            if (mentionsDirty) {
                builder.withValue(ConversationContract.ConversationEntry.MENTION_NOTIFICATIONS.name(), withNewMentionValue);
            }

            Ln.v("CustomNotificationTagOperation onEnqueue update db muted: %s (%s) messages: %s (%s) mentions: %s (%s) messagesDirty: %s mentionsDirty: %s",
                    withNewMutedValue,   (withNewMutedValue   == null ? "null" : withNewMutedValue),
                    withNewMessageValue, (withNewMessageValue == null ? "null" : withNewMessageValue),
                    withNewMentionValue, (withNewMentionValue == null ? "null" : withNewMentionValue),
                    messagesDirty,
                    mentionsDirty
            );
            Batch batch = newBatch();
            batch.add(builder.build());
            batch.apply();
        }
        return super.onEnqueue();
    }
}
