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

package com.ciscowebex.androidsdk.internal.crypto;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.cisco.wx2.sdk.kms.KmsRequest;
import com.cisco.wx2.sdk.kms.KmsResponseBody;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.internal.*;
import com.ciscowebex.androidsdk.internal.model.*;
import com.ciscowebex.androidsdk.internal.queue.BackgroundQueue;
import com.ciscowebex.androidsdk.internal.queue.Queue;
import com.ciscowebex.androidsdk.utils.Lists;
import com.ciscowebex.androidsdk.utils.UriUtils;
import com.github.benoitdion.ln.Ln;
import com.nimbusds.jose.jwk.JWK;
import me.helloworld.utils.Checker;
import me.helloworld.utils.collection.Maps;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class KeyManager {

    public static KeyManager shared = new KeyManager();

    private final Queue queue = new BackgroundQueue();
    private final Map<String, KmsRequestWrapper<KeyObject>> requests = new ConcurrentHashMap<>();

    private KmsInfoModel kmsInfo;
    private KeyObject ephemeralKey;
    private final Map<String, KeyObject> keys = new ConcurrentHashMap<>();
    private final Map<String, String> convKeyUrls = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userIds = new ConcurrentHashMap<>();

    public void processKmsMessage(@NonNull KmsMessageModel model) {
        queue.run(() -> {
            JWK decryptKmsMessageKey = (ephemeralKey == null ? CryptoUtils.convertStringToJWK(kmsInfo.getRsaPublicKey()) : ephemeralKey.getKeyValueAsJWK());
            List<KmsResponseBody> decryptedKmsMessages = CryptoUtils.decryptKmsMessages(model, decryptKmsMessageKey);
            for (KmsResponseBody decryptedKmsMessage : decryptedKmsMessages) {
                KmsRequestWrapper<KeyObject> kmsRequest = requests.remove(decryptedKmsMessage.getRequestId());
                if (kmsRequest == null) {
                    Ln.d("ERROR unexpected KMS message: " + decryptedKmsMessage);
                    continue;
                }
                Ln.d("Response for Request ID " + decryptedKmsMessage.getRequestId() + " type:" + kmsRequest.getType() + " response " + decryptedKmsMessage.toString());
                if (kmsRequest.getType() == KmsRequestWrapper.RequestType.CREATE_EPHEMERAL_KEY) {
                    ephemeralKey = CryptoUtils.generateEphemeralKey(model, kmsInfo.getRsaPublicKey(), kmsRequest.getKmsRequest());
                    kmsRequest.getCallback().onComplete(ephemeralKey == null ? ResultImpl.error("Get the illegal ephemeral key") : ResultImpl.success(ephemeralKey));
                    return;
                } else if (kmsRequest.getType() == KmsRequestWrapper.RequestType.GET_KEYS) {
                    if (decryptedKmsMessage.getKey() != null) {
                        KeyObject key = CryptoUtils.convertKmsKeytoKeyObject(decryptedKmsMessage.getKey());
                        if (key != null) {
                            this.keys.put(key.getKeyUrl(), key);
                        }
                        kmsRequest.getCallback().onComplete(key == null ? ResultImpl.error("Get the illegal key") : ResultImpl.success(key));
                        return;
                    } else if (!Checker.isEmpty(decryptedKmsMessage.getKeys()) && kmsRequest.getConvUrl() != null) {
                        KeyObject key = CryptoUtils.convertKmsKeytoKeyObject(decryptedKmsMessage.getKeys().get(0));
                        if (key == null) {
                            kmsRequest.getCallback().onComplete(ResultImpl.error("Get the illegal key"));
                            return;
                        }
                        this.keys.put(key.getKeyUrl(), key);
                        Set<String> userIds = this.userIds.remove(kmsRequest.getConvUrl());
                        Device device = kmsRequest.getDevice();
                        Credentials credentials = kmsRequest.getCredentials();
                        getEphemeralKey(credentials, device, ephemeral -> {
                            if (ephemeral.getError() != null) {
                                kmsRequest.getCallback().onComplete(ResultImpl.error(ephemeral));
                                return;
                            }
                            String requestId = UUID.randomUUID().toString();
                            KmsRequest kr = CryptoUtils.createResourceRequest(device.getDeviceUrl(), credentials, requestId, new ArrayList<>(userIds), Lists.asList(UriUtils.toURI(key.getKeyUrl())));
                            if (kr == null) {
                                kmsRequest.getCallback().onComplete(ResultImpl.error("Failed generating key request message"));
                                return;
                            }
                            String encryptedBlob = kr.asEncryptedBlob(this.ephemeralKey.getKeyValueAsJWK());
                            if (Checker.isEmpty(encryptedBlob)) {
                                kmsRequest.getCallback().onComplete(ResultImpl.error("Generate Key Request is empty"));
                                return;
                            }
                            Map<String, Object> params = new HashMap<>();
                            params.put("objectType", ObjectModel.Type.activity);
                            params.put("verb", ActivityModel.Verb.updateKey.name());
                            params.put("object", Maps.makeMap("objectType", ObjectModel.Type.conversation, "defaultActivityEncryptionKeyUrl", key.getKeyUrl()));
                            params.put("target", Maps.makeMap("objectType", ObjectModel.Type.conversation, "id", kmsRequest.getConvId()));
                            params.put("kmsMessage", encryptedBlob);
                            ServiceReqeust.make(kmsRequest.getConvUrl())
                                    .post(params)
                                    .to("activities")
                                    .auth(credentials.getAuthenticator())
                                    .queue(queue)
                                    .model(ActivityModel.class)
                                    .error(kmsRequest.getCallback())
                                    .async((Closure<ActivityModel>) result -> {
                                        if (kmsRequest.getConvUrl() != null) {
                                            convKeyUrls.put(kmsRequest.getConvUrl(), key.getKeyUrl());
                                        }
                                        kmsRequest.getCallback().onComplete(ResultImpl.success(key));
                                    });
                        });
                    }
                }
            }
        });
    }

    public void tryRefresh(String convUrl, String encryptionUrl) {
        if (encryptionUrl != null && !Checker.isEqual(this.convKeyUrls.get(convUrl), encryptionUrl)) {
            convKeyUrls.put(convUrl, encryptionUrl);
        }
    }

    public void getConvEncryptionKey(String convUrl, String convId, Credentials credentials, Device device, CompletionHandler<KeyObject> callback) {
        queue.run(() -> getConvEncryptionKeyUrl(convUrl, credentials.getAuthenticator(), device, result -> {
            if (result.getError() != null) {
                callback.onComplete(ResultImpl.error(result.getError()));
                return;
            }
            getConvEncryptionKey(convUrl, convId, result.getData(), credentials, device, callback);
        }));
    }

    private void getConvEncryptionKeyUrl(String convUrl, Authenticator authenticator, Device device, CompletionHandler<String> callback) {
        queue.run(() -> {
            String keyUrl = convKeyUrls.get(convUrl);
            if (keyUrl != null) {
                callback.onComplete(ResultImpl.success(keyUrl));
                return;
            }
            ServiceReqeust.make(convUrl).get()
                    .with("includeActivities", String.valueOf(false))
                    .with("includeParticipants", String.valueOf(false))
                    .auth(authenticator)
                    .queue(queue)
                    .model(ConversationModel.class)
                    .error(callback)
                    .async((Closure<ConversationModel>) model -> {
                        if (model == null) {
                            callback.onComplete(ResultImpl.error("No result"));
                            return;
                        }
                        String encryptionKeyUrl = model.getTitleEncryptionKeyUrl();
                        if (encryptionKeyUrl == null) {
                            encryptionKeyUrl = model.getDefaultActivityEncryptionKeyUrl();
                        }
//                        if (encryptionKeyUrl == null) {
//                            if (model.getKmsResourceObject() == null) {
//                                callback.onComplete(ResultImpl.error("No result"));
//                                return;
//                            }
                            ItemsModel<PersonModel> items = model.getParticipants();
                            List<PersonModel> participants = items == null ? null : items.getItems();
                            Set<String> participantUuids = participants == null ? null : new HashSet<>(participants.size());
                            if (participantUuids != null) {
                                for (PersonModel person : participants) {
                                    participantUuids.add(person.getUuid());
                                }
                            }
                            userIds.put(convUrl, participantUuids);
//                        }
                        if (encryptionKeyUrl != null) {
                            convKeyUrls.put(convUrl, encryptionKeyUrl);
                        }
                        callback.onComplete(ResultImpl.success(encryptionKeyUrl));
                    });
        });
    }

    private void getConvEncryptionKey(@NonNull String convUrl, String convId, @Nullable String keyUrl, @NonNull Credentials credentials, @NonNull Device device, @NonNull CompletionHandler<KeyObject> callback) {
        queue.run(() -> {
            KeyObject key = keyUrl == null ? null : keys.get(keyUrl);
            if (key != null) {
                callback.onComplete(ResultImpl.success(key));
                return;
            }
            getEphemeralKey(credentials, device, ephemeral -> {
                if (ephemeral.getError() != null) {
                    callback.onComplete(ephemeral);
                    return;
                }
                String requestId = UUID.randomUUID().toString();
                KmsRequest kmsRequest;
                if (keyUrl == null) {
                    kmsRequest = CryptoUtils.generateUnboundKeyRequest(device.getDeviceUrl(), credentials, requestId, 1);
                } else {
                    kmsRequest = CryptoUtils.generateBoundKeyRequest(device.getDeviceUrl(), credentials, requestId, keyUrl);
                }
                if (kmsRequest == null) {
                    Ln.d("Failed generating key request message");
                    callback.onComplete(ResultImpl.error("Failed generating key request message"));
                    return;
                }
                if (ephemeral.getData() == null) {
                    Ln.d("No ephemeral key");
                    callback.onComplete(ResultImpl.error("No ephemeral key"));
                    return;
                }
                String encryptedBlob = kmsRequest.asEncryptedBlob(ephemeral.getData().getKeyValueAsJWK());
                if (Checker.isEmpty(encryptedBlob)) {
                    Ln.d("Generate Key Request is empty");
                    callback.onComplete(ResultImpl.error("Generate Key Request is null"));
                    return;
                }
                KmsRequestWrapper<KeyObject> wrapper = new KmsRequestWrapper<>(KmsRequestWrapper.RequestType.GET_KEYS, kmsRequest, callback);
                if (keyUrl == null) {
                    wrapper.setDevice(device);
                    wrapper.setCredentials(credentials);
                    wrapper.setConvUrl(convUrl);
                    wrapper.setConvId(convId);
                }
                requests.put(requestId, wrapper);
                Service.Kms.homed(device)
                        .post(Maps.makeMap("kmsMessages", Lists.asList(encryptedBlob), "destination", kmsInfo.getKmsCluster().toString()))
                        .to("kms/messages")
                        .header("Cisco-Request-ID", requestId)
                        .auth(credentials.getAuthenticator()).queue(queue)
                        .error((CompletionHandler<KeyObject>) result -> {
                            requests.remove(requestId);
                            callback.onComplete(result);
                        })
                        .async(null);
            });
        });
    }

    private void getEphemeralKey(Credentials credentials, Device device, CompletionHandler<KeyObject> callback) {
        queue.run(() -> {
            if (ephemeralKey != null) {
                callback.onComplete(ResultImpl.success(ephemeralKey));
                return;
            }
            getClusterAndRSAPubKey(credentials.getAuthenticator(), device, info -> {
                if (info.getError() != null) {
                    callback.onComplete(ResultImpl.error(info.getError()));
                    return;
                }
                if (device == null || device.getDeviceUrl() == null) {
                    Ln.d("No Device Info");
                    callback.onComplete(ResultImpl.error("No Device Info"));
                    return;
                }
                if (kmsInfo == null) {
                    Ln.d("Failed getting KMS Info");
                    callback.onComplete(ResultImpl.error("Failed getting KMS Info"));
                    return;
                }
                String requestId = UUID.randomUUID().toString();
                KmsRequest kmsRequest = CryptoUtils.generateEphemeralKeyRequest(device.getDeviceUrl(), credentials, kmsInfo.getKmsCluster(), requestId);
                if (kmsRequest == null) {
                    Ln.d("Generate Ephemeral Key Request is null");
                    callback.onComplete(ResultImpl.error("Generate Ephemeral Key Request is null"));
                    return;
                }
                String encryptedRequest = kmsRequest.asEncryptedBlob(CryptoUtils.convertStringToJWK(kmsInfo.getRsaPublicKey()));
                if (Checker.isEmpty(encryptedRequest)) {
                    Ln.d("Generate Ephemeral Key Request is empty");
                    callback.onComplete(ResultImpl.error("Generate Ephemeral Key Request is null"));
                    return;
                }
                requests.put(requestId, new KmsRequestWrapper<>(KmsRequestWrapper.RequestType.CREATE_EPHEMERAL_KEY, kmsRequest, callback));

                Service.Kms.homed(device)
                        .post(Maps.makeMap("kmsMessages", encryptedRequest, "destination", kmsInfo.getKmsCluster().toString()))
                        .to("kms/messages")
                        .header("Cisco-Request-ID", requestId)
                        .auth(credentials.getAuthenticator())
                        .queue(queue)
                        .error((CompletionHandler<KeyObject>) result -> {
                            requests.remove(requestId);
                            callback.onComplete(result);
                        })
                        .async(null);
            });
        });
    }

    private void getClusterAndRSAPubKey(Authenticator authenticator, Device device, CompletionHandler<KmsInfoModel> callback) {
        if (kmsInfo != null) {
            callback.onComplete(ResultImpl.success(kmsInfo));
            return;
        }
        Service.Kms.homed(device).get("kms").auth(authenticator).queue(queue).model(KmsInfoModel.class).error(callback)
                .async((Closure<KmsInfoModel>) model -> {
                    kmsInfo = model;
                    callback.onComplete(ResultImpl.success(kmsInfo));
                });
    }


}
