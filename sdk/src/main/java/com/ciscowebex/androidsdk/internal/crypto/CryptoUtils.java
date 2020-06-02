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

package com.ciscowebex.androidsdk.internal.crypto;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;
import com.cisco.wx2.sdk.kms.*;
import com.ciscowebex.androidsdk.internal.Credentials;
import com.ciscowebex.androidsdk.internal.ErrorDetail;
import com.ciscowebex.androidsdk.internal.model.KmsMessageModel;
import com.ciscowebex.androidsdk.utils.UriUtils;
import com.github.benoitdion.ln.Ln;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jose.util.JSONObjectUtils;

import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

import me.helloworld.utils.Checker;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.util.*;

public class CryptoUtils {

    private static boolean isInit;

    public static void init() {
        if (isInit) {
            return;
        }
        Security.removeProvider("BC");
        Security.insertProviderAt(new org.bouncycastle.jce.provider.BouncyCastleProvider(), 1);
        KmsApi.setLogger(new KmsApiLn());
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
        if (kmsKey == null || kmsKey.getJwk() == null) {
            return null;
        }
        String[] resourceUri = null;
        if (kmsKey.getResourceUri() != null) {
            resourceUri = new String[]{UriUtils.toString(kmsKey.getResourceUri())};
        }
        return new KeyObject(UriUtils.toString(kmsKey.getUri()), new SymmetricJWK(kmsKey.getJwk().getK(), UriUtils.parseIfNotNull(kmsKey.getJwk().getKid())), null, resourceUri, kmsKey.getExpirationDate());
    }

