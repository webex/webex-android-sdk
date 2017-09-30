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
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ProviderInfo;
import com.cisco.spark.android.core.BackgroundCheck;
import com.cisco.spark.android.core.SquaredContentProvider;
import com.cisco.spark.android.media.MediaEngine;
import com.cisco.spark.android.sync.ConversationContentProvider;
import com.cisco.spark.android.util.UserAgentProvider;
import com.ciscospark.androidsdk.auth.Authenticator;
import com.ciscospark.androidsdk.internal.SparkInjector;
import com.ciscospark.androidsdk.membership.MembershipClient;
import com.ciscospark.androidsdk.membership.internal.MembershipClientImpl;
import com.ciscospark.androidsdk.message.MessageClient;
import com.ciscospark.androidsdk.message.internal.MessageClientImpl;
import com.ciscospark.androidsdk.people.PersonClient;
import com.ciscospark.androidsdk.people.internal.PersonClientImpl;
import com.ciscospark.androidsdk.phone.Phone;
import com.ciscospark.androidsdk.phone.internal.PhoneImpl;
import com.ciscospark.androidsdk.room.RoomClient;
import com.ciscospark.androidsdk.room.internal.RoomClientImpl;
import com.ciscospark.androidsdk.team.TeamClient;
import com.ciscospark.androidsdk.team.TeamMembershipClient;
import com.ciscospark.androidsdk.team.internal.TeamClientImpl;
import com.ciscospark.androidsdk.team.internal.TeamMembershipClientImpl;
import com.ciscospark.androidsdk.utils.http.DefaultHeadersInterceptor;
import com.ciscospark.androidsdk.utils.log.NoLn;
import com.ciscospark.androidsdk.utils.log.WarningLn;
import com.ciscospark.androidsdk.webhook.WebhookClient;
import com.ciscospark.androidsdk.webhook.internal.WebhookClientImpl;
import com.github.benoitdion.ln.DebugLn;
import com.github.benoitdion.ln.InfoLn;
import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;
import com.github.benoitdion.ln.ReleaseLn;
import com.webex.wme.MediaSessionAPI;

/**
 * Spark object is the entry point to use this Cisco Spark Android SDK.
 *
 * @since 0.1
 */
public class Spark {

	static {
		UserAgentProvider.APP_NAME = DefaultHeadersInterceptor.APP_NAME;
		UserAgentProvider.APP_VERSION = DefaultHeadersInterceptor.APP_VERSION;
	}
	
	/**
	 * The enumeration of log message level
	 * 
	 * @since 0.1
	 */
	public enum LogLevel {
        NO, ERROR, WARNING, INFO, DEBUG, VERBOSE, ALL
    }
	
    private SparkInjector _injector;

    private Authenticator _authenticator;

    private PhoneImpl _phone;

    @Inject
    MediaEngine _mediaEngine;
    
    @Inject
    BackgroundCheck _backgroundCheck;

	/**
	 * Constructs a new Spark object with an {@link Authenticator} and Application
	 * 
	 * @param application The android application
	 * @param authenticator The authentication strategy for this SDK 	
	 * @since 0.1
	 */
	public Spark(Application application, Authenticator authenticator) {
	    SquaredContentProvider.CONTENT_AUTHORITY = getAuthority(application.getApplicationContext());
	    ConversationContentProvider.resetUriMatcher();
	    com.cisco.spark.android.core.Application.setApplication(application);
        _authenticator = authenticator;
        _injector = new SparkInjector(application);
        _injector.create();
        _injector.inject(this);
        _injector.inject(_authenticator);
        _phone = new PhoneImpl(application.getApplicationContext(), _authenticator, _injector);
		setLogLevel(null);
    }
	
    /**
     * Get current SDK version
     *
     * @return major.minor.build-alpha/beta
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
			_backgroundCheck.tryBackground();
		}
		else {
			_backgroundCheck.tryForeground();
		}
	}
	
	/**
	 * @return The {@link Authenticator} object from the application when constructing this Spark object. It can be used to check and modify authentication state.
	 * @since 0.1
	 */
	public Authenticator getAuthenticator() {
        return _authenticator;
    }

	/**
	 * {@link Phone} can be used to make audio and video calls on Cisco Spark.
	 * 
	 * @return The {@link Phone} represents a calling device in Cisco Spark Android SDK.
	 * @since 0.1
	 */
	public Phone phone() {
        return _phone;
    }

	/**
	 * Messages are how we communicate in a room.
	 * 
	 * @return The {@link MessageClient} is uesd to manage the messages on behalf of the authenticated user.
	 * @since 0.1
	 * @see RoomClient
	 * @see MembershipClient
	 */
	public MessageClient messages() {
        return new MessageClientImpl(this._authenticator);
    }

