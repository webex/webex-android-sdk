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

package com.ciscospark.androidsdk;

import javax.inject.Inject;

import android.app.Application;
import com.cisco.spark.android.media.MediaEngine;
import com.ciscospark.androidsdk.auth.Authenticator;
import com.ciscospark.androidsdk.core.SparkInjector;
import com.ciscospark.androidsdk.membership.MembershipClient;
import com.ciscospark.androidsdk.message.MessageClient;
import com.ciscospark.androidsdk.people.PersonClient;
import com.ciscospark.androidsdk.phone.Phone;
import com.ciscospark.androidsdk.phone.internal.PhoneImpl;
import com.ciscospark.androidsdk.room.RoomClient;
import com.ciscospark.androidsdk.team.TeamClient;
import com.ciscospark.androidsdk.team.TeamMembershipClient;
import com.ciscospark.androidsdk.utils.log.NoLn;
import com.ciscospark.androidsdk.utils.log.WarningLn;
import com.ciscospark.androidsdk.webhook.WebhookClient;
import com.github.benoitdion.ln.DebugLn;
import com.github.benoitdion.ln.InfoLn;
import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.ReleaseLn;
import com.webex.wme.MediaSessionAPI;

/**
 * @author Allen Xiao<xionxiao@cisco.com>
 * @version 0.1
 */
public class Spark {

    public enum LogLevel {
        NO,
        ERROR,
        WARNING,
        INFO,
        DEBUG,
        VERBOSE
    }

    private LogLevel _logLevel = LogLevel.DEBUG;

    private SparkInjector _injector;

    private Authenticator _authenticator;

    private Phone _phone;

    @Inject
    MediaEngine _mediaEngine;

    public Spark(Application application, Authenticator authenticator) {
        com.cisco.spark.android.core.Application.setApplication(application);
        _authenticator = authenticator;
        _injector = new SparkInjector(application);
        _injector.create();
        _injector.inject(this);
        _injector.inject(_authenticator);
        _phone = new PhoneImpl(application.getApplicationContext(), _authenticator, _injector);
    }

    /**
     * Get current sdk version
     *
     * @return major.minor.build-alpha/beta
     */
    public String version() {
        return "0.1";
    }

    public Authenticator getAuthenticator() {
        return _authenticator;
    }

    public Phone phone() {
        return _phone;
    }

    public MessageClient messages() {
        return new MessageClient(this._authenticator);
    }

    public PersonClient people() {
        return new PersonClient(this._authenticator);
    }

    public MembershipClient memberships() {
        return new MembershipClient(this._authenticator);
    }

    public TeamClient teams() {
        return new TeamClient(this._authenticator);
    }

    public TeamMembershipClient teamMembershipClient() {
        return new TeamMembershipClient(this._authenticator);
    }

    public WebhookClient webhooks() {
        return new WebhookClient(this._authenticator);
    }

    public RoomClient rooms() {
        return new RoomClient(this._authenticator);
    }

    public void setLogLevel(LogLevel logLevel) {
        _logLevel = logLevel;
        switch (logLevel) {
            case NO:
                Ln.initialize(new NoLn());
                _mediaEngine.setLoggingLevel(MediaSessionAPI.TraceLevelMask.TRACE_LEVEL_MASK_NOTRACE);
                break;
            case ERROR:
                Ln.initialize(new ReleaseLn());
                _mediaEngine.setLoggingLevel(MediaSessionAPI.TraceLevelMask.TRACE_LEVEL_MASK_ERROR);
                break;
            case WARNING:
                Ln.initialize(new WarningLn());
                _mediaEngine.setLoggingLevel(MediaSessionAPI.TraceLevelMask.TRACE_LEVEL_MASK_WARNING);
                break;
            case INFO:
                Ln.initialize(new InfoLn());
                _mediaEngine.setLoggingLevel(MediaSessionAPI.TraceLevelMask.TRACE_LEVEL_MASK_INFO);
                break;
            case DEBUG:
                Ln.initialize(new com.ciscospark.androidsdk.utils.log.DebugLn());
                _mediaEngine.setLoggingLevel(MediaSessionAPI.TraceLevelMask.TRACE_LEVEL_MASK_DEBUG);
                break;
            case VERBOSE:
                Ln.initialize(new DebugLn());
                _mediaEngine.setLoggingLevel(MediaSessionAPI.TraceLevelMask.TRACE_LEVEL_MASK_DETAIL);
                break;
        }
    }
}
