package com.cisco.spark.android.model;

import android.net.Uri;

import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.util.Strings;
import com.google.gson.Gson;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

// TODO: set cloud-apps-common to use Java 1.6 so we can use it as a dependency instead of rewriting
// all of these classes.
/**
 * Base class for things that can be the Object of an Activity
 */
public class ActivityObject {
    public static <T extends ActivityObject> T deepCopy(Gson gson, T activityObject) {
        return (T) gson.fromJson(gson.toJson(activityObject, activityObject.getClass()), activityObject.getClass());
    }

    private String id;
    private Uri url;
    private String objectType;
    private Date published;
    private String content;
    private String displayName;
    private String clientTempId;
    private String kmsMessage;

    public ActivityObject() {
    }

    public ActivityObject(String objectType) {
        this.objectType = objectType;
    }

    public String getId() {
        return id;
    }

    public ActivityObject setId(String id) {
        this.id = id;
        return this;
    }

    public Uri getUrl() {
        return url;
    }

    public ActivityObject setUri(Uri uri) {
        this.url = uri;
        return this;
    }

    public String getObjectType() {
        return objectType;
    }

    public ActivityObject setObjectType(String objectType) {
        this.objectType = objectType;
        return this;
    }

    public Date getPublished() {
        if (published != null)
            return published;
        return new Date(0);
    }

    public ActivityObject setPublished(Date published) {
        this.published = published;
        return this;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ActivityObject setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Creates a copy of the activity object with only the required fields to use when referencing it as part of an activity.
     * @return The skinny conversation
     */
    public ActivityObject getSkinnyObject() {
        ActivityObject object = new ActivityObject();
        object.setId(this.getId());
        object.url = this.url;
        object.setObjectType(this.getObjectType());
        return object;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ActivityObject)) return false;

        ActivityObject that = (ActivityObject) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public boolean isPerson() {
        return this.objectType.equals(ObjectType.person);
    }

    public boolean isConversation() {
        return this.objectType.equals(ObjectType.conversation);
    }

    public boolean isActivity() {
        return this.objectType.equals(ObjectType.activity);
    }

    public boolean isComment() {
        return this.objectType.equals(ObjectType.comment);
    }

    public boolean isContent() {
        return this.objectType.equals(ObjectType.content);
    }

    public boolean isLocus() {
        return this.objectType.equals(ObjectType.locus);
    }

    public boolean isEvent() {
        return this.objectType.equals(ObjectType.event);
    }

    public boolean isTeamObject() {
        return this.objectType.equals(ObjectType.team);
    }

    public boolean isLocusSessionSummary() {
        return this.objectType.equals(ObjectType.locusSessionSummary);
    }

    public String getClientTempId() {
        return clientTempId;
    }

    public void setClientTempId(String clientTempId) {
        this.clientTempId = clientTempId;
    }

    @Override
    public String toString() {
        if (objectType == null)
            return "empty";

        //Displayname can contain private stuff, mask it
        StringBuilder ret = new StringBuilder(objectType)
                .append(" name=char[").append((getDisplayName() == null) ? "0" : getDisplayName().length())
                .append("] id=").append(getId());

        return ret.toString();
    }

    public void encrypt(KeyObject key) throws IOException {
        if (Strings.notEmpty(content)) {
            content = CryptoUtils.encryptToJwe(key, content);
        }
    }

    public void decrypt(KeyObject key) throws IOException, ParseException, NullPointerException {
        if (CryptoUtils.looksLikeCipherText(content)) {
            content = CryptoUtils.decryptFromJwe(key, content);
        }
    }

    public String getEncryptedKmsMessage() {
        return kmsMessage;
    }

    public void setEncryptedKmsMessage(String encryptedKmsMessage) {
        this.kmsMessage = encryptedKmsMessage;
    }
}
