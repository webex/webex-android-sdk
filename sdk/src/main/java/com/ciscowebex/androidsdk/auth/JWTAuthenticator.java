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

package com.ciscowebex.androidsdk.auth;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.WebexError;
import com.ciscowebex.androidsdk.internal.Closure;
import com.ciscowebex.androidsdk.internal.Settings;
import com.ciscowebex.androidsdk.internal.model.TokenModel;
import com.ciscowebex.androidsdk.internal.queue.Queue;
import com.ciscowebex.androidsdk.utils.Json;
import com.ciscowebex.androidsdk.internal.Service;
import com.ciscowebex.androidsdk.internal.model.JWTLoginModel;
import com.ciscowebex.androidsdk.internal.ResultImpl;
import com.github.benoitdion.ln.Ln;
import com.google.gson.reflect.TypeToken;

import me.helloworld.utils.Converter;

/**
 * A <a href="https://jwt.io/introduction">JSON Web Token</a> (JWT) based authentication strategy is to be used to authenticate a user on Cisco Webex.
 *
 * @since 0.1
 */
public class JWTAuthenticator implements Authenticator {

    private String jwt;

    private TokenModel tokenModel;

    /**
     * Creates a new JWT authentication strategy
     *
     * @since 0.1
     */
    public JWTAuthenticator() {
    }

    public void afterAssociated() {
        if (tokenModel != null) {
            Settings.shared.store(TokenModel.AUTHENTICATION_INFO, Json.get().toJson(tokenModel));
        }
    }

    /**
     * @see Authenticator
     */
    @Override
    public boolean isAuthorized() {
        return getUnexpiredJwt() != null;
    }

    /**
     * Sets the JWT access token on the authorization strategy, overriting any existing access token.
     *
     * @param jwt the new JSON Web Token to use
     * @since 0.1
     */
    public void authorize(@NonNull String jwt) {
        deauthorize();
        this.jwt = jwt;
    }

    /**
     * @see Authenticator
     */
    @Override
    public void deauthorize() {
        this.jwt = null;
        this.tokenModel = null;
        Settings.shared.clear();
    }

    /**
     * @see Authenticator
     */
    @Override
    public void getToken(@NonNull CompletionHandler<String> handler) {
        String jwt = getUnexpiredJwt();
        if (jwt == null) {
            ResultImpl.errorInMain(handler, WebexError.from("JWT is null"));
            return;
        }
        String token = getUnexpiredAccessToken();
        if (token != null) {
            handler.onComplete(ResultImpl.success(token));
            return;
        }
        refreshToken(handler);
    }

    @Override
    public void refreshToken(CompletionHandler<String> handler) {
        String jwt = getUnexpiredJwt();
        if (jwt == null) {
            ResultImpl.errorInMain(handler, WebexError.from("JWT is null"));
            return;
        }
        Service.Hydra.global().post().to("jwt/login").header("Authorization", jwt).header("Cache-Control", "no-cache")
                .queue(Queue.main).model(JWTLoginModel.class).error(handler)
                .async((Closure<JWTLoginModel>) model -> {
            tokenModel = model.toToken(jwt);
            Settings.shared.store(TokenModel.AUTHENTICATION_INFO, Json.get().toJson(tokenModel));
            handler.onComplete(ResultImpl.success(tokenModel.getAccessToken()));
        });
    }

    /**
     * Returns the expire date of the access token.
     *
     * @since 2.3.0
     */
    public Date getExpiration() {
        String jwt = getUnexpiredJwt();
        if (jwt == null) {
            return null;
        }
        if (tokenModel == null){
            tokenModel = Settings.shared.get(TokenModel.AUTHENTICATION_INFO, TokenModel.class, null);
        }
        if (tokenModel != null) {
            return new Date(tokenModel.getExpiresIn() * 1000);
        }
        return null;
    }

    private @Nullable String getUnexpiredJwt() {
        if (jwt == null && tokenModel == null) {
            tokenModel = Settings.shared.get(TokenModel.AUTHENTICATION_INFO, TokenModel.class, null);
        }
        if (jwt == null && tokenModel != null) {
            jwt = tokenModel.getRefreshToken();
        }
        if (jwt == null) {
            return null;
        }
        Map<String, Object> map = parseJWT(jwt);
        if (map == null) {
            return null;
        }
        try {
            long exp = Converter.toLong(map.get("exp"), -1);
            if (exp > 0 && exp <= (System.currentTimeMillis() / 1000)) {
                return null;
            }
        } catch (Exception e) {
            Ln.e(e);
        }
        return jwt;
    }

    private @Nullable String getUnexpiredAccessToken() {
        if (!isAuthorized()) {
            return null;
        }
        if (tokenModel == null) {
            tokenModel = Settings.shared.get(TokenModel.AUTHENTICATION_INFO, TokenModel.class, null);
        }
        if (tokenModel == null || tokenModel.getExpiresIn() <= (System.currentTimeMillis() / 1000) + (15 * 60)) {
            return null;
        }
        return tokenModel.getAccessToken();
    }

    private @Nullable Map<String, Object> parseJWT(String jwt) {
        String[] split = jwt.split("\\.");
        if (split.length != 3) {
            return null;
        }
        String json = new String(Base64.decode(split[1], Base64.URL_SAFE), StandardCharsets.UTF_8);
        return Json.fromJson(json, new TypeToken<HashMap<String, Object>>(){}.getType());
    }
}
