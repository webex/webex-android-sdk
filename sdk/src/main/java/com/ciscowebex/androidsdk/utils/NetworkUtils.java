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

package com.ciscowebex.androidsdk.utils;

import android.support.annotation.Nullable;
import com.ciscowebex.androidsdk.internal.ErrorDetail;
import com.ciscowebex.androidsdk.internal.Service;
import com.ciscowebex.androidsdk.internal.ServiceReqeust;
import com.github.benoitdion.ln.Ln;
import com.google.gson.JsonSyntaxException;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.EOFException;
import java.io.IOException;
import java.net.*;
import java.util.Enumeration;

public class NetworkUtils {

    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address
                            && !intf.getDisplayName().startsWith("usbnet")) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ex) {
            Ln.e("getLocalIpAddress()", ex.toString());
        }
        return null;
    }

    public static boolean isBehindProxy() {
        return ProxySelector.getDefault().select(URI.create(Service.Wdm.baseUrl(null) + "/ping")).get(0) != Proxy.NO_PROXY;
    }


    public static String resolveHostName(String hostName) {
        InetAddress serverAddr;
        try {
            serverAddr = InetAddress.getByName(hostName);
        } catch (java.net.UnknownHostException exception) {
            Ln.e("resolveHostName()", exception.toString());
            return "";
        }
        return serverAddr.getHostAddress();
    }

    @Nullable
    public static String getTrackingId(@Nullable Response response) {
        if (response == null) {
            return null;
        }
        return response.headers().get(ServiceReqeust.HEADER_TRACKING_ID);
    }

    /**
     * Extract the 429 'Retry-After' header value if it exists, and make sure it's in the allowed
     * range. If the response doesn't include the Retry-After header, the value can't be parsed, or
     * the value is negative, 0 is returned.
     *
     * @param response
     * @return The value or 0 if the value is invalid
     */
    public static int get429RetryAfterSeconds(final Response response, final int minimum, final int maximum) {
        if (minimum > maximum) {
            throw new IllegalArgumentException("Minimum (" + minimum + ") should be smaller than maximum (" + maximum + ")");
        }

        if (response == null || response.code() != 429) {
            return 0;
        }

        final String retryAfterHeader = response.headers().get("Retry-After");
        if (retryAfterHeader == null) {
            return 0;
        }

        final int retrySeconds;
        try {
            retrySeconds = Integer.parseInt(retryAfterHeader);
        } catch (final NumberFormatException e) {
            Ln.w("Failed parsing Retry-After header");
            return 0;
        }

        if (retrySeconds < 0) {
            return 0;
        }

        return Math.min(Math.max(retrySeconds, minimum), maximum);
    }

    public static @Nullable ErrorDetail parseErrorDetailFromResponse(@Nullable okhttp3.Response response) {
        try {
            if (response != null && !isResponseSuccessful(response)) {
                ResponseBody body = response.body();
                String data = body == null ? null : body.string();
                return data == null ? null : Json.fromJson(data, ErrorDetail.class);
            }
        } catch (JsonSyntaxException e) {
            Ln.w(e, "Syntax exception while attempting to parsing the errorBody of a response");
        } catch (EOFException e) {
            if (response.code() != HttpURLConnection.HTTP_NOT_MODIFIED) {
                Ln.w(e, "This exception may be due to the error body being parsed twice from the same object");
            }
        } catch (IOException e) {
            Ln.e(e, "Error while attempting to parsing the errorBody of a response");
        }
        return null;
    }

    public static boolean isResponseSuccessful(@Nullable okhttp3.Response response) {
        return response != null && response.code() < 400;
    }
}