	/**
	 * People are registered users of Cisco Spark.
	 * 
	 * @return The {@link PersonClient} is used to find a person on behalf of the authenticated user.
	 * @since 0.1
	 * @see MembershipClient
	 * @see MessageClient
	 */
	public PersonClient people() {
        return new PersonClientImpl(this._authenticator);
    }

	/**
	 * Memberships represent a person's relationships to rooms.
	 * 
	 * @return The {@link MembershipClient} is used to manage the authenticated user's relationship to rooms.
	 * @since 0.1
	 * @see RoomClient
	 * @see MessageClient
	 */
	public MembershipClient memberships() {
        return new MembershipClientImpl(this._authenticator);
    }

	/**
	 * Teams are groups of people with a set of rooms that are visible to all members of that team.
	 * 
	 * @return The {@link TeamClient} is used to create and manage the teams on behalf of the authenticated user.
	 * @since 0.1
	 * @see TeamMembershipClient
	 * @see MembershipClient
	 */
	public TeamClient teams() {
        return new TeamClientImpl(this._authenticator);
    }

	/**
	 * Team Memberships represent a person's relationships to teams.
	 * 
	 * @return The {@link TeamMembershipClient} is used to create and manage the team membership on behalf of the authenticated user.
	 * @since 0.1
	 * @see TeamClient
	 * @see RoomClient
	 */
	public TeamMembershipClient teamMembershipClient() {
        return new TeamMembershipClientImpl(this._authenticator);
    }

	/**
	 * Webhooks allow the application to be notified via HTTP (or HTTPS?) when a specific event occurs in Cisco Spark, e.g. a new message is posted into a specific room.
	 * 
	 * @return The {@link WebhookClient} is used to create and manage the webhooks for specific events.
	 * @since 0.1
	 */
	public WebhookClient webhooks() {
        return new WebhookClientImpl(this._authenticator);
    }

	/**
	 * Rooms are virtual meeting places in Cisco Spark where people post messages and collaborate to get work done.
	 * 
	 * @return The {@link RoomClient} is used to manage the rooms on behalf of the authenticated user.
	 * @since 0.1
	 * @see MembershipClient
	 * @see MessageClient
	 */
	public RoomClient rooms() {
        return new RoomClientImpl(this._authenticator);
    }

	/**
	 * Set the log level of the logging.
	 * 
	 * @param logLevel log message level
	 */
	public void setLogLevel(LogLevel logLevel) {
		NaturalLog logger = new com.ciscospark.androidsdk.utils.log.DebugLn();
		MediaSessionAPI.TraceLevelMask mask = MediaSessionAPI.TraceLevelMask.TRACE_LEVEL_MASK_WARNING;
		if (logLevel != null) {
			switch (logLevel) {
				case NO:
					logger = new NoLn();
					mask = MediaSessionAPI.TraceLevelMask.TRACE_LEVEL_MASK_NOTRACE;
					break;
				case ERROR:
					logger = new ReleaseLn();
					mask = MediaSessionAPI.TraceLevelMask.TRACE_LEVEL_MASK_ERROR;
					break;
				case WARNING:
					logger = new WarningLn();
					mask = MediaSessionAPI.TraceLevelMask.TRACE_LEVEL_MASK_WARNING;
					break;
				case INFO:
					logger = new InfoLn();
					mask = MediaSessionAPI.TraceLevelMask.TRACE_LEVEL_MASK_WARNING;
					break;
				case DEBUG:
					logger = new com.ciscospark.androidsdk.utils.log.DebugLn();
					mask = MediaSessionAPI.TraceLevelMask.TRACE_LEVEL_MASK_DEBUG;
					break;
				case VERBOSE:
					logger = new DebugLn();
					mask = MediaSessionAPI.TraceLevelMask.TRACE_LEVEL_MASK_DETAIL;
					break;
				case ALL:
					logger = new DebugLn();
					mask = MediaSessionAPI.TraceLevelMask.TRACE_LEVEL_MASK_DETAIL;
			}
		}
		Ln.initialize(logger);
		if (_mediaEngine != null) {
			_mediaEngine.setLoggingLevel(mask);
		}
    }

	private static String getAuthority(final Context appContext) {
		try {
			final ComponentName componentName = new ComponentName(appContext, ConversationContentProvider.class.getName());
			final ProviderInfo providerInfo = appContext.getPackageManager().getProviderInfo(componentName, 0);
			return providerInfo.authority;
		}
		catch (Throwable t) {
			Ln.d("Content provider not found.");
			return "com.ciscospark.androidsdk.CPOSC";
		}
	}
}
