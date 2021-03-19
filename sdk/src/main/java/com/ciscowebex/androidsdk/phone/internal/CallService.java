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

package com.ciscowebex.androidsdk.phone.internal;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.Result;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.internal.Closure;
import com.ciscowebex.androidsdk.internal.Device;
import com.ciscowebex.androidsdk.internal.ResultImpl;
import com.ciscowebex.androidsdk.internal.Service;
import com.ciscowebex.androidsdk.internal.ServiceReqeust;
import com.ciscowebex.androidsdk.internal.model.LocusListResponseModel;
import com.ciscowebex.androidsdk.internal.model.LocusMediaResponseModel;
import com.ciscowebex.androidsdk.internal.model.LocusModel;
import com.ciscowebex.androidsdk.internal.model.LocusUrlResponseModel;
import com.ciscowebex.androidsdk.internal.model.MediaConnectionModel;
import com.ciscowebex.androidsdk.internal.model.MediaEngineReachabilityModel;
import com.ciscowebex.androidsdk.internal.model.MediaInfoModel;
import com.ciscowebex.androidsdk.internal.model.MediaShareModel;
import com.ciscowebex.androidsdk.internal.queue.Queue;
import com.ciscowebex.androidsdk.people.Person;
import com.ciscowebex.androidsdk.people.internal.PersonClientImpl;
import com.ciscowebex.androidsdk.phone.CallMembership;
import com.ciscowebex.androidsdk.phone.MediaOption;
import com.ciscowebex.androidsdk.phone.Phone;
import com.ciscowebex.androidsdk.utils.EmailAddress;
import com.ciscowebex.androidsdk.utils.Json;
import com.ciscowebex.androidsdk.utils.Lists;
import com.ciscowebex.androidsdk.utils.WebexId;
import com.github.benoitdion.ln.Ln;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.helloworld.utils.Checker;
import me.helloworld.utils.collection.Maps;

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

    public void call(@NonNull String address, @NonNull MediaOption option, @NonNull Phone.VideoStreamMode streamMode, @NonNull String correlationId, @NonNull Device device, @NonNull String sdp, MediaEngineReachabilityModel reachabilities, @NonNull CompletionHandler<LocusModel> callback) {
        Service.Locus.homed(device)
                .post(makeBody(correlationId, device, sdp, address, option, streamMode, reachabilities))
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
                    if (option.getCompositedLayout() == null) {
                        callback.onComplete(ResultImpl.success(locus));
                        return;
                    }
                    if (locus != null && locus.getSelf() != null) {
                        layout(locus.getSelf().getUrl(), device, option.getCompositedLayout(), result -> callback.onComplete(ResultImpl.success(locus)));
                    }
                });
    }

    public void join(@NonNull String url, @NonNull MediaOption option, @NonNull Phone.VideoStreamMode streamMode, @NonNull String correlationId, @NonNull Device device, @NonNull String sdp, MediaEngineReachabilityModel reachabilities, @NonNull CompletionHandler<LocusModel> callback) {
        ServiceReqeust.make(url)
                .post(makeBody(correlationId, device, sdp, null, option, streamMode, reachabilities))
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
                    if (option.getCompositedLayout() == null) {
                        callback.onComplete(ResultImpl.success(locus));
                        return;
                    }
                    if (locus != null && locus.getSelf() != null) {
                        layout(locus.getSelf().getUrl(), device, option.getCompositedLayout(), result -> callback.onComplete(ResultImpl.success(locus)));
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
        listLoci(device, null, new CompletionHandler<LocusListResponseModel>() {
            private List<LocusModel> loci = new ArrayList<>();

            @Override
            public void onComplete(Result<LocusListResponseModel> result) {
                if (result.isSuccessful()) {
                    LocusListResponseModel model = result.getData();
                    if (model == null || model.getLoci() == null) {
                        callback.onComplete(ResultImpl.success(loci));
                    } else {
                        loci.addAll(model.getLoci());
                        List<String> remoteLocusClusterUrls = model.getRemoteLocusClusterUrls();
                        if (remoteLocusClusterUrls != null && !remoteLocusClusterUrls.isEmpty()) {
                            String url = remoteLocusClusterUrls.get(0);
                            listLoci(null, url, this);
                        } else {
                            callback.onComplete(ResultImpl.success(loci));
                        }
                    }
                } else {
                    callback.onComplete(ResultImpl.error(result));
                }
            }
        });
    }

    private void listLoci(@Nullable Device device, @Nullable String url, @NonNull CompletionHandler<LocusListResponseModel> callback) {
        ServiceReqeust serviceReqeust;
        if (url != null) {
            serviceReqeust = new ServiceReqeust(url).get();
        } else {
            serviceReqeust = Service.Locus.homed(device).get("loci");
        }
        serviceReqeust.auth(authenticator)
                .queue(Queue.main)
                .model(LocusListResponseModel.class)
                .error(callback)
                .async((Closure<LocusListResponseModel>) model -> callback.onComplete(ResultImpl.success(model)));
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

    public void layout(@NonNull String participantUrl, @NonNull Device device, @NonNull MediaOption.CompositedVideoLayout layout, @NonNull CompletionHandler<LocusModel> callback) {
        String type = "ActivePresence";
        if (layout == MediaOption.CompositedVideoLayout.SINGLE) {
            type = "Single";
        } else if (layout == MediaOption.CompositedVideoLayout.GRID) {
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

    private Object makeBody(String correlationId, Device device, String sdp, String callee, MediaOption option, Phone.VideoStreamMode streamMode, MediaEngineReachabilityModel reachabilities) {
        Map<String, Object> json = new HashMap<>();
        MediaConnectionModel mc = new MediaConnectionModel();
        mc.setLocalSdp(Json.get().toJson(new MediaInfoModel(sdp, reachabilities == null ? null : reachabilities.reachability)));
        mc.setType("SDP");
        json.put("localMedias", Lists.asList(mc));
//        json.put("device", device.toJsonMap(Device.Type.WEB_CLIENT.getTypeName()));
        json.put("device", device.toJsonMap(null));
        if (streamMode == Phone.VideoStreamMode.COMPOSITED) {
            json.put("clientMediaPreferences", Maps.makeMap("preferTranscoding", true));
        }
        json.put("respOnlySdp", true);
        json.put("correlationId", correlationId);
        json.put("moderator", option.isModerator());
        if (option.getPin() != null) {
            json.put("pin", option.getPin());
        }
        if (callee != null) {
            json.put("invitee", Maps.makeMap("address", callee));
            json.put("supportsNativeLobby", true);
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
