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

package com.ciscowebex.androidsdk.internal.media;

import android.os.Build;
import android.os.Environment;
import com.ciscowebex.androidsdk.phone.AdvancedSetting;
import com.ciscowebex.androidsdk.phone.Phone;
import com.ciscowebex.androidsdk.utils.Lists;
import com.github.benoitdion.ln.Ln;
import com.webex.wme.MediaConfig;
import com.webex.wme.MediaConnection;
import me.helloworld.utils.Checker;
import org.json.JSONObject;

import java.io.File;
import java.util.*;

public class MediaCapability {

    private static final String DEFAULT_HW_VIDEO_SETTING = "{\"hw-whitelist\": { "
            + "\"android\": { "
            + "\"defaults\": { "
            + "\"mediaCodec\":true,"
            + "\"yv12Capture\":false"
            + "}}}}";

    private static final List<String> DEFAULT_AE_MODLES =  Lists.asList("SM-G93", "SM-G95", "SM-G96", "SM-G97", "SM-N95", "SM-N96", "GM19");

    private static final int DEFAULT_MAX_STREAMS = 4;

    private int audioMaxRxBandwidth = Phone.DefaultBandwidth.MAX_BANDWIDTH_AUDIO.getValue();

    private int videoMaxRxBandwidth = Phone.DefaultBandwidth.MAX_BANDWIDTH_720P.getValue();

    private int videoMaxTxBandwidth = Phone.DefaultBandwidth.MAX_BANDWIDTH_720P.getValue();

    private int sharingMaxRxBandwidth = Phone.DefaultBandwidth.MAX_BANDWIDTH_SESSION.getValue();

    private String hardwareVideoSetting = DEFAULT_HW_VIDEO_SETTING;

    private boolean hardwareCodecEnable = false;

    private boolean enableCamera2 = true;

    private boolean isAudioEnhancement = false;

    private String audioPlaybackFile = "";

    private String videoPlaybackFile = "";

    private String deviceSettings = null;

    private int maxNumberStreams = DEFAULT_MAX_STREAMS;

    private boolean multistream = true;

    private WMEngine.Camera camera = WMEngine.Camera.FRONT;

    private Map<Class<? extends AdvancedSetting>, AdvancedSetting> settings = null;

    private EnumSet<MediaConstraint> constraints = EnumSet.noneOf(MediaConstraint.class);

    public MediaCapability() {
        setAudioEnhancementModels(null);
    }

    public void setAudioMaxRxBandwidth(int bandwidth) {
        if (bandwidth > 0) {
            this.audioMaxRxBandwidth = bandwidth;
        }
    }

    public void setVideoMaxRxBandwidth(int bandwidth) {
        if (bandwidth > 0) {
            this.videoMaxRxBandwidth = bandwidth;
        }
    }

    public void setVideoMaxTxBandwidth(int bandwidth) {
        if (bandwidth > 0) {
            this.videoMaxTxBandwidth = bandwidth;
        }
    }

    public void setSharingMaxRxBandwidth(int bandwidth) {
        if (bandwidth > 0) {
            this.sharingMaxRxBandwidth = bandwidth;
        }
    }

    public void setHardwareVideoSetting(String hardwareVideoSetting) {
        if (!Checker.isEmpty(hardwareVideoSetting)) {
            this.hardwareVideoSetting = hardwareVideoSetting;
        }
    }

    public boolean isHardwareCodecEnable() {
        return hardwareCodecEnable;
    }

    public void setHardwareCodecEnable(boolean hardwareCodecEnable) {
        this.hardwareCodecEnable = hardwareCodecEnable;
    }

    public boolean isCamera2Enabled() {
        return enableCamera2;
    }

    public boolean isAudioEnhancement() {
        return isAudioEnhancement;
    }

    public void setAudioEnhancementModels(List<String> models) {
        List<String> audioEnhancementModels = models == null ? DEFAULT_AE_MODLES : models;
        String currentModel = Build.MODEL;
        if (currentModel != null) {
            for (String model : audioEnhancementModels) {
                if (currentModel.equalsIgnoreCase(model) || currentModel.startsWith(model)) {
                    Ln.d("Enable audio enhancement for " + currentModel);
                    isAudioEnhancement = true;
                    return;
                }
            }
        }
        isAudioEnhancement = false;
    }

    public void setDeviceSettings(String deviceSettings) {
        this.deviceSettings = deviceSettings;
    }

    public WMEngine.Camera getDefaultCamera() {
        return camera;
    }

    public void setDefaultCamera(WMEngine.Camera camera) {
        this.camera = camera;
    }

