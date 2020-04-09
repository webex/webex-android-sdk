/*
 * Copyright 2016-2020 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscowebex.androidsdk.auth.internal;

import android.support.annotation.NonNull;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.internal.model.TokenModel;
import com.ciscowebex.androidsdk.internal.ResultImpl;

public class TokenAuthenticator implements Authenticator {

    private TokenModel tokenModel;

    public TokenAuthenticator() {
    }

    public void authorize(@NonNull String token, int expire) {
        deauthorize();
        tokenModel = new TokenModel(token, expire + (System.currentTimeMillis() / 1000), token);
    }

    @Override
    public boolean isAuthorized() {
        return tokenModel != null
                && tokenModel.getAccessToken() != null
                && System.currentTimeMillis() < (tokenModel.getExpiresIn() * 1000);
    }

    @Override
    public void deauthorize() {
        tokenModel = null;
    }

    @Override
    public void getToken(CompletionHandler<String> handler) {
        handler.onComplete(ResultImpl.success(tokenModel.getAccessToken()));
    }

    @Override
    public void refreshToken(CompletionHandler<String> handler) {
        handler.onComplete(ResultImpl.success(tokenModel.getRefreshToken()));
    }
}
