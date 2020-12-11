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

package com.ciscowebex.androidsdk.space.internal;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.Result;
import com.ciscowebex.androidsdk.internal.*;
import com.ciscowebex.androidsdk.internal.model.ConversationModel;
import com.ciscowebex.androidsdk.internal.model.LocusModel;
import com.ciscowebex.androidsdk.internal.queue.Queue;
import com.ciscowebex.androidsdk.phone.internal.PhoneImpl;
import com.ciscowebex.androidsdk.space.*;
import com.ciscowebex.androidsdk.internal.model.ItemsModel;

import com.ciscowebex.androidsdk.utils.WebexId;
import com.google.gson.reflect.TypeToken;
import me.helloworld.utils.Objects;
import me.helloworld.utils.collection.Maps;

public class SpaceClientImpl implements SpaceClient {

    private final PhoneImpl phone;

    public SpaceClientImpl(PhoneImpl phone) {
        this.phone = phone;
    }

    @Override
    public void setSpaceObserver(SpaceObserver observer) {
        this.phone.setSpaceObserver(observer);
    }

    @Override
    public void list(@Nullable String teamId, int max, @Nullable Space.SpaceType type, @Nullable SortBy sortBy, @NonNull CompletionHandler<List<Space>> handler) {
        Service.Hydra.global().get("rooms")
                .with("teamId", teamId)
                .with("type", type == null ? null : type.serializedName())
                .with("sortBy", sortBy == null ? null : sortBy.serializedName())
                .with("max", max <= 0 ? null : String.valueOf(max))
                .auth(phone.getAuthenticator())
                .queue(Queue.main)
                .model(new TypeToken<ItemsModel<Space>>(){}.getType())
                .error(handler)
                .async((Closure<ItemsModel<Space>>) result -> handler.onComplete(ResultImpl.success(result.getItems())));
    }

    @Override
    public void get(@NonNull String spaceId, @NonNull CompletionHandler<Space> handler) {
        Service.Hydra.global().get("rooms/" + spaceId)
                .auth(phone.getAuthenticator())
                .queue(Queue.main)
                .model(Space.class)
                .error(handler)
                .async((Closure<Space>) result -> handler.onComplete(ResultImpl.success(result)));
    }

    @Override
    public void create(@NonNull String title, @Nullable String teamId, @NonNull CompletionHandler<Space> handler) {
        Service.Hydra.global().post(Maps.makeMap("title", title, "teamId", teamId)).to("rooms")
                .auth(phone.getAuthenticator())
                .queue(Queue.main)
                .model(Space.class)
                .error(handler)
                .async((Closure<Space>) result -> handler.onComplete(ResultImpl.success(result)));
    }

    @Override
    public void update(@NonNull String spaceId, @NonNull String title, @NonNull CompletionHandler<Space> handler) {
       Service.Hydra.global().put(Maps.makeMap("title", title)).to("rooms/" + spaceId)
               .auth(phone.getAuthenticator())
               .queue(Queue.main)
               .model(Space.class)
               .error(handler)
               .async((Closure<Space>) result -> handler.onComplete(ResultImpl.success(result)));
    }

    @Override
    public void delete(@NonNull String spaceId, @NonNull CompletionHandler<Void> handler) {
        Service.Hydra.global().delete("rooms/" + spaceId)
                .auth(phone.getAuthenticator())
                .queue(Queue.main)
                .error(handler)
                .async((Closure<Void>) result -> handler.onComplete(ResultImpl.success(result)));
    }

    @Override
    public void getMeetingInfo(@NonNull String spaceId, @NonNull CompletionHandler<SpaceMeetingInfo> handler) {
        Service.Hydra.global().get("rooms/" + spaceId + "/meetingInfo")
                .auth(phone.getAuthenticator())
                .queue(Queue.main)
                .model(SpaceMeetingInfo.class)
                .error(handler)
                .async((Closure<SpaceMeetingInfo>) result -> handler.onComplete(ResultImpl.success(result)));
    }

    @Override
    public void getWithReadStatus(@NonNull String spaceId, @NonNull CompletionHandler<SpaceReadStatus> handler) {
        WebexId conversation = WebexId.from(spaceId);
        if (conversation == null) {
            handler.onComplete(ResultImpl.error("Cannot found the space: " + spaceId));
            return;
        }
        ServiceReqeust.make(conversation.getUrl(phone.getDevice())).get()
                .with("uuidEntryFormat", "true")
                .with("personRefresh", "true")
                .with("includeParticipants", "false")
                .with("activitiesLimit", "0")
                .with("includeConvWithDeletedUserUUID", "false")
                .auth(phone.getAuthenticator())
                .queue(Queue.main)
                .model(ConversationModel.class)
                .error(handler)
                .async((Closure<ConversationModel>) model -> {
                    if (model == null) {
                        handler.onComplete(ResultImpl.success(null));
                        return;
                    }
                    handler.onComplete(ResultImpl.success(new InternalSpaceReadStatus(model, conversation.getClusterId())));
                });
    }

    @Override
    public void listWithReadStatus(int max, @NonNull CompletionHandler<List<SpaceReadStatus>> handler) {
        // TODO additionalUrls
        Service.Conv.homed(phone.getDevice()).get("conversations")
                .with("uuidEntryFormat", "true")
                .with("personRefresh", "true")
                .with("participantsLimit", "0")
                .with("activitiesLimit", "0")
                .with("includeConvWithDeletedUserUUID", "false")
                .with("conversationsLimit", String.valueOf(max))
                .auth(phone.getAuthenticator())
                .queue(Queue.main)
                .model(new TypeToken<ItemsModel<ConversationModel>>(){}.getType())
                .error(handler)
                .async((Closure<ItemsModel<ConversationModel>>) model -> {
                    if (model == null || model.size() == 0) {
                        handler.onComplete(ResultImpl.success(Collections.emptyList()));
                        return;
                    }
                    List<SpaceReadStatus> result = new ArrayList<>();
                    for (ConversationModel conversation : model.getItems()) {
                        result.add(new InternalSpaceReadStatus(conversation, phone.getDevice().getClusterId(conversation.getUrl())));
                    }
                    handler.onComplete(ResultImpl.success(result));
                });
    }

    @Override
    public void listWithActiveCalls(@NonNull CompletionHandler<List<String>> handler) {
        this.phone.getService().list(phone.getDevice(), result -> {
            if (result.isSuccessful()) {
                List<LocusModel> models = Objects.defaultIfNull(result.getData(), Collections.emptyList());
                List<String> spaces = new ArrayList<>();
                for (LocusModel model : models) {
                    String convUrl = model.getConversationUrl();
                    if (!model.isOneOnOne() && convUrl != null) {
                        WebexId space = WebexId.from(convUrl, phone.getDevice());
                        spaces.add(space.getBase64Id());
                    }
                }
                ResultImpl.inMain(handler, spaces);
            }
            else {
                ResultImpl.errorInMain(handler, result);
            }
        });
    }
}
