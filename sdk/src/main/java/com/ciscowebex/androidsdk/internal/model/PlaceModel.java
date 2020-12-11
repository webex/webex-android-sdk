/*
 * Copyright 2016-2021 Cisco Systems Inc
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

import me.helloworld.utils.Checker;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceModel {

    private static final Pattern COORDINATE_PATTERN = Pattern.compile("^([+-][0-9]*\\.?[0-9]*)([+-][0-9]*\\.?[0-9]*)");
    private String displayName;
    private String position;

    public String getDisplayName() {
        return displayName;
    }

    public String getGeoUri() {
        if (Checker.isEmpty(position)) {
            return null;
        }
        try {
            Matcher matcher = COORDINATE_PATTERN.matcher(position);
            matcher.find();
            float latitude = Float.parseFloat(matcher.group(1));
            float longitude = Float.parseFloat(matcher.group(2));
            // Geo uris requires the following format:  <lat>,<long>?q=<lat>,<long>(Label+Name)
            String geoUriFormat = "geo:%.4f,%.4f?q=%.4f,%.4f(%s)";
            return String.format(Locale.US, geoUriFormat, latitude, longitude, latitude, longitude, displayName);
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * An ISO6709-formatted string identifying the position at which this
     * activity occurred. For example, '+27.5916+086.5640+8850/'
     */
    public String getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return "Place{" +
                "displayName='" + displayName + '\'' +
                ", position='" + position + '\'' +
                '}';
    }
}
