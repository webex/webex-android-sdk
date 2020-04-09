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

package com.ciscowebex.androidsdk.membership.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.internal.*;
import com.ciscowebex.androidsdk.internal.queue.Queue;
import com.ciscowebex.androidsdk.internal.model.ActivityModel;
import com.ciscowebex.androidsdk.internal.model.ConversationModel;
import com.ciscowebex.androidsdk.internal.model.PersonModel;
import com.ciscowebex.androidsdk.membership.Membership;
import com.ciscowebex.androidsdk.membership.MembershipClient;
import com.ciscowebex.androidsdk.membership.MembershipObserver;
import com.ciscowebex.androidsdk.membership.MembershipReadStatus;
import com.ciscowebex.androidsdk.phone.internal.PhoneImpl;
import com.ciscowebex.androidsdk.utils.WebexId;
import com.ciscowebex.androidsdk.internal.model.ItemsModel;

import com.google.gson.reflect.TypeToken;
import me.helloworld.utils.collection.Maps;

public class MembershipClientImpl implements MembershipClient, ActivityListener {

    private PhoneImpl phone;
    private MembershipObserver observer;

    public MembershipClientImpl(PhoneImpl phone) {
        this.phone = phone;
    }

    @Override
    public void setMembershipObserver(MembershipObserver observer) {
        this.observer = observer;
    }

    public void processActivity(@NonNull ActivityModel activity) {
        if (observer == null) {
            return;
        }
        final MembershipObserver.MembershipEvent event;
        if (activity.isAddParticipant()) {
            event = new InternalMembership.InternalMembershipCreated(new InternalMembership(activity), activity);
        }
        else if (activity.isLeaveActivity()) {
            event = new InternalMembership.InternalMembershipDeleted(new InternalMembership(activity), activity);
        }
        else if (activity.getVerb() == ActivityModel.Verb.assignModerator || activity.getVerb() == ActivityModel.Verb.unassignModerator) {
            event = new InternalMembership.InternalMembershipUpdated(new InternalMembership(activity), activity);
        }
        else if (activity.getVerb() == ActivityModel.Verb.acknowledge) {
            event = new InternalMembership.InternalMembershipMessageSeen(new InternalMembership(activity), activity, new WebexId(WebexId.Type.MESSAGE_ID, activity.getObject().getId()).toHydraId());
        }
        else {
            event = null;
        }
        if (event != null) {
            Queue.main.run(() -> observer.onEvent(event));
        }
    }

    @Override
    public void list(@Nullable String spaceId, @Nullable String personId, @Nullable String personEmail, int max, @NonNull CompletionHandler<List<Membership>> handler) {
        Service.Hydra.get("memberships")
                .with("roomId", spaceId)
                .with("spaceId", spaceId)
                .with("personId", personId)
                .with("personEmail", personEmail)
                .with("max", max <= 0 ? null : String.valueOf(max))
                .auth(phone.getAuthenticator())
                .queue(Queue.main)
                .model(new TypeToken<ItemsModel<Membership>>(){}.getType())
                .error(handler)
                .async((Closure<ItemsModel<Membership>>) result -> handler.onComplete(ResultImpl.success(result.getItems())));
    }

    @Override
    public void get(@NonNull String membershipId, @NonNull CompletionHandler<Membership> handler) {
        Service.Hydra.get("memberships", membershipId)
                .auth(phone.getAuthenticator())
                .queue(Queue.main)
                .model(Membership.class)
                .error(handler)
                .async((Closure<Membership>) result -> handler.onComplete(ResultImpl.success(result)));
    }

    @Override
    public void create(@NonNull String spaceId, @Nullable String personId, @Nullable String personEmail, boolean isModerator, @NonNull CompletionHandler<Membership> handler) {
        Service.Hydra.post(Maps.makeMap("roomId", spaceId, "spaceId", spaceId, "personId", personId, "personEmail", personEmail, "isModerator", isModerator))
                .to("memberships")
                .auth(phone.getAuthenticator())
                .queue(Queue.main)
                .model(Membership.class)
                .error(handler)
                .async((Closure<Membership>) result -> handler.onComplete(ResultImpl.success(result)));
    }

    @Override
    public void update(@NonNull String membershipId, boolean isModerator, @NonNull CompletionHandler<Membership> handler) {
        Service.Hydra.put(Maps.makeMap("isModerator", isModerator))
                .to("memberships", membershipId)
                .auth(phone.getAuthenticator())
                .queue(Queue.main)
                .model(Membership.class)
                .error(handler)
                .async((Closure<Membership>) result -> handler.onComplete(ResultImpl.success(result)));
    }

    @Override
    public void delete(@NonNull String membershipId, @NonNull CompletionHandler<Void> handler) {
        Service.Hydra.delete("memberships", membershipId)
                .auth(phone.getAuthenticator())
                .queue(Queue.main)
                .error(handler)
                .async((Closure<Void>) result -> handler.onComplete(ResultImpl.success(result)));
    }

    @Override
    public void listWithReadStatus(@NonNull String spaceId, @NonNull CompletionHandler<List<MembershipReadStatus>> handler) {
        Service.Conv.get("conversations", WebexId.translate(spaceId))
                .with("uuidEntryFormat", "true")
                .with("personRefresh", "true")
                .with("includeParticipants", "true")
                .with("participantAckFilter", "all")
                .with("activitiesLimit", "0")
                .auth(phone.getAuthenticator())
                .device(phone.getDevice())
                .queue(Queue.main)
                .model(ConversationModel.class)
                .error(handler)
                .async((Closure<ConversationModel>) model -> {
                    if (model == null) {
                        handler.onComplete(ResultImpl.success(Collections.emptyList()));
                        return;
                    }
                    List<MembershipReadStatus> result = new ArrayList<>();
                    for (PersonModel person : model.getParticipants().getItems()) {
                        try {
                            result.add(new InternalMembershipReadStatus(model, person));
                        } catch (Throwable ignored) {
                        }
                    }
                    handler.onComplete(ResultImpl.success(result));
                });
    }
}
