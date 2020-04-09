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

package com.ciscowebex.androidsdk.internal.model;

import com.ciscowebex.androidsdk.internal.crypto.CryptoUtils;
import com.ciscowebex.androidsdk.internal.crypto.KeyObject;
import me.helloworld.utils.Checker;

import java.util.Date;

public class ObjectModel {

    public static class Type {
        public static final String activity = "activity";
        public static final String person = "person";
        public static final String comment = "comment";
        public static final String conversation = "conversation";
        public static final String file = "file";
        public static final String locus = "locus";
        public static final String content = "content";
        public static final String event = "event";
        public static final String locusSessionSummary = "locusSessionSummary";
        public static final String team = "team";
        public static final String microappInstance = "microappInstance";
        public static final String spaceProperty = "spaceProperty";
        public static final String groupMention = "groupMention";
        public static final String giphy = "giphy";
        public static final String link = "link";
    }

    private String id;
    private String url;
    private String objectType;
    private Date published;
    private String content;
    private String displayName;
    private String clientTempId;
    private String kmsMessage;

    public ObjectModel(String type) {
        objectType = type;
    }

    public String getId() {
        return id;
    }

    public ObjectModel setId(String id) {
        this.id = id;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getObjectType() {
        return objectType;
    }

    public Date getPublished() {
        return published;
    }

    public void setPublished(Date published) {
        this.published = published;
    }

    public String getContent() {
        return content;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getClientTempId() {
        return clientTempId;
    }

    public String getKmsMessage() {
        return kmsMessage;
    }

    public boolean isPerson() {
        return this.objectType.equals(ObjectModel.Type.person);
    }

    public boolean isConversation() {
        return this.objectType.equals(ObjectModel.Type.conversation);
    }

    public boolean isActivity() {
        return this.objectType.equals(ObjectModel.Type.activity);
    }

    public boolean isComment() {
        return this.objectType.equals(ObjectModel.Type.comment);
    }

    public boolean isContent() {
        return this.objectType.equals(ObjectModel.Type.content);
    }

    public boolean isLocus() {
        return this.objectType.equals(ObjectModel.Type.locus);
    }

    public boolean isEvent() {
        return this.objectType.equals(ObjectModel.Type.event);
    }

    public boolean isTeamObject() {
        return this.objectType.equals(ObjectModel.Type.team);
    }

    public boolean isLocusSessionSummary() {
        return this.objectType.equals(ObjectModel.Type.locusSessionSummary);
    }

    public boolean isMicroAppInstance() {
        return ObjectModel.Type.microappInstance.equals(objectType);
    }

    public boolean isSpaceProperty() {
        return ObjectModel.Type.spaceProperty.equals(objectType);
    }

    public void encrypt(KeyObject key) {
        if (key != null && !Checker.isEmpty(content)) {
            content = CryptoUtils.encryptToJwe(key, content);
        }
    }

    public void decrypt(KeyObject key) {
        if (key != null && CryptoUtils.looksLikeCipherText(content)) {
            content = CryptoUtils.decryptFromJwe(key, content);
        }
    }
}
