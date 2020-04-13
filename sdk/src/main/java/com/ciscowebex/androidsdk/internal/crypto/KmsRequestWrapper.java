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

import com.cisco.wx2.sdk.kms.KmsRequest;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.internal.Credentials;
import com.ciscowebex.androidsdk.internal.Device;

public class KmsRequestWrapper<T> {

    public enum RequestType {
        AUTHORIZATIONS,
        CREATE_EPHEMERAL_KEY,
        GET_KEYS,
        PING,
        CREATE_RESOURCE;
    }

    private RequestType type;
    private KmsRequest kmsRequest;
    private CompletionHandler<T> callback;
    private String conversationId;
    private Device device;
    private Credentials credentials;

    public KmsRequestWrapper(RequestType type, KmsRequest kmsRequest, CompletionHandler<T> callback) {
        this.type = type;
        this.kmsRequest = kmsRequest;
        this.callback = callback;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public Device getDevice() {
        return device;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public RequestType getType() {
        return type;
    }

    public KmsRequest getKmsRequest() {
        return kmsRequest;
    }

    public String getConversationId() {
        return conversationId;
    }

    public CompletionHandler<T> getCallback() {
        return callback;
    }
}
