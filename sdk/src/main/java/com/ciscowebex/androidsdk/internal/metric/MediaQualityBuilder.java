package com.ciscowebex.androidsdk.internal.metric;

import com.cisco.wx2.diagnostic_events.MQEInterval;
import com.cisco.wx2.diagnostic_events.MQESourceMetadata;
import com.cisco.wx2.diagnostic_events.MediaQualityEvent;
import com.cisco.wx2.diagnostic_events.ValidationException;
import com.ciscowebex.androidsdk.BuildConfig;
import com.ciscowebex.androidsdk.internal.Device;
import com.ciscowebex.androidsdk.internal.model.GenericMetricModel;
import com.ciscowebex.androidsdk.internal.model.LocusKeyModel;
import com.ciscowebex.androidsdk.phone.internal.CallImpl;
import com.ciscowebex.androidsdk.phone.internal.PhoneImpl;
import com.ciscowebex.androidsdk.utils.Json;
import com.github.benoitdion.ln.Ln;
import com.google.gson.reflect.TypeToken;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MediaQualityBuilder extends DiagnosticsEventBuilder {

    private MediaQualityEvent.Builder mediaQualityBuilder = MediaQualityEvent.builder();

    public MediaQualityBuilder(PhoneImpl phone) {
        super(phone);
    }

    public MediaQualityBuilder addCall(CallImpl call) {
        addCallInfo(call);
        return this;
    }

    public MediaQualityBuilder addCall(String correlationId, LocusKeyModel locusKey) {
        addCallInfo(correlationId, locusKey);
        return this;
    }

    public MediaQualityBuilder setMediaQualityEvent(String metrics) {
        try {
            // create MQESourceMetadata
            DateTime utcTime = DateTime.now().toDateTime(DateTimeZone.UTC);
            MQESourceMetadata.Builder metadata = MQESourceMetadata.builder();
            metadata.applicationSoftwareType(BuildConfig.APPLICATION_ID)
                    .applicationSoftwareVersion(BuildConfig.VERSION_NAME)
                    .mediaEngineSoftwareType(Device.Type.ANDROID_MEDIA_ENGINE.getTypeName())
                    .mediaEngineSoftwareVersion(getStandardMediaEngineVersion(phone.getEngine().getVersion()))
                    .startTime(utcTime.toString());
            // convert WME media quality metrics to MQEInterval
            Type mqeIntervalType = new TypeToken<MQEInterval>() { }.getType();
            MQEInterval mqeInterval = Json.fromJson(metrics, mqeIntervalType);
            List<MQEInterval> intervals = new ArrayList<>();
            intervals.add(mqeInterval);
            mediaQualityBuilder.name(MediaQualityEvent.Name.CLIENT_MEDIAQUALITY_EVENT).canProceed(true).intervals(intervals).sourceMetadata(metadata.build());
        } catch (ValidationException e) {
            Ln.e(e, "Failed to build valid source metadata for media quality");
            Ln.e(e.getValidationError().getErrors().toString());
        }
        return this;
    }

    public GenericMetricModel build() {
        return super.build(mediaQualityBuilder);
    }
}
