package com.cisco.spark.android.util;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;

import com.cisco.crypto.scr.SecureContentReference;
import com.cisco.spark.android.core.AuthenticatedUser;
import com.cisco.spark.android.model.Activity;
import com.cisco.spark.android.model.ErrorDetail;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.model.KmsRequestResponseComplete;
import com.cisco.spark.android.model.SymmetricJWK;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.sync.ConversationAvatarContentReference;
import com.cisco.spark.android.sync.ConversationContentProviderOperation;
import com.cisco.spark.android.sync.ConversationContract;
import com.cisco.spark.android.sync.EncryptedConversationProcessor;
import com.cisco.spark.android.sync.KmsResourceObject;
import com.cisco.spark.android.sync.Message;
import com.cisco.wx2.sdk.kms.KmsApi;
import com.cisco.wx2.sdk.kms.KmsApiLogger;
import com.cisco.wx2.sdk.kms.KmsKey;
import com.cisco.wx2.sdk.kms.KmsRequest;
import com.cisco.wx2.sdk.kms.KmsRequestBody;
import com.cisco.wx2.sdk.kms.KmsRequestFactory;
import com.cisco.wx2.sdk.kms.KmsResponse;
import com.cisco.wx2.sdk.kms.KmsResponseBody;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.JWEEncrypter;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jose.util.JSONObjectUtils;

import org.spongycastle.crypto.engines.AESFastEngine;
import org.spongycastle.crypto.io.CipherInputStream;
import org.spongycastle.crypto.io.CipherOutputStream;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;
import javax.inject.Provider;

import retrofit2.Response;

public class CryptoUtils {
    private static KmsRequestFactory kmsRequestFactory = null;

    private static boolean isInit;

    public static void init() {
        if (isInit)
            return;
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
        KmsApi.useGson(true);
        KmsApi.useSpongyCastle(true);
        KmsApi.setLogger(new KmsApiLn());
        kmsRequestFactory = KmsRequestFactory.getInstance();
        isInit = true;
    }

    public static JWK convertStringToJWK(String keyString) {
        JWK jwk = null;
        try {
            jwk = JWK.parse(keyString);
        } catch (ParseException e) {
            Ln.e(false, "Parse Exception while extracting public key", e.getMessage());
        }
        return jwk;
    }

    public static KeyObject convertKmsKeytoKeyObject(KmsKey kmsKey) {
        if (kmsKey == null || (kmsKey != null && kmsKey.getJwk() == null))
            return null;
        String[] resourceUri = null;
        if (kmsKey.getResourceUri() != null) {
            resourceUri = new String[]{UriUtils.toString(kmsKey.getResourceUri())};
        }
        KeyObject keyObject = new KeyObject(UriUtils.parseFromURI(kmsKey.getUri()), new SymmetricJWK(kmsKey.getJwk().getK(), UriUtils.parseIfNotNull(kmsKey.getJwk().getKid())), null, resourceUri, kmsKey.getExpirationDate());
        return keyObject;
    }

    public static KmsRequestBody.Client getKmsRequestBodyClient(String deviceId, AuthenticatedUser user) {
        KmsRequestBody.Client client = new KmsRequestBody.Client();
        client.setClientId(deviceId);
        client.getCredential().setUserId(user.getUserId());
        client.getCredential().setBearer(user.getKmsAuthorizationHeader());
        return client;
    }

    public static KmsRequest generateEphemeralKeyRequest(String deviceId, AuthenticatedUser authenticatedUser, Uri kmsCluster, String requestId) {
        init();
        if (authenticatedUser == null)
            return null;
        KmsRequestBody.Client client = getKmsRequestBodyClient(deviceId, authenticatedUser);
        KmsRequest kmsRequest = kmsRequestFactory.newCreateEphemeralKeyRequest(client, requestId, kmsCluster.toString());
        return kmsRequest;
    }

    public static KmsRequest getUnboundKeyRequest(String deviceId, AuthenticatedUser authenticatedUser, String requestId, int count) {
        init();
        String userId = authenticatedUser.getKey().getUuid();
        KmsRequestBody.Client client = getKmsRequestBodyClient(deviceId, authenticatedUser);
        KmsRequest kmsRequest = kmsRequestFactory.newCreateKeysRequest(client, requestId, count);
        return kmsRequest;
    }

