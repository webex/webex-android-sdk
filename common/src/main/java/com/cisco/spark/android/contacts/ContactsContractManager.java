package com.cisco.spark.android.contacts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.text.TextUtils;

import com.cisco.spark.android.BuildConfig;
import com.cisco.spark.android.R;
import com.cisco.spark.android.authenticator.AuthenticatedUserProvider;
import com.cisco.spark.android.authenticator.LogoutEvent;
import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.core.AvatarProvider;
import com.cisco.spark.android.core.PermissionsHelper;
import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.sync.ActorRecord;
import com.cisco.spark.android.sync.ActorRecordProvider;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ContentDataCacheRecord;
import com.cisco.spark.android.sync.ContentManager;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.util.Action;
import com.cisco.spark.android.util.FileUtils;
import com.cisco.spark.android.util.LoggingLock;
import com.cisco.spark.android.util.NameUtils;
import com.cisco.spark.android.util.Strings;
import com.github.benoitdion.ln.Ln;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

import javax.inject.Provider;

import de.greenrobot.event.EventBus;

public class ContactsContractManager {
    public static final String ACCOUNT_TYPE = "com.ciscospark";
    private final Context context;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final Provider<Batch> batchProvider;
    private final ActorRecordProvider actorRecordProvider;
    private final ContentResolver contentResolver;
    private final AvatarProvider avatarProvider;
    private final ContentManager contentManager;
    private final LoggingLock lock = new LoggingLock(BuildConfig.DEBUG, "ContactsContractManager");

    public static class SparkDataKind {
        public static final String EMAIL = ContactsContract.Data.DATA1;
        public static final String ITEM_SUMMARY = ContactsContract.Data.DATA2;
        public static final String ITEM_DETAIL = ContactsContract.Data.DATA3;
        public static final String USER_UUID = ContactsContract.Data.DATA4;

        private static String contentType;

        public static String getContentType(Context context) {
            if (contentType == null)
                contentType = context.getString(R.string.spark_contact_mimetype);
            return contentType;
        }
    }

    public ContactsContractManager(Context context, AuthenticatedUserProvider authenticatedUserProvider, Provider<Batch> batchProvider, ActorRecordProvider actorRecordProvider, ContentResolver contentResolver, AvatarProvider avatarProvider, ContentManager contentManager, EventBus bus) {
        this.context = context;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.batchProvider = batchProvider;
        this.actorRecordProvider = actorRecordProvider;
        this.contentResolver = contentResolver;
        this.avatarProvider = avatarProvider;
        this.contentManager = contentManager;
        bus.register(this);
    }

    @SuppressLint("MissingPermission")  // lint is dum
    public Account getAccount() {
        if (!(new PermissionsHelper(context)).hasGetAccountsPermission()) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null;
        }

