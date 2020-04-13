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

package com.ciscowebex.androidsdk.utils;

import android.net.Uri;
import android.text.TextUtils;
import com.ciscowebex.androidsdk.internal.model.ImageModel;

public abstract class GiphyUtils {
    public static final String DEFAULT_GIPHY_URL = "https://giphy.com";
    private static final String DEAFULT_GIPHY_API_URL = "https://api.giphy.com/";
    private static final String MIME_TYPE_GIF = "image/gif";
    private static final String URL_PREFIX_GIPHY = "giphy.com";
    private static final String FILE_FORMAT_GIPHY = ".gif";

    public static boolean isMimeTypeGiphy(String mimeType) {
        return MIME_TYPE_GIF.equalsIgnoreCase(mimeType);
    }

    public static boolean isImageGiphy(ImageModel image) {
        return image != null && isUriGiphy(image.getUrl());
    }

    public static boolean isUriGiphy(Uri uri) {
        return uri != null && isUriGiphy(uri.getHost());
    }

    public static boolean isUriGiphy(String host) {
        return !TextUtils.isEmpty(host) && host.endsWith(URL_PREFIX_GIPHY);
    }

    public static boolean containsGiphyDomain(Uri uri) {
        return uri != null && containsGiphyDomain(uri.getHost());
    }

    public static boolean containsGiphyDomain(String host) {
        return !TextUtils.isEmpty(host) && host.contains(URL_PREFIX_GIPHY);
    }

    public static Uri getDefaultGiphyUrl() {
        return Uri.parse(DEFAULT_GIPHY_URL);
    }

    public static String getFileFormatGiphy() {
        return FILE_FORMAT_GIPHY;
    }

    public static String getMimeTypeGif() {
        return MIME_TYPE_GIF;
    }

    public static String getDefaultGiphyApiUrl() {
        return DEAFULT_GIPHY_API_URL;
    }
}

