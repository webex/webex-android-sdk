/*
 * Copyright 2016-2017 Cisco Systems Inc
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

package com.ciscospark.androidsdk.auth;

import com.ciscospark.androidsdk.CompletionHandler;

/**
 * An interface for generic authentication strategies in Cisco Spark.
 * Each authentication strategy is responsible for providing an accessToken used throughout this SDK.
 *
 * @since 0.1
 */
public interface Authenticator {

    /**
     * Returns True if the user is logically authorized.
     * <p>
     * This may not mean the user has a valid access token yet,
     * but the authentication strategy should be able to obtain one without further user interaction.
     *
     * @return True if the user is logically authorized
     * @since 0.1
     */
    boolean isAuthorized();

    /**
     * Deauthorizes the current user and clears any persistent state with regards to the current user.
     * If the {@link com.ciscospark.androidsdk.phone.Phone} is registered,
     * it should be deregistered before calling this method.
     *
     * @since 0.1
     */
    void deauthorize();

    /**
     * Returns an access token of this authenticator.
     * <p>
     * This may involve long-running operations such as service calls, but may also return immediately.
     * The application should not make assumptions about how quickly this completes.
     *
     * @param handler a callback to be executed when completed, with the access token if successfuly retrieved, otherwise nil.
     * @since 0.1
     */
    void getToken(CompletionHandler<String> handler);

}
