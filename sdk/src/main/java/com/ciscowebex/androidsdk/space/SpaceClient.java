/*
 * Copyright 2016-2019 Cisco Systems Inc
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

package com.ciscowebex.androidsdk.space;


import java.util.List;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ciscowebex.androidsdk.CompletionHandler;

/**
 * A client wrapper of the Cisco Webex <a href="https://developer.webex.com/resource-rooms.html">Spaces REST API</a>
 *
 * @since 0.1
 */
public interface SpaceClient {

    /**
     * Sort results by space ID (id), most recent activity (lastactivity), or most recently created (created).
     *
     * @since 0.1
     */
    enum SortBy {
        ID, LASTACTIVITY, CREATED;

        /**
         * Return serialized name
         *
         * @since 2.1.1
         */
        public String serializedName() {
            return super.name().toLowerCase();
        }
    }

    /**
     * Lists all spaces where the authenticated user belongs.
     *
     * @param teamId  If not nil, only list the spaces that are associated with the team by team id.
     * @param max     The maximum number of spaces in the response.
     * @param type    If not nil, only list the spaces of this type. Otherwise all spaces are listed.
     * @param sortBy  Sort results by space ID (id), most recent activity (lastactivity), or most recently created (created).
     * @param handler A closure to be executed once the request has finished.
     * @since 0.1
     */
    void list(@Nullable String teamId, int max, @Nullable Space.SpaceType type, @Nullable SortBy sortBy, @NonNull CompletionHandler<List<Space>> handler);

    /**
     * Creates a space. The authenticated user is automatically added as a member of the space. See the Memberships API to learn how to add more people to the space.
     *
     * @param title   A user-friendly name for the space.
     * @param teamId  If not nil, this space will be associated with the team by team id. Otherwise, this space is not associated with any team.
     * @param handler A closure to be executed once the request has finished.
     * @see com.ciscowebex.androidsdk.membership.MembershipClient
     * @since 0.1
     */
    void create(@NonNull String title, @Nullable String teamId, @NonNull CompletionHandler<Space> handler);

    /**
     * Retrieves the details for a space by id.
     *
     * @param spaceId  The identifier of the space.
     * @param handler The queue on which the completion handler is dispatched.
     * @since 0.1
     */
    void get(@NonNull String spaceId, @NonNull CompletionHandler<Space> handler);

    /**
     * Updates the details for a space by id.
     *
     * @param spaceId  The identifier of the space.
     * @param title   A user-friendly name for the space.
     * @param handler A closure to be executed once the request has finished.
     * @since 0.1
     */
    void update(@NonNull String spaceId, @NonNull String title, @NonNull CompletionHandler<Space> handler);

    /**
     * Deletes a space by id.
     *
     * @param spaceId The identifier of the space.
     * @param handler A closure to be executed once the request has finished.
     * @since 0.1
     */
    void delete(@NonNull String spaceId, @NonNull CompletionHandler<Void> handler);

    /**
     * Get space meeting details.
     * @param spaceId The identifier of the space.
     * @param handler A closure to be executed once the request has finished.
     * @since 2.2.0
     */
    void getMeeting(@NonNull String spaceId, @NonNull CompletionHandler<SpaceMeeting> handler);

    /**
     * Returns a single space object with details about the data of the last
     * activity in the space, and the date of the users last presence in the space.
     * For spaces where lastActivityDate > lastSeenDate the room can be considered to be "unread"
     *
     * @param spaceId The identifier of the space.
     * @param handler A closure to be executed once the request has finished.
     * @since 2.2.0
     */
    void getWithReadStatus(@NonNull String spaceId, @NonNull CompletionHandler<SpaceReadStatus> handler);
}
