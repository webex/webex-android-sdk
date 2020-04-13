package com.ciscowebex.androidsdk.utils;

import android.support.annotation.Nullable;
import com.github.benoitdion.ln.Ln;
import okhttp3.Headers;
import okhttp3.Response;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
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
        Headers headers = response.headers();
        if (headers == null) {
            return null;
        }
        return response.headers().get("trackingid");
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
}
