package com.cisco.spark.android.lyra;

public interface BindingBackend {

    void bind(String roomIdentity, BindingRequest bindingRequest, BindingCallback callback);

    void unbind(String roomIdentity, String  bindingUrl, String kmsMessage, BindingCallback callback);

    void updateBindings(String roomIdentity, UpdateBindingCallback callback);
}
