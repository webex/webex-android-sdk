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

package com.ciscowebex.androidsdk.team;


import java.util.List;

import android.support.annotation.NonNull;

import com.ciscowebex.androidsdk.CompletionHandler;

/**
 * An client wrapper of the Cisco Webex <a href="https://developer.webex.com/docs/api/v1/teams">Teams REST API</a>
 *
 * @since 0.1
 */
public interface TeamClient {

    /**
     * Lists teams to which the authenticated user belongs.
     *
     * @param max     The maximum number of teams in the response.
     * @param handler A closure to be executed once the request has finished.
     * @since 0.1
     */
    void list(int max, @NonNull CompletionHandler<List<Team>> handler);

    /**
     * Creates a team. The authenticated user is automatically added as a member of the team.
     * <p>
     * See the Team Memberships API to learn how to add more people to the team.
     *
     * @param name    A user-friendly name for the team.
     * @param handler A closure to be executed once the request has finished.
     * @see TeamMembershipClient
     * @since 0.1
     */
    void create(@NonNull String name, @NonNull CompletionHandler<Team> handler);

    /**
     * Retrieves the details for a team by id.
     *
     * @param teamId  The identifier of the team.
     * @param handler A closure to be executed once the request has finished.
     * @since 0.1
     */
    void get(@NonNull String teamId, @NonNull CompletionHandler<Team> handler);

    /**
     * Updates the details for a team by id.
     *
     * @param teamId  The identifier of the team.
     * @param name    A user-friendly name for the team.
     * @param handler A closure to be executed once the request has finished.
     * @since 0.1
     */
    void update(@NonNull String teamId, String name, @NonNull CompletionHandler<Team> handler);

    /**
     * @param teamId  The identifier of the team.
     * @param handler A closure to be executed once the request has finished.
     * @since 0.1
     */
    void delete(@NonNull String teamId, @NonNull CompletionHandler<Void> handler);

}
