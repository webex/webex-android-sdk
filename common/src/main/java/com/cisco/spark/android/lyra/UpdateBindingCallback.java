package com.cisco.spark.android.lyra;

public interface UpdateBindingCallback {

    void onSuccess(BindingResponses bindingResponses);

    void onError();
}
