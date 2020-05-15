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

package com.ciscowebex.androidsdk.message;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.support.annotation.NonNull;
import com.ciscowebex.androidsdk.internal.Credentials;
import com.ciscowebex.androidsdk.internal.model.*;
import com.ciscowebex.androidsdk.message.internal.DraftImpl;
import com.ciscowebex.androidsdk.message.internal.RemoteFileImpl;
import com.ciscowebex.androidsdk.utils.Utils;
import com.ciscowebex.androidsdk.utils.WebexId;
import com.ciscowebex.androidsdk.space.Space;
import com.google.gson.Gson;

/**
 * This class represents a Message on Cisco Webex.
 *
 * @since 0.1
 */
public class Message {

    /**
     * Write a draft to be posted.
     *
     * @param text The content of draft to be posted. The content can be plain text, html, or markdown.
     * @since 2.5.0
     */
    public static Draft draft(Text text) {
        return new DraftImpl(text);
    }

    /**
     * Encapsulating the content will be post. A draft is created by {@link Message#draft(Text)}}
     *
     * @since 2.5.0
     */
    public interface Draft {

        /**
         * Attach local files to the draft.
         *
         * @param files Local files to be attached with the draft.
         * @since 2.5.0
         */
        Draft addAttachments(LocalFile... files);

        /**
         * Mention either one people or all people in a space.
         *
         * @param mentions Notify either one or all in a space about this message.
         * @since 2.5.0
         */
        Draft addMentions(Mention... mentions);

        /**
         * Set the parent message of the threading message.
         *
         * @param parent The parent message of the threading message.
         * @since 2.5.0
         */
        Draft setParent(Message parent);
    }

    /**
     * The wrapper for the message text in different formats: plain text, markdown, and html.
     * Please note this version of the SDK requires the application to convert markdown to html.
     * Future version of the SDK will provide auto conversion from markdown to html.
     *
     * @since 2.3.0
     */
    public static class Text {

        /**
         * Make a Text object for the plain text.
         *
         * @param plain the plain text.
         */
        public static Text plain(String plain) {
            return new Text(plain, null, null);
        }

        /**
         * Make a Text object for the html.
         *
         * @param html the text with the html markup.
         * @param plain the alternate plain text for cases that do not support html markup.
         */
        public static Text html(String html, String plain) {
            return new Text(plain, html, null);
        }

        /**
         * Make a Text object for the markdown.
         *
         * @param markdown the text with the markdown markup.
         * @param html the html text for how to render the markdown. This will be optional in the future.
         * @param plain the alternate plain text for cases that do not support markdown and html markup.
         */
        public static Text markdown(String markdown, String html, String plain) {
            return new Text(plain, html, markdown);
        }

        private String plain;

        private String html;

        private String markdown;

        private Text(String plain, String html, String markdown) {
            this.plain = plain;
            this.html = html;
            this.markdown = markdown;
        }

        private Text(@NonNull ObjectModel object) {
            this.plain = object.getDisplayName();
            this.html = object.getContent();
            if (object instanceof MarkdownableModel) {
                this.markdown = ((MarkdownableModel) object).getMarkdown();
            }
        }

        /**
         * Returns the markdown if exist.
         */
        public String getMarkdown() {
            return markdown;
        }

        /**
         * Returns the html if exist.
         *
         */
        public String getHtml() {
            return html;
        }

        /**
         * Returns the plain text if exist
         */
        public String getPlain() {
            return plain;
        }
    }

    private transient ActivityModel activity;

    private String id;

    private String personId;

    private String personEmail;

    private String personDisplayName;

    private String spaceId;

    private Space.SpaceType spaceType;

    private String toPersonId;

    private String toPersonEmail;

    private boolean isSelfMentioned;

    private Text textAsObject;

    private transient List<RemoteFile> remoteFiles;

    private ParentModel parent;

