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

package com.ciscowebex.androidsdk.internal.media;

import com.webex.wme.WmeError;

public class MediaError {

    private String description;

    private String serverity;

    private String errorCode;

    public static boolean succeeded(long result) {
        return result == 0;
    }

    public static MediaError of(long result) {
        if (result == 0) {
            return null;
        }
        String serverity = toSeverity(result);
        if (serverity == null) {
            // We can ignore these error codes (below warning level)
            return null;
        }
        String description;
        if (result == WmeError.E_VIDEO_CAMERA_FAIL) {
            description = "Camera Fail";
        }
        else if (result == WmeError.E_VIDEO_CAMERA_NOT_AUTHORIZED) {
            description = "Camera Not Authorized";
        }
        else if (result == WmeError.E_VIDEO_CAMERA_NO_DEVICE) {
            description = "Camera No Device";
        }
        else if (result == WmeError.E_VIDEO_CAMERA_RUNTIME_DIE) {
            description = "Camera Runtime Die";
        }
        else {
            description = "Error returned from WME";
        }
        return new MediaError(String.valueOf(result), serverity, description);
    }

    private static String toSeverity(long result) {
        long code = (result) & 0x0000F000;
        if (code == 0x00002000) {
            return "LOW";
        }
        else if (code == 0x00004000) {
            return "MID";
        }
        else if (code == 0x00008000) {
            return "HIGH";
        }
        return null;
    }

    private MediaError(String errorCode, String serverity, String description) {
        this.errorCode = errorCode;
        this.serverity = serverity;
        this.description = description;
    }

}
