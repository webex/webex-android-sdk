package com.cisco.spark.android.model;

import com.cisco.spark.android.util.Strings;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Place {
    private static final Pattern COORDINATE_PATTERN = Pattern.compile("^([+-][0-9]*\\.?[0-9]*)([+-][0-9]*\\.?[0-9]*)");
    private String displayName;
    private String position;

    public Place() { }

    public Place(String displayName, String position) {
        this.displayName = displayName;
        this.position = position;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getGeoUri() {
        if (Strings.isEmpty(position))
            return null;
        try {
            Matcher matcher = COORDINATE_PATTERN.matcher(position);
            matcher.find();
            float latitude = Float.valueOf(matcher.group(1));
            float longitude = Float.valueOf(matcher.group(2));
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

    public void setPosition(String position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return "Place{" +
                "displayName='" + displayName + '\'' +
                ", position='" + position + '\'' +
                '}';
    }
}
