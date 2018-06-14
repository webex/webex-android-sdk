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

package com.ciscowebex.androidsdk.room;


import java.util.List;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ciscowebex.androidsdk.CompletionHandler;

/**
 * A client wrapper of the Cisco Webex <a href="https://developer.webex.com/resource-rooms.html">Rooms REST API</a>
 *
 * @since 0.1
 */
public interface RoomClient {

    /**
     * Sort results by room ID (id), most recent activity (lastactivity), or most recently created (created).
     *
     * @since 0.1
     */
    enum SortBy {
        ID, LASTACTIVITY, CREATED
    }

    /**
     * Lists all rooms where the authenticated user belongs.
     *
     * @param teamId  If not nil, only list the rooms that are associated with the team by team id.
     * @param max     The maximum number of rooms in the response.
     * @param type    If not nil, only list the rooms of this type. Otherwise all rooms are listed.
     * @param sortBy  Sort results by room ID (id), most recent activity (lastactivity), or most recently created (created).
     * @param handler A closure to be executed once the request has finished.
     * @since 0.1
     */
    void list(@Nullable String teamId, int max, @Nullable Room.RoomType type, @Nullable SortBy sortBy, @NonNull CompletionHandler<List<Room>> handler);

    /**
     * Creates a room. The authenticated user is automatically added as a member of the room. See the Memberships API to learn how to add more people to the room.
     *
     * @param title   A user-friendly name for the room.
     * @param teamId  If not nil, this room will be associated with the team by team id. Otherwise, this room is not associated with any team.
     * @param handler A closure to be executed once the request has finished.
     * @see com.ciscowebex.androidsdk.membership.MembershipClient
     * @since 0.1
     */
    void create(@NonNull String title, @Nullable String teamId, @NonNull CompletionHandler<Room> handler);

    /**
     * Retrieves the details for a room by id.
     *
     * @param roomId  The identifier of the room.
     * @param handler The queue on which the completion handler is dispatched.
     * @since 0.1
     */
    void get(@NonNull String roomId, @NonNull CompletionHandler<Room> handler);

    /**
     * Updates the details for a room by id.
     *
     * @param roomId  The identifier of the room.
     * @param title   A user-friendly name for the room.
     * @param handler A closure to be executed once the request has finished.
     * @since 0.1
     */
    void update(@NonNull String roomId, @NonNull String title, @NonNull CompletionHandler<Room> handler);

    /**
     * Deletes a room by id.
     *
     * @param roomId  The identifier of the room.
     * @param handler A closure to be executed once the request has finished.
     * @since 0.1
     */
    void delete(@NonNull String roomId, @NonNull CompletionHandler<Void> handler);

}
