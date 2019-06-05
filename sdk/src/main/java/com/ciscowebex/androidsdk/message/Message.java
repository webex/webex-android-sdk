/*
 * Copyright 2016-2017 Cisco Systems Inc
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

package com.ciscowebex.androidsdk.message;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.cisco.spark.android.model.AuthenticatedUser;
import com.cisco.spark.android.model.ItemCollection;
import com.cisco.spark.android.model.Person;
import com.cisco.spark.android.model.SpaceProperty;
import com.cisco.spark.android.model.conversation.Activity;
import com.cisco.spark.android.model.conversation.Content;
import com.cisco.spark.android.model.conversation.File;
import com.ciscowebex.androidsdk.message.internal.RemoteFileImpl;
import com.ciscowebex.androidsdk.message.internal.WebexId;
import com.ciscowebex.androidsdk.space.Space;
import com.google.gson.Gson;

/**
 * This class represents a Message on Cisco Webex.
 *
 * @since 0.1
 */
public class Message {

    protected Activity activity;

    private String id;

    private String personId;

    private String spaceId;

    private Space.SpaceType spaceType;

    private String toPersonId;

    private String toPersonEmail;

    private boolean isSelfMentioned;

    private List<RemoteFile> remoteFiles;

    protected Message(Activity activity, AuthenticatedUser user, boolean received) {
        this.activity = activity;
        this.id = new WebexId(WebexId.Type.MESSAGE_ID, activity.getId()).toHydraId();
        if (activity.getActor() != null) {
            this.personId = new WebexId(WebexId.Type.PEOPLE_ID, activity.getActor().getId()).toHydraId();
        }
        if (activity.getTarget() instanceof SpaceProperty) {
            this.spaceId = new WebexId(WebexId.Type.ROOM_ID, activity.getTarget().getId()).toHydraId();
            this.spaceType = ((SpaceProperty)activity.getTarget()).getTags().contains("ONE_ON_ONE") ? Space.SpaceType.DIRECT : Space.SpaceType.GROUP;
        }
        else if (activity.getTarget() instanceof Person) {
            this.toPersonId = new WebexId(WebexId.Type.PEOPLE_ID, activity.getTarget().getId()).toHydraId();
            this.toPersonEmail = ((Person) activity.getTarget()).getEmail();
        }
        if (this.spaceId == null) {
            this.spaceId = new WebexId(WebexId.Type.ROOM_ID, activity.getConversationId()).toHydraId();
        }
        if (user != null) {
            if (this.toPersonId == null && received) {
                this.toPersonId = new WebexId(WebexId.Type.PEOPLE_ID, user.getUserId()).toHydraId();
            }
            if (this.toPersonEmail == null && received) {
                this.toPersonEmail = user.getEmail();
            }
            this.isSelfMentioned = activity.isSelfMention(user, 0);
        }

        ArrayList<RemoteFile> remoteFiles = new ArrayList<>();
        if (activity.getObject().isContent()) {
            Content content = (Content) activity.getObject();
            ItemCollection<File> files = content.getContentFiles();
            for (File file : files.getItems()) {
                RemoteFile remoteFile = new RemoteFileImpl(file);
                remoteFiles.add(remoteFile);
            }
        }
        this.remoteFiles = remoteFiles;
    }

    /**
     * Returns The identifier of this message.
     * @return The identifier of this message.
     * @since 0.1
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the identifier of the person who sent this message.
     * @return The identifier of the person who sent this message.
     * @since 0.1
     */
    public String getPersonId() {
        return personId;
    }

    /**
     * Returns the email address of the person who sent this message.
     * @return The email address of the person who sent this message.
     * @since 0.1
     */
    public String getPersonEmail() {
        return activity.getActor() != null ? activity.getActor().getEmail() : null;
    }

    /**
     * Returns the identifier of the space where this message was posted.
     * @return The identifier of the space where this message was posted.
     * @since 0.1
     */
    public String getSpaceId() {
        return spaceId;
    }

    /**
     * @return The type of the space where this message was posted.
     * @since 0.1
     */
    public Space.SpaceType getSpaceType() {
        return spaceType;
    }

    /**
     * Returns the content of the message.
     * @return The content of the message.
     * @since 0.1
     */
    public String getText() {
        if (activity.getObject().getContent() != null) {
            return activity.getObject().getContent();
        }
        else if (activity.getObject().getDisplayName() != null) {
            return activity.getObject().getDisplayName();
        }
        return null;
    }

    /**
     * Returns the identifier of the recipient when sending a private 1:1 message.
     * @return The identifier of the recipient when sending a private 1:1 message.
     * @since 0.1
     */
    public String getToPersonId() {
        return toPersonId;
    }

    /**
     * Returns the email address of the recipient when sending a private 1:1 message
     * @return The email address of the recipient when sending a private 1:1 message.
     * @since 0.1
     */
    public String getToPersonEmail() {
        return toPersonEmail;
    }

    /**
     * Returns the {@link java.util.Date} that the message being created.
     * @return The {@link java.util.Date} that the message being created.
     * @since 0.1
     */
    public Date getCreated() {
        return activity.getPublished();
    }

    /**
     * Returns true if the message is the recepient of the message is included in message's mention list
     * @return True if the message is the recepient of the message is included in message's mention list
     */
    public boolean isSelfMentioned() {
        return this.isSelfMentioned;
    }

    /**
     * Returns a list of files attached to this message.
     * @return A list of files attached to this message.
     *
     * @deprecated
     */
    @Deprecated
    public List<RemoteFile> getRemoteFiles() {
        return getFiles();
    }

    /**
     * Return a list of files attached to this message.
     * @return A list of files attached to this message.
     *
     * @since 2.1.0
     */
    public List<RemoteFile> getFiles() {
        return this.remoteFiles;
    }

    /**
     * Returns the message in JSON string format.
     * @return the message in JSON string format.
     */
    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
