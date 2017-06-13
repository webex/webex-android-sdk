package com.cisco.spark.android.model;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.cisco.spark.android.sync.KmsResourceObject;
import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.util.UriUtils;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.util.Base64URL;

import java.net.URI;
import java.util.Date;
import java.util.UUID;

public class KeyObject {
    private Uri keyUrl;
    private SymmetricJWK keyValue;
    private String[] authorizations;
    private String[] resources;
    private Date expirationDate;

    public KeyObject(@NonNull Uri keyUrl, @NonNull SymmetricJWK keyString, String[] authorizations, String[] resources, Date expirationDate) {
        this(keyUrl, keyString, authorizations, resources);
        this.expirationDate = expirationDate;
    }

    public KeyObject(@NonNull Uri keyUrl, @NonNull SymmetricJWK keyString, String[] authorizations, String[] resources) {
        this.keyUrl = keyUrl;
        this.keyValue = keyString;
        this.authorizations = authorizations;
        this.resources = resources;
    }

    public KeyObject(@NonNull Uri keyUrl, @NonNull String keyString, @NonNull Uri kid) {
        this.keyUrl = keyUrl;
        this.keyValue = new SymmetricJWK(keyString, kid);
    }

    public KeyObject(@NonNull Uri keyUrl, @NonNull String keyString, @NonNull Uri kid, long expirationTime) {
        this.keyUrl = keyUrl;
        this.keyValue = new SymmetricJWK(keyString, kid);
        this.expirationDate = new Date(expirationTime);
    }

    private KeyObject(Uri keyUrl) {
        this.keyUrl = keyUrl;
    }

    public SymmetricJWK getKeyValue() {
        return this.keyValue;
    }

    public OctetSequenceKey getKeyValueAsJWK() {
        if (this.keyValue != null) {
            return CryptoUtils.parseOctetSequenceKey(this);
        }
        return null;
    }

    public void setKey(SymmetricJWK keyValue) {
        this.keyValue = keyValue;
    }

    public String getKey() {

        if (keyValue == null)
            return null;

        return this.keyValue.getOctetSequenceKey();
    }

    public byte[] getKeyBytes() {
        return KeyObject.getKeyBytes(keyValue.getOctetSequenceKey());
    }

    public static byte[] getKeyBytes(String key) {
        Base64URL base64Key = new Base64URL(key);
        return base64Key.decode();
    }

    public Uri getKeyUrl() {
        return keyUrl;
    }

    public void setKeyUrl(Uri keyUrl) {
        this.keyUrl = keyUrl;
    }

    public String[] getAuthorizations() {
        return this.authorizations;
    }

    public String[] getResources() {
        return this.resources;
    }

    public KmsResourceObject getKmsResourceObject() {
        if (resources == null || resources.length == 0)
            return null;

        Uri uri = UriUtils.parseIfNotNull(resources[0]);

        if (uri == null)
            return null;

        return new KmsResourceObject(uri);
    }

    public void setResources(String uriString) {
        this.resources = new String[]{uriString};
    }

    public Uri getKeyId() {
        return this.keyValue.getKId();
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public long getExpirationTime() {
        if (this.expirationDate != null) {
            return this.expirationDate.getTime();
        }
        return 0;
    }

    public boolean isKeyExpired() {
        return ((System.currentTimeMillis() - getExpirationTime()) >= 0);
    }

    public boolean isValid() {
        return getKeyId() != null && getKeyValue() != null && !isKeyExpired();
    }

    public URI getRelativeURI() {
        UUID uuid = UriUtils.extractUUID(keyUrl);
        return java.net.URI.create("/keys/" + uuid);
    }

    public static KeyObject failedKeyRequest(Uri uri) {
        return new KeyObject(uri);
    }
}
