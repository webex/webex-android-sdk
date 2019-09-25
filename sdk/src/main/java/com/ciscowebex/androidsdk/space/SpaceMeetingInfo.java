/*
 * Copyright 2016-2019 Cisco Systems Inc
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

package com.ciscowebex.androidsdk.space;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * The Webex meeting details for a space such as the SIP address, meeting URL, toll-free and toll dial-in numbers.
 *
 * @since 2.3.0
 */
public class SpaceMeetingInfo {
    @SerializedName(value = "roomId", alternate = "spaceId")
    private String _spaceId;

    @SerializedName("meetingLink")
    private String _meetingLink;

    @SerializedName("sipAddress")
    private String _sipAddress;

    @SerializedName("meetingNumber")
    private String _meetingNumber;

    @SerializedName("callInTollFreeNumber")
    private String _callInTollFreeNumber;

    @SerializedName("callInTollNumber")
    private String _callInTollNumber;

    /**
     * A unique identifier for the space.
     *
     * @return a unique identifier for the space.
     */
    public String getSpaceId() {
        return _spaceId;
    }

    /**
     * The Webex meeting URL for the space.
     *
     * @return the Webex meeting URL for the space.
     */
    public String getMeetingLink() {
        return _meetingLink;
    }

    /**
     * The SIP address for the space.
     *
     * @return the SIP address for the space.
     */
    public String getSipAddress() {
        return _sipAddress;
    }

    /**
     * The Webex meeting number for the space.
     *
     * @return the Webex meeting number for the space.
     */
    public String getMeetingNumber() {
        return _meetingNumber;
    }

    /**
     * The toll-free PSTN number for the space.
     *
     * @return the toll-free PSTN number for the space.
     */
    public String getCallInTollFreeNumber() {
        return _callInTollFreeNumber;
    }

    /**
     * The toll (local) PSTN number for the space.
     *
     * @return the toll (local) PSTN number for the space.
     */
    public String getCallInTollNumber() {
        return _callInTollNumber;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}