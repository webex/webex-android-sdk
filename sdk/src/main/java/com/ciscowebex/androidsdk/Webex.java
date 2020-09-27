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

package com.ciscowebex.androidsdk;

import android.app.Application;

import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.internal.reachability.BackgroundChecker;
import com.ciscowebex.androidsdk.internal.reachability.ForegroundChecker;
import com.ciscowebex.androidsdk.internal.Settings;
import com.ciscowebex.androidsdk.membership.MembershipClient;
import com.ciscowebex.androidsdk.membership.internal.MembershipClientImpl;
import com.ciscowebex.androidsdk.message.MessageClient;
import com.ciscowebex.androidsdk.message.internal.MessageClientImpl;
import com.ciscowebex.androidsdk.people.PersonClient;
import com.ciscowebex.androidsdk.people.internal.PersonClientImpl;
import com.ciscowebex.androidsdk.phone.Phone;
import com.ciscowebex.androidsdk.phone.internal.MediaEngine;
import com.ciscowebex.androidsdk.phone.internal.PhoneImpl;
import com.ciscowebex.androidsdk.space.SpaceClient;
import com.ciscowebex.androidsdk.space.internal.SpaceClientImpl;
import com.ciscowebex.androidsdk.team.TeamClient;
import com.ciscowebex.androidsdk.team.TeamMembershipClient;
import com.ciscowebex.androidsdk.team.internal.TeamClientImpl;
import com.ciscowebex.androidsdk.team.internal.TeamMembershipClientImpl;
import com.ciscowebex.androidsdk.utils.UserAgent;
import com.ciscowebex.androidsdk.utils.Utils;
import com.ciscowebex.androidsdk.utils.http.HttpClient;
import com.ciscowebex.androidsdk.webhook.WebhookClient;
import com.ciscowebex.androidsdk.webhook.internal.WebhookClientImpl;
import com.github.benoitdion.ln.Ln;
import me.helloworld.utils.reflect.Methods;

/**
 * Webex object is the entry point to use this Cisco Webex Android SDK.
 *
 * @since 0.1
 */
public class Webex {

    public static final String APP_NAME = "webex_android_sdk";

    public static final String APP_VERSION = BuildConfig.VERSION_NAME + "(" + BuildConfig.BUILD_TIME + "_" + BuildConfig.BUILD_REVISION + ")";

    /**
     * The enumeration of log message level
     *
     * @since 0.1
     */
    public enum LogLevel {
        NO, ERROR, WARNING, INFO, DEBUG_NO_HTTP_DETAILS, DEBUG, VERBOSE, ALL
    }

    private Authenticator authenticator;

    private PhoneImpl phone;

    private MessageClientImpl messages;

    private MembershipClientImpl memberships;

    private SpaceClientImpl spaces;

    private MediaEngine engine;

    private final BackgroundChecker checker;

    /**
     * Constructs a new Webex object with an {@link Authenticator} and Application
     *
     * @param application   The android application
     * @param authenticator The authentication strategy for this SDK
     * @since 0.1
     */
    public Webex(Application application, Authenticator authenticator) {
        Settings.shared.init(application.getApplicationContext());
        ForegroundChecker.init(application);
        try {
            Methods.invoke(authenticator, "afterAssociated", (Object[]) null);
        } catch (Throwable t) {
            Ln.d("Authenticator doest't support afterAssociated method");
        }
        this.authenticator = authenticator;
        engine = new MediaEngine(application.getApplicationContext(), LogLevel.DEBUG);
        phone = new PhoneImpl(application.getApplicationContext(), this, authenticator, engine);
        messages = new MessageClientImpl(application.getApplicationContext(), phone);
        memberships = new MembershipClientImpl(phone);
        spaces = new SpaceClientImpl(phone);
        checker = new BackgroundChecker(application, phone);
        phone.setChecker(checker);
        setLogLevel(LogLevel.DEBUG);
        Ln.i(UserAgent.value);
    }

    /**
     * Get current SDK version
     *
     * @return major.minor.build-alpha/beta/SNAPSHOT
     * @since 0.1
     */
    public String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    /**
     * Invoke this method when the application switches between background and foreground.
     *
     * @param background application run in background or not.
     * @since 0.1
     */
    public void runInBackground(boolean background) {
        if (background) {
            checker.tryBackground();
        } else {
            checker.tryForeground();
        }
    }

    /**
     * @return The {@link Authenticator} object from the application when constructing this Webex object. It can be used to check and modify authentication state.
     * @since 0.1
     */
    public Authenticator getAuthenticator() {
        return authenticator;
    }

    /**
     * {@link Phone} can be used to make audio and video calls on Cisco Webex.
     *
     * @return The {@link Phone} represents a calling device in Cisco Webex Android SDK.
     * @since 0.1
     */
    public Phone phone() {
        return phone;
    }

    /**
     * Messages are how we communicate in a space.
     *
     * @return The {@link MessageClient} is uesd to manage the messages on behalf of the authenticated user.
     * @see SpaceClient
     * @see MembershipClient
     * @since 0.1
     */
    public MessageClient messages() {
        return messages;
    }

    /**
     * People are registered users of Cisco Webex.
     *
     * @return The {@link PersonClient} is used to find a person on behalf of the authenticated user.
     * @see MembershipClient
     * @see MessageClient
     * @since 0.1
     */
    public PersonClient people() {
        return new PersonClientImpl(this.authenticator);
    }

    /**
     * Memberships represent a person's relationships to spaces.
     *
     * @return The {@link MembershipClient} is used to manage the authenticated user's relationship to spaces.
     * @see SpaceClient
     * @see MessageClient
     * @since 0.1
     */
    public MembershipClient memberships() {
        return memberships;
    }

    /**
     * Teams are groups of people with a set of spaces that are visible to all members of that team.
     *
     * @return The {@link TeamClient} is used to create and manage the teams on behalf of the authenticated user.
     * @see TeamMembershipClient
     * @see MembershipClient
     * @since 0.1
     */
    public TeamClient teams() {
        return new TeamClientImpl(this.authenticator);
    }

    /**
     * Team Memberships represent a person's relationships to teams.
     *
     * @return The {@link TeamMembershipClient} is used to create and manage the team membership on behalf of the authenticated user.
     * @see TeamClient
     * @see SpaceClient
     * @since 0.1
     */
    public TeamMembershipClient teamMembershipClient() {
        return new TeamMembershipClientImpl(this.authenticator);
    }

    /**
     * Webhooks allow the application to be notified via HTTP (or HTTPS?) when a specific event occurs in Cisco Webex, e.g. a new message is posted into a specific space.
     *
     * @return The {@link WebhookClient} is used to create and manage the webhooks for specific events.
     * @since 0.1
     */
    public WebhookClient webhooks() {
        return new WebhookClientImpl(this.authenticator);
    }

    /**
     * Spaces are virtual meeting places in Cisco Webex where people post messages and collaborate to get work done.
     *
     * @return The {@link SpaceClient} is used to manage the spaces on behalf of the authenticated user.
     * @see MembershipClient
     * @see MessageClient
     * @since 0.1
     */
    public SpaceClient spaces() {
        return spaces;
    }

    /**
     * Set the log level of the logging.
     *
     * @param logLevel log message level
     */
    public void setLogLevel(LogLevel logLevel) {
        Ln.initialize(Utils.toLnLog(logLevel));
        HttpClient.setLogLevel(Utils.toHttpLogLevel(logLevel));
        engine.setLoggingLevel(logLevel);
    }


}
