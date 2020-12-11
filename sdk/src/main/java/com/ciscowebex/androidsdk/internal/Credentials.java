/*
 * Copyright 2016-2021 Cisco Systems Inc
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

package com.ciscowebex.androidsdk.internal;

import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.people.Person;
import com.ciscowebex.androidsdk.people.internal.PersonClientImpl;
import com.ciscowebex.androidsdk.utils.WebexId;

public class Credentials {

    public static void auth(Authenticator authenticator, CompletionHandler<Credentials> callback) {
        authenticator.getToken(tokenResult -> {
            String token = tokenResult.getData();
            if (token == null) {
                callback.onComplete(ResultImpl.error(tokenResult.getError()));
            }
            else {
                new PersonClientImpl(authenticator).getMe(personResult -> {
                    Person person = personResult.getData();
                    if (person == null) {
                        callback.onComplete(ResultImpl.error(personResult.getError()));
                    }
                    else {
                        callback.onComplete(ResultImpl.success(new Credentials(authenticator, person, token)));
                    }
                });
            }
        });
    }

    private Authenticator authenticator;
    private String userId;
    private String orgId;
    private Person person;
    private String token;

    public Credentials(Authenticator authenticator, Person person, String token) {
        this.authenticator =authenticator;
        this.userId = WebexId.uuid(person.getId());
        this.orgId = WebexId.uuid(person.getOrgId());
        this.person = person;
        this.token = token;
    }

    public Authenticator getAuthenticator() {
        return authenticator;
    }

    public String getUserId() {
        return userId;
    }

    public String getOrgId() {
        return orgId;
    }

    public Person getPerson() {
        return person;
    }

    public String getToken() {
        return token;
    }
}
