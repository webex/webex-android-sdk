package com.cisco.spark.android.client;

import android.content.Context;

import com.cisco.spark.android.core.Settings;
import com.cisco.spark.android.util.TestUtils;
import com.github.benoitdion.ln.Ln;

import java.util.Locale;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Tracking id's are generated on the client and passed as a request header on every api calls.
 * They're logged both client and server side and help us troubleshoot issues.
 */
@Singleton
public class TrackingIdGenerator {
    private final String base;
    private final Context context;
    private final Settings settings;
    private int maxCounter = 65536;
    private int counter;
    private String currentTrackingId;

    @Inject
    public TrackingIdGenerator(Context context, Settings settings) {
        this.context = context;
        this.settings = settings;
        base = UUID.randomUUID().toString();
        currentTrackingId = "";
    }

    public TrackingIdGenerator(int maxCounter) {
        this(null, null);
        this.maxCounter = maxCounter;
    }

    public synchronized String nextTrackingId() {
        String trackingId;
        if (TestUtils.isInstrumentation() || (settings != null && !settings.getMediaOverrideIpAddress().isEmpty())) {
            trackingId = String.format(Locale.US, "ITCLIENT_%s_%d", base, counter);
        } else {
            trackingId = String.format(Locale.US, "CLIENT_%s_%d", base, counter);
        }

        counter++;
        if (counter > maxCounter) {
            counter = 0;
        }

        currentTrackingId = trackingId;

        Ln.report(null, "Tracking-Id: " + currentTrackingId);
        return trackingId;
    }

    public String base() {
        return base;
    }

    public String currentTrackingId() {
        return currentTrackingId;
    }
}