    protected Message(ActivityModel activity, Credentials user, boolean received) {
        this.activity = activity;
        this.id = new WebexId(WebexId.Type.MESSAGE_ID, activity.getId()).toHydraId();
        if (activity.getVerb().equals(ActivityModel.Verb.delete) && activity.getObject() != null) {
            this.id = new WebexId(WebexId.Type.MESSAGE_ID, activity.getObject().getId()).toHydraId();
        }
        if (activity.getActor() != null) {
            this.personId = new WebexId(WebexId.Type.PEOPLE_ID, activity.getActor().getId()).toHydraId();
            this.personEmail = activity.getActor().getEmail();
            this.personDisplayName = activity.getActor().getDisplayName();
        }
        if (activity.getObject() != null) {
            this.textAsObject = new Text(activity.getObject());
        }
        if (activity.getTarget() instanceof ConversationModel) {
            this.spaceId = new WebexId(WebexId.Type.ROOM_ID, activity.getTarget().getId()).toHydraId();
            this.spaceType = ((ConversationModel) activity.getTarget()).isOneOnOne() ? Space.SpaceType.DIRECT : Space.SpaceType.GROUP;
        } else if (activity.getTarget() instanceof SpacePropertyModel) {
            this.spaceId = new WebexId(WebexId.Type.ROOM_ID, activity.getTarget().getId()).toHydraId();
            this.spaceType = ((ConversationModel) activity.getTarget()).getTags().contains("ONE_ON_ONE") ? Space.SpaceType.DIRECT : Space.SpaceType.GROUP;
        } else if (activity.getTarget() instanceof PersonModel) {
            this.spaceType = Space.SpaceType.DIRECT;
            this.toPersonId = new WebexId(WebexId.Type.PEOPLE_ID, activity.getTarget().getId()).toHydraId();
            this.toPersonEmail = ((PersonModel) activity.getTarget()).getEmail();
        }
        if (this.spaceId == null) {
            this.spaceId = new WebexId(WebexId.Type.ROOM_ID, activity.getConversationId()).toHydraId();
        }
        if (user != null) {
            if (this.toPersonId == null && received) {
                this.toPersonId = new WebexId(WebexId.Type.PEOPLE_ID, user.getUserId()).toHydraId();
            }
            if (this.toPersonEmail == null && received && user.getPerson() != null) {
                this.toPersonEmail = Utils.getFirst(user.getPerson().getEmails());
            }
            this.isSelfMentioned = activity.isSelfMention(user, 0);
        }

        ArrayList<RemoteFile> remoteFiles = new ArrayList<>();
        if (activity.getObject() != null && activity.getObject().isContent()) {
            ContentModel content = (ContentModel) activity.getObject();
            ItemsModel<FileModel> files = content.getContentFiles();
            for (FileModel file : files.getItems()) {
                RemoteFile remoteFile = new RemoteFileImpl(file);
                remoteFiles.add(remoteFile);
            }
        }
        this.remoteFiles = remoteFiles;
        this.parent = activity.getParent();
    }

    /**
     * Returns The identifier of this message.
     *
     * @return The identifier of this message.
     * @since 0.1
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the identifier of the person who sent this message.
     *
     * @return The identifier of the person who sent this message.
     * @since 0.1
     */
    public String getPersonId() {
        return personId;
    }

    /**
     * Returns the email address of the person who sent this message.
     *
     * @return The email address of the person who sent this message.
     * @since 0.1
     */
    public String getPersonEmail() {
        return personEmail;
    }

    /**
     * Returns the name of the person who sent this message.
     *
     * @return The name of the person who sent this message.
     * @since 2.3.0
     */
    public String getPersonDisplayName() {
        return personDisplayName;
    }

    /**
     * Returns the identifier of the space where this message was posted.
     *
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
     *
     * @return The content of the message.
     * @since 0.1
     */
    public String getText() {
        if (textAsObject == null) {
            return null;
        }
        String formatedText = textAsObject.getHtml();
        if (formatedText != null) {
            return formatedText;
        }
        return textAsObject.getMarkdown() != null ? textAsObject.getMarkdown() : textAsObject.getPlain();
    }

    /**
     * Returns the content of the message in as {@link Message.Text} object.
     *
     * @return The content of the message.
     * @since 2.3.0
     */
    public Text getTextAsObject() {
        return textAsObject;
    }

    /**
     * Returns the identifier of the recipient when sending a private 1:1 message.
     *
     * @return The identifier of the recipient when sending a private 1:1 message.
     * @since 0.1
     */
    public String getToPersonId() {
        return toPersonId;
    }

    /**
     * Returns the email address of the recipient when sending a private 1:1 message
     *
     * @return The email address of the recipient when sending a private 1:1 message.
     * @since 0.1
     */
    public String getToPersonEmail() {
        return toPersonEmail;
    }

    /**
     * Returns the {@link java.util.Date} that the message being created.
     *
     * @return The {@link java.util.Date} that the message being created.
     * @since 0.1
     */
    public Date getCreated() {
        return activity.getPublished();
    }

    /**
     * Returns true if the message is the recepient of the message is included in message's mention list
     *
     * @return True if the message is the recepient of the message is included in message's mention list
     */
    public boolean isSelfMentioned() {
        return this.isSelfMentioned;
    }

    /**
     * Returns a list of files attached to this message.
     *
     * @return A list of files attached to this message.
     * @deprecated
     */
    @Deprecated
    public List<RemoteFile> getRemoteFiles() {
        return getFiles();
    }

    /**
     * Return a list of files attached to this message.
     *
     * @return A list of files attached to this message.
     * @since 2.1.0
     */
    public List<RemoteFile> getFiles() {
        return this.remoteFiles;
    }

    /**
     * Update the file list of message.
     *
     * @param files The new remote files
     * @since 2.6.0
     */
    public void setFiles(List<RemoteFile> files) {
        this.remoteFiles = files;
    }

    /**
     * Checks if this is a reply message.
     *
     * @return true if this is a reply message.
     * @since 2.5.0
     */
    public boolean isReply() {
        return activity.isReply();
    }

    /**
     * Returns the parent if this message is a reply message.
     *
     * @return the parent or null if not reply message.
     * @since 2.5.0
     */
    public String getParentId() {
        return parent == null ? null : new WebexId(WebexId.Type.MESSAGE_ID, parent.getId()).toHydraId();
    }

    /**
     * Returns the message in JSON string format.
     *
     * @return the message in JSON string format.
     */
    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
