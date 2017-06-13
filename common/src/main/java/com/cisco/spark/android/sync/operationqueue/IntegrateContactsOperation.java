package com.cisco.spark.android.sync.operationqueue;

import android.annotation.TargetApi;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.authenticator.AuthenticatedUserProvider;
import com.cisco.spark.android.contacts.ContactsContractManager;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.core.Injector;
import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.operationqueue.core.Operation;
import com.cisco.spark.android.sync.operationqueue.core.RetryPolicy;
import com.cisco.spark.android.util.NameUtils;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.util.ArrayList;

import javax.inject.Inject;

import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState.READY;
import static com.cisco.spark.android.sync.ConversationContract.SyncOperationEntry.SyncState.SUCCEEDED;

public class IntegrateContactsOperation extends Operation {

    private final boolean enabled;

    @Inject
    transient Context context;

    @Inject
    transient ContactsContractManager contactsContractManager;

    @Inject
    transient AuthenticatedUserProvider authenticatedUserProvider;

    @Inject
    transient ApiClientProvider apiClientProvider;

    @Inject
    transient ApiTokenProvider apiTokenProvider;

    public IntegrateContactsOperation(Injector injector, boolean enabled) {
        super(injector);
        this.enabled = enabled;
    }

    @NonNull
    @Override
    public ConversationContract.SyncOperationEntry.OperationType getOperationType() {
        return ConversationContract.SyncOperationEntry.OperationType.INTEGRATE_CONTACTS;
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState onEnqueue() {
        return READY;
    }

    @NonNull
    @Override
    protected ConversationContract.SyncOperationEntry.SyncState doWork() throws IOException {
        if (enabled)
            return enable();
        return disable();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private ConversationContract.SyncOperationEntry.SyncState enable() {
        AuthenticatedUser self = authenticatedUserProvider.getAuthenticatedUser();
        String selfDomain = NameUtils.getDomainFromEmail(self.getEmail());
        if (self.isConsumer())
            selfDomain = null;

        ArrayList<String> emailsToLookUp = new ArrayList<>();

        Cursor contactsCursor = null;
        Cursor actorCursor = null;
        try {
            // First make sure any existing spark raw contacts have their RAW_CONTACT_ID populated in the app
            contactsContractManager.resolveExistingSparkContacts();

            // ContactsCursor, for iterating over ContactsContract.Data.DATA1 values looking for
            // interesting email addresses, excluding ones added by the Spark app
            contactsCursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                    new String[]{ContactsContract.Data.DATA1},
                    ContactsContract.Data.ACCOUNT_TYPE_AND_DATA_SET + " != ?",
                    new String[]{ContactsContractManager.ACCOUNT_TYPE},
                    ContactsContract.Data.DATA1 + " ASC");

            if (contactsCursor == null)
                return null;

            // ActorCursor, for iterating over ActorEntry rows who do not have a corresponding RawContact
            actorCursor = context.getContentResolver().query(ConversationContract.ActorEntry.CONTENT_URI,
                    new String[]{ConversationContract.ActorEntry.EMAIL.toString(), ConversationContract.ActorEntry.ACTOR_UUID.toString(), ConversationContract.ActorEntry.DISPLAY_NAME.toString()},
                    ConversationContract.ActorEntry.RAW_CONTACT_ID + " IS NULL AND (" + ConversationContract.ActorEntry.TYPE + "=? OR " + ConversationContract.ActorEntry.TYPE + " IS NULL)", new String[]{Person.PERSON},
                    ConversationContract.ActorEntry.EMAIL.toString() + " ASC");

            if (actorCursor == null)
                return null;

            contactsCursor.moveToFirst();
            actorCursor.moveToFirst();

            // Iterate through both cursors in parallel looking for matches
            String actorEmail, contactEmail;
            int compare;
            Batch actorsBatch = newBatch();
            while (!contactsCursor.isAfterLast() && !actorCursor.isAfterLast()) {
                actorEmail = actorCursor.getString(0);
                if (TextUtils.isEmpty(actorEmail) || TextUtils.equals(actorEmail, self.getEmail())) {
                    actorCursor.moveToNext();
                    continue;
                }

                contactEmail = contactsCursor.getString(0);
                if (TextUtils.isEmpty(contactEmail) || TextUtils.equals(contactEmail, self.getEmail())) {
                    contactsCursor.moveToNext();
                    continue;
                }

                compare = contactEmail.compareTo(actorEmail);

                if (compare == 0) {
                    contactsContractManager.addRawContact(actorCursor.getString(1), actorsBatch);
                    contactsCursor.moveToNext();
                    actorCursor.moveToNext();
                } else if (compare < 0) {
                    if (TextUtils.equals(selfDomain, NameUtils.getDomainFromEmail(contactEmail))) {
                        // We share an org, add to rawcontact candidates
                        emailsToLookUp.add(contactEmail);
                    }
                    contactsCursor.moveToNext();
                } else {
                    actorCursor.moveToNext();
                }
            }
            actorsBatch.apply();

            addNotDeletedUsers(emailsToLookUp);
        } catch (Exception e) {
            Ln.w(e, "Failed syncing contacts");
        } finally {
            if (contactsCursor != null)
                contactsCursor.close();
            if (actorCursor != null)
                actorCursor.close();
        }

        return SUCCEEDED;
    }

    private void addNotDeletedUsers(ArrayList<String> emails) {
        for (String email : emails) {
            //TODO decide if this is enough or if we should query CI to see if they're
            // spark users, or if they're former (terminated) spark users
            contactsContractManager.addRawContact(email);
        }
    }

    @Override
    public boolean isOperationRedundant(Operation newOperation) {
        if (newOperation.getOperationType() == getOperationType())
            cancel();
        return false;
    }

    private ConversationContract.SyncOperationEntry.SyncState disable() {
        contactsContractManager.removeAccount();
        Batch batch = newBatch();
        batch.add(ContentProviderOperation.newUpdate(ConversationContract.ActorEntry.CONTENT_URI)
                .withValue(ConversationContract.ActorEntry.RAW_CONTACT_ID.name(), null)
                .build()
        );
        batch.apply();
        return SUCCEEDED;
    }

    @Override
    protected void onNewOperationEnqueued(Operation newOperation) {
    }

    @Override
    protected ConversationContract.SyncOperationEntry.SyncState checkProgress() {
        return getState();
    }

    @NonNull
    @Override
    public RetryPolicy buildRetryPolicy() {
        return RetryPolicy.newLimitAttemptsPolicy();
    }
}
