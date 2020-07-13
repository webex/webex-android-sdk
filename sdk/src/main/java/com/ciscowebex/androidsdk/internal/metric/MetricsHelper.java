package com.ciscowebex.androidsdk.internal.metric;

import com.cisco.wx2.diagnostic_events.MediaLine;
import com.cisco.wx2.diagnostic_events.ValidationException;
import com.ciscowebex.androidsdk.internal.model.WMEIceListModel;
import com.ciscowebex.androidsdk.internal.model.WMEIceModel;
import com.ciscowebex.androidsdk.internal.model.WMEIceResultModel;
import com.ciscowebex.androidsdk.utils.Json;
import com.github.benoitdion.ln.Ln;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class MetricsHelper {

    public static MediaLine getMediaLineFromIceResult(com.cisco.wx2.diagnostic_events.MediaType mediaType, WMEIceResultModel iceResult) {
        MediaLine mediaLine = null;
        try {
            MediaLine.Builder mediaBuilder = MediaLine.builder().
                    direction(MediaLine.Direction.fromValue(iceResult.getDirection())).
                    localIP(iceResult.getLocalIp()).
                    localPort(iceResult.getLocalPort()).
                    mediaType(mediaType).
                    protocol(MediaLine.Protocol.fromValue(iceResult.getProtocol())).
                    remoteIP(iceResult.getRemoteIp()).
                    remotePort(iceResult.getRemotePort()).
                    status(MediaLine.Status.fromValue(iceResult.getStatus()));

            if (iceResult.getFailureReason() != null) {
                // in failure case, add error code and failure reason.
                mediaLine = mediaBuilder.failureReason(MediaLine.FailureReason.fromValue(iceResult.getFailureReason()))
                        .errorCode(iceResult.getErrorCode())
                        .build();
            } else {
                mediaLine = mediaBuilder.build();
            }
        } catch (ValidationException e) {
            Ln.e(e, "getMediaLineFromIceResult ValidationException");
        }
        return mediaLine;
    }

    public static boolean convertICEtoMedialines(WMEIceModel wmeIce, com.cisco.wx2.diagnostic_events.MediaType mediaType, List<MediaLine> mediaLines) {
        boolean connectSuccess = true;
        if (wmeIce != null && wmeIce.getIceResults() != null && wmeIce.getIceResults().size() >= 1) {
            if (!WMEIceModel.SUCCEEDED.equalsIgnoreCase(wmeIce.getIceFailed())) {
                connectSuccess = false;
            }

            for (WMEIceResultModel wmeIceResult : wmeIce.getIceResults()) {
                MediaLine mediaLine = MetricsHelper.getMediaLineFromIceResult(mediaType, wmeIceResult);
                if (mediaLine != null) {
                    mediaLines.add(mediaLine);
                }
            }
        }
        return connectSuccess;
    }

    public static boolean convertWMEIceListToMediaLines(String iceReport, List<MediaLine> mediaLines) {
        boolean connectSuccess = true;
        try {
            Type wmeIceListType = new TypeToken<WMEIceListModel>() { }.getType();
            WMEIceListModel iceList = Json.fromJson(iceReport, wmeIceListType);

            if (iceList != null && mediaLines != null) {
                // audio
                WMEIceModel iceAudio = iceList.getAudio();
                boolean audioConnectSuccess = MetricsHelper.convertICEtoMedialines(iceAudio, com.cisco.wx2.diagnostic_events.MediaType.AUDIO, mediaLines);
                // video
                WMEIceModel iceVideo = iceList.getVideo();
                boolean videoConnectSuccess = MetricsHelper.convertICEtoMedialines(iceVideo, com.cisco.wx2.diagnostic_events.MediaType.VIDEO, mediaLines);
                // share
                WMEIceModel iceShare = iceList.getShare();
                boolean shareConnectSuccess = MetricsHelper.convertICEtoMedialines(iceShare, com.cisco.wx2.diagnostic_events.MediaType.SHARE, mediaLines);
                connectSuccess = audioConnectSuccess && videoConnectSuccess && shareConnectSuccess;
            }
        } catch (Exception ex) {
            Ln.e(ex, "Failed to convert WMEIceList To MediaLines");
        }
        return connectSuccess;
    }
}
