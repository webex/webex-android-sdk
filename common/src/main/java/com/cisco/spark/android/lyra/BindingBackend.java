package com.cisco.spark.android.lyra;

import android.net.Uri;

public interface BindingBackend {

    void bind(Uri conversationUrl, String roomIdentity, String conversationId);

    void unbind(String roomIdentity, String  bindingUrl, String kmsMessage, BindingCallback callback);

    void updateBindings(String roomIdentity, UpdateBindingCallback callback);
}
