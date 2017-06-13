package com.cisco.spark.android.contacts;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

public class ConversationSyncAdapter extends AbstractThreadedSyncAdapter {

    public ConversationSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    public ConversationSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {

    }
}
