package com.cisco.spark.android.contacts;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import com.cisco.spark.android.core.RootModule;
import com.github.benoitdion.ln.Ln;

import javax.inject.Inject;

public class ViewContactNotifyService extends IntentService {

    @Inject
    ContactsContractManager contactsContractManager;

    @Inject
    ContentResolver contentResolver;

    public ViewContactNotifyService() {
        super(ViewContactNotifyService.class.getSimpleName());
        RootModule.getInjector().inject(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Cursor c = null;
        try {
            Uri rawContactUri = intent.getData();
            Ln.i("Contact viewed : " + rawContactUri);

            c = contentResolver.query(rawContactUri, null, null, null, null);
            c.moveToFirst();
            String actorUuid = c.getString(c.getColumnIndex(ContactsContract.RawContacts.SOURCE_ID));

            if (!contactsContractManager.hasPhoto(rawContactUri)) {
                contactsContractManager.updatePhoto(actorUuid, rawContactUri);
            }

            // TODO write presence? Social stream?
        } catch (Exception e) {
            Ln.w("Failed handling intent " + intent);
        } finally {
            if (c != null)
                c.close();
        }
    }

}