        AccountManager am = AccountManager.get(context);
        Account[] accounts = am.getAccountsByType(ACCOUNT_TYPE);
        for (Account account : accounts) {
            return account;
        }
        return null;
    }

    public void ensureAccountExists() {
        if (getAccount() != null)
            return;

        lock.lock();
        try {
            if (getAccount() != null)
                return;

            AuthenticatedUser user = authenticatedUserProvider.getAuthenticatedUser();

            if (user != null) {
                AccountManager am = AccountManager.get(context);
                am.addAccountExplicitly(new Account(user.getEmail(), ACCOUNT_TYPE), null, null);

                try {
                    // Ungrouped contacts are hidden by default.
                    // This might not be needed, on the S6 & M new contacts are automatically added to a
                    // default group associated with this account.
                    ContentValues values = new ContentValues();
                    values.put(ContactsContract.RawContacts.ACCOUNT_NAME, user.getEmail());
                    values.put(ContactsContract.RawContacts.ACCOUNT_TYPE, ACCOUNT_TYPE);
                    values.put(ContactsContract.Settings.UNGROUPED_VISIBLE, 1);

                    context.getContentResolver().insert(ContactsContract.Settings.CONTENT_URI, values);
                    context.getContentResolver().setSyncAutomatically(getAccount(), ContactsContract.AUTHORITY, true);
                } catch (Exception e) {

                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void removeAccount() {
        lock.lock();
        try {
            Account account = getAccount();
            if (account != null) {
                AccountManager am = AccountManager.get(context);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    am.removeAccountExplicitly(account);
                } else {
                    am.removeAccount(account, null, null);
                }
            }
            Batch batch = batchProvider.get();
            clearRawContactIdsFromActorEntries(batch);
            batch.apply();
        } catch (Exception e) {
            Ln.w(e, "Failed removing AccountManager account");
        } finally {
            lock.unlock();
        }
    }

    /**
     * For every raw contact in the Contacts DB created by us, make sure the corresponding
     * ActorEntry's RAW_CONTACT_ID is populated
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void resolveExistingSparkContacts() {
        ensureAccountExists();
        Cursor contactsCursor = null;
        lock.lock();
        try {
            Batch batch = batchProvider.get();
            clearRawContactIdsFromActorEntries(batch);

            contactsCursor = context.getContentResolver().query(ContactsContract.RawContactsEntity.CONTENT_URI,
                    new String[]{ContactsContract.RawContactsEntity._ID, ContactsContract.Data.DATA1},
                    ContactsContract.RawContactsEntity.ACCOUNT_TYPE_AND_DATA_SET + " == ?",
                    new String[]{ContactsContractManager.ACCOUNT_TYPE},
                    null);

            while (contactsCursor != null && contactsCursor.moveToNext()) {
                ContentProviderOperation op = ContentProviderOperation.newUpdate(ConversationContract.ActorEntry.CONTENT_URI)
                        .withValue(ConversationContract.ActorEntry.RAW_CONTACT_ID.name(), contactsCursor.getLong(0))
                        .withSelection(ConversationContract.ActorEntry.EMAIL + "=? AND (" + ConversationContract.ActorEntry.TYPE + "=? OR " + ConversationContract.ActorEntry.TYPE + " IS NULL)", new String[]{contactsCursor.getString(1), Person.PERSON})
                        .build();
                batch.add(op);
            }
            batch.apply();
        } catch (Exception e) {
            Ln.e(e, "Failed resolving existing Spark contacts");
        } finally {
            lock.unlock();
            if (contactsCursor != null)
                contactsCursor.close();
        }
    }

    private void clearRawContactIdsFromActorEntries(Batch batch) {
        batch.add(ContentProviderOperation.newUpdate(ConversationContract.ActorEntry.CONTENT_URI)
                .withValue(ConversationContract.ActorEntry.RAW_CONTACT_ID.name(), null)
                .build());
    }

    public Uri addRawContact(String emailOrUuid) {
        Batch batch = batchProvider.get();
        Uri ret = addRawContact(emailOrUuid, batch);
        batch.apply();
        return ret;
    }

    /**
     * Write a RawContact record to the Contacts DB. See https://developer.android.com/guide/topics/providers/contacts-provider.html
     *
     * @param emailOrUuid      An actors UUID or email. If an email that does not exist in the
     *                         ActorEntry table is provided, it's assumed that they are not spark
     *                         users and the user will get an "Invite to Spark" entry rather than
     *                         the usual "Spark" one.
     * @param actorUpdateBatch Batch for updating the ActorEntry table with the new RawContact Id
     */
    public Uri addRawContact(String emailOrUuid, Batch actorUpdateBatch) {

        if (getAccount() == null)
            return null;

        ActorRecord actorRecord = actorRecordProvider.get(emailOrUuid);
        boolean isSparkUser = true;

        if (actorRecord == null && Strings.isEmailAddress(emailOrUuid)) {
            actorRecord = new ActorRecord(new ActorRecord.ActorKey(emailOrUuid),
                    emailOrUuid, emailOrUuid, Person.PERSON, false, null, false);
            isSparkUser = false;
        }

        if (actorRecord == null) {
            Ln.w("Failed adding raw contact");
            return null;
        }

        AuthenticatedUser self = authenticatedUserProvider.getAuthenticatedUser();
        if (TextUtils.equals(self.getEmail(), emailOrUuid) || TextUtils.equals(self.getUserId(), emailOrUuid)) {
            return null;
        }

        Ln.d("Adding raw contact : " + actorRecord);

        lock.lock();
        try {

            Uri existingRawContactUri = getRawContact(emailOrUuid);
            if (existingRawContactUri != null) {
                Ln.d("Skipping raw contact creation, they already exist in contacts db");
                return existingRawContactUri;
            }

            Batch contactsBatch = batchProvider.get();
            contactsBatch.setAuthority(ContactsContract.AUTHORITY);

            ContentValues values = new ContentValues();
            values.put(ContactsContract.RawContacts.ACCOUNT_TYPE, ContactsContractManager.ACCOUNT_TYPE);
            values.put(ContactsContract.RawContacts.ACCOUNT_NAME, self.getEmail());

            // add the raw contact
            contactsBatch.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, ContactsContractManager.ACCOUNT_TYPE)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, self.getEmail())
                    .withValue(ContactsContract.RawContacts.SOURCE_ID, actorRecord.getUuidOrEmail())
                    .build());

            // display name
            contactsBatch.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, NameUtils.getShortName(actorRecord.getDisplayName()))
                    .build());

            // spark id
            String labelFormat = context.getString(isSparkUser ? R.string.contact_view_space : R.string.contact_invite_to_spark);
            contactsBatch.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, SparkDataKind.getContentType(context))
                    .withValue(SparkDataKind.EMAIL, actorRecord.getEmail())
                    .withValue(SparkDataKind.ITEM_SUMMARY, context.getString(R.string.spark))
                    .withValue(SparkDataKind.ITEM_DETAIL, String.format(Locale.US, labelFormat, NameUtils.getFirstName(actorRecord.getDisplayName())))
                    .withValue(SparkDataKind.USER_UUID, actorRecord.getUuidOrEmail())
                    .build());

            // email address
            contactsBatch.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                    .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, actorRecord.getEmail())
                    .build());

            contactsBatch.apply();

            // Write the new rawcontact id to the ActorEntry table
            ContentProviderResult[] results = contactsBatch.getResults();

            if (results != null && results.length > 0) {
                long newRawContactId = ContentUris.parseId(results[0].uri);
                ContentProviderOperation op = ContentProviderOperation.newUpdate(ConversationContract.ActorEntry.CONTENT_URI)
                        .withValue(ConversationContract.ActorEntry.RAW_CONTACT_ID.name(), newRawContactId)
                        .withSelection(ConversationContract.ActorEntry.ACTOR_UUID + "=?", new String[]{actorRecord.getUuidOrEmail()})
                        .build();
                actorUpdateBatch.add(op);

                return results[0].uri;
            }
        } finally {
            lock.unlock();
        }
        return null;
    }

    public Uri getRawContact(String emailOrUuid) {
        Cursor c = null;
        try {
            c = contentResolver.query(ContactsContract.Data.CONTENT_URI,
                    null,
                    ContactsContract.Data.MIMETYPE + "=? AND (" + SparkDataKind.EMAIL + "=? OR " + SparkDataKind.USER_UUID + "=?)",
                    new String[]{SparkDataKind.getContentType(context), emailOrUuid, emailOrUuid},
                    null);

            if (c != null && c.moveToNext()) {
                long rawContactId = c.getLong(c.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID));
                return ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawContactId);
            }
        } catch (Exception e) {
            Ln.w(e, "Failed getting raw contact");
        } finally {
            if (c != null)
                c.close();
        }
        return null;
    }

    public void writePhotoFromFile(Uri rawContactUri, File file) {
        if (file != null && file.isFile()) {
            Uri rawContactPhotoUri = Uri.withAppendedPath(
                    rawContactUri,
                    ContactsContract.RawContacts.DisplayPhoto.CONTENT_DIRECTORY);
            AssetFileDescriptor fd;
            try {
                fd = contentResolver.openAssetFileDescriptor(rawContactPhotoUri, "w");
                OutputStream os = fd.createOutputStream();
                FileUtils.streamCopy(new FileInputStream(file), os);
                fd.close();
            } catch (FileNotFoundException e) {
                Ln.w(e, "Caught exception");
            } catch (IOException e) {
                Ln.w(e, "Caught exception");
            }
        }
    }

    public boolean hasPhoto(Uri rawContactUri) {
        long rawContactId = ContentUris.parseId(rawContactUri);

        Cursor c = null;
        try {
            c = contentResolver.query(ContactsContract.Data.CONTENT_URI,
                    new String[]{
                            ContactsContract.Data.MIMETYPE
                    },
                    ContactsContract.Data.RAW_CONTACT_ID + "=?",
                    new String[]{String.valueOf(rawContactId)},
                    null);

            while (c != null && c.moveToNext()) {
                String mimeType = c.getString(0);
                if (ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE.equals(mimeType))
                    return true;
            }
        } finally {
            if (c != null)
                c.close();
        }
        return false;
    }

    public void updatePhoto(String actorUuid, Uri rawContactUri) {
        Uri avatarUri = avatarProvider.getUri(actorUuid, AvatarProvider.AvatarSize.BIG);
        contentManager.getCacheRecord(ConversationContract.ContentDataCacheEntry.Cache.AVATAR,
                avatarUri, null, "avatar.png", new GetPhotoAction(rawContactUri));
    }

    private class GetPhotoAction extends Action<ContentDataCacheRecord> {

        private final Uri rawContactUri;

        public GetPhotoAction(Uri rawContactUri) {
            this.rawContactUri = rawContactUri;
        }

        @Override
        public void call(ContentDataCacheRecord item) {
            if (item != null)
                writePhotoFromFile(rawContactUri, item.getLocalUriAsFile());
        }
    }

    public void onEventBackgroundThread(LogoutEvent event) {
        removeAccount();
    }
}
