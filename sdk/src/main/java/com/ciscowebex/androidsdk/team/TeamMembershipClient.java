/*
 * Copyright 2016-2021 Cisco Systems Inc
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

package com.ciscowebex.androidsdk.team;

import java.util.List;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ciscowebex.androidsdk.CompletionHandler;

/**
 * A client wrapper of the Cisco Webex <a href="https://developer.webex.com/docs/api/v1/team-memberships">TeamMemberships REST API</a>
 *
 * @since 0.1
 */
public interface TeamMembershipClient {

    /**
     * Lists all team memberships where the authenticated user belongs.
     *
     * @param teamId  Limit results to a specific team, by ID.
     * @param max     The maximum number of team memberships in the response.
     * @param handler A closure to be executed once the request has finished.
     * @since 0.1
     */
    void list(@Nullable String teamId, int max, @NonNull CompletionHandler<List<TeamMembership>> handler);

    /**
     * Adds a person to a team by person id; optionally making the person a moderator of the team.
     *
     * @param teamId      The identifier of the team.
     * @param personId    The identifier of the person.
     * @param personEmail The email of the person.
     * @param isModerator If true, make the person a moderator of the team. The default is false.
     * @param handler     A closure to be executed once the request has finished.
     * @since 0.1
     */
    void create(@NonNull String teamId, @Nullable String personId, @Nullable String personEmail, boolean isModerator, @NonNull CompletionHandler<TeamMembership> handler);

    /**
     * Retrieves the details for a membership by id.
     *
     * @param membershipId The identifier of the membership.
     * @param handler      A closure to be executed once the request has finished.
     * @since 0.1
     */
    void get(@NonNull String membershipId, @NonNull CompletionHandler<TeamMembership> handler);

    /**
     * Updates the details for a membership by id.
     *
     * @param membershipId The identifier of the membership.
     * @param isModerator  If true, make the person a moderator of the team. The default is false.
     * @param handler      A closure to be executed once the request has finished.
     * @since 0.1
     */
    void update(@NonNull String membershipId, boolean isModerator, @NonNull CompletionHandler<TeamMembership> handler);

    /**
     * Deletes a membership by id.
     *
     * @param membershipId The identifier of the membership.
     * @param handler      A closure to be executed once the request has finished.
     * @since 0.1
     */
    void delete(@NonNull String membershipId, @NonNull CompletionHandler<Void> handler);

}