    public static KmsRequest getBoundKeyRequest(String deviceId, AuthenticatedUser authenticatedUser, String requestId, Uri keyUri) {
        init();
        String userId = authenticatedUser.getKey().getUuid();
        KmsRequestBody.Client client = getKmsRequestBodyClient(deviceId, authenticatedUser);
        URI requestUri = UriUtils.toURI(keyUri);
        if (requestUri != null) {
            Ln.v("Request ID" + requestId + " Building bound key request for " + keyUri + " requestUri" + requestUri);
            KmsRequest kmsRequest = kmsRequestFactory.newRetrieveKeyRequest(client, requestId, requestUri);
            return kmsRequest;
        }
        return null;
    }

    public static KmsRequest getBoundKeyAuthorizationRequest(String deviceId, AuthenticatedUser authenticatedUser, String requestId, KmsResourceObject kro) {
        init();
        String userId = authenticatedUser.getKey().getUuid();
        KmsRequestBody.Client client = getKmsRequestBodyClient(deviceId, authenticatedUser);
        Uri requestUri = kro.getUri().buildUpon().appendPath("authorizations").build();
        if (requestUri != null) {
            Ln.v("Request ID" + requestId + " Building bound key request for requestUri" + requestUri);
            KmsRequest kmsRequest = kmsRequestFactory.newRetrieveAuthorizationsRequest(client, requestId, URI.create(requestUri.toString()));
            return kmsRequest;
        }
        return null;
    }

    public static KmsRequest authorizeNewParticipantsRequest(String deviceId, AuthenticatedUser authenticatedUser, String requestId, KmsResourceObject kro, Collection<String> uuIDs) {
        init();
        KmsRequestBody.Client client = getKmsRequestBodyClient(deviceId, authenticatedUser);
        if (kro == null)
            return null;
        KmsRequest kmsRequest = kmsRequestFactory.newCreateAuthorizationsRequest(client, requestId, kro.getURI(), new ArrayList<>(uuIDs));
        return kmsRequest;
    }

    public static KmsRequest updateEncryptionKeyRequest(String deviceId, AuthenticatedUser authenticatedUser, String requestId, KmsResourceObject kro, KeyObject newKey) {
        init();
        KmsRequestBody.Client client = getKmsRequestBodyClient(deviceId, authenticatedUser);
        KmsRequest kmsRequest = kmsRequestFactory.newUpdateKeyRequest(client, requestId, newKey.getRelativeURI(), kro.getURI());
        return kmsRequest;
    }

    public static KmsRequest createResourceRequest(String deviceId, AuthenticatedUser self, String requestId, List<String> userIds, Uri oneKeyUri) {
        List<URI> uriList = Collections.singletonList(UriUtils.toURI(oneKeyUri));
        return createResourceRequest(deviceId, self, requestId, userIds, uriList);
    }

    public static KmsRequest createResourceRequest(String deviceId, AuthenticatedUser self, String requestId, List<String> userIds, List<URI> keyUris) {
        init();
        String userId = self.getUserId();
        KmsRequestBody.Client client = getKmsRequestBodyClient(deviceId, self);
        return kmsRequestFactory.newCreateResourceRequest(client, requestId, userIds, keyUris);
    }

    public static boolean verify(String rsaPublicKeyString, String jwsObjectString) {
        JWSObject jwsObject;
        RSAKey rsaPublicKey;
        try {
            rsaPublicKey = (RSAKey) JWK.parse(rsaPublicKeyString);
            JWSVerifier verifier = new RSASSAVerifier(rsaPublicKey.toRSAPublicKey());
            jwsObject = JWSObject.parse(jwsObjectString);
            return jwsObject.verify(verifier);
        } catch (JOSEException e) {
            Ln.e(e.getMessage(), "JOSE Exception while verifying public key");
        } catch (ParseException e) {
            Ln.e(e.getMessage(), "Parse Exception while verifying key");
        } catch (InvalidKeySpecException e) {
            Ln.e(e.getMessage(), "Invalid key spec exception while verifying key");
        } catch (NoSuchAlgorithmException e) {
            Ln.e(e.getMessage(), "Incorrect algorithm exception while verifying key");
        }
        return false;
    }

