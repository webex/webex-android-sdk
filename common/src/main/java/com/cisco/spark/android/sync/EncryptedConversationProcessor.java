package com.cisco.spark.android.sync;

import android.content.Context;
import android.net.Uri;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.metrics.EncryptionDurationMetricManager;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.ActivityObject;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.model.UserEmailRequest;
import com.cisco.spark.android.model.UserIdentityKey;
import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.util.SafeAsyncTask;
import com.cisco.spark.android.util.UriUtils;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.wx2.sdk.kms.KmsRequest;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Provider;

import retrofit2.Response;

public class EncryptedConversationProcessor {
    private final KeyManager keyManager;
    private final Context context;
    private final ApiTokenProvider apiTokenProvider;
    private final EncryptionDurationMetricManager encryptionDurationMetricManager;
    private final Gson gson;
    private ApiClientProvider apiClientProvider;
    private final DeviceRegistration deviceRegistration;
    private final Provider<Batch> batchProvider;

    public EncryptedConversationProcessor(Context context, KeyManager keyManager, ApiTokenProvider apiTokenProvider, EncryptionDurationMetricManager encryptionDurationMetricManager, Gson gson, ApiClientProvider apiClientProvider, DeviceRegistration deviceRegistration, Provider<Batch> batchProvider) {
        this.context = context;
        this.keyManager = keyManager;
        this.apiTokenProvider = apiTokenProvider;
        this.encryptionDurationMetricManager = encryptionDurationMetricManager;
        this.gson = gson;
        this.apiClientProvider = apiClientProvider;
        this.deviceRegistration = deviceRegistration;
        this.batchProvider = batchProvider;
    }

    public EncryptionDurationMetricManager getEncryptionDurationMetricManager() {
        return encryptionDurationMetricManager;
    }

    //Messages should be encrypted with the conversation.defaultEncryptionKeyUrl whereas Content should be encrypted using activity.encryptionKeyUrl
    public Activity copyAndEncryptActivity(Activity activity, Uri... conversationDefaultKeyUri) throws IOException {
        // Other parts of the code might still need to do work with the unencrypted version of the activity.
        final Activity deepCopy = ActivityObject.deepCopy(gson, activity);
        Uri encryptionKeyUri;
        if (conversationDefaultKeyUri != null && conversationDefaultKeyUri.length > 0) {
            encryptionKeyUri = conversationDefaultKeyUri[0];
        } else {
            encryptionKeyUri = deepCopy.getEncryptionKeyUrl();
        }
        String conversationId = deepCopy.getConversationId();
        KeyObject storedKeyForEncryption = getKeyForEncryption(conversationId, encryptionKeyUri);

        if (storedKeyForEncryption != null && storedKeyForEncryption.getKey() != null) {
            deepCopy.encrypt(storedKeyForEncryption);
            deepCopy.setEncryptionKeyUrl(storedKeyForEncryption.getKeyUrl());
        }
        activity.setEncrypted((storedKeyForEncryption == null) ? (encryptionKeyUri != null) : (storedKeyForEncryption.getKey() == null));
        return deepCopy;
    }

    public KeyObject getKeyForEncryption(String conversationId, Uri keyUrl) {
        KeyObject keyObject = null;
        if (keyUrl != null) {
            keyObject = keyManager.getBoundKey(keyUrl);
        }

        if (keyObject == null) {
            keyObject = ConversationContentProviderQueries.getKeyForEncryption(context.getContentResolver(), conversationId);
        }
        return keyObject;
    }

    public KeyObject getKeyForDecryption(Uri keyUrl) {
        return keyManager.getBoundKey(keyUrl);
    }

    public boolean decryptMessage(Message message, Uri keyUrl) {
        KeyObject key = getKeyForDecryption(keyUrl);
        if (key != null) {
            try {
                message.decrypt(key);
                return true;
            } catch (ParseException e) {
                Ln.e(false, e, "Unable to decrypt message due to parse exception");
            } catch (Exception e) {
                Ln.e(e, "Unable to decrypt message");
            }
        }

        if (CryptoUtils.isPlainTextMessage(message.getText())) {
            writeDecryptedData(message.getActivities().iterator().next(), gson.toJson(message));
        }
        return false;
    }

