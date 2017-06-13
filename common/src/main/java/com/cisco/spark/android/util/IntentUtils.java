package com.cisco.spark.android.util;

import android.net.Uri;
import android.text.TextUtils;

public class IntentUtils {
    private static final String SPARK_SCHEME = "spark";

    public static Uri getSparkConversationUri(String conversationId) {
        if (TextUtils.isEmpty(conversationId))
            return null;

        return new Uri.Builder().scheme(SPARK_SCHEME).authority("rooms").appendQueryParameter("id", conversationId).build();
    }
}