    public void setAudioPlaybackFile(String audioPlaybackFile) {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File directory = Environment.getExternalStorageDirectory();
            File file = new File(directory + "/" + audioPlaybackFile);
            if (file.exists()) {
                this.audioPlaybackFile = file.getAbsolutePath();
            }
        }
    }

    public void setVideoPlaybackFile(String videoPlaybackFile) {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File directory = Environment.getExternalStorageDirectory();
            File file = new File(directory + "/" + videoPlaybackFile);
            if (file.exists()) {
                this.videoPlaybackFile = file.getAbsolutePath();
            }
        }
    }

    public void setMultistream(boolean multistream) {
        this.multistream = multistream;
    }

    public int getMaxNumberStreams() {
        return maxNumberStreams;
    }

    public void setAdvanceSettings(Map<Class<? extends AdvancedSetting>, AdvancedSetting> settings) {
        this.settings = settings;
    }

    public void addConstraints(MediaConstraint... constraints) {
        if (!Checker.isEmpty(constraints)) {
            this.constraints.addAll(Arrays.asList(constraints));
        }
    }

    public boolean hasAudio() {
        return constraints != null && (constraints.contains(MediaConstraint.SendAudio) || constraints.contains(MediaConstraint.ReceiveAudio));
    }

    public boolean hasVideo() {
        return constraints != null && (constraints.contains(MediaConstraint.SendVideo) || constraints.contains(MediaConstraint.ReceiveVideo));
    }

    public boolean hasSharing() {
        return constraints != null && (constraints.contains(MediaConstraint.SendSharing) || constraints.contains(MediaConstraint.ReceiveSharing));
    }

    public boolean hasSupport(MediaConstraint... constraints) {
        if (constraints == null || Checker.isEmpty(constraints)) {
            return false;
        }
        for (MediaConstraint constraint : constraints) {
            if (!this.constraints.contains(constraint)) {
                return false;
            }
        }
        return true;
    }

    public void setupConnection(MediaConnection connection) {
        applyGlobalConfig(connection);
        applyAudioConfig(connection);
        applyVideoConfig(connection);
        applySharingConfig(connection);
    }

    private void applyGlobalConfig(MediaConnection connection) {
        MediaConfig.GlobalConfig config = connection.GetGlobalConfig();
        config.EnableMQECallback(true);
        config.EnableICE(true);
        config.EnableSRTP(true);
        config.EnableQos(true);
        config.EnableMultiStream(this.multistream);
        config.EnablePerformanceTraceDump(MediaConfig.WmePerformanceDumpType.WmePerformanceDumpNone);
        config.SetNetworkNotificationParam(MediaConfig.WmeNetworkStatus.WmeNetwork_bad, MediaConfig.WmeNetworkDirection.DIRECTION_BOTHLINK, 5000);
        config.SetNetworkNotificationParam(MediaConfig.WmeNetworkStatus.WmeNetwork_video_off, MediaConfig.WmeNetworkDirection.DIRECTION_BOTHLINK, 7000);
        config.SetNetworkNotificationParam(MediaConfig.WmeNetworkStatus.WmeNetwork_recovered, MediaConfig.WmeNetworkDirection.DIRECTION_BOTHLINK, 10000);
        config.SetICETimeoutParams(10 * 1000, 10 * 1000, 10 * 1000);
        config.SetQoSMaxLossRatio(0.08f);
//        if (!Checker.isEmpty(deviceSettings)) {
//            Ln.d("Default Settings: " + deviceSettings);
//            config.SetDeviceMediaSettings(deviceSettings);
//        }

        // For SPARK-166469
        config.SetDeviceMediaSettings("{\"audio\": {\"AECType\": 2,\"Version\": 4, \"AudioMode\": 2, \"CaptureMode\": 20, \"PlaybackMode\": 12}}");

        if (isHardwareCodecEnable()) {
            Ln.d("HW Settings: " + hardwareVideoSetting);
            config.SetDeviceMediaSettings(hardwareVideoSetting);
        }
//        config.enableTCAEC(false);
//        config.SetShowStunTraceIP(true);
        config.EnableFixAudioProcessingArch(true);
    }

    private void applyAudioConfig(MediaConnection connection) {
        MediaConfig.AudioConfig config = connection.GetAudioConfig(WMEngine.Media.Audio.mid());
        config.SetSelectedCodec(MediaConfig.WmeCodecType.WmeCodecType_OPUS);
        config.SetPreferedCodec(MediaConfig.WmeCodecType.WmeCodecType_OPUS);
        config.EnableFec(true);
        config.EnableRecordLossData(false);
        config.EnableClientMix(1);
        config.SetMaxBandwidth(audioMaxRxBandwidth);
        if (!Checker.isEmpty(audioPlaybackFile)) {
            config.EnableFileCapture(audioPlaybackFile, true);
        }
    }

    private void applySharingConfig(MediaConnection connection) {
        MediaConfig.ShareConfig config = connection.GetShareConfig(WMEngine.Media.Sharing.mid());
        config.SetMaxBandwidth(sharingMaxRxBandwidth);
    }

    private void applyVideoConfig(MediaConnection connection) {
        MediaConfig.VideoConfig config = connection.GetVideoConfig(WMEngine.Media.Video.mid());
        config.EnableFec(true);
        config.EnableRecordLossData(false);
        config.SetPreferedCodec(MediaConfig.WmeCodecType.WmeCodecType_AVC);
        config.SetSelectedCodec(MediaConfig.WmeCodecType.WmeCodecType_AVC);
        config.SetPacketizationMode(MediaConfig.WmePacketizationMode.WmePacketizationMode_1);
        config.SetMaxBandwidth(videoMaxRxBandwidth * 2);

        boolean hw = isHardwareCodecEnable();
        config.EnableHWAcceleration(hw, MediaConfig.WmeHWAccelerationConfig.WmeHWAcceleration_Encoder);
        config.EnableHWAcceleration(hw, MediaConfig.WmeHWAccelerationConfig.WmeHWAcceleration_Decoder);

        MediaSCR decoderSCR = MediaSCR.get(videoMaxRxBandwidth);
        MediaConfig.WmeVideoCodecCapability decoderCodec = new MediaConfig.WmeVideoCodecCapability();
        decoderCodec.uProfileLevelID = decoderSCR.levelId;
        decoderCodec.max_br = videoMaxRxBandwidth / 1000;
        decoderCodec.max_mbps = decoderSCR.maxMbps;
        decoderCodec.max_fs =  decoderSCR.maxFs;
        decoderCodec.max_fps = decoderSCR.maxFps;
        config.SetDecodeParams(MediaConfig.WmeCodecType.WmeCodecType_AVC, decoderCodec);

        MediaSCR encoderSCR = MediaSCR.get(videoMaxTxBandwidth);
        int fps = encoderSCR.maxFps / 100;
        if (!Checker.isEmpty(this.settings)) {
            AdvancedSetting.VideoMaxTxFPS setting = (AdvancedSetting.VideoMaxTxFPS) this.settings.get(AdvancedSetting.VideoMaxTxFPS.class);
            if (setting != null && setting.getValue() != null && setting.getValue() > 0 && !setting.getValue().equals(AdvancedSetting.VideoMaxTxFPS.defaultVaule)) {
                fps = setting.getValue();
            }
        }

        MediaConfig.WmeVideoCodecCapability encoderCodec = new MediaConfig.WmeVideoCodecCapability();
        encoderCodec.uProfileLevelID = encoderSCR.levelId;
        encoderCodec.max_br = videoMaxTxBandwidth / 1000;
        encoderCodec.max_mbps = encoderSCR.maxMbps;
        encoderCodec.max_fs = encoderSCR.maxFs;
        encoderCodec.max_fps = fps * 100;
        config.SetEncodeParams(MediaConfig.WmeCodecType.WmeCodecType_AVC, encoderCodec);

        Ln.d("Video bandwidth: " + videoMaxTxBandwidth + "/" + videoMaxRxBandwidth + "/" + fps);

        config.EnableCHP(false);
        config.Disable90PVideo(true);
        config.EnableAVCSimulcast(true);
        config.EnableSelfPreviewHorizontalMirror(true);
        if (!Checker.isEmpty(videoPlaybackFile)) {
            config.EnableFileCapture(videoPlaybackFile, true);
        }

        if (!Checker.isEmpty(this.settings)) {
            AdvancedSetting.VideoEnableDecoderMosaic mosaic = (AdvancedSetting.VideoEnableDecoderMosaic) this.settings.get(AdvancedSetting.VideoEnableDecoderMosaic.class);
            JSONObject mParams = new JSONObject();
            if (mosaic != null && mosaic.getValue() != AdvancedSetting.VideoEnableDecoderMosaic.defaultVaule) {
                try {
                    mParams.put("enableDecoderMosaic", mosaic.getValue());
                } catch (Exception e) {
                    Ln.e(e);
                }
            }
            if (mParams.length() > 0) {
                JSONObject root = new JSONObject();
                try {
                    root.put("video", mParams);
                } catch (Exception e) {
                    Ln.e(e);
                }
                connection.setParameters(WMEngine.Media.Video.mid(), root.toString());
            }

            AdvancedSetting.VideoEnableCamera2 camera2 = (AdvancedSetting.VideoEnableCamera2) this.settings.get(AdvancedSetting.VideoEnableCamera2.class);
            this.enableCamera2 = camera2 == null ? AdvancedSetting.VideoEnableCamera2.defaultVaule : camera2.getValue();


        }

        JSONObject root = new JSONObject();
        try {
            root.put("enableDPC", true);
        } catch (Exception e) {
            Ln.e(e);
        }
        connection.setParameters(-1, root.toString());
    }
}
