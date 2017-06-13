package com.cisco.spark.android.util;

import android.util.Patterns;

public class Sanitize {

    private static boolean active;

    public static void activateSanitize(boolean activate) {
        active = activate;
    }

    public static String sanitize(String msg) {

        if (msg == null) {
            return null;
        }

        if (!active) {
            return msg;
        }

        msg = msg.replaceAll("Bearer \\S+", "<redacted>");
        msg = msg.replaceAll("\"refresh_token\"\\s*:\\s*\"[a-zA-Z0-9-_.]*\"", "\"refresh_token\":<redacted>");
        msg = msg.replaceAll("refresh_token=[a-zA-Z0-9-_.]+", "refresh_token=<redacted>");
        msg = msg.replaceAll("\"access_token\"\\s*:\\s*\"[a-zA-Z0-9-_.]*\"", "\"access_token\":<redacted>");
        msg = msg.replaceAll("access_token=[a-zA-Z0-9-_.]+", "access_token=<redacted>");
        msg = msg.replaceAll("\"displayName\":\"[a-zA-Z0-9-_\\s]+\"", "\"displayName\":<redacted>");
        msg = msg.replaceAll("\"userName\":\"[a-zA-Z0-9-_\\s]+\"", "\"userName\":<redacted>");
        msg = msg.replaceAll("\"name\":\"[a-zA-Z0-9-_\\s]+\"", "\"name\":<redacted>");
        msg = msg.replaceAll("\"email\":\"[0-9A-Za-z-!#$%&'*+\\/=?^_{|}~.@]+\"", "\"email\":<redacted>");
        msg = msg.replaceAll("\"invitee\":\"[0-9A-Za-z-!#$%&'*+\\/=?^_{|}~.@\\s]+\"", "\"invitee\":<redacted>");
        msg = msg.replaceAll(Patterns.EMAIL_ADDRESS.toString(), "<redacted>");
        msg = msg.replaceAll("\"queryString\":\"[0-9A-Za-z-!#$%&'*+\\/=?^_{|}~.@\\s]+\"", "\"queryString\":<redacted>");
        msg = msg.replaceAll("Name[\\s][a-zA-Z0-9-_\\s]+?\\n", "Name <redacted>\n");
        return msg;
    }
}

