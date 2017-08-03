package com.cisco.spark.android.model;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.cisco.spark.android.authenticator.AuthenticatedUserProvider;
import com.cisco.spark.android.authenticator.LogoutEvent;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.events.KmsKeyEvent;
import com.cisco.spark.android.mercury.events.KeyPushEvent;
import com.cisco.spark.android.mercury.events.KmsAckEvent;
import com.cisco.spark.android.mercury.events.KmsMessageResponseEvent;
import com.cisco.spark.android.metrics.EncryptionDurationMetricManager;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationContentProviderOperation;
import com.cisco.spark.android.sync.ConversationContentProviderQueries;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.KmsMessageRequestObject;
import com.cisco.spark.android.sync.KmsMessageRequestType;
import com.cisco.spark.android.sync.SearchManager;
import com.cisco.spark.android.sync.operationqueue.core.OperationQueue;
import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.util.LoggingUtils;
import com.cisco.spark.android.util.UriUtils;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.wx2.sdk.kms.KmsKey;
import com.cisco.wx2.sdk.kms.KmsRequest;
import com.cisco.wx2.sdk.kms.KmsRequestBody;
import com.cisco.wx2.sdk.kms.KmsResponseBody;
import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;
import com.jakewharton.rxrelay.PublishRelay;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetSequenceKey;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
import retrofit2.Response;
import rx.Observable;

@Singleton
public class KeyManager {
    private final Map<Uri, KeyObject> unboundKeys = new ConcurrentHashMap<>();
    private final Map<Uri, KeyObject> boundKeys = new ConcurrentHashMap<>();
    private final ContentResolver contentResolver;
    private KeyObject searchKeyObject;
    private EventBus bus = null;
    private OperationQueue operationQueue;
    private DeviceRegistration deviceRegistration;
    private ApiClientProvider apiClientProvider;
    private EncryptionDurationMetricManager encryptionDurationMetricManager;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final Provider<Batch> batchProvider;
    protected KeyObject sharedKeyWithKMS;
    private NaturalLog ln;
    private SearchManager searchManager;
    private final Object sync = new Object();

    PublishRelay<KeyObject> keyRelay = PublishRelay.create();

    public static int defaultUnboundKeyLimit = 10;
    public static final String SHARED_KEY_ID = "Shared-key-with-kms";
    private static final int FETCH_KEY_DELAY = 10;

    /* For every KmsMessage request that goes out to the KMS, the KmsRequest, type(CREATE_EPHEMERAL, PING, UNBOUND_KEYS) and requestId are stored in KmsMessagesRequestMap .
    *  When we receive a KmsMessageResponseEvent, the KmsRequest is retrieved from KmsMessagesRequestMap based on requestId. KmsApi library needs the KmsRequest object to derive Ephemeral Keys from the KmsResponse etc.
    */
    private Map<String, KmsMessageRequestObject> kmsMessagesRequestMap = new ConcurrentHashMap<>();
    private KmsInfo kmsInfo;


    @Inject
    public KeyManager(EventBus bus, ContentResolver contentResolver, OperationQueue operationQueue, DeviceRegistration deviceRegistration, ApiClientProvider apiClientProvider, EncryptionDurationMetricManager encryptionDurationMetricManager, Ln.Context lnContext, SearchManager searchManager, AuthenticatedUserProvider authenticatedUserProvider, Provider<Batch> batchProvider) {
        this.bus = bus;
        this.contentResolver = contentResolver;
        this.operationQueue = operationQueue;
        this.deviceRegistration = deviceRegistration;
        this.apiClientProvider = apiClientProvider;
        this.encryptionDurationMetricManager = encryptionDurationMetricManager;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.batchProvider = batchProvider;
        this.ln = Ln.get(lnContext, "KeyManager");
        this.searchManager = searchManager;
        if (this.bus != null) {
            this.bus.register(this);
        }
    }

    public int getUnboundKeyCount() {
        synchronized (sync) {
            return unboundKeys.size();
        }
    }

    public void addKeys(List<KeyObject> keyObjects) {
        if (keyObjects == null)
            return;

        // Populate the in-memory caches
        synchronized (sync) {
            for (KeyObject keyObject : keyObjects) {
                if (isKeyBound(keyObject)) {
                    addBoundKey(keyObject);
                } else {
                    addUnBoundKey(keyObject);
                }
            }
        }
    }

    public boolean isKeyBound(KeyObject keyObject) {
        synchronized (sync) {
            if (boundKeys.containsKey(keyObject.getKeyUrl()))
                return true;
        }
        if (keyObject.getKmsResourceObject() != null)
            return true;

        return isKeyBoundInDb(keyObject);
    }

