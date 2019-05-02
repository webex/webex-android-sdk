package com.ciscowebex.androidsdk.auth.internal;

import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.internal.ResultImpl;

public class TokenAuthenticator implements Authenticator {

    @Override
    public boolean isAuthorized() {
        return true;
    }

    @Override
    public void deauthorize() {

    }

    @Override
    public void getToken(CompletionHandler<String> handler) {
        handler.onComplete(ResultImpl.success("NDRiMGQzZjEtYWI0OS00MWU1LWI1NTItZTRlYjg4NWY2NjViM2ZlZDZjYmQtZDI3_PF84_1eb65fdf-9643-417f-9974-ad72cae0e10f"));
    }

    @Override
    public void refreshToken(CompletionHandler<String> handler) {
        handler.onComplete(ResultImpl.success("NDRiMGQzZjEtYWI0OS00MWU1LWI1NTItZTRlYjg4NWY2NjViM2ZlZDZjYmQtZDI3_PF84_1eb65fdf-9643-417f-9974-ad72cae0e10f"));
    }
}
