/*
 * Copyright (c) 2016-2017 Cisco Systems Inc
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

package com.ciscospark;

import com.cisco.spark.android.media.MediaEngine;
import com.ciscospark.auth.Authenticator;
import com.ciscospark.core.SparkApplication;
import com.ciscospark.membership.MembershipClient;
import com.ciscospark.message.MessageClient;
import com.ciscospark.people.PersonClient;
import com.ciscospark.phone.Phone;
import com.ciscospark.room.RoomClient;
import com.ciscospark.team.TeamClient;
import com.ciscospark.team.TeamMembershipClient;
import com.github.benoitdion.ln.DebugLn;
import com.github.benoitdion.ln.InfoLn;
import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.ReleaseLn;
import com.webex.wme.MediaSessionAPI;

import javax.inject.Inject;

import static com.ciscospark.Utils.checkNotNull;

/**
 * @author Allen Xiao<xionxiao@cisco.com>
 * @version 0.1
 */
public class Spark {
    private Authenticator authenticator;

    @Inject
    MediaEngine mediaEngine;

    /**
     * Log level of spark
     */
    public enum LogLevel {
        RELEASE,
        DEBUG,
        INFO,
    }

    public Spark(Authenticator authenticator) {
        this();
        setAuthenticator(authenticator);
    }

    public Spark() {
        if (SparkApplication.getInstance() != null) {
            // should throw exception
            SparkApplication.getInstance().inject(this);
        }
    }

    /**
     * Get current sdk version
     *
     * @return major.minor.build-alpha/beta
     */
    public String version() {
        return "0.1";
    }

    public void setAuthenticator(Authenticator authenticator) {
        checkNotNull(authenticator, "Authenticator is null");
        if (this.authenticator == null) {
            this.authenticator = authenticator;
        } else {
            deauthorize();
            this.authenticator = authenticator;
        }
    }

    public Authenticator getAuthenticator() {
        return this.authenticator;
    }

    public void authorize(CompletionHandler<String> listener) {
        authenticator.authorize(listener);
    }

    public void deauthorize() {
        authenticator.deauthorize();
    }

    public boolean isAuthorized() {
        return authenticator.isAuthorized();
    }

    public Phone phone() {
        return isAuthorized() ? new Phone(this) : null;
    }

    public MessageClient messages() {
        return isAuthorized() ? new MessageClient() : null;
    }

    public PersonClient people() {
        return isAuthorized() ? new PersonClient(this) : null;
    }

    public MembershipClient memberships() {
        return isAuthorized() ? new MembershipClient(this) : null;
    }

    public TeamClient teams() {
        return isAuthorized() ? new TeamClient(this) : null;
    }

    public TeamMembershipClient teamMembershipClient() {
        return isAuthorized() ? new TeamMembershipClient(this) : null;
    }

    public RoomClient rooms() {
        return isAuthorized() ? new RoomClient(this) : null;
    }


    /**
     * @param logLevel @enum LogLevel
     * @brief set log level of spark common-lib and wme
     */
    public void setLogLevel(LogLevel logLevel) {

        MediaSessionAPI.TraceLevelMask mask = MediaSessionAPI.TraceLevelMask.TRACE_LEVEL_MASK_INFO;

        switch (logLevel) {
            case DEBUG:
                Ln.initialize(new DebugLn());
                mask = MediaSessionAPI.TraceLevelMask.TRACE_LEVEL_MASK_DEBUG;
                break;
            case RELEASE:
                Ln.initialize(new ReleaseLn());
                mask = MediaSessionAPI.TraceLevelMask.TRACE_LEVEL_MASK_ERROR;
                break;
            case INFO:
                Ln.initialize(new InfoLn());
                mask = MediaSessionAPI.TraceLevelMask.TRACE_LEVEL_MASK_INFO;
                break;
        }
        if (mediaEngine.isInitialized()) {
            mediaEngine.setLoggingLevel(mask);
        }
    }
}
