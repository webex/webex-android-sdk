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

package com.ciscowebex.androidsdk.people;

import java.util.List;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ciscowebex.androidsdk.CompletionHandler;

/**
 * A client wrapper of the Cisco Webex <a href="https://developer.webex.com/docs/api/v1/people">People REST API</a>
 *
 * @since 0.1
 */
public interface PersonClient {

    /**
     * Lists people in the authenticated user's organization.
     *
     * @param email       List people with this email address. For non-admin requests, either this or displayName are required..
     * @param displayName List people whose name starts with this string. For non-admin requests, either this or email are required.
     * @param max         The maximum number of people in the response.
     * @param handler     A closure to be executed once the request has finished.
     * @since 0.1
     */
    void list(@NonNull String email, @Nullable String displayName, int max, @NonNull CompletionHandler<List<Person>> handler);

    /**
     * Lists people in the authenticated user's organization.
     *
     * @param email       List people with this email address. For non-admin requests, either this or displayName are required..
     * @param displayName List people whose name starts with this string. For non-admin requests, either this or email are required.
     * @param id          List people by ID. Accepts up to 85 person IDs separated by commas.
     * @param orgId       List people in this organization. Only admin users of another organization (such as partners) may use this parameter.
     * @param max         The maximum number of people in the response.
     * @param handler     A closure to be executed once the request has finished.
     * @since 2.1.1
     */
    void list(@Nullable String email, @Nullable String displayName, @Nullable String id, @Nullable String orgId, int max, @NonNull CompletionHandler<List<Person>> handler);

    /**
     * Retrieves the details for a person by person id.
     *
     * @param personId The identifier of the person.
     * @param handler  A closure to be executed once the request has finished.
     * @since 0.1
     */
    void get(@NonNull String personId, @NonNull CompletionHandler<Person> handler);

    /**
     * Retrieves the details for the authenticated user.
     *
     * @param handler A closure to be executed once the request has finished.
     * @since 0.1
     */
    void getMe(@NonNull CompletionHandler<Person> handler);

    /** Create people in the authenticated user's organization.
     *  Only admins are able to use this function
     *
     * @param email       Email address of the person.
     * @param displayName Full name of the person.
     * @param firstName   firstName name of the person.
     * @param lastName    lastName firstName name of the person.
     * @param avatar      URL to the person's avatar in PNG format.
     * @param orgId       ID of the organization to which this person belongs.
     * @param roles       Roles of the person.
     * @param licenses    Licenses allocated to the person.
     * @param handler     A closure to be executed once the request has finished.
     * @since 1.4
     */
    void create(@NonNull String email, @Nullable String displayName, @Nullable String firstName, @Nullable String lastName,
                @Nullable String avatar, @Nullable String orgId, @Nullable String roles, @Nullable String licenses, @NonNull CompletionHandler<Person> handler);

    /** Update people in the authenticated user's organization.
     *  Only admins are able to use this function
     *
     * @param personId    The identifier of the person.
     * @param email       Email address of the person.
     * @param displayName Full name of the person.
     * @param firstName   firstName name of the person.
     * @param lastName    lastName firstName name of the person.
     * @param avatar      URL to the person's avatar in PNG format.
     * @param orgId       ID of the organization to which this person belongs.
     * @param roles       Roles of the person.
     * @param licenses    Licenses allocated to the person.
     * @param handler     A closure to be executed once the request has finished.
     * @since 1.4
     */
    void update(@NonNull String personId, @Nullable String email, @Nullable String displayName, @Nullable String firstName, @Nullable String lastName,
                @Nullable String avatar, @Nullable String orgId, @Nullable String roles, @Nullable String licenses, @NonNull CompletionHandler<Person> handler);

    /** Delete the details of person by person id.
     *  Only admins are able to use this function
     *
     * @param personId    The identifier of the person.
     * @param handler     A closure to be executed once the request has finished.
     * @since 1.4
     */
    void delete(@NonNull String personId, @NonNull CompletionHandler<Void> handler);

}
