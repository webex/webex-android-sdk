package com.cisco.spark.android.util;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.cisco.spark.android.sync.DatabaseHelper;
import com.github.benoitdion.ln.Ln;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class UriUtils {

    public static final String DEEP_LINKING_PATH_MEET = "meet";
    public static final String DEEP_LINKING_PATH_ROOMS = "rooms";
    public static final String DEEP_LINKING_PATH_SPACES = "spaces";
    public static final String DEEP_LINKING_PATH_TEAMS = "teams";
    public static final String DEEP_LINKING_MEET_URL_REGEX = "https:\\/\\/web\\.ciscospark\\.com\\/"
            + DEEP_LINKING_PATH_MEET + "\\/[A-Za-z0-9._%+-]+";
    public static final String DEEP_LINKING_SPACES_URL_REGEX = "(https?:\\/\\/)?web\\.ciscospark\\.com\\/(#\\/)?("
            + DEEP_LINKING_PATH_ROOMS + "|" + DEEP_LINKING_PATH_SPACES + ")\\/[A-Za-z0-9._%+-]+(\\/chat|\\/)?";
    public static final String DEEP_LINKING_TEAMS_URL_REGEX = "(https?:\\/\\/)?web\\.ciscospark\\.com\\/(#\\/)?"
            + DEEP_LINKING_PATH_TEAMS + "\\/[A-Za-z0-9._%+-]+(\\/chat|\\/)?";
    public static final String PMR_URL_REGEX_HTTP = "https?:\\/\\/([\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)\\.webex\\.com\\/meet\\/[A-Za-z0-9._%+-]+";
    public static final String PMR_URL_REGEX_SIP = "(sip:)?(([A-Za-z0-9._%+-]+)@([\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)\\.webex\\.com)";

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

    public static String stripHttps(Uri uri) {
        if (uri == null) {
            return null;
        }
        if (uri.toString().startsWith("https://"))
            return uri.toString().replaceFirst("https://", "");

        return uri.toString();
    }

    public static boolean isBlacklisted(Context context, ArrayList<Uri> uris) {
        for (Uri uri : uris) {
            boolean uriBlacklisted = isBlacklisted(context, uri);

            if (!uriBlacklisted) {
                return false;
            }
        }

        return true;
    }

    public static boolean isBlacklisted(Context context, Uri uri) {
        if (!"file".equals(uri.getScheme()))
            return false;

        try {
            // If the file and its parent directory names match those of the db, blacklist it
            File dbfile = context.getDatabasePath(DatabaseHelper.SERVICE_DB_NAME);
            if (dbfile.getName().equals(uri.getLastPathSegment())
                    && dbfile.getParent().equals(uri.getPathSegments().get(uri.getPathSegments().size() - 2)))
                return true;

            // this catches shared prefs files
            if (uri.toString().contains("com.cisco.wx2.android" + File.pathSeparator)) {
                if (uri.getLastPathSegment().startsWith("com.cisco.wx2.android"))
                    return true;
            }
        } catch (Throwable e) {
            Ln.e(e, "Blacklisted file " + uri);
            return true;
        }

        return false;
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

    public static URI toURI(Uri uri) {
        try {
            return new URI(uri.toString());
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public static boolean isSupportedDeepLinkingUri(@NonNull Uri uri) {
        return isMeetDeepLinkingUri(uri) || isSpacesDeepLinkingUri(uri) || isTeamsDeepLinkingUri(uri);
    }

    public static boolean isMeetDeepLinkingUri(@NonNull Uri uri) {
        return Pattern.compile(DEEP_LINKING_MEET_URL_REGEX, Pattern.CASE_INSENSITIVE).matcher(uri.toString()).matches();
    }

    public static boolean isSpacesDeepLinkingUri(@NonNull Uri uri) {
        return Pattern.compile(DEEP_LINKING_SPACES_URL_REGEX, Pattern.CASE_INSENSITIVE).matcher(uri.toString()).matches();
    }

    public static boolean isTeamsDeepLinkingUri(@NonNull Uri uri) {
        return Pattern.compile(DEEP_LINKING_TEAMS_URL_REGEX, Pattern.CASE_INSENSITIVE).matcher(uri.toString()).matches();
    }

    public static String extractDeepLinkId(@NonNull Uri uri, String deepLinkTag) {
        uri = Uri.parse(uri.toString().replaceAll("/#/", "/"));

        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments == null || Strings.isEmpty(deepLinkTag)) {
            return null;
        }

        int tagIndex = pathSegments.indexOf(deepLinkTag);
        if (tagIndex < 0 || tagIndex == pathSegments.size() - 1) {
            return null;
        }

        return pathSegments.get(tagIndex + 1);
    }

}
