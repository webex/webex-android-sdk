package com.ciscospark.androidsdk.phone;

import android.app.AlertDialog;
import android.support.annotation.NonNull;
import android.view.View;
import com.ciscospark.androidsdk.CompletionHandler;

/**
 * Created by zhiyuliu on 04/09/2017.
 */
public interface Phone {

    enum FacingMode {
        USER, ENVIROMENT
    }

    interface IncomingCallListener {
        void onIncomingCall(Call call);
    }

    IncomingCallListener getIncomingCallListener();

    void setIncomingCallListener(IncomingCallListener listener);

    FacingMode getDefaultFacingMode();

    void setDefaultFacingMode(FacingMode mode);

    void register(@NonNull CompletionHandler<Void> callback);

    void deregister(@NonNull CompletionHandler<Void> callback);

    void dial(@NonNull String dialString, @NonNull CallOption option, @NonNull CompletionHandler<Call> callback);

    void startPreview(View view);

    void stopPreview();

    void requestVideoCodecActivation(@NonNull AlertDialog.Builder builder, @NonNull CompletionHandler<Boolean> callback);

    void disableVideoCodecActivation();
}
