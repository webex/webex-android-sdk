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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.github.benoitdion.ln.Ln;
import me.helloworld.utils.Checker;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UriUtils {

    public static final String DEEP_LINKING_ACTIVATE_PATH = "activate";
    public static final String DEEP_LINKING_ACTIVATE_PARAM_EMAIL = "email";
    public static final String DEEP_LINKING_ACTIVATE_PARAM_TOKEN = "t";
    public static final String DEEP_LINKING_ACTIVATE_PARAM_VERIFICATION_TYPE = "vt";

    private static final String DEEP_LINKING_BASE_URL_REGEX = "(web\\.ciscospark\\.com|teams\\.webex\\.com)\\/";
    private static final String DEEP_LINKING_ID_REGEX = "\\/[A-Za-z0-9._%+-]+";

    public static final String DEEP_LINKING_MEET_PATH = "meet";
    public static final String DEEP_LINKING_MEET_URL_REGEX = "https:\\/\\/" + DEEP_LINKING_BASE_URL_REGEX + DEEP_LINKING_MEET_PATH + DEEP_LINKING_ID_REGEX;

    public static final String DEEP_LINKING_ROOMS_PATH = "rooms";
    public static final String DEEP_LINKING_SPACES_PATH = "spaces";
    public static final String DEEP_LINKING_SPACES_URL_REGEX = "(https?:\\/\\/)?" + DEEP_LINKING_BASE_URL_REGEX + "(#\\/)?(" + DEEP_LINKING_ROOMS_PATH + "|" + DEEP_LINKING_SPACES_PATH + ")" + DEEP_LINKING_ID_REGEX  + "(\\/chat|\\/)?";

    public static final String DEEP_LINKING_TEAMS_PATH = "teams";
    public static final String DEEP_LINKING_TEAMS_URL_REGEX = "(https?:\\/\\/)?" + DEEP_LINKING_BASE_URL_REGEX + "(#\\/)?" + DEEP_LINKING_TEAMS_PATH + DEEP_LINKING_ID_REGEX + "(\\/chat|\\/)?";

    public static final String PMR_URL_REGEX_HTTP = "https?:\\/\\/([\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)\\.webex\\.com\\/meet" + DEEP_LINKING_ID_REGEX;
    public static final String PMR_URL_REGEX_SIP = "(sips?:)?(([A-Za-z0-9._%+-]+)@([\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)\\.webex\\.com)";

    // TODO: The differences between this regex and the PMR_URL_REGEX_HTTP above needs to be resolved into a single regex used throughout the app. The regex above
    //       is used for matching on clickable links within a conversation. The regex below is the same as that found in meeting_rules.json and is used by MeetingHub/JoinHub.
    private static final String PMR_URL_REGEX_HTTP_FOR_MEETING_HUB = "(?:https?://)?(?:[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)\\.webex\\.com/(?:meet|join)" + DEEP_LINKING_ID_REGEX;

    public static final String WEBEX_SPACE_MEETING_REGEX = "https?:\\/\\/([\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)\\.webex\\.com\\/m\\/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    // https://avatar-intb.ciscospark.com/avatar/api/v1/profile/f0b30c25-038c-45b6-840a-09dcd8d29bef?s=96
    public static final String AVATAR_REMOTE_URI_REGEX = "(.)*(ciscospark|wbx2)\\.com\\/avatar\\/api\\/v[1-9]\\/profile" + DEEP_LINKING_ID_REGEX + "(\\?s=[0-9]+)?";

    public static String removeUrlBase(URI base, URI url) {
        String baseString = base.toString().replace("http://", "").replace("https://", "");
        String urlString = url.toString().replace("http://", "").replace("https://", "");
        if (urlString.startsWith(baseString)) {
            return urlString.substring(baseString.length() + 1);
        } else {
            return url.toString();
        }
    }

    public static Uri parseFromURI(URI javaUri) {
        if (javaUri != null)
            return Uri.parse(javaUri.toString());
        else
            return null;
    }

    public static String toString(URI javaUri) {
        Uri androidUri = parseFromURI(javaUri);
        return UriUtils.toString(androidUri);
    }

    public static Uri parseIfNotNull(String uriString) {
        try {
            if (!TextUtils.isEmpty(uriString))
                return Uri.parse(uriString);
        } catch (Exception e) {
            Ln.w("Failed parsing uri");
        }

        return null;
    }

    public static ArrayList<Uri> parseIfNotNull(ArrayList<String> uriStrings) {
        if (uriStrings == null) {
            return null;
        }

        ArrayList<Uri> parsedUris = new ArrayList<>();

        for (String uriString : uriStrings) {
            Uri parsedUri = parseIfNotNull(uriString);

            parsedUris.add(parsedUri);
        }

        return parsedUris;
    }

    public static String toString(Uri uri) {
        if (uri == null) {
            return null;
        }
        return uri.toString();
    }

    public static String toNonNullString(Uri uri) {

        if (uri == null) {
            return "";
        }

        return uri.toString();
    }

    public static String stripHttps(Uri uri) {
        if (uri == null) {
            return null;
        }
        if (uri.toString().startsWith("https://"))
            return uri.toString().replaceFirst("https://", "");

        return uri.toString();
    }

    public static boolean equals(Uri lhs, Uri rhs) {
        if (lhs == rhs)
            return true;
        if (lhs == null || rhs == null)
            return false;

        return lhs.equals(rhs);
    }

    public static boolean equals(String lhs, Uri rhs) {
        return equals(rhs, lhs);
    }

    public static boolean equals(Uri lhs, String rhs) {
        if (lhs == null || rhs == null)
            return false;

        return lhs.toString().equals(rhs);
    }

    public static UUID extractUUID(Uri uri) {
        if (uri == null)
            return null;
        String getUUIDFromUri = uri.getLastPathSegment();
        if (getUUIDFromUri == null)
            return null;

        try {
            return UUID.fromString(getUUIDFromUri);
        } catch (Exception e) {
            return null;
        }
    }

    public static URI toURI(String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public static URI toURI(Uri uri) {
        try {
            return new URI(uri.toString());
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public static boolean isSupportedDeepLinkingUri(@NonNull Uri uri) {
        return isMeetDeepLinkingUri(uri) || isSpacesDeepLinkingUri(uri) || isTeamsDeepLinkingUri(uri) || isActivateDeepLinkingUri(uri);
    }

    public static boolean isMeetDeepLinkingUri(@NonNull Uri uri) {
        return compileRegex(DEEP_LINKING_MEET_URL_REGEX, uri) || compileRegex(WEBEX_SPACE_MEETING_REGEX, uri);
    }

    public static boolean isSpacesDeepLinkingUri(@NonNull Uri uri) {
        return compileRegex(DEEP_LINKING_SPACES_URL_REGEX, uri);
    }

    public static boolean isTeamsDeepLinkingUri(@NonNull Uri uri) {
        return compileRegex(DEEP_LINKING_TEAMS_URL_REGEX, uri);
    }

    public static boolean isActivateDeepLinkingUri(@NonNull Uri uri) {
        if (!uri.isHierarchical()) {
            return false;
        }
        List<String> pathSegments = uri.getPathSegments();
        Set<String> queryParameters = uri.getQueryParameterNames();
        return pathSegments != null && queryParameters != null
                && pathSegments.contains(DEEP_LINKING_ACTIVATE_PATH)
                && queryParameters.contains(DEEP_LINKING_ACTIVATE_PARAM_EMAIL)
                && queryParameters.contains(DEEP_LINKING_ACTIVATE_PARAM_TOKEN);
    }

    private static boolean compileRegex(String regexString, @NonNull Uri uri) {
        return Pattern.compile(regexString, Pattern.CASE_INSENSITIVE).matcher(uri.toString()).matches();
    }

    public static String extractDeepLinkId(@NonNull Uri uri, String deepLinkTag) {
        uri = Uri.parse(uri.toString().replaceAll("/#/", "/"));

        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments == null || Checker.isEmpty(deepLinkTag)) {
            return null;
        }

        int tagIndex = pathSegments.indexOf(deepLinkTag);
        if (tagIndex < 0 || tagIndex == pathSegments.size() - 1) {
            return null;
        }

        return pathSegments.get(tagIndex + 1);
    }

    public static String extractUUIDFromRemoteUri(@NonNull Uri uri) {
        if (!Pattern.compile(AVATAR_REMOTE_URI_REGEX, Pattern.CASE_INSENSITIVE).matcher(uri.toString()).matches()) {
            return null;
        }

        return uri.getLastPathSegment();
    }


    public static String convertWebExPmrHttpUrlToSipAddress(@Nullable String pmrUrl) {
        if (TextUtils.isEmpty(pmrUrl)) {
            return null;
        }

        String sipAddress = null;
        String url = pmrUrl.trim();
        Matcher matcher = Pattern.compile(PMR_URL_REGEX_HTTP_FOR_MEETING_HUB, Pattern.CASE_INSENSITIVE).matcher(url);
        if (matcher.find()) {
            Uri uri = Uri.parse(url.toLowerCase());
            if (uri.getScheme() == null) {
                // the URL does not have a scheme supplied. Parameter came into method as
                // "go.webex.com/meet/user_name". Need to add a scheme to use the Uri class.
                url = "http://" + url;
                uri = Uri.parse(url.toLowerCase());
            }

            sipAddress = uri.getLastPathSegment() + "@" + uri.getHost();
        }

        return sipAddress;
    }

    /**
     * When pulling the meta data from a site, before we go out to the site there needs to be a scheme
     * on the URL. So if the user did not enter a site (or it did not parse for whatever reason), we
     * will use this method to prepend it with http://
     *
     * @param urlString The URL that may or may not need an prepended scheme
     * @return The URL that was passed in, now with a scheme if it didn't have one before
     */
    public static String prependSchemeIfNecessary(final String urlString) {
        String returnUrl = urlString;

        Uri uri = Uri.parse(urlString);
        if (uri.getScheme() == null) {
            returnUrl = "http://" + urlString;
        }

        return returnUrl;
    }

    /**
     * Take a url and ensure it as appended with a slash, as retrofit expects a baseUrl in that format
     */
    public static Uri properFormatBaseUrl(String url) {
        Uri potentialImproperFormattedUri = Uri.parse(url);
        return Uri.withAppendedPath(potentialImproperFormattedUri, "");
    }
}

