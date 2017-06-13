package com.cisco.spark.android.sync;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Context;

import java.util.ArrayList;

abstract public class Batch extends ArrayList<ContentProviderOperation> {

    protected String authority;
    protected Context context;
    protected ContentProviderResult[] results;

    public Batch(Context context) {
        this(context, ConversationContract.CONTENT_AUTHORITY);
    }
    public Batch(Context context, String authority) {
        this.context = context;
        this.authority = authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public ContentProviderResult[] getResults() {
        return results;
    }

    public abstract boolean apply();

    @Override
    public void clear() {
        super.clear();
        results = null;
    }
}