    public static KmsResponseBody decryptKmsMessage(String kmsMessage, JWK sharedKeyWithKms) {
        init();
        if (TextUtils.isEmpty(kmsMessage) || sharedKeyWithKms == null) {
            return null;
        }
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

    public static List<KmsResponseBody> decryptKmsMessages(KmsMessageModel encryptionKmsMessage, JWK sharedKeyWithKms) {
        init();
        ArrayList<KmsResponseBody> decryptedKmsMessageList = new ArrayList<>();
        KmsResponseBody decryptedKmsResponse;
        for (String kmsMessage : encryptionKmsMessage.getKmsMessages()) {
            decryptedKmsResponse = decryptKmsMessage(kmsMessage, sharedKeyWithKms);
            if (decryptedKmsResponse != null) {
                decryptedKmsMessageList.add(decryptedKmsResponse);
            }
        }
        return decryptedKmsMessageList;
    }

    public static KeyObject generateEphemeralKey(KmsMessageModel encryptionKmsMessage, String kmsStaticKeyString, KmsRequest kmsRequest) {
        init();
        if (kmsRequest == null) {
            return null;
        }
        if (encryptionKmsMessage.getKmsMessages() != null && !encryptionKmsMessage.getKmsMessages().isEmpty()) {
            KmsResponse kmsResponse;
            try {
                kmsResponse = new KmsResponse(encryptionKmsMessage.getKmsMessages().get(0));
                KmsResponseBody kmsResponseBody = kmsResponse.getBody(convertStringToJWK(kmsStaticKeyString));
                KmsKey sharedKey = kmsResponseBody.getKey();
                OctetSequenceKey sharedKeyValue = kmsRequest.deriveEphemeralKey(kmsResponse);
                if (sharedKeyValue != null && sharedKeyValue.getKeyValue() != null && sharedKeyValue.getKeyID() != null) {
                    Uri keyUrl = UriUtils.parseIfNotNull(sharedKeyValue.getKeyID());
                    if (keyUrl != null) {
                        return new KeyObject(keyUrl.toString(), new SymmetricJWK(sharedKeyValue.getKeyValue().toString(),
                                UriUtils.parseIfNotNull(sharedKeyValue.getKeyID())), null, null, sharedKey.getExpirationDate());
                    }
                }
            } catch (ParseException e) {
                Ln.e(false, e, "Unable to derive Ephemeral Key from KmsResponse");
            }
        }
        return null;
    }

    public static KmsRequest generateEphemeralKeyRequest(String deviceId, Credentials credentials, Uri kmsCluster, String requestId) {
        init();
        KmsRequestBody.Client client = getKmsRequestBodyClient(deviceId, credentials);
        return KmsRequestFactory.getInstance().newCreateEphemeralKeyRequest(client, requestId, kmsCluster.toString());
    }

    public static KmsRequest generateBoundKeyRequest(String deviceId, Credentials credentials, String requestId, String keyUri) {
        init();
        KmsRequestBody.Client client = getKmsRequestBodyClient(deviceId, credentials);
        URI requestUri = UriUtils.toURI(keyUri);
        if (requestUri != null) {
            Ln.v("Request ID" + requestId + " Building bound key request for " + keyUri + " requestUri" + requestUri);
            return KmsRequestFactory.getInstance().newRetrieveKeyRequest(client, requestId, requestUri);
        }
        return null;
    }

    public static KmsRequest generateUnboundKeyRequest(String deviceId, Credentials credentials, String requestId, int count) {
        init();
        KmsRequestBody.Client client = getKmsRequestBodyClient(deviceId, credentials);
        return KmsRequestFactory.getInstance().newCreateKeysRequest(client, requestId, count);
    }

    public static KmsRequest getBoundKeyAuthorizationRequest(String deviceId, Credentials credentials, String requestId, KmsResourceObject kro) {
        init();
        KmsRequestBody.Client client = getKmsRequestBodyClient(deviceId, credentials);
        Uri requestUri = UriUtils.parseIfNotNull(kro.getUri()).buildUpon().appendPath("authorizations").build();
        if (requestUri != null) {
            Ln.v("Request ID" + requestId + " Building bound key request for requestUri" + requestUri);
            return KmsRequestFactory.getInstance().newRetrieveAuthorizationsRequest(client, requestId, URI.create(requestUri.toString()));
        }
        return null;
    }

    public static KmsRequest authorizeNewParticipantsRequest(String deviceId, Credentials credentials, String requestId, KmsResourceObject kro, Collection<String> uuIDs) {
        init();
        KmsRequestBody.Client client = getKmsRequestBodyClient(deviceId, credentials);
        if (kro == null) {
            return null;
        }
        return KmsRequestFactory.getInstance().newCreateAuthorizationsRequest(client, requestId, kro.getURI(), new ArrayList<>(uuIDs));
    }

    public static KmsRequest updateEncryptionKeyRequest(String deviceId, Credentials credentials, String requestId, KmsResourceObject kro, KeyObject newKey) {
        init();
        KmsRequestBody.Client client = getKmsRequestBodyClient(deviceId, credentials);
        return KmsRequestFactory.getInstance().newUpdateKeyRequest(client, requestId, UriUtils.toURI(newKey.getKeyUrl()), kro.getURI());
    }

    public static KmsRequest createResourceRequest(String deviceId, Credentials self, String requestId, List<String> userIds, Uri oneKeyUri) {
        List<URI> uriList = Collections.singletonList(UriUtils.toURI(oneKeyUri));
        return createResourceRequest(deviceId, self, requestId, userIds, uriList);
    }

    public static KmsRequest createResourceRequest(String deviceId, Credentials self, String requestId, List<String> userIds, List<URI> keyUris) {
        init();
        String userId = self.getUserId();
        KmsRequestBody.Client client = getKmsRequestBodyClient(deviceId, self);
        return KmsRequestFactory.getInstance().newCreateResourceRequest(client, requestId, userIds, keyUris);
    }

    public static boolean verify(String rsaPublicKeyString, String jwsObjectString) {
        JWSObject jwsObject;
        RSAKey rsaPublicKey;
        try {
            rsaPublicKey = (RSAKey) JWK.parse(rsaPublicKeyString);
            JWSVerifier verifier = new RSASSAVerifier(rsaPublicKey.toRSAPublicKey());
            jwsObject = JWSObject.parse(jwsObjectString);
            return jwsObject.verify(verifier);
        } catch (Exception e) {
            Ln.e(e.getMessage(), "Exception while verifying public key");
        }
        return false;
    }

    public static String encryptToJwe(KeyObject key, String message) {
        if (Checker.isEmpty(message) || key == null) {
            return message;
        }
        try {
            JWEObject jwe = new JWEObject(new JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A256GCM), new Payload(message));
            jwe.encrypt(new DirectEncrypter(new SecretKeySpec(key.getKeyBytes(), "AES")));
            return jwe.serialize();
        } catch (JOSEException e) {
            Ln.d(e);
        }
        return message;
    }

