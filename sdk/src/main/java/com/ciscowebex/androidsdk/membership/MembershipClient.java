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

package com.ciscowebex.androidsdk.membership;

import java.util.List;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ciscowebex.androidsdk.CompletionHandler;

/**
 * A client wrapper of the Cisco Webex <a href="https://developer.webex.com/resource-memberships.html">Space Memberships REST API</a>
 *
 * @since 0.1
 */
public interface MembershipClient {

    /**
     * Sets a {@link MembershipObserver} in this client.
     *
     * @param observer the callback to receive events about membership changes.
     * @see MembershipObserver
     * @since 2.3.0
     */
    void setMembershipObserver(MembershipObserver observer);

    /**
     * Lists all space memberships where the authenticated user belongs.
     *
     * @param spaceId      The identifier of the space where the membership belongs.
     * @param personId    The identifier of the person who has the memberships.
     * @param personEmail The email address of the person who has the memberships.
     * @param max         The maximum number of items in the response.
     * @param handler     A closure to be executed once the request has finished.
     * @since 0.1
     */
    void list(@Nullable String spaceId, @Nullable String personId, @Nullable String personEmail, int max, @NonNull CompletionHandler<List<Membership>> handler);

    /**
     * Adds a person to a space by person id; optionally making the person a moderator.
     *
     * @param spaceId      The identifier of the space where the person is to be added.
     * @param personId    The identifier of the person to be added.
     * @param personEmail The email address of the person to be added.
     * @param isModerator If true, make the person a moderator of the space. The default is false.
     * @param handler     A closure to be executed once the request has finished.
     * @since 0.1
     */
    void create(@NonNull String spaceId, @Nullable String personId, @Nullable String personEmail, boolean isModerator, @NonNull CompletionHandler<Membership> handler);

    /**
     * Retrieves the details for a membership by membership id.
     *
     * @param membershipId The identifier of the membership.
     * @param handler      A closure to be executed once the request has finished.
     * @since 0.1
     */
    void get(@NonNull String membershipId, @NonNull CompletionHandler<Membership> handler);

    /**
     * Updates the properties of a membership by membership id.
     *
     * @param membershipId The identifier of the membership.
     * @param isModerator  If true, make the person a moderator of the space in this membership. The default is false.
     * @param handler      A closure to be executed once the request has finished.
     * @since 0.1
     */
    void update(@NonNull String membershipId, boolean isModerator, @NonNull CompletionHandler<Membership> handler);

    /**
     * Deletes a membership by membership id. It removes the person from the space where the membership belongs.
     *
     * @param membershipId The identifier of the membership.
     * @param handler      A closure to be executed once the request has finished.
     * @since 0.1
     */
    void delete(@NonNull String membershipId, @NonNull CompletionHandler<Void> handler);

    /**
     * Returns a list of memberships with details about the lastSeenId for each user so that application can tell
     * which message was the last message was read by each user.
     * @param spaceId The identifier of the space.
     * @param handler A closure to be executed once the request has finished.
     * @see MembershipReadStatus
     * @since 2.3.0
     */
    void listWithReadStatus(@NonNull String spaceId, @NonNull CompletionHandler<List<MembershipReadStatus>> handler);


}
