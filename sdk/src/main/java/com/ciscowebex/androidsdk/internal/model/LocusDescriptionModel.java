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

package com.ciscowebex.androidsdk.internal.model;

import java.util.HashSet;
import java.util.Set;

public class LocusDescriptionModel {

    // Fields set by gson
    private Set<LocusModel.LocusTag> locusTags;
    private String owner;
    private String sipUri;
    private boolean isPmr;
    private DisplayHintsModel displayHints;
    private String webExMeetingId;
    private String webExMeetingName;
    private String callInTollNumber;
    private String callInTollFreeNumber;
    private boolean isSparkPstnEnabled;
    private boolean isCallInEnabled;
    private boolean isGlobalCallInEnabled;
    private boolean isCallOutEnabled;
    private boolean isInternalCallOutEnabled;
    private boolean isGlobalCallOutEnabled;
    private String conversationUrl;
    private String webExSite;

    public Set<LocusModel.LocusTag> getLocusTags() {
        if (locusTags != null) {
            return locusTags;
        } else {
            return new HashSet<>();
        }
    }

    public String getOwner() {
        return owner;
    }

    public String getSipUri() {
        return sipUri;
    }

    public boolean isPmr() {
        return isPmr;
    }

    public DisplayHintsModel getLocusDisplayHints() {
        return displayHints;
    }

    public String getWebExMeetingId() {
        return webExMeetingId;
    }

    public String getWebExMeetingName() {
        return webExMeetingName;
    }

    public String getCallInTollNumber() {
        return callInTollNumber;
    }

    public String getCallInTollFreeNumber() {
        return callInTollFreeNumber;
    }

    public boolean isSparkPstnEnabled() {
        return isSparkPstnEnabled;
    }

    public boolean isCallInEnabled() {
        return isCallInEnabled;
    }

    public boolean isGlobalCallInEnabled() {
        return isGlobalCallInEnabled;
    }

    public boolean isCallOutEnabled() {
        return isCallOutEnabled;
    }

    public boolean isInternalCallOutEnabled() {
        return isInternalCallOutEnabled;
    }

    public boolean isGlobalCallOutEnabled() {
        return isGlobalCallOutEnabled;
    }

    public String getConversationUrl() {
        return conversationUrl;
    }

    public String getWebExSite() {
        return webExSite;
    }
}
