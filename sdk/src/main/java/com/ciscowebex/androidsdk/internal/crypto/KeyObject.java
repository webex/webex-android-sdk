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
import android.support.annotation.NonNull;
import com.ciscowebex.androidsdk.utils.UriUtils;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.util.Base64URL;

import java.util.Date;

public class KeyObject {
    private String keyUrl;
    private SymmetricJWK keyValue;
    private String[] authorizations;
    private String[] resources;
    private Date expirationDate;

    public KeyObject(@NonNull String keyUrl, @NonNull SymmetricJWK keyString, String[] authorizations, String[] resources, Date expirationDate) {
        this(keyUrl, keyString, authorizations, resources);
        this.expirationDate = expirationDate;
    }

    public KeyObject(@NonNull String keyUrl, @NonNull SymmetricJWK keyString, String[] authorizations, String[] resources) {
        this.keyUrl = keyUrl;
        this.keyValue = keyString;
        this.authorizations = authorizations;
        this.resources = resources;
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

    public String getKey() {
        if (keyValue == null) {
            return null;
        }
        return this.keyValue.getOctetSequenceKey();
    }

    public byte[] getKeyBytes() {
        Base64URL base64Key = new Base64URL(getKey());
        return base64Key.decode();
    }

    public String getKeyUrl() {
        return keyUrl;
    }

    public String[] getAuthorizations() {
        return this.authorizations;
    }

    public String[] getResources() {
        return this.resources;
    }

    public KmsResourceObject getKmsResourceObject() {
        if (resources == null || resources.length == 0) {
            return null;
        }
        String uri = resources[0];
        if (uri == null) {
            return null;
        }
        return new KmsResourceObject(uri);
    }

    public Uri getKeyId() {
        return this.keyValue.getKId();
    }

    public Date getExpirationDate() {
        return expirationDate;
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

}
