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

package com.ciscowebex.androidsdk.phone.internal;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.internal.*;
import com.ciscowebex.androidsdk.internal.model.*;
import com.ciscowebex.androidsdk.internal.queue.Queue;
import com.ciscowebex.androidsdk.people.Person;
import com.ciscowebex.androidsdk.people.internal.PersonClientImpl;
import com.ciscowebex.androidsdk.phone.CallMembership;
import com.ciscowebex.androidsdk.phone.MediaOption;
import com.ciscowebex.androidsdk.utils.EmailAddress;
import com.ciscowebex.androidsdk.utils.Json;
import com.ciscowebex.androidsdk.utils.Lists;
import com.ciscowebex.androidsdk.utils.WebexId;
import com.github.benoitdion.ln.Ln;
import me.helloworld.utils.Checker;
import me.helloworld.utils.collection.Maps;

import java.util.*;

public class CallService {

    interface DialTarget {
        static void lookup(@NonNull String address, Authenticator authenticator, Closure<DialTarget> callback) {
            WebexId id = WebexId.from(address);
            Ln.d("Call target: %s, %s", id, address);
            if (id != null && id.is(WebexId.Type.PEOPLE)) {
                callback.invoke(new CallableTarget(id.getUUID()));
            } else if (id != null && id.is(WebexId.Type.ROOM)) {
                callback.invoke(new JoinableTarget(id));
            } else if (EmailAddress.isValid(address)) {
                if (address.contains("@") && !address.contains(".")) {
                    new PersonClientImpl(authenticator).list(address, null, 1, result -> {
                        List<Person> data = result.getData();
                        WebexId person = Checker.isEmpty(data) ? null : WebexId.from(data.get(0).getId());
                        if (person == null) {
                            callback.invoke(new CallableTarget(address));
                        } else {
                            callback.invoke(new CallableTarget(person.getUUID()));
                        }
                    });
                } else {
                    callback.invoke(new CallableTarget(address));
                }
            } else {
                callback.invoke(new CallableTarget(address));
            }
        }
    }

    static class JoinableTarget implements DialTarget {
        private WebexId target;

        public JoinableTarget(WebexId target) {
            this.target = target;
        }

        public WebexId getConversation() {
            return target;
        }
    }

    static class CallableTarget implements DialTarget {
        private String target;

        public CallableTarget(String target) {
            this.target = target;
        }

        public String getCallee() {
            return target;
        }
    }

    private Authenticator authenticator;

