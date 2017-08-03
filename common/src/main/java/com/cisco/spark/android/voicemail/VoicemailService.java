package com.cisco.spark.android.voicemail;

import android.text.TextUtils;

import com.cisco.spark.android.callcontrol.events.CallControlVoicemailInfoEvent;
import com.cisco.spark.android.core.ApplicationController;
import com.cisco.spark.android.core.Component;
import com.cisco.spark.android.mercury.events.VoicemailInfoEvent;
import com.cisco.spark.android.voicemail.model.VoicemailInfoData;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.github.benoitdion.ln.Ln;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
import retrofit2.adapter.rxjava.HttpException;
import rx.schedulers.Schedulers;


@Singleton
public class VoicemailService implements Component {
    private VoicemailClientProvider voicemailClientProvider;
    private EventBus bus;
    private DeviceRegistration deviceRegistration;

    private VoicemailInfoData vmInfo;


    @Inject
    public VoicemailService(VoicemailClientProvider voicemailClientProvider, EventBus bus, DeviceRegistration deviceRegistration) {
        this.voicemailClientProvider = voicemailClientProvider;
        this.bus = bus;
        this.deviceRegistration = deviceRegistration;
        resetVmInfo();
    }


    public boolean isVoicemailEnabled() {
        boolean vmFeatureEnabled = deviceRegistration.getFeatures().isVoicemailV1Enabled();
        boolean vmPilotNumberExists = pilotNumberExists();

        Ln.i("isVoicemailEnabled(): %b, vmFeatureEnabled=%b and vmPilotNumberExists=%b", vmFeatureEnabled && vmPilotNumberExists, vmFeatureEnabled, vmPilotNumberExists);
        return vmFeatureEnabled && vmPilotNumberExists;
    }

    public String getVoicemailPilotNumber() {
        Ln.i("getVoicemailPilotNumber(): '%s'", vmInfo.getVoicemailPilot());
        return vmInfo.getVoicemailPilot();
    }

    public boolean pilotNumberExists() {
        return !TextUtils.isEmpty(vmInfo.getVoicemailPilot());
    }

    public boolean getMwiStatus() {
        Ln.i("getMwiStatus(): %b", vmInfo.getMwiStatus());
        return vmInfo.getMwiStatus();
    }

    public VoicemailInfoData getVmInfoData() {
        return vmInfo;
    }


    public void onEvent(VoicemailInfoEvent event) {
        Ln.i("VoicemailInfoEvent received: %s", event.getVmInfo().toString());
        updateVmInfo(event.getVmInfo());
    }


    private void getVmInfo() {
        Ln.d("getVmInfo()");
        VoicemailClient vmClient = voicemailClientProvider.getVoicemailClient();
        vmClient.getVoicemailInfo().subscribeOn(Schedulers.computation()).subscribe(vmInfo -> updateVmInfo(vmInfo.getVmInfoData()), throwable -> {
            if (throwable instanceof HttpException) {
                HttpException httpResponse = (HttpException) throwable;
                Ln.w(false, "Failed to get VM Info: " + httpResponse.code() + " - " + httpResponse.message());
            }
        });
    }

    private void updateVmInfo(VoicemailInfoData vmInfo) {
        Ln.d("updateVmInfo() with: %s", vmInfo.toString());
        this.vmInfo = vmInfo;
        bus.post(new CallControlVoicemailInfoEvent(vmInfo));
    }

    private void resetVmInfo() {
        Ln.d("resetVmInfo()");
        vmInfo = new VoicemailInfoData();
    }


    // Component Interface implementations
    @Override
    public boolean shouldStart() {
        boolean isFeatureEnabled = deviceRegistration.getFeatures().isVoicemailV1Enabled();
        Ln.i("App->VoicemailService: shouldStart (i.e. is voicemail feature enabled) = %b", isFeatureEnabled);
        return isFeatureEnabled;
    }

    @Override
    public void start() {
        Ln.i("App->VoicemailService: start()");
        if (!bus.isRegistered(this))
            bus.register(this);
        getVmInfo();
    }

    @Override
    public void stop() {
        Ln.i("App->VoicemailService: stop()");
        if (bus.isRegistered(this))
            bus.unregister(this);
    }

    public void setApplicationController(ApplicationController applicationController) {
        Ln.i("setApplicationController()");
        applicationController.register(this);
    }
}