    protected String encryptKmsRequest(KmsRequest kmsRequest) {
        if (kmsRequest == null)
            return null;
        return kmsRequest.asEncryptedBlob(keyManager.getSharedKeyAsJWK());
    }

    public String associateNewEncryptionKeyUsingKmsMessagingApi(KmsResourceObject kro, KeyObject key) {
        KmsRequest kmsRequest = CryptoUtils.updateEncryptionKeyRequest(getDeviceUriString(), apiTokenProvider.getAuthenticatedUser(), UUID.randomUUID().toString(), kro, key);
        return encryptKmsRequest(kmsRequest);
    }

    public String authorizeNewParticipantsUsingKmsMessagingApi(KmsResourceObject kro, Person person) {
        Set<String> userIds = null;
        userIds = new HashSet<>();
        if (person.getKey() != null && person.getKey().getUuid() != null) {
            userIds.add(person.getKey().getUuid());
        } else {
            try {
                // This happens when sideboarding a new user
                UserEmailRequest uuidReq = new UserEmailRequest(person.getEmail());
                Response<Map<String, UserIdentityKey>> resp = apiClientProvider.getUserClient().getOrCreateUserID(apiTokenProvider.getAuthenticatedUser().getConversationAuthorizationHeader(), Collections.singletonList(uuidReq)).execute();
                if (resp.isSuccessful() && resp.body() != null && resp.body().containsKey(uuidReq.getEmail())) {
                    userIds.add(resp.body().get(uuidReq.getEmail()).getId());
                } else {
                    Ln.w("Failed getOrCreateUserID");
                    return null;
                }
            } catch (IOException e) {
                Ln.w(e, "Failed getOrCreateUserID");
                return null;
            }
        }
        return authorizeNewParticipantsUsingKmsMessagingApi(kro, userIds);
    }

    public String authorizeNewParticipantsUsingKmsMessagingApi(KmsResourceObject kro, Collection<String> userIds) {
        KmsRequest kmsRequest = CryptoUtils.authorizeNewParticipantsRequest(getDeviceUriString(), apiTokenProvider.getAuthenticatedUser(), UUID.randomUUID().toString(), kro, new ArrayList<>(userIds));
        return encryptKmsRequest(kmsRequest);
    }

    public String createNewResource(List<String> userIds, List<Uri> keyUris) {
        ArrayList<URI> keyURIs = new ArrayList<>();
        try {
            for (Uri uri : keyUris) {
                keyURIs.add(new URI(uri.toString()));
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        String requestId = UUID.randomUUID().toString();
        KmsRequest kmsRequest = CryptoUtils.createResourceRequest(getDeviceUriString(), apiTokenProvider.getAuthenticatedUser(), requestId, userIds, keyURIs);
        keyManager.addKmsMessageRequest(requestId, KmsMessageRequestType.CREATE_RESOURCE, kmsRequest);
        return encryptKmsRequest(kmsRequest);
    }

    public String removeParticipantUsingKmsMessagingApi(KmsResourceObject kro, String actorKeyUUID) {
        KmsRequest kmsRequest = CryptoUtils.leaveKmsMessagingApiRequest(getDeviceUriString(), kro, apiTokenProvider.getAuthenticatedUser(), UUID.randomUUID().toString(), actorKeyUUID);
        return encryptKmsRequest(kmsRequest);
    }

    public void writeDecryptedData(final String activityId, final String activityData) {
        if (activityId == null) {
            return;
        }

        new SafeAsyncTask<Void>() {
            @Override
            public Void call() throws Exception {
                Batch batch = batchProvider.get();
                batch.add(ConversationContentProviderOperation.clearActivityEncryptedFlag(activityId, activityData));
                batch.apply();
                return null;
            }
        }.execute();

    }

    private String getDeviceUriString() {
        return UriUtils.toString(deviceRegistration.getUrl());
    }
}