    public void addUnBoundKey(KeyObject keyObject) {
        synchronized (sync) {
            if (!isKeyBound(keyObject)) {
                unboundKeys.put(keyObject.getKeyUrl(), keyObject);
            }
        }
        encryptionDurationMetricManager.onFinishFetchUnboundKeys();
    }

    public void addBoundKey(KeyObject keyObject) {
        synchronized (sync) {
            boundKeys.put(keyObject.getKeyUrl(), keyObject);
        }
    }

    public KeyObject getCachedUnboundKey() {
        KeyObject ret = null;

        synchronized (sync) {
            while (!unboundKeys.isEmpty() && ret == null) {
                Uri keyUri = unboundKeys.keySet().iterator().next();
                KeyObject key = unboundKeys.remove(keyUri);
                if (key.isValid()) {
                    boundKeys.put(key.getKeyUrl(), key);
                    ret = key;
                }
            }
        }
        triggerSync();
        return ret;
    }

    public KeyObject getSearchKey() {
        if (searchKeyObject == null) {
            searchKeyObject = getCachedUnboundKey();
        }
        return searchKeyObject;
    }

    protected KeyObject getBoundKey(Uri keyUri, boolean dbFallback) {
        if (keyUri == null)
            return null;

        KeyObject key = boundKeys.get(keyUri);
        if (key != null)
            return key;

        if (dbFallback)
            key = getKeyFromDb(keyUri);

        if (key != null)
            return key;

        operationQueue.requestKey(keyUri);

        return null;
    }

    /**
     * Get the key from memory or the DB, spawning a fetch key operation if needed
     *
     * @param uri the key uri
     * @return the key or null
     */
    public KeyObject getBoundKey(Uri uri) {
        return getBoundKey(uri, true);
    }

    public Observable<KeyObject> getBoundKeySync(@NonNull final Uri uri) {
        KeyObject boundKey = getBoundKey(uri);
        if (boundKey != null) {
            return Observable.just(boundKey);
        } else {
            // A sync has been triggered
            return keyRelay.filter(keyObject -> uri.equals(keyObject.getKeyUrl()));
        }
    }

    public Observable<KeyObject> getBoundKeyDelaySync(@NonNull final Uri uri) {
        KeyObject boundKey = getBoundKey(uri);
        if (boundKey != null) {
            return Observable.just(boundKey);
        } else {
            return Observable.timer(FETCH_KEY_DELAY, TimeUnit.SECONDS).map(ko -> getBoundKey(uri));
        }
    }

    /**
     * Get the key iff it's in memory
     *
     * @param uri the key uri
     * @return the key or null
     */
    public KeyObject getCachedBoundKey(Uri uri) {
        return getBoundKey(uri, false);
    }

    protected KeyObject getKeyFromDb(Uri keyUri) {
        if (!Looper.getMainLooper().equals(Looper.myLooper()) && contentResolver != null) {
            KeyObject key = ConversationContentProviderQueries.getBoundKeyFromUri(contentResolver, keyUri);
            if (key != null) {
                addBoundKey(new KeyObject(keyUri, key.getKey(), key.getKeyId()));
                return key;
            }
        }
        return null;
    }

    protected boolean isKeyBoundInDb(KeyObject keyObject) {
        return ConversationContentProviderQueries.isKeyBound(contentResolver, keyObject.getKeyUrl());
    }

    protected KeyObject getSharedKeyFromDb() {
        KeyObject ret = ConversationContentProviderQueries.getSharedKmsKeyFromCache(contentResolver, SHARED_KEY_ID);

        if (ret == null)
            return null;

        if (!ret.isValid()) {
            Batch batch = batchProvider.get();
            batch.add(ConversationContentProviderOperation.clearEncryptionKey(SHARED_KEY_ID));
            batch.apply();
            ret = null;
        }

        return ret;
    }

    // Checks to see if the key is cached in memory without making any attempt to fetch it if it is not
    public boolean isBoundKeyCached(Uri keyUri) {
        return boundKeys.containsKey(keyUri);
    }

    public void onEvent(LogoutEvent logoutEvent) {
        clear();
    }

    private void clear() {
        synchronized (sync) {
            unboundKeys.clear();
            boundKeys.clear();
            this.sharedKeyWithKMS = null;
            this.searchKeyObject = null;
        }
    }

    public void removeBoundKey(Uri keyUri) {
        if (keyUri == null) {
            return;
        }

        synchronized (sync) {
            boundKeys.remove(keyUri);
        }

        Batch batch = batchProvider.get();
        batch.add(ConversationContentProviderOperation.clearEncryptionKey(keyUri.toString()));
        batch.apply();
    }


    public void addKmsMessageRequest(String requestId, KmsMessageRequestType type, KmsRequest kmsRequest) {
        kmsMessagesRequestMap.put(requestId, new KmsMessageRequestObject(type, kmsRequest));
    }

