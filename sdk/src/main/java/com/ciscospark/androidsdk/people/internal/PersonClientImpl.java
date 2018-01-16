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

package com.ciscospark.androidsdk.people.internal;

import java.util.List;

import com.ciscospark.androidsdk.CompletionHandler;
import com.ciscospark.androidsdk.auth.Authenticator;
import com.ciscospark.androidsdk.people.Person;
import com.ciscospark.androidsdk.people.PersonClient;
import com.ciscospark.androidsdk.utils.http.ListBody;
import com.ciscospark.androidsdk.utils.http.ListCallback;
import com.ciscospark.androidsdk.utils.http.ObjectCallback;
import com.ciscospark.androidsdk.utils.http.ServiceBuilder;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class PersonClientImpl implements PersonClient {

    private Authenticator _authenticator;

    private PersonService _service;

    public PersonClientImpl(Authenticator authenticator) {
        _authenticator = authenticator;
        _service = new ServiceBuilder().build(PersonService.class);
    }

    public void list(String email, String displayName, int max, CompletionHandler<List<Person>> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.list(s, email, displayName, null, null, max <= 0 ? null : max).enqueue(new ListCallback<>(handler));
        });
    }

    public void get(String personId, CompletionHandler<Person> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.get(s, personId).enqueue(new ObjectCallback<>(handler));
        });
    }

    public void getMe(CompletionHandler<Person> handler) {
        ServiceBuilder.async(_authenticator, handler, s -> {
            _service.getMe(s).enqueue(new ObjectCallback<>(handler));
        });
    }

    private interface PersonService {
        @GET("people")
        Call<ListBody<Person>> list(@Header("Authorization") String authorizationHeader,
                                    @Query("email") String email,
                                    @Query("displayName") String displayName,
                                    @Query("id") String id,
                                    @Query("orgId") String orgId,
                                    @Query("max") Integer max);

        @GET("people/{personId}")
        Call<Person> get(@Header("Authorization") String authorizationHeader, @Path("personId") String personId);

        @GET("people/me")
        Call<Person> getMe(@Header("Authorization") String authorizationHeader);
    }
}
