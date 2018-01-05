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

package com.ciscospark.androidsdk.phone;

/**
 * A data type represents a relationship between *Call* and *Person* at Cisco Spark cloud.
 * 
 * @since 0.1
 */
public interface CallMembership {

	/**
	 * The enumeration of the status of the person in the membership.
	 * 
	 * @since 0.1
	 */
	enum State {
		/**
		 * The person status is unknown.
		 *
		 * @since 0.1
		 */
		UNKNOWN,
		/**
		 * The person is idle w/o any call.
		 *
		 * @since 0.1
		 */
		IDLE,
		/**
		 * The person has been notified about the call.
		 *
		 * @since 0.1* 
		 */
		NOTIFIED,
		/**
		 * The person has joined the call.
		 *
		 * @since 0.1
		 */
		JOINED,
		/**
		 * The person has left the call.
		 *
		 * @since 0.1
		 */
		LEFT,
		/**
		 * The person has declined the call.
		 *
		 * @since 0.1
		 */
		DECLINED
    }

	/**
	 * @return True if the person is the initiator of the call.
	 * @since 0.1
	 */
	boolean isInitiator();

	/**
	 * @return The identifier of the person.
	 * @since 0.1
	 */
	String getPersonId();

	/**
	 * @return The status of the person in this CallMembership.
	 * @since 0.1
	 */
	State getState();

	/**
	 * @return The email address of the person in this CallMembership.
	 * @since 0.1
	 */
	String getEmail();

	/**
	 * @return The SIP address of the person in this CallMembership.
	 * @since 0.1
	 */
	String getSipUrl();

	/**
	 * @return The phone number of the person in this CallMembership.
	 * @since 0.1
	 */
	String getPhoneNumber();

	/**
	 * @return  True if the CallMembership is sending video. Otherwise, false.
	 * @since 0.1
	 */
	boolean isSendingVideo();

	/**
	 * @return True if the CallMembership is sending audio. Otherwise, false.
	 * @since 0.1
	 */
	boolean isSendingAudio();

	/**
	 * @return True if the CallMembership is sending screen share. Otherwise, false.
	 * @since 1.3
	 */
	boolean isSendingScreenShare();
}