    public String getDeviceId() {
        return deviceRegistration.getUrl().toString();
    }


    public void generateSharedSecret(KmsRequestResponseComplete encryptionKmsMessage, KmsMessageRequestObject kmsMessages) {
        //TODO: Verification(CryptoUtils.verify(this.kmsInfo.getRsaPublicKey()))
        synchronized (sync) {
            sharedKeyWithKMS = CryptoUtils.deriveEphemeralKey(encryptionKmsMessage, getKmsInfo().getRsaPublicKey(), kmsMessages.getKmsRequest());
            if (sharedKeyWithKMS != null && sharedKeyWithKMS.isValid()) {
                Batch batch = batchProvider.get();
                batch.add(ConversationContentProviderOperation.insertEncryptionKey(SHARED_KEY_ID, sharedKeyWithKMS.getKey(), UriUtils.toString(sharedKeyWithKMS.getKeyId()), sharedKeyWithKMS.getExpirationTime()));
                batch.apply();
                encryptionDurationMetricManager.onFinishSharedKeyNegotiation();
            } else {
                sharedKeyWithKMS = null;
                encryptionDurationMetricManager.onFinishSharedKeyNegotiation();
            }
        }
    }

    public KeyObject getSharedKeyWithKMS() {
        synchronized (sync) {
            return (getSharedKeyWithKMS(true));
        }
    }

    /**
     * @param negotiateIfEmpty Determines whether to negotiate the key if we come up empty. False is
     *                         passed from hasSharedKeyWithKms to avoid unneeded recursion
     * @return
     */
    protected KeyObject getSharedKeyWithKMS(boolean negotiateIfEmpty) {
        if (sharedKeyWithKMS != null && sharedKeyWithKMS.isValid())
            return sharedKeyWithKMS;

        sharedKeyWithKMS = getSharedKeyFromDb();

        if (sharedKeyWithKMS != null)
            return sharedKeyWithKMS;

        if (negotiateIfEmpty)
            operationQueue.setUpSharedKey();

        return null;
    }

    public OctetSequenceKey getSharedKeyAsJWK() {
        synchronized (sync) {
            KeyObject key = getSharedKeyWithKMS();
            if (key != null)
                return key.getKeyValueAsJWK();
        }
        return null;
    }

    public boolean hasSharedKeyWithKMS() {
        synchronized (sync) {
            return getSharedKeyWithKMS(false) != null;
        }
    }

    public void triggerSync() {
        if (needMoreUnboundKeys()) {
            ln.d("Trigger Sync ");
            operationQueue.refreshUnboundKeys();
        }
    }

    public boolean needMoreUnboundKeys() {
        synchronized (sync) {
            return getUnboundKeyCount() <= (defaultUnboundKeyLimit / 2);
        }
    }

    public void addBoundKey(Uri keyUri, String keyValue, Uri keyId) {
        addBoundKey(new KeyObject(keyUri, keyValue, keyId));
    }

    public void onEvent(KeyPushEvent event) {
        ln.d("Response from KMS for key request " + event.getRequestId() + " status code " + event.getStatusCode() + " " + event.getFailureReason());
        addKeys(event.getKeys());
    }

    public void onEvent(KmsAckEvent event) {
        ln.i("Response from KMS for request " + event.getRequestId() + " status code " + event.getStatusCode() + " Failure Reason " + event.getFailureReason());
        if (event.getStatusCode() != 200) {
            //TODO :Retry mechanism by getting request from requestMap and then call authorizeParticipants
        }
    }

    public void onEvent(KmsMessageResponseEvent event) throws ParseException {
        ln.d("Received kmsMessage event");
        JWK decryptKmsMessageKey = null;
        //Decrypt KmsMessage
        if (!hasSharedKeyWithKMS()) {
            decryptKmsMessageKey = CryptoUtils.convertStringToJWK(getKmsInfo().getRsaPublicKey());
        } else {
            decryptKmsMessageKey = this.getSharedKeyAsJWK();
        }
        //For each message in KmsMessages, decrypt and send appropriate event
        //TODO: Need a better way of identifying the kid of the key used to encrypt the KmsMessages prior to decryption
        List<KmsResponseBody> decryptedKmsMessages = CryptoUtils.decryptKmsMessages(event.getEncryptionKmsMessage(), decryptKmsMessageKey);
        for (KmsResponseBody decryptedKmsMessage : decryptedKmsMessages) {
            //Retrieve type by matching requestId of response to that in KmsMessageRequestMap
            KmsMessageRequestObject kmsRequest = kmsMessagesRequestMap.remove(decryptedKmsMessage.getRequestId());
            if (kmsRequest == null) {
                ln.v("ERROR unexpected KMS message: " + decryptedKmsMessage);
                continue;
            }
            ln.v("Response for Request ID " + decryptedKmsMessage.getRequestId() + " type:" + kmsRequest.getType() + " response " + decryptedKmsMessage.toString());
            switch (kmsRequest.getType()) {
                case CREATE_EPHEMERAL_KEY:
                    generateSharedSecret(event.getEncryptionKmsMessage(), kmsRequest);
                    break;
                case GET_KEYS:
                    processGetKeys(decryptedKmsMessage, kmsRequest);
                    break;
                case PING:
                case AUTHORIZATIONS:
                case CREATE_RESOURCE:
                    break;
                default:
                    ln.w(false, "Unknown KmsMessage Type: %s", kmsRequest.getType());
                    break;
            }
        }
    }

