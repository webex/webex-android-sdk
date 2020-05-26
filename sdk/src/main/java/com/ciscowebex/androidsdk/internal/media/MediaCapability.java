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
import com.ciscowebex.androidsdk.phone.Phone;
import com.ciscowebex.androidsdk.utils.Lists;
import com.github.benoitdion.ln.Ln;
import com.webex.wme.MediaConfig;
import com.webex.wme.MediaConnection;
import me.helloworld.utils.Checker;

import java.io.File;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

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

    private boolean isAudioEnhancement = false;

    private String audioPlaybackFile = "";

    private String videoPlaybackFile = "";

    private String deviceSettings = null;

    private int maxNumberStreams = DEFAULT_MAX_STREAMS;

    private boolean multistream = true;

    private WMEngine.Camera camera = WMEngine.Camera.FRONT;

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
        applyConfig(connection.GetGlobalConfig());
        applyConfig(connection.GetAudioConfig(WMEngine.Media.Audio.mid()));
        applyConfig(connection.GetVideoConfig(WMEngine.Media.Video.mid()));
        applyConfig(connection.GetShareConfig(WMEngine.Media.Sharing.mid()));
    }

    private void applyConfig(MediaConfig.GlobalConfig config) {
        config.EnableMQECallback(true);
        config.EnableICE(true);
        config.EnableSRTP(true);
        config.EnableQos(true);
        config.EnableMultiStream(multistream);
        config.EnablePerformanceTraceDump(MediaConfig.WmePerformanceDumpType.WmePerformanceDumpNone);
        config.SetNetworkNotificationParam(MediaConfig.WmeNetworkStatus.WmeNetwork_bad, MediaConfig.WmeNetworkDirection.DIRECTION_BOTHLINK, 5000);
        config.SetNetworkNotificationParam(MediaConfig.WmeNetworkStatus.WmeNetwork_video_off, MediaConfig.WmeNetworkDirection.DIRECTION_BOTHLINK, 7000);
        config.SetNetworkNotificationParam(MediaConfig.WmeNetworkStatus.WmeNetwork_recovered, MediaConfig.WmeNetworkDirection.DIRECTION_BOTHLINK, 10000);
        config.SetICETimeoutParams(10 * 1000, 10 * 1000, 10 * 1000);
        config.SetQoSMaxLossRatio(0.08f);
        if (!Checker.isEmpty(deviceSettings)) {
            Ln.d("Default Settings: " + deviceSettings);
            config.SetDeviceMediaSettings(deviceSettings);
        }
        if (isHardwareCodecEnable()) {
            Ln.d("HW Settings: " + hardwareVideoSetting);
            config.SetDeviceMediaSettings(hardwareVideoSetting);
        }
        // config.enableTCAEC(false);
        // config.SetShowStunTraceIP(true);
        config.EnableFixAudioProcessingArch(true);
    }

    private void applyConfig(MediaConfig.AudioConfig config) {
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

    private void applyConfig(MediaConfig.ShareConfig config) {
        config.SetMaxBandwidth(sharingMaxRxBandwidth);
    }

    private void applyConfig(MediaConfig.VideoConfig config) {
        config.EnableFec(true);
        config.EnableRecordLossData(false);
        config.SetPreferedCodec(MediaConfig.WmeCodecType.WmeCodecType_AVC);
        config.SetSelectedCodec(MediaConfig.WmeCodecType.WmeCodecType_AVC);
        config.SetPacketizationMode(MediaConfig.WmePacketizationMode.WmePacketizationMode_1);
        config.SetMaxBandwidth(videoMaxRxBandwidth);

        boolean hw = isHardwareCodecEnable();
        config.EnableHWAcceleration(hw, MediaConfig.WmeHWAccelerationConfig.WmeHWAcceleration_Encoder);
        config.EnableHWAcceleration(hw, MediaConfig.WmeHWAccelerationConfig.WmeHWAcceleration_Decoder);

        MediaConfig.WmeVideoCodecCapability videoDecoderCodecCapability = new MediaConfig.WmeVideoCodecCapability();
        videoDecoderCodecCapability.uProfileLevelID = hw ? 0x420016 : 0x42000D;
        videoDecoderCodecCapability.max_mbps = hw ? MediaSCR.p720.maxMbps : MediaSCR.p360.maxMbps;
        videoDecoderCodecCapability.max_fs =  MediaSCR.p1080.maxFs; // hw ? MediaSCR.p720.maxFs : MediaSCR.p360.maxFs;
        videoDecoderCodecCapability.max_br = (hw ? MediaSCR.p720.maxBr : MediaSCR.p360.maxBr)/1000;
        videoDecoderCodecCapability.max_fps = hw ? MediaSCR.p720.maxFps : MediaSCR.p360.maxFps;
        config.SetDecodeParams(MediaConfig.WmeCodecType.WmeCodecType_AVC, videoDecoderCodecCapability);

        int bitrates = videoMaxTxBandwidth / 1000;
        int levelId = 0x420016;
        if (bitrates <= 384) {
            levelId = 0x42000C;
        }
        else if (bitrates <= 768) {
            levelId = 0x42000D;
        }
        MediaConfig.WmeVideoCodecCapability codecCapability = new MediaConfig.WmeVideoCodecCapability();
        codecCapability.uProfileLevelID = levelId;
        codecCapability.max_br = bitrates;
        codecCapability.max_mbps = 0;
        codecCapability.max_fs = 0;
        codecCapability.max_fps = 0;
        config.SetEncodeParams(MediaConfig.WmeCodecType.WmeCodecType_AVC, codecCapability);

        config.Disable90PVideo(true);
        config.EnableAVCSimulcast(true);
        config.EnableSelfPreviewHorizontalMirror(true);
        //config.SetInitSubscribeCount(maxNumberStreams);
        if (!Checker.isEmpty(videoPlaybackFile)) {
            config.EnableFileCapture(videoPlaybackFile, true);
        }
    }
}