    public CallService(@NonNull Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    public void getOrCreatePermanentLocus(WebexId conversation, Device device, CompletionHandler<String> callback) {
        ServiceReqeust.make(conversation.getUrl(device))
                .get("/locus")
                .auth(authenticator)
                .queue(Queue.main)
                .model(LocusUrlResponseModel.class)
                .error(callback)
                .async((Closure<LocusUrlResponseModel>) model -> {
                    if (model == null || model.getLocusUrl() == null) {
                        callback.onComplete(ResultImpl.error("No locus uri"));
                    } else {
                        callback.onComplete(ResultImpl.success(model.getLocusUrl()));
                    }
                });
    }

    public void call(@NonNull String address, boolean isModerator, @Nullable String PIN, @NonNull String correlationId, @NonNull Device device, @NonNull String sdp, @Nullable MediaOption.VideoLayout layout, MediaEngineReachabilityModel reachabilities, @NonNull CompletionHandler<LocusModel> callback) {
        Service.Locus.homed(device)
                .post(makeBody(correlationId, layout, device, sdp, address, isModerator, PIN, reachabilities))
                .to("loci/call")
                .auth(authenticator)
                .queue(Queue.main)
                .model(LocusMediaResponseModel.class)
                .error(callback)
                .async((Closure<LocusMediaResponseModel>) model -> {
                    LocusModel locus = model.getLocus();
                    if (locus != null && model.getMediaConnections() != null) {
                        locus.setMediaConnections(model.getMediaConnections());
                    }
                    if (layout == null) {
                        callback.onComplete(ResultImpl.success(locus));
                        return;
                    }
                    if (locus != null && locus.getSelf() != null) {
                        layout(locus.getSelf().getUrl(), device, layout, result -> callback.onComplete(ResultImpl.success(locus)));
                    }
                });
    }

    public void join(@NonNull String url, boolean isModerator, @Nullable String PIN, @NonNull String correlationId, @NonNull Device device, @NonNull String sdp, @Nullable MediaOption.VideoLayout layout, MediaEngineReachabilityModel reachabilities, @NonNull CompletionHandler<LocusModel> callback) {
        ServiceReqeust.make(url)
                .post(makeBody(correlationId, layout, device, sdp, null, isModerator, PIN, reachabilities))
                .to("participant")
                .auth(authenticator)
                .queue(Queue.main)
                .model(LocusMediaResponseModel.class)
                .error(callback)
                .async((Closure<LocusMediaResponseModel>) model -> {
                    LocusModel locus = model.getLocus();
                    if (locus != null && model.getMediaConnections() != null) {
                        locus.setMediaConnections(model.getMediaConnections());
                    }
                    if (layout == null) {
                        callback.onComplete(ResultImpl.success(locus));
                        return;
                    }
                    if (locus != null && locus.getSelf() != null) {
                        layout(locus.getSelf().getUrl(), device, layout, result -> callback.onComplete(ResultImpl.success(locus)));
                    }
                });
    }

    public void leave(@NonNull String participantUrl, @NonNull Device device, @NonNull CompletionHandler<LocusModel> callback) {
        ServiceReqeust.make(participantUrl)
                .put(makeBody(device.getDeviceUrl()))
                .to("leave")
                .auth(authenticator)
                .queue(Queue.main)
                .model(LocusMediaResponseModel.class)
                .error(callback)
                .async((Closure<LocusMediaResponseModel>) model -> {
                    LocusModel locus = model == null ? null : model.getLocus();
                    if (locus != null && model.getMediaConnections() != null) {
                        locus.setMediaConnections(model.getMediaConnections());
                    }
                    callback.onComplete(ResultImpl.success(locus));
                });
    }

    public void decline(@NonNull String url, @NonNull Device device, @NonNull CompletionHandler<Void> callback) {
        ServiceReqeust.make(url)
                .put(makeBody(device.getDeviceUrl()))
                .to("participant/decline")
                .auth(authenticator)
                .queue(Queue.main)
                .model(LocusMediaResponseModel.class)
                .error(callback)
                .async((Closure<LocusMediaResponseModel>) data -> callback.onComplete(ResultImpl.success(null)));
    }

    public void alert(@NonNull String url, @NonNull Device device, @NonNull CompletionHandler<Void> callback) {
        ServiceReqeust.make(url)
                .put(makeBody(device.getDeviceUrl()))
                .to("participant/alert")
                .auth(authenticator)
                .queue(Queue.main)
                .model(LocusMediaResponseModel.class)
                .error(callback)
                .async((Closure<LocusMediaResponseModel>) data -> callback.onComplete(ResultImpl.success(null)));
    }

    public void update(@NonNull String mediaUrl, @NonNull String mediaId, @NonNull Device device, @NonNull MediaInfoModel media, @NonNull CompletionHandler<LocusModel> callback) {
        Map<String, Object> json = new HashMap<>();
        MediaConnectionModel mc = new MediaConnectionModel();
        mc.setLocalSdp(Json.get().toJson(media));
        mc.setType(media.getType());
        mc.setMediaId(mediaId);
        json.put("localMedias", Lists.asList(mc));
        json.put("deviceUrl", device.getDeviceUrl());
        json.put("respOnlySdp", true);
        ServiceReqeust.make(mediaUrl)
                .put(json).apply()
                .auth(authenticator)
                .queue(Queue.main)
                .model(LocusMediaResponseModel.class)
                .error(callback)
                .async((Closure<LocusMediaResponseModel>) model -> {
                    LocusModel locus = model == null ? null : model.getLocus();
                    if (locus != null && model.getMediaConnections() != null) {
                        locus.setMediaConnections(model.getMediaConnections());
                    }
                    callback.onComplete(ResultImpl.success(locus));
                });
    }

    public void update(@NonNull MediaShareModel share, @NonNull String url, @NonNull Device device, @NonNull CompletionHandler<Void> callback) {
        Map<String, Object> floor = new HashMap<>();
        floor.put("disposition", share.getDisposition());
        floor.put("requester", Maps.makeMap("url", share.getRequesterUrl()));
        floor.put("beneficiary", Maps.makeMap("url", share.getBeneficiaryUrl(), "devices", Maps.makeMap("url", device.getDeviceUrl())));
        ServiceReqeust.make(url).put(Maps.makeMap("floor", floor)).apply()
                .auth(authenticator)
                .queue(Queue.main)
                .model(LocusMediaResponseModel.class)
                .error(callback)
                .async((Closure<LocusMediaResponseModel>) data -> callback.onComplete(ResultImpl.success(null)));
    }

    public void get(@NonNull String callUrl, @NonNull CompletionHandler<LocusModel> callback) {
        ServiceReqeust.make(callUrl)
                .get()
                .auth(authenticator)
                .queue(Queue.main)
                .model(LocusMediaResponseModel.class)
                .error(callback)
                .async((Closure<LocusMediaResponseModel>) model -> {
                    LocusModel locus = model == null ? null : model.getLocus();
                    if (locus != null && model.getMediaConnections() != null) {
                        locus.setMediaConnections(model.getMediaConnections());
                    }
                    callback.onComplete(ResultImpl.success(locus));
                });
    }

    public void list(@Nullable Device device, @NonNull CompletionHandler<List<LocusModel>> callback) {
        Service.Locus.homed(device).get("loci")
                .auth(authenticator)
                .queue(Queue.main)
                .model(LocusListResponseModel.class)
                .error(callback)
                .async((Closure<LocusListResponseModel>) model -> callback.onComplete(ResultImpl.success(model == null ? Collections.emptyList() : model.getLoci())));
    }

    public void admit(@NonNull String url, @NonNull List<CallMembership> memberships, @NonNull CompletionHandler<LocusModel> callback) {
        List<String> ids = new ArrayList<>(memberships.size());
        for (CallMembership membership : memberships) {
            ids.add(((CallMembershipImpl) membership).getId());
        }
        Map<Object, Object> params = Maps.makeMap("admit", Maps.makeMap("participantIds", ids));
        ServiceReqeust.make(url).patch(params).to("controls")
                .auth(authenticator)
                .queue(Queue.main)
                .model(LocusMediaResponseModel.class)
                .error(callback)
                .async((Closure<LocusMediaResponseModel>) model -> {
                    LocusModel locus = model == null ? null : model.getLocus();
                    if (locus != null && model.getMediaConnections() != null) {
                        locus.setMediaConnections(model.getMediaConnections());
                    }
                    callback.onComplete(ResultImpl.success(locus));
                });
    }

    public void layout(@NonNull String participantUrl, @NonNull Device device, @NonNull MediaOption.VideoLayout layout, @NonNull CompletionHandler<LocusModel> callback) {
        String type = "ActivePresence";
        if (layout == MediaOption.VideoLayout.SINGLE) {
            type = "Single";
        } else if (layout == MediaOption.VideoLayout.GRID) {
            type = "Equal";
        }
        Map<String, Object> params = Maps.makeMap("layout", Maps.makeMap("deviceUrl", device.getDeviceUrl(), "type", type));
        ServiceReqeust.make(participantUrl).patch(params).to("controls")
                .auth(authenticator)
                .queue(Queue.main)
                .model(LocusMediaResponseModel.class)
                .error(callback)
                .async((Closure<LocusMediaResponseModel>) model -> {
                    LocusModel locus = model == null ? null : model.getLocus();
                    if (locus != null && model.getMediaConnections() != null) {
                        locus.setMediaConnections(model.getMediaConnections());
                    }
                    callback.onComplete(ResultImpl.success(locus));
                });
    }

    public void keepalive(@NonNull String url, @NonNull CompletionHandler<Void> callback) {
        ServiceReqeust.make(url).get().auth(authenticator).queue(Queue.main).error(callback).async((Closure<Void>) data -> callback.onComplete(ResultImpl.success(null)));
    }

    public void dtmf(@NonNull String participantUrl, @NonNull Device device, int correlation, @NonNull String event, @Nullable Integer volume, @Nullable Integer durationMillis, @NonNull CompletionHandler<Void> callback) {
        Map<String, Object> dtmf = new HashMap<>();
        dtmf.put("tones", event);
        dtmf.put("correlationId", correlation);
        if (volume != null) {
            dtmf.put("volume", volume);
        }
        if (durationMillis != null) {
            dtmf.put("durationMillis", durationMillis);
        }
        Map<String, Object> json = new HashMap<>();
        json.put("deviceUrl", device.getDeviceUrl());
        json.put("respOnlySdp", true);
        json.put("dtmf", dtmf);
        ServiceReqeust.make(participantUrl).post(json).to("sendDtmf")
                .auth(authenticator)
                .queue(Queue.main)
                .model(LocusMediaResponseModel.class)
                .error(callback)
                .async((Closure<LocusMediaResponseModel>) data -> callback.onComplete(ResultImpl.success(null)));
    }

    private Object makeBody(String correlationId, MediaOption.VideoLayout layout, Device device, String sdp, String callee, boolean isModerator, String PIN, MediaEngineReachabilityModel reachabilities) {
        Map<String, Object> json = new HashMap<>();
        MediaConnectionModel mc = new MediaConnectionModel();
        mc.setLocalSdp(Json.get().toJson(new MediaInfoModel(sdp, reachabilities == null ? null : reachabilities.reachability)));
        mc.setType("SDP");
        json.put("localMedias", Lists.asList(mc));
        json.put("device", device.toJsonMap(layout == MediaOption.VideoLayout.SINGLE ? null : Device.Type.WEB_CLIENT.getTypeName()));
        json.put("respOnlySdp", true);
        json.put("correlationId", correlationId);
        if (callee != null) {
            json.put("invitee", Maps.makeMap("address", callee));
            json.put("supportsNativeLobby", true);
            json.put("moderator", false);
        }
        if (isModerator) {
            json.put("moderator", isModerator);
        }
        if (PIN != null) {
            json.put("pin", PIN);
        }
        return json;
    }

    private Object makeBody(String deviceUrl) {
        Map<String, Object> json = new HashMap<>();
        json.put("deviceUrl", deviceUrl);
        json.put("respOnlySdp", true);
        return json;
    }
}