    private void processGetKeys(KmsResponseBody decryptedKmsMessage, KmsMessageRequestObject kmsRequest) {

        List<KeyObject> keyObjectList = new ArrayList<>();
        if (decryptedKmsMessage.getKey() != null) {
            KmsKey kmsKey = decryptedKmsMessage.getKey();
            keyObjectList.add(CryptoUtils.convertKmsKeytoKeyObject(kmsKey));
        }

        if (decryptedKmsMessage.getKeys() != null) {
            for (KmsKey kmsKey : decryptedKmsMessage.getKeys()) {
                keyObjectList.add(CryptoUtils.convertKmsKeytoKeyObject(kmsKey));
            }
        }

        addKeys(keyObjectList);
        bus.post(new KmsKeyEvent(keyObjectList));

        if (decryptedKmsMessage.getStatus() == HttpURLConnection.HTTP_FORBIDDEN) {
            KeyObject key = parseKeyFailure(kmsRequest);
            if (key != null) {
                keyRelay.call(key);
            }
        }

        for (KeyObject keyObject : keyObjectList) {
            keyRelay.call(keyObject);
        }
    }

    @Nullable
    private KeyObject parseKeyFailure(@NonNull KmsMessageRequestObject kmsRequest) {

        KmsRequest request = kmsRequest.getKmsRequest();
        if (request == null)
            return null;

        KmsRequestBody body = request.getBody();
        if (body == null)
            return null;

        URI failedKeyUri = body.getUri();
        if (failedKeyUri == null)
            return null;

        Uri parse = Uri.parse(failedKeyUri.toString());
        return KeyObject.failedKeyRequest(parse);
    }

    public KmsInfo getKmsInfo() {
        if (kmsInfo == null) {
            synchronized (sync) {
                if (kmsInfo == null) {
                    AuthenticatedUser user = authenticatedUserProvider.getAuthenticatedUserOrNull();
                    if (user == null)
                        return null;
                    String requestId = UUID.randomUUID().toString();

                    try {
                        Response<KmsInfo> response = apiClientProvider.getSecurityClient().getKmsInfo(requestId, getDeviceId(), user.getUserId()).execute();
                        if (response.isSuccessful())
                            kmsInfo = response.body();
                        else
                            Ln.e("Failed getting KMS info : " + LoggingUtils.toString(response));
                    } catch (IOException e) {
                        Ln.e(e, "Failed getting KMS info");
                    }
                }
            }
        }
        return kmsInfo;
    }

    public static Uri getTeamKroUri(ContentResolver contentResolver, ApiClientProvider apiClientProvider, String teamId) {
        String ret = ConversationContentProviderQueries.getOneValue(contentResolver,
                ConversationContract.ConversationEntry.KMS_RESOURCE_OBJECT_URI,
                ConversationContract.ConversationEntry.CONVERSATION_ID + "=?",
                new String[]{teamId});

        if (ret == null) {
            Ln.i("Couldn't find a team KRO URI for team " + teamId + " in the DB, fetching it from the server");
            try {
                Response<Team> teamResponse = apiClientProvider.getConversationClient().getTeam(teamId).execute();
                if (teamResponse.isSuccessful()) {
                    Team team = teamResponse.body();
                    Response<Conversation> convResponse = apiClientProvider.getConversationClient().getConversation(team.getGeneralConversationUuid()).execute();
                    if (convResponse.isSuccessful()) {
                        Conversation teamConv = convResponse.body();
                        ret = teamConv.getKmsResourceObject().getURI().toString();
                    } else {
                        Ln.w("Failed getting General conversation for team from service: " + LoggingUtils.toString(convResponse));
                    }
                } else {
                    Ln.w("Failed getting team from conversation service: " + LoggingUtils.toString(teamResponse));
                }
            } catch (IOException e) {
                Ln.w(e);
            }
        }
        return Uri.parse(ret);
    }

    // used for testing
    public Collection<KeyObject> getBoundKeys() {
        return boundKeys.values();
    }

    //used for testing
    public void clearUnboundKeys() {
        synchronized (sync) {
            unboundKeys.clear();
        }
    }
}
