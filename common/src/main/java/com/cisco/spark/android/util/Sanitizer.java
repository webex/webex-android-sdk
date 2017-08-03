package com.cisco.spark.android.util;

import android.util.Patterns;

import com.github.benoitdion.ln.Ln;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class Sanitizer {

    private static final String JSON_PATTERN_TEMPLATE = "\"%s\"\\s*:\\s*\"[\\w!#$%%&'*+/=?^{|}~.@\\s-]*\"";

    private static final Set<String> JSON_KEYS_TO_REDACT = new HashSet<>(Arrays.asList(
            "access_token",
            "displayName",
            "email",
            "invitee",
            "name",
            "primaryDisplayString",
            "queryString",
            "refresh_token",
            "userName"));

    private Map<String, Pattern> patterns;

    private static final Pattern BEARER_PATTERN = Pattern.compile("Bearer \\S+");
    private static final Pattern REFRESH_TOKEN_PATTERN = Pattern.compile("refresh_token=[\\w.-]+");
    private static final Pattern NAME_PATTERN = Pattern.compile("Name[\\s][\\w\\s'-]+?\\n");
    private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("access_token=[\\w.-]+");

    private boolean active = false;
    private final int maxLength;

    public Sanitizer(boolean active, int maxLength) {

        patterns = new HashMap<>();
        for (String s : JSON_KEYS_TO_REDACT) {
            patterns.put(s, Pattern.compile(String.format(JSON_PATTERN_TEMPLATE, s)));
        }

        this.active = active;
        this.maxLength = maxLength;
    }

    public Sanitizer(boolean active) {
        this(active, 10000);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String sanitize(String msg) {
        return sanitize(msg, true);
    }

    public String sanitize(String msg, boolean enforceMaxLength) {

        if (msg == null) {
            return null;
        }

        if (msg.length() > maxLength && enforceMaxLength) {
            Ln.d("Sanitizer truncating string of length %d", msg.length());
            msg = msg.substring(0, maxLength);
        }

        if (!active) {
            return msg;
        }

        msg = BEARER_PATTERN.matcher(msg).replaceAll("<redacted>");
        msg = REFRESH_TOKEN_PATTERN.matcher(msg).replaceAll("refresh_token=<redacted>");
        msg = ACCESS_TOKEN_PATTERN.matcher(msg).replaceAll("access_token=<redacted>");
        msg = NAME_PATTERN.matcher(msg).replaceAll("Name <redacted>\n");

        msg = Patterns.EMAIL_ADDRESS.matcher(msg).replaceAll("<redacted>");

        String matchString;
        Pattern matchPattern;
        for (Map.Entry<String, Pattern> stringPatternEntry : patterns.entrySet()) {
            matchString = stringPatternEntry.getKey();
            matchPattern = stringPatternEntry.getValue();
            msg = matchPattern.matcher(msg).replaceAll(String.format("\"%s\":<redacted>", matchString));
        }
        return msg;
    }
}

