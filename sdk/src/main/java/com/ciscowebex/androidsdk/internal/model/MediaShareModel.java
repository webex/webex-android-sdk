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

import android.net.Uri;
import android.support.annotation.StringDef;
import me.helloworld.utils.Objects;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class MediaShareModel {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            SHARE_CONTENT_TYPE,
            SHARE_WHITEBOARD_TYPE
    })
    public @interface ShareType { }
    public static final String SHARE_CONTENT_TYPE = "content";
    public static final String SHARE_WHITEBOARD_TYPE = "whiteboard";

    private String name;
    private String url;
    private String resourceUrl;
    private FloorModel floor;

    public MediaShareModel(String name, String url, FloorModel floor) {
        this.name = name;
        this.url = url;
        this.floor = floor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public FloorModel getFloor() {
        return floor;
    }

    public void setFloor(FloorModel floor) {
        this.floor = floor;
    }

    public String getResourceUrl() {
        return resourceUrl;
    }

    public boolean isValidType() {
        return isWhiteboard() || isContent();
    }

    public boolean isWhiteboard() {
        return SHARE_WHITEBOARD_TYPE.equals(name);
    }

    public boolean isContent() {
        return SHARE_CONTENT_TYPE.equals(name);
    }

    public boolean isMediaShareGranted() {
        boolean result = false;
        if (getFloor() != null && getFloor().getDisposition() == FloorModel.Disposition.GRANTED) {
            result = true;
        }
        return result;
    }

    public boolean isMediaShareReleased() {
        return getFloor() != null && getFloor().getDisposition() == FloorModel.Disposition.RELEASED;
    }

    public void setResourceUrl(Uri resourceUrl) {
        this.resourceUrl = resourceUrl.toString();
    }

    public String getDisposition() {
        FloorModel.Disposition ret = FloorModel.Disposition.RELEASED;
        FloorModel floorModel = getFloor();
        if (floorModel != null) {
            ret = Objects.defaultIfNull(floorModel.getDisposition(), ret);
        }
        return ret.name();
    }

    public String getRequesterUrl() {
        FloorModel floor = getFloor();
        if (floor == null) {
            return null;
        }
        LocusParticipantModel participant = floor.getRequester();
        if (participant == null) {
            return null;
        }
        return participant.getUrl();
    }

    public String getBeneficiaryUrl() {
        FloorModel floor = getFloor();
        if (floor == null) {
            return null;
        }
        LocusParticipantModel participant = floor.getBeneficiary();
        if (participant == null) {
            return null;
        }
        return participant.getUrl();
    }
}
