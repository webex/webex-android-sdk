package com.cisco.spark.android.util;



import java.util.Locale;

public class NameUtils {
    private NameUtils() {
    }

    public static String getShortName(String displayName) {
        if (displayName == null) {
            return "";
        }

        displayName = displayName.trim();

        int index = displayName.indexOf(" -X");

        if (index <= 0)
            index = displayName.indexOf(" -T");
        if (index <= 0)
            index = displayName.indexOf(" (");

        if (index > 0) {
            return displayName.substring(0, index);
        }
        return displayName;
    }

    public static String getFirstName(String displayName) {
        if (displayName == null) {
            return "";
        }

        displayName = displayName.trim();

        String[] parts = displayName.split(" ");
        if (parts.length > 1) {
            return parts[0];
        } else {
            return getShortName(displayName);
        }
    }

    public static String getLastName(String displayName) {
        String shortName = getShortName(displayName);
        String firstName = getFirstName(displayName);
        return shortName.replace(firstName, "").trim();
    }

    public static String getLocalPartFromEmail(String email) {
        if (email == null || !email.contains("@"))
            return null;

        return email.toLowerCase(Locale.getDefault()).substring(0, email.indexOf("@"));
    }

    public static String getDomainFromEmail(String email) {
        if (email == null)
            return null;

        int domainIndex = email.indexOf('@') + 1;

        if (domainIndex == 0 || domainIndex >= email.length())
            return null;

        return email.substring(domainIndex, email.length());
    }

    public static String getLongName(String name, String email) {
        return String.format(Locale.getDefault(), "%s (%s)", name, getLocalPartFromEmail(email));
    }

    public static String getNameInitials(String displayName) {
        String shortName = getShortName(displayName).toUpperCase().trim();

        if (shortName.isEmpty()) {
            return "";
        }

        String[] subNames = shortName.split(" ");
        if (subNames.length == 1) {
            return subNames[0].substring(0, 1);
        } else {
            char firstNameChar = subNames[0].charAt(0);
            char lastNameChar = subNames[subNames.length - 1].charAt(0);
            return "" + firstNameChar + lastNameChar;
        }
    }

    public static String stripDialableProtocol(String dialable) {
        if (dialable == null)
            return null;
        return dialable.replaceFirst("^(sip:|tel:)", "");
    }

}
