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
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.internal.Closure;
import com.ciscowebex.androidsdk.internal.Service;
import com.ciscowebex.androidsdk.internal.ResultImpl;
import com.ciscowebex.androidsdk.internal.queue.Queue;
import com.ciscowebex.androidsdk.team.Team;
import com.ciscowebex.androidsdk.team.TeamClient;
import com.ciscowebex.androidsdk.internal.model.ItemsModel;
import com.google.gson.reflect.TypeToken;
import me.helloworld.utils.collection.Maps;

public class TeamClientImpl implements TeamClient {

    private Authenticator authenticator;

    public TeamClientImpl(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    public void list(int max, @NonNull CompletionHandler<List<Team>> handler) {
        Service.Hydra.get("teams").with("max", max <= 0 ? null : String.valueOf(max))
                .auth(authenticator)
                .queue(Queue.main)
                .model(new TypeToken<ItemsModel<Team>>(){}.getType())
                .error(handler)
                .async((Closure<ItemsModel<Team>>) result -> handler.onComplete(ResultImpl.success(result.getItems())));
    }

    public void get(@NonNull String teamId, @NonNull CompletionHandler<Team> handler) {
        Service.Hydra.get("teams", teamId)
                .auth(authenticator)
                .queue(Queue.main)
                .model(Team.class)
                .error(handler)
                .async((Closure<Team>) result -> handler.onComplete(ResultImpl.success(result)));
    }

    public void create(@NonNull String name, @NonNull CompletionHandler<Team> handler) {
        Service.Hydra.post(Maps.makeMap("name", name)).to("teams")
                .auth(authenticator)
                .queue(Queue.main)
                .model(Team.class)
                .error(handler)
                .async((Closure<Team>) result -> handler.onComplete(ResultImpl.success(result)));
    }

    public void update(@NonNull String teamId, String name, @NonNull CompletionHandler<Team> handler) {
        Service.Hydra.put(Maps.makeMap("name", name)).to("teams", teamId)
                .auth(authenticator)
                .queue(Queue.main)
                .model(Team.class)
                .error(handler)
                .async((Closure<Team>) result -> handler.onComplete(ResultImpl.success(result)));
    }

    public void delete(@NonNull String teamId, @NonNull CompletionHandler<Void> handler) {
        Service.Hydra.delete("teams", teamId)
                .auth(authenticator)
                .queue(Queue.main)
                .error(handler)
                .async((Closure<Void>) result -> handler.onComplete(ResultImpl.success(result)));
    }
}