    public static String encryptToJwe(KeyObject key, String message) throws IOException {
        try {
            JWEObject jwe = new JWEObject(
                    new JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A256GCM),
                    new Payload(message));

            jwe.encrypt(new DirectEncrypter(new SecretKeySpec(key.getKeyBytes(), "AES")));

            return jwe.serialize();
        } catch (JOSEException e) {
            throw new IOException("Unable to encrypt", e);
        }
    }

    public static String decryptFromJwe(KeyObject key, String message) throws IOException, ParseException, NullPointerException {
        if (Strings.isEmpty(message)) {
            return message;
        }
        try {
            JWEObject jwe = JWEObject.parse(message);
            jwe.decrypt(new DirectDecrypter(new SecretKeySpec(key.getKeyBytes(), "AES")));
            return jwe.getPayload().toString();
        } catch (JOSEException e) {
            Ln.v("Failed to decrypt message: " + message);
            String exceptionMessage = "Unable to decrypt ";
            if (key.getKeyValue() != null && !UriUtils.equals(key.getKeyUrl(), key.getKeyId())) {
                exceptionMessage += "Mismatched keys";
            }
            throw new IOException(exceptionMessage, e);
        } catch (ParseException e) {
            Ln.v("Failed to decrypt message: " + message);
            throw e;
        } catch (NullPointerException e) {
            Ln.v("Failed to decrypt message: " + message);
            throw e;
        }
    }

    public static String encryptAES(String message, String key) {
        if (message == null) {
            return null;
        }
        String jweString = null;

        try {
            Base64URL base64Key = new Base64URL(key);
            byte[] byteKey = base64Key.decode();
            SecretKeySpec secretKeySpec = new SecretKeySpec(byteKey, "AES");
            JWEEncrypter encrypter = new DirectEncrypter(secretKeySpec);
            JWEHeader header = new JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A256GCM);
            Payload payload = new Payload(message);
            JWEObject jweObj = new JWEObject(header, payload);
            jweObj.encrypt(encrypter);
            jweString = jweObj.serialize();

        } catch (JOSEException ex) {
            Ln.e(ex.getMessage(), "encryption JOSE exception");
        }
        return jweString;
    }

    public static String decryptAES(String encryptedText, String key) {
        if (encryptedText == null || key == null) {
            return null;
        }

        String msgOut = null;
        try {

            Base64URL base64Key = new Base64URL(key);
            byte[] byteKey = base64Key.decode();
            SecretKeySpec secretKeySpec = new SecretKeySpec(byteKey, "AES");
            JWEObject jweObjOut = JWEObject.parse(encryptedText);
            JWEDecrypter decrypter = new DirectDecrypter(secretKeySpec);
            jweObjOut.decrypt(decrypter);
            msgOut = jweObjOut.getPayload().toString();
        } catch (JOSEException ex) {
            Ln.e(ex.getMessage(), "Decryption Exception");
        } catch (ParseException ex) {
            Ln.e(ex.getMessage(), "Decryption parse Exception");
        }
        return msgOut;
    }

    public static ECPublicKey extractPublicKey(String jwkPublicString) {
        try {
            JWSObject jwsObject = JWSObject.parse(jwkPublicString);
            Payload payload = jwsObject.getPayload();
            ECKey serverPublicKey = ECKey.parse(payload.toJSONObject());
            return serverPublicKey.toECPublicKey();

        } catch (ParseException e) {
            Ln.e(e.getMessage(), "Parse Exception while extracting public key");
        } catch (InvalidKeySpecException e) {
            Ln.e(e.getMessage(), "Invalid Key Exception while extracting public key");
        } catch (NoSuchAlgorithmException e) {
            Ln.e(e.getMessage(), "No such algorithm Exception while extracting public key");
        }
        return null;

    }

    public static void encryptAES(String key, InputStream in, OutputStream out) {
        byte[] byteKey = Hex.decode(key);
        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESFastEngine()));
        cipher.init(true, new KeyParameter(byteKey)); // FIXME: Do we want to specify an initialization vector? (ParametersWithIV)
        org.spongycastle.crypto.io.CipherOutputStream cipherOut = new CipherOutputStream(out, cipher);
        FileUtils.streamCopy(in, cipherOut);
    }

    public static void decryptAES(String key, InputStream in, OutputStream out) {
        byte[] byteKey = Hex.decode(key);
        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESFastEngine()));
        cipher.init(false, new KeyParameter(byteKey));
        org.spongycastle.crypto.io.CipherInputStream cipherIn = new CipherInputStream(in, cipher);
        FileUtils.streamCopy(cipherIn, out);
    }

    public static String base64EncodedString(Uri value) {
        if (value == null) {
            return null;
        }
        String base64 = Base64.encodeToString(value.toString().getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);
        return base64;
    }

    public static void decryptMessage(KeyObject key, Message message) throws IOException, ParseException, NullPointerException {
        message.decrypt(key);
    }

    public static void decryptActivity(KeyObject key, Activity activity) throws IOException, ParseException, NullPointerException {
        if (key == null) {
            Ln.w(false, "Missing key for activity " + activity.getId());
            return;
        }

        try {
            activity.decrypt(key);
            activity.setEncrypted(key.getKey() == null);
        } catch (IOException e) {
            Ln.v(e, "Unable to decrypt activity" + activity + " key" + key.getKeyUrl() + "=byte[" + key.getKey().length() + "]");
            throw e;
        } catch (ParseException e) {
            Ln.d(e, "Unable to decrypt activity");
            throw e;
        }
    }

    public static boolean isPlainTextMessage(String message) {
        if (message == null)
            return false;

        return !looksLikeCipherText(message);
    }

    public static boolean looksLikeCipherText(String str) {
        if (TextUtils.isEmpty(str))
            return false;

        String[] parts = TextUtils.split(str, "\\.");
        if (parts.length != 5) {
            return false;
        }

        try {
            JSONObjectUtils.parseJSONObject(new String(Base64.decode(parts[0], Base64.URL_SAFE)));
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    public static KmsResponseBody extractKmsResponseBody(Response response, Gson gson, OctetSequenceKey sharedKeyWithKMSAsJWK) {
        try {
            String body = response.errorBody().string();
            ErrorDetail errorDetail = gson.fromJson(body, ErrorDetail.class);
            if (errorDetail == null)
                return null;

            ErrorDetail.CustomErrorCode errorCode = errorDetail.extractCustomErrorCode();
            if (errorCode == ErrorDetail.CustomErrorCode.KmsMessageOperationFailed) {
                if (errorDetail.getMessage() != null) {
                    KmsResponseBody errorMessage = CryptoUtils.decryptKmsMessage(errorDetail.getMessage(), sharedKeyWithKMSAsJWK);
                    return errorMessage;
                }
            }
        } catch (IOException e) {
            Ln.w(e, "Failed reading response");
        } catch (Exception e) {
            Ln.w(e, "Failure while decrypting Kms Error Message");
        }
        return null;
    }

    public static KmsResponseBody decryptKmsMessage(String kmsMessage, JWK sharedKeyWithKms) {
        init();
        if (TextUtils.isEmpty(kmsMessage) || sharedKeyWithKms == null)
            return null;
        KmsResponse kmsResponse = null;
        try {
            kmsResponse = new KmsResponse(kmsMessage);
            return (kmsResponse.getBody(sharedKeyWithKms));
        } catch (ParseException e) {
            Ln.e(false, e, "Exception while trying to decrypt kmsMessages. Shared key kid is used to encrypt this kmsMessage " + kmsResponse.getKid().equals(sharedKeyWithKms.getKeyID()));
        } catch (Exception e) {
            Ln.e(false, e, "Exception while trying to decrypt kmsMessages");
        }
        return null;
    }

    public static List<KmsResponseBody> decryptKmsMessages(KmsRequestResponseComplete encryptionKmsMessage, JWK sharedKeyWithKms) {
        init();
        ArrayList<KmsResponseBody> decryptedKmsMessageList = new ArrayList<KmsResponseBody>();
        KmsResponseBody decryptedKmsResponse = null;
        for (String kmsMessage : encryptionKmsMessage.getKmsMessages()) {
            decryptedKmsResponse = decryptKmsMessage(kmsMessage, sharedKeyWithKms);
            if (decryptedKmsResponse != null) {
                decryptedKmsMessageList.add(decryptedKmsResponse);
            }

        }
        return decryptedKmsMessageList;
    }

    public static KeyObject deriveEphemeralKey(KmsRequestResponseComplete encryptionKmsMessage, String kmsStaticKeyString, KmsRequest kmsRequest) {
        init();
        if (kmsRequest == null) {
            return null;
        }
        if (encryptionKmsMessage.getKmsMessages() != null && !encryptionKmsMessage.getKmsMessages().isEmpty()) {
            KmsResponse kmsResponse = null;
            try {
                kmsResponse = new KmsResponse(encryptionKmsMessage.getKmsMessages().get(0));
                KmsResponseBody kmsResponseBody = kmsResponse.getBody(convertStringToJWK(kmsStaticKeyString));
                KmsKey sharedKey = kmsResponseBody.getKey();
                OctetSequenceKey sharedKeyValue = kmsRequest.deriveEphemeralKey(kmsResponse);
                return convertOctetSequenceKeyToKeyObject(sharedKeyValue, sharedKey.getExpirationDate());
            } catch (ParseException e) {
                Ln.e(false, e, "Unable to derive Ephemeral Key from KmsResponse");
            }
        }
        return null;
    }

    public static KeyObject convertOctetSequenceKeyToKeyObject(OctetSequenceKey octetSequenceKey, Date expirationDate) {
        if (octetSequenceKey != null && octetSequenceKey.getKeyValue() != null && octetSequenceKey.getKeyID() != null) {
            return new KeyObject(UriUtils.parseIfNotNull(octetSequenceKey.getKeyID()), new SymmetricJWK(octetSequenceKey.getKeyValue().toString(), UriUtils.parseIfNotNull(octetSequenceKey.getKeyID())), null, null, expirationDate);
        }
        return null;
    }

    public static OctetSequenceKey parseOctetSequenceKey(KeyObject key) {

        if (key == null || (key != null && TextUtils.isEmpty(key.getKey()))) {
            return null;
        }
        OctetSequenceKey.Builder keyBuilder = new OctetSequenceKey.Builder(new Base64URL(key.getKey()));
        keyBuilder.keyID(UriUtils.toString(key.getKeyId()));
        return keyBuilder.build();
    }

    public static KmsRequest leaveKmsMessagingApiRequest(String deviceId, KmsResourceObject kro, AuthenticatedUser authenticatedUser, String requestId, String userId) {
        init();
        KmsRequestBody.Client client = getKmsRequestBodyClient(deviceId, authenticatedUser);
        if (kro == null || TextUtils.isEmpty(userId))
            return null;
        Uri requestUri = kro.getUri().buildUpon()
                .appendPath("authorizations")
                .appendQueryParameter("authId", userId).build();
        URI requestURI = URI.create(UriUtils.toString(requestUri));
        KmsRequest kmsRequest = kmsRequestFactory.newDeleteAuthorizationByResourceRequest(client, requestId, requestURI);
        return kmsRequest;
    }


    public static String getKmsErrorMessage(Response response, Gson gson, OctetSequenceKey sharedKeyWithKMSAsJWK) {

        KmsResponseBody errorMessage = CryptoUtils.extractKmsResponseBody(response, gson, sharedKeyWithKMSAsJWK);
        String message = "Encryption service error: ";
        if (errorMessage != null) {
            message += errorMessage.getReason();
        }
        return message;
    }

    private static class KmsApiLn implements KmsApiLogger {

        @Override
        public void log(Level level, String s, Exception e) {
            switch (level) {

                case DEBUG:
                    Ln.d(e, s);
                    break;
                case VERBOSE:
                    Ln.v(e, s);
                    break;
                case INFO:
                    Ln.i(e, s);
                    break;
                case WARN:
                    Ln.w(false, e, s);
                    break;
                case ERROR:
                    Ln.e(false, e, s);
                    break;
            }
        }

        @Override
        public void log(Level level, String s) {
            switch (level) {
                case DEBUG:
                    Ln.d(s);
                    break;
                case VERBOSE:
                    Ln.v(s);
                    break;
                case INFO:
                    Ln.i(s);
                    break;
                case WARN:
                    Ln.w(s);
                    break;
                case ERROR:
                    Ln.e(s);
                    break;
            }
        }

    }

    public static boolean healEncryptedConversationTitlesAndSummaries(ContentResolver contentResolver, EncryptedConversationProcessor conversationProcessor, Provider<Batch> batchProvider) {
        boolean ret = false;
        Batch batch = batchProvider.get();
        Cursor c = null;
        try {
            c = contentResolver.query(ConversationContract.ConversationEntry.CONTENT_URI, new String[]{ConversationContract.ConversationEntry.TITLE_ENCRYPTION_KEY_URL.name()}, ConversationContract.ConversationEntry.IS_TITLE_ENCRYPTED + "=1", null, null);
            while (c != null && c.moveToNext()) {
                // This will request the key if we don't have it already
                KeyObject key = conversationProcessor.getKeyForDecryption(UriUtils.parseIfNotNull(c.getString(0)));
                if (key != null) {
                    String updatedConv = decryptConversationTitleAndSummary(contentResolver, batch, key);
                    if (!TextUtils.isEmpty(updatedConv))
                        ret = true;
                }
            }
        } finally {
            if (c != null)
                c.close();
        }
        batch.apply();
        return ret;
    }

    public static boolean healEncryptedConversationAvatars(ContentResolver contentResolver, EncryptedConversationProcessor conversationProcessor, Provider<Batch> batchProvider, Gson gson) {
        boolean ret = false;
        Batch batch = batchProvider.get();
        Cursor c = null;
        try {
            c = contentResolver.query(ConversationContract.ConversationEntry.CONTENT_URI, new String[]{ConversationContract.ConversationEntry.AVATAR_ENCRYPTION_KEY_URL.name()}, ConversationContract.ConversationEntry.IS_AVATAR_ENCRYPTED + "=1", null, null);
            while (c != null && c.moveToNext()) {
                // This will request the key if we don't have it already
                KeyObject key = conversationProcessor.getKeyForDecryption(UriUtils.parseIfNotNull(c.getString(0)));
                if (key != null) {
                    String updatedConv = decryptConversationAvatar(contentResolver, batch, key, gson);
                    if (!TextUtils.isEmpty(updatedConv))
                        ret = true;
                }
            }
        } finally {
            if (c != null)
                c.close();
        }
        batch.apply();
        return ret;
    }

    public static String decryptConversationAvatar(ContentResolver contentResolver, Batch batch, KeyObject key, Gson gson) {
        Cursor c = null;
        try {
            c = contentResolver.query(
                    ConversationContract.vw_Conversation.CONTENT_URI,
                    ConversationContract.vw_Conversation.DEFAULT_PROJECTION,
                    ConversationContract.vw_Conversation.CONVERSATION_AVATAR_ENCRYPTION_KEY_URL + "=? AND " + ConversationContract.vw_Conversation.CONVERSATION_AVATAR_CONTENT_REFERENCE + " IS NOT NULL AND " + ConversationContract.vw_Conversation.IS_CONVERSATION_AVATAR_ENCRYPTED + " =1",
                    new String[]{key.getKeyUrl().toString()}, null);

            // Only one result expected
            if (c != null && c.moveToNext()) {
                ConversationAvatarContentReference conversationAvatarContentReference = gson.fromJson(c.getString(ConversationContract.vw_Conversation.CONVERSATION_AVATAR_CONTENT_REFERENCE.ordinal()), ConversationAvatarContentReference.class);
                String encryptedScr = conversationAvatarContentReference.getScr();
                String id = c.getString(ConversationContract.vw_Conversation.CONVERSATION_ID.ordinal());

                Ln.v("decrypting avatar SCR for " + id);

                try {
                    boolean isAvatarEncrypted = looksLikeCipherText(encryptedScr);

                    if (!isAvatarEncrypted)
                        return null;

                    String scr = null;
                    if (isAvatarEncrypted) {
                        scr = decryptFromJwe(key, encryptedScr);
                    }

                    String keyUrlString = UriUtils.toString(key.getKeyUrl());


                    ContentProviderOperation.Builder updateBuilder = ContentProviderOperation.newUpdate(ConversationContract.ConversationEntry.CONTENT_URI);

                    if (conversationAvatarContentReference == null) {
                        conversationAvatarContentReference = new ConversationAvatarContentReference();
                    }

                    conversationAvatarContentReference.setScr(scr);
                    conversationAvatarContentReference.setSecureContentReference(SecureContentReference.fromJWE(key.getKeyBytes(), encryptedScr));

                    if (scr != null) {
                        updateBuilder = updateBuilder
                                .withValue(ConversationContract.ConversationEntry.CONVERSATION_AVATAR_CONTENT_REFERENCE.name(), gson.toJson(conversationAvatarContentReference, ConversationAvatarContentReference.class))
                                .withValue(ConversationContract.ConversationEntry.IS_AVATAR_ENCRYPTED.name(), 0);
                    }

                    updateBuilder = updateBuilder
                            .withSelection(ConversationContract.ConversationEntry.TITLE_ENCRYPTION_KEY_URL + "=?", new String[]{keyUrlString})
                            .withValue(ConversationContract.ConversationEntry.IS_TITLE_ENCRYPTED.name(), 0);


                    batch.add(updateBuilder.build());
                    return id;

                } catch (Exception e) {
                    Ln.e(e, "Unable to decrypt conversation avatar");
                }
            }
        } finally {
            if (c != null)
                c.close();
        }
        return null;
    }

    public static String decryptConversationTitleAndSummary(ContentResolver contentResolver, Batch batch, KeyObject key) {
        Cursor c = null;
        try {
            c = contentResolver.query(
                    ConversationContract.vw_Conversation.CONTENT_URI,
                    ConversationContract.vw_Conversation.DEFAULT_PROJECTION,
                    ConversationContract.vw_Conversation.TITLE_ENCRYPTION_KEY_URL + "=? AND " + ConversationContract.vw_Conversation.TITLE + " IS NOT NULL AND " + ConversationContract.vw_Conversation.IS_TITLE_ENCRYPTED + " =1",
                    new String[]{key.getKeyUrl().toString()}, null);

            // Only one result expected
            if (c != null && c.moveToNext()) {
                String encryptedTitle = c.getString(ConversationContract.ConversationEntry.TITLE.ordinal());
                String encryptedSummary = c.getString(ConversationContract.ConversationEntry.SUMMARY.ordinal());
                String id = c.getString(ConversationContract.vw_Conversation.CONVERSATION_ID.ordinal());

                Ln.v("decrypting title " + encryptedTitle + " for " + id);

                try {
                    boolean isTitleEncrypted = looksLikeCipherText(encryptedTitle);
                    boolean isSummaryEncrypted = looksLikeCipherText(encryptedSummary);
                    if (!isTitleEncrypted && !isSummaryEncrypted)
                        return null;

                    String title = null;
                    String summary = null;

                    if (isTitleEncrypted) {
                        title = decryptFromJwe(key, encryptedTitle);
                    }

                    if (isSummaryEncrypted) {
                        summary = decryptFromJwe(key, encryptedSummary);
                    }

                    String keyUrlString = UriUtils.toString(key.getKeyUrl());

                    ContentProviderOperation.Builder updateBuilder = ContentProviderOperation.newUpdate(ConversationContract.ConversationEntry.CONTENT_URI);

                    if (title != null) {
                        updateBuilder = updateBuilder
                                .withValue(ConversationContract.ConversationEntry.TITLE.name(), title)
                                .withValue(ConversationContract.ConversationEntry.IS_TITLE_ENCRYPTED.name(), 0)
                                .withValue(ConversationContract.ConversationEntry.CONVERSATION_DISPLAY_NAME.name(), title);
                    }

                    if (summary != null) {
                        updateBuilder = updateBuilder.withValue(ConversationContract.ConversationEntry.SUMMARY.name(), summary);
                    }

                    updateBuilder = updateBuilder
                            .withSelection(ConversationContract.ConversationEntry.TITLE_ENCRYPTION_KEY_URL + "=?", new String[]{keyUrlString})
                            .withValue(ConversationContract.ConversationEntry.IS_TITLE_ENCRYPTED.name(), 0);

                    batch.add(updateBuilder.build());
                    //update title info in ConversationSearchEntry
                    batch.add(ConversationContentProviderOperation.updateConversationSearchEntryTitle(title, id));
                    return id;

                } catch (Exception e) {
                    Ln.e(e, "Unable to decrypt title and/or summary");
                }
            }
        } finally {
            if (c != null)
                c.close();
        }
        return null;
    }

}
