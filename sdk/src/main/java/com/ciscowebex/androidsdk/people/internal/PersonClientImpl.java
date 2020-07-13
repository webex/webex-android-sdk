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

package com.ciscowebex.androidsdk.people.internal;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.internal.Closure;
import com.ciscowebex.androidsdk.internal.Service;
import com.ciscowebex.androidsdk.internal.ResultImpl;
import com.ciscowebex.androidsdk.internal.queue.Queue;
import com.ciscowebex.androidsdk.people.Person;
import com.ciscowebex.androidsdk.people.PersonClient;
import com.ciscowebex.androidsdk.internal.model.ItemsModel;

import com.google.gson.reflect.TypeToken;
import me.helloworld.utils.collection.Maps;
import org.jetbrains.annotations.NotNull;

public class PersonClientImpl implements PersonClient {

    private Authenticator authenticator;

    public PersonClientImpl(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    public void list(@NotNull String email, String displayName, int max, @NotNull CompletionHandler<List<Person>> handler) {
        this.list(email, displayName, null, null, max, handler);
    }

    public void list(String email, String displayName, String id, String orgId, int max, @NotNull CompletionHandler<List<Person>> handler) {
        Service.Hydra.global().get("people")
                .with("email", email)
                .with("displayName", displayName)
                .with("id", id)
                .with("orgId", orgId)
                .with("max", max <= 0 ? null : String.valueOf(max))
                .auth(authenticator)
                .queue(Queue.main)
                .model(new TypeToken<ItemsModel<Person>>(){}.getType())
                .error(handler)
                .async((Closure<ItemsModel<Person>>) result -> handler.onComplete(ResultImpl.success(result.getItems())));
    }

    public void get(@NotNull String personId, @NotNull CompletionHandler<Person> handler) {
        Service.Hydra.global().get("people/" + personId)
                .auth(authenticator)
                .queue(Queue.main)
                .model(Person.class)
                .error(handler)
                .async((Closure<Person>) result -> handler.onComplete(ResultImpl.success(result)));
    }

    public void getMe(@NotNull CompletionHandler<Person> handler) {
        Service.Hydra.global().get("people/me")
                .auth(authenticator)
                .queue(Queue.main)
                .model(Person.class)
                .error(handler)
                .async((Closure<Person>) result -> handler.onComplete(ResultImpl.success(result)));
    }

    @Override
    public void create(@NonNull String email, @Nullable String displayName, @Nullable String firstName, @Nullable String lastName, @Nullable String avatar, @Nullable String orgId, @Nullable String roles, @Nullable String licenses, @NonNull CompletionHandler<Person> handler) {
        Service.Hydra.global().post(Maps.makeMap("email", email, "displayName", displayName, "firstName", firstName, "lastName", lastName,
                "avatar", avatar, "orgId", orgId, "roles", roles, "licenses", licenses))
                .to("people")
                .auth(authenticator)
                .queue(Queue.main)
                .model(Person.class)
                .error(handler)
                .async((Closure<Person>) result -> handler.onComplete(ResultImpl.success(result)));
    }

    @Override
    public void update(@NonNull String personId, @Nullable String email, @Nullable String displayName, @Nullable String firstName, @Nullable String lastName, @Nullable String avatar, @Nullable String orgId, @Nullable String roles, @Nullable String licenses, @NonNull CompletionHandler<Person> handler) {
        Service.Hydra.global().put(Maps.makeMap("email", email, "displayName", displayName, "firstName", firstName, "lastName", lastName,
                "avatar", avatar, "orgId", orgId, "roles", roles, "licenses", licenses))
                .to("people/" + personId)
                .auth(authenticator)
                .queue(Queue.main)
                .model(Person.class)
                .error(handler)
                .async((Closure<Person>) result -> {
                    handler.onComplete(ResultImpl.success(result));
                });
    }

    @Override
    public void delete(@NonNull String personId, @NonNull CompletionHandler<Void> handler) {
        Service.Hydra.global().delete("people/" + personId)
                .auth(authenticator)
                .queue(Queue.main)
                .error(handler)
                .async((Closure<Void>) result -> handler.onComplete(ResultImpl.success(result)));
    }
}
