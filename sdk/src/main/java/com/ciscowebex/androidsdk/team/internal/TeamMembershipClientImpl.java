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

package com.ciscowebex.androidsdk.team.internal;

import java.util.List;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.internal.Closure;
import com.ciscowebex.androidsdk.internal.Service;
import com.ciscowebex.androidsdk.internal.ResultImpl;
import com.ciscowebex.androidsdk.internal.queue.Queue;
import com.ciscowebex.androidsdk.team.TeamMembership;
import com.ciscowebex.androidsdk.team.TeamMembershipClient;
import com.ciscowebex.androidsdk.internal.model.ItemsModel;

import com.google.gson.reflect.TypeToken;
import me.helloworld.utils.collection.Maps;

public class TeamMembershipClientImpl implements TeamMembershipClient {

    private Authenticator authenticator;

    public TeamMembershipClientImpl(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    public void list(@Nullable String teamId, int max, @NonNull CompletionHandler<List<TeamMembership>> handler) {
        Service.Hydra.global().get("team/memberships")
                .with("teamId", teamId)
                .with("max", max <= 0 ? null : String.valueOf(max))
                .auth(authenticator)
                .queue(Queue.main)
                .model(new TypeToken<ItemsModel<TeamMembership>>(){}.getType())
                .error(handler)
                .async((Closure<ItemsModel<TeamMembership>>) result -> handler.onComplete(ResultImpl.success(result.getItems())));
    }

    public void get(@NonNull String membershipId, @NonNull CompletionHandler<TeamMembership> handler) {
        Service.Hydra.global().get("team/memberships/" + membershipId)
                .auth(authenticator)
                .queue(Queue.main)
                .model(TeamMembership.class)
                .error(handler)
                .async((Closure<TeamMembership>) result -> handler.onComplete(ResultImpl.success(result)));
    }

    public void create(@NonNull String teamId, @Nullable String personId, @Nullable String personEmail, boolean isModerator, @NonNull CompletionHandler<TeamMembership> handler) {
        Service.Hydra.global().post(Maps.makeMap("teamId", teamId, "personId", personId, "personEmail", personEmail, "isModerator", isModerator))
                .to("team/memberships")
                .auth(authenticator)
                .queue(Queue.main)
                .model(TeamMembership.class)
                .error(handler)
                .async((Closure<TeamMembership>) result -> handler.onComplete(ResultImpl.success(result)));
    }

    public void update(@NonNull String membershipId, boolean isModerator, @NonNull CompletionHandler<TeamMembership> handler) {
        Service.Hydra.global().put(Maps.makeMap("isModerator", isModerator))
                .to("team/memberships/" + membershipId)
                .auth(authenticator)
                .queue(Queue.main)
                .model(TeamMembership.class)
                .error(handler)
                .async((Closure<TeamMembership>) result -> handler.onComplete(ResultImpl.success(result)));
    }

    public void delete(@NonNull String membershipId, @NonNull CompletionHandler<Void> handler) {
        Service.Hydra.global().delete("team/memberships/" + membershipId)
                .auth(authenticator)
                .queue(Queue.main)
                .error(handler)
                .async((Closure<Void>) result -> handler.onComplete(ResultImpl.success(result)));
    }
}