    public static String decryptFromJwe(KeyObject key, String message) {
        if (Checker.isEmpty(message) || key == null) {
            return message;
        }
        try {
            JWEObject jwe = JWEObject.parse(message);
            jwe.decrypt(new DirectDecrypter(new SecretKeySpec(key.getKeyBytes(), "AES")));
            return jwe.getPayload().toString();
        } catch (JOSEException e) {
            String exceptionMessage = "Unable to decrypt ";
            if (key.getKeyValue() != null && !UriUtils.equals(key.getKeyUrl(), key.getKeyId())) {
                exceptionMessage += "Mismatched keys";
            }
            Ln.d(exceptionMessage + ", " + message, e);
        } catch (ParseException | NullPointerException e) {
            Ln.d("Failed to decrypt message: " + message, e);
        }
        return message;
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
        } catch (Exception e) {
            Ln.e(e.getMessage(), "Exception while extracting public key");
        }
        return null;
    }

    public static String base64EncodedString(Uri value) {
        if (value == null) {
            return null;
        }
        return Base64.encodeToString(value.toString().getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);
    }

    public static boolean isPlainTextMessage(String message) {
        if (message == null) {
            return false;
        }
        return !looksLikeCipherText(message);
    }

    public static boolean looksLikeCipherText(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        String[] parts = TextUtils.split(str, "\\.");
        if (parts.length != 5) {
            return false;
        }
        try {
            JSONObjectUtils.parseJSONObject(new String(Base64.decode(parts[0], Base64.URL_SAFE)));
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    public static KmsResponseBody extractKmsResponseBody(@Nullable ErrorDetail errorDetail, OctetSequenceKey sharedKeyWithKMSAsJWK) {
        try {
            if (errorDetail == null)
                return null;

            ErrorDetail.CustomErrorCode errorCode = errorDetail.extractCustomErrorCode();
            if (errorCode == ErrorDetail.CustomErrorCode.KmsMessageOperationFailed) {
                if (errorDetail.getMessage() != null) {
                    return CryptoUtils.decryptKmsMessage(errorDetail.getMessage(), sharedKeyWithKMSAsJWK);
                }
            }
        } catch (Exception e) {
            Ln.w(e, "Failure while decrypting Kms Error Message");
        }
        return null;
    }

    public static OctetSequenceKey parseOctetSequenceKey(KeyObject key) {
        if (key == null || TextUtils.isEmpty(key.getKey())) {
            return null;
        }
        OctetSequenceKey.Builder keyBuilder = new OctetSequenceKey.Builder(new Base64URL(key.getKey()));
        keyBuilder.keyID(UriUtils.toString(key.getKeyId()));
        return keyBuilder.build();
    }

    public static KmsRequest leaveKmsMessagingApiRequest(String deviceId, KmsResourceObject kro, Credentials credentials, String requestId, String userId) {
        init();
        KmsRequestBody.Client client = getKmsRequestBodyClient(deviceId, credentials);
        if (kro == null || TextUtils.isEmpty(userId)) {
            return null;
        }
        Uri requestUri = UriUtils.parseIfNotNull(kro.getUri()).buildUpon().appendPath("authorizations").appendQueryParameter("authId", userId).build();
        URI requestURI = URI.create(UriUtils.toString(requestUri));
        return KmsRequestFactory.getInstance().newDeleteAuthorizationByResourceRequest(client, requestId, requestURI);
    }

    public static String getKmsErrorMessage(ErrorDetail errorDetail, OctetSequenceKey sharedKeyWithKMSAsJWK) {

        KmsResponseBody errorMessage = CryptoUtils.extractKmsResponseBody(errorDetail, sharedKeyWithKMSAsJWK);
        String message = "Encryption service error: ";
        if (errorMessage != null) {
            message += errorMessage.getReason();
        }
        return message;
    }

    private static KmsRequestBody.Client getKmsRequestBodyClient(String deviceId, Credentials user) {
        KmsRequestBody.Client client = new KmsRequestBody.Client();
        client.setClientId(deviceId);
        client.getCredential().setUserId(user.getUserId());
        client.getCredential().setBearer(user.getToken());
        return client;
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

    public static Key createDESKey(final byte[] bytes, final int offset) {
        final byte[] keyBytes = new byte[7];
        System.arraycopy(bytes, offset, keyBytes, 0, 7);
        final byte[] material = new byte[8];
        material[0] = keyBytes[0];
        material[1] = (byte) (keyBytes[0] << 7 | (keyBytes[1] & 0xff) >>> 1);
        material[2] = (byte) (keyBytes[1] << 6 | (keyBytes[2] & 0xff) >>> 2);
        material[3] = (byte) (keyBytes[2] << 5 | (keyBytes[3] & 0xff) >>> 3);
        material[4] = (byte) (keyBytes[3] << 4 | (keyBytes[4] & 0xff) >>> 4);
        material[5] = (byte) (keyBytes[4] << 3 | (keyBytes[5] & 0xff) >>> 5);
        material[6] = (byte) (keyBytes[5] << 2 | (keyBytes[6] & 0xff) >>> 6);
        material[7] = (byte) (keyBytes[6] << 1);
        oddParity(material);
        return new SecretKeySpec(material, "DES");
    }

    public static byte[] hmacMD5(byte[] key, byte[] data) {
        KeyParameter param = new KeyParameter(key);
        HMac hmac = new HMac(new MD5Digest());
        byte[] result;
        hmac.init(param);
        hmac.update(data, 0, data.length);
        result = new byte[hmac.getMacSize()];
        hmac.doFinal(result, 0);
        return result;
    }

    public static byte[] hmacMd5(Key key, byte[] data) {
        byte[] hashedData;
        try {
            Mac mac = Mac.getInstance("HmacMd5");
            mac.init(key);
            hashedData = mac.doFinal(data);
            return hashedData;
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            // TODO: Handle....
            return null;
        }
    }

    private static void oddParity(final byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            final byte b = bytes[i];
            final boolean needsParity = (((b >>> 7) ^ (b >>> 6) ^ (b >>> 5) ^ (b >>> 4) ^ (b >>> 3)
                    ^ (b >>> 2) ^ (b >>> 1)) & 0x01) == 0;
            if (needsParity) {
                bytes[i] |= (byte) 0x01;
            } else {
                bytes[i] &= (byte) 0xfe;
            }
        }
    }
}

