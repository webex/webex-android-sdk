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

import java.util.List;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ciscowebex.androidsdk.CompletionHandler;

/**
 * A client wrapper of the Cisco Webex <a href="https://developer.webex.com/resource-messages.html">Messages REST API</a>
 *
 * @since 0.1
 */
public interface MessageClient {

    /**
     * Lists all messages in a space by space Id. If present, it includes the associated media content attachment for each message.
     * <p>
     * The list sorts the messages in descending order by creation date.
     *
     * @param spaceId          The identifier of the space.
     * @param before          If not nil, only list messages sent only before this date and time, in ISO8601 format.
     * @param beforeMessage   If not nil, only list messages sent only before this message by id.
     * @param mentionedPeople If not nil, only list messages metion people.
     * @param max             The maximum number of messages in the response.
     * @param handler         A closure to be executed once the request has finished.
     * @since 0.1
     */
    void list(@NonNull String spaceId, @Nullable String before, @Nullable String beforeMessage, @Nullable String mentionedPeople, int max, @NonNull CompletionHandler<List<Message>> handler);

    /**
     * Posts a plain text message, and optionally, a media content attachment, to a space by space Id.
     *
     * @param spaceId      The identifier of the space where the message is to be posted.
     * @param personId    The identifier of the recipient of this private 1:1 message.
     * @param personEmail The email address of the recipient when sending a private 1:1 message.
     * @param text        The plain text message to be posted to the space.
     * @param markdown    The markdown text message to be posted to the space.
     * @param files       A public URL that Cisco Webex can use to fetch attachments. Currently supports only a single URL. Cisco Webex downloads the content from the URL one time shortly after the message is created and automatically converts it to a format that all Cisco Webex clients can render.
     * @param handler     A closure to be executed once the request has finished.
     * @since 0.1
     */
    void post(@Nullable String spaceId,
              @Nullable String personId,
              @Nullable String personEmail,
              @Nullable String text,
              @Nullable String markdown,
              @Nullable String[] files,
              @NonNull CompletionHandler<Message> handler);

    /**
     * @param idOrEmail     The identifier of the space or person or person email where the message is to be posted.
     * @param text          The plain text message to be posted to the space.
     * @param mentions      Mention people, if post to a person, this param should be null
     * @param files         Files to be posted
     * @param handler       A closure to be executed once the request has finished.
     * @since 1.4.0
     */
    void post(@NonNull String idOrEmail,
              @Nullable String text,
              @Nullable Mention[] mentions,
              @Nullable LocalFile[] files,
              @NonNull CompletionHandler<Message> handler);

    /**
     * @param observer
     * @since 1.4.0
     */
    void setMessageObserver(MessageObserver observer);

    /**
     * @param remoteFile        The RemoteFile object need to be downloaded.
     * @param to                The local file directory for saving downloaded file.
     * @param progressHandler   The download progress indicator.
     * @param completionHandler A closure to be executed when download completed.
     * @since 1.4.0
     */
    void downloadFile(RemoteFile remoteFile, String to, ProgressHandler progressHandler, CompletionHandler<Uri> completionHandler);

    /**
     * @param remoteFile        The RemoteFile object need to be downloaded.
     * @param to                The local file directory for saving downloaded file.
     * @param progressHandler   The download progress indicator.
     * @param completionHandler A closure to be executed when download completed.
     * @since 1.4.0
     */
    void downloadThumbnail(RemoteFile remoteFile, String to, ProgressHandler progressHandler, CompletionHandler<Uri> completionHandler);

    /**
     * Retrieves the details for a message by message Id.
     *
     * @param messageId The identifier of the message.
     * @param handler   A closure to be executed once the request has finished.
     * @since 0.1
     */
    void get(@NonNull String messageId, @NonNull CompletionHandler<Message> handler);

    /**
     * Deletes a message by message id.
     *
     * @param messageId The identifier of the message.
     * @param handler   A closure to be executed once the request has finished.
     * @since 0.1
     */
    void delete(@NonNull String messageId, @NonNull CompletionHandler<Void> handler);


    /**
     * Progress indicator interface
     * @since 1.4.0
     */

    public void postWithLocalFile(@NonNull String spaceId,@NonNull String text,@NonNull java.io.File file,  @NonNull CompletionHandler<Message> handler);

    /**
     * Progress indicator interface
     * @since 1.4.0
     */
    interface ProgressHandler {
        void onProgress(double percentage);
    }
}
