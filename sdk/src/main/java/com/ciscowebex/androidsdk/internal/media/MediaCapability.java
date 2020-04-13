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

    private int audioMaxBandwidth = Phone.DefaultBandwidth.MAX_BANDWIDTH_AUDIO.getValue() ;

    private int videoMaxBandwidth = Phone.DefaultBandwidth.MAX_BANDWIDTH_720P.getValue() ;

    private int sharingMaxBandwidth = Phone.DefaultBandwidth.MAX_BANDWIDTH_SESSION.getValue() ;

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

    public void setAudioMaxBandwidth(int bandwidth) {
        if (bandwidth > 0) {
            this.audioMaxBandwidth = bandwidth;
        }
    }

    public void setVideoMaxBandwidth(int bandwidth) {
        if (bandwidth > 0) {
            this.videoMaxBandwidth = bandwidth;
        }
    }

    public void setSharingMaxBandwidth(int bandwidth) {
        if (bandwidth > 0) {
            this.sharingMaxBandwidth = bandwidth;
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
        if (isHardwareCodecEnable()) {
            config.SetDeviceMediaSettings(hardwareVideoSetting);
        }
        if (!Checker.isEmpty(deviceSettings)) {
            config.SetDeviceMediaSettings(deviceSettings);
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
        config.SetMaxBandwidth(audioMaxBandwidth);
        if (!Checker.isEmpty(audioPlaybackFile)) {
            config.EnableFileCapture(audioPlaybackFile, true);
        }
    }

    private void applyConfig(MediaConfig.ShareConfig config) {
        config.SetMaxBandwidth(sharingMaxBandwidth);
    }

    private void applyConfig(MediaConfig.VideoConfig config) {
        config.EnableFec(true);
        config.EnableRecordLossData(false);
        config.SetPreferedCodec(MediaConfig.WmeCodecType.WmeCodecType_AVC);
        config.SetSelectedCodec(MediaConfig.WmeCodecType.WmeCodecType_AVC);
        config.SetPacketizationMode(MediaConfig.WmePacketizationMode.WmePacketizationMode_1);
        config.SetMaxBandwidth(videoMaxBandwidth);
        boolean hw = isHardwareCodecEnable();
        long hardwareEncoderEnabled = config.EnableHWAcceleration(hw, MediaConfig.WmeHWAccelerationConfig.WmeHWAcceleration_Encoder);
        long hardwareDecoderEnabled = config.EnableHWAcceleration(hw, MediaConfig.WmeHWAccelerationConfig.WmeHWAcceleration_Decoder);
        String profileID;
        int maxMbps, maxFs, maxBr;
        if (hw && MediaError.succeeded(hardwareEncoderEnabled) && MediaError.succeeded(hardwareDecoderEnabled)) {
            profileID = "420014";
            maxMbps = 108000;
            maxFs = 3600;
            maxBr = 1500;
        }
        else { // 360p
            profileID = "42000D";
            maxMbps = 27600;
            maxFs = 920;
            maxBr = 1000;
        }
        int maxFps = 3000;
        MediaConfig.WmeVideoCodecCapability videoEncoderCodecCapability = new MediaConfig.WmeVideoCodecCapability();
        videoEncoderCodecCapability.uProfileLevelID = Long.parseLong(profileID, 16);
        videoEncoderCodecCapability.max_mbps = maxMbps;
        videoEncoderCodecCapability.max_fs = maxFs;
        videoEncoderCodecCapability.max_br = maxBr;
        videoEncoderCodecCapability.max_fps = maxFps;
        config.SetEncodeParams(MediaConfig.WmeCodecType.WmeCodecType_AVC, videoEncoderCodecCapability);
        MediaConfig.WmeVideoCodecCapability videoDecoderCodecCapability = new MediaConfig.WmeVideoCodecCapability();
        videoDecoderCodecCapability.uProfileLevelID = Long.parseLong(profileID, 16);
        videoDecoderCodecCapability.max_mbps = maxMbps;
        videoDecoderCodecCapability.max_fs = 8160;
        videoDecoderCodecCapability.max_br = maxBr;
        videoDecoderCodecCapability.max_fps = maxFps;
        config.SetDecodeParams(MediaConfig.WmeCodecType.WmeCodecType_AVC, videoDecoderCodecCapability);
        config.Disable90PVideo(true);
        config.EnableAVCSimulcast(true);
        config.EnableSelfPreviewHorizontalMirror(true);
        //config.SetInitSubscribeCount(maxNumberStreams);
        if (!Checker.isEmpty(videoPlaybackFile)) {
            config.EnableFileCapture(videoPlaybackFile, true);
        }
        config.SetPacketizationMode(MediaConfig.WmePacketizationMode.WmePacketizationMode_0);
    }
}
