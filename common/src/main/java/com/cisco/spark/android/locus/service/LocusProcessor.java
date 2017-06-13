package com.cisco.spark.android.locus.service;

import android.net.Uri;

import com.cisco.spark.android.callcontrol.events.CallControlBindingEvent;
import com.cisco.spark.android.callcontrol.events.CallControlHeldEvent;
import com.cisco.spark.android.callcontrol.events.CallControlMeetingControlsEvent;
import com.cisco.spark.android.callcontrol.events.CallControlMeetingControlsLockEvent;
import com.cisco.spark.android.callcontrol.events.CallControlMeetingRecordEvent;
import com.cisco.spark.android.callcontrol.events.CallControlModeratorMutedParticipantEvent;
import com.cisco.spark.android.callcontrol.events.CallControlParticipantAudioMuteEvent;
import com.cisco.spark.android.callcontrol.events.CallControlParticipantVideoMutedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlResumedEvent;
import com.cisco.spark.android.callcontrol.events.CallControlSelfParticipantLeftEvent;
import com.cisco.spark.android.callcontrol.events.DismissCallNotificationEvent;
import com.cisco.spark.android.core.ApiClientProvider;
import com.cisco.spark.android.locus.events.FloorGrantedEvent;
import com.cisco.spark.android.locus.events.FloorReleasedEvent;
import com.cisco.spark.android.locus.events.IncomingCallEvent;
import com.cisco.spark.android.locus.events.JoinedLobbyEvent;
import com.cisco.spark.android.locus.events.JoinedMeetingEvent;
import com.cisco.spark.android.locus.events.LocusDataCacheChangedEvent;
import com.cisco.spark.android.locus.events.LocusDataCacheReplacesEvent;
import com.cisco.spark.android.locus.events.LocusPmrChangedEvent;
import com.cisco.spark.android.locus.events.ParticipantChangedEvent;
import com.cisco.spark.android.locus.events.ParticipantDeclinedEvent;
import com.cisco.spark.android.locus.events.ParticipantJoinedEvent;
import com.cisco.spark.android.locus.events.ParticipantJoinedLobbyEvent;
import com.cisco.spark.android.locus.events.ParticipantLeftEvent;
import com.cisco.spark.android.locus.events.ParticipantNotifiedEvent;
import com.cisco.spark.android.locus.events.ParticipantPairedWithRoomSystemEvent;
import com.cisco.spark.android.locus.events.ParticipantUnPairedWithRoomSystemEvent;
import com.cisco.spark.android.locus.model.Locus;
import com.cisco.spark.android.locus.model.LocusControl;
import com.cisco.spark.android.locus.model.LocusData;
import com.cisco.spark.android.locus.model.LocusDataCache;
import com.cisco.spark.android.locus.model.LocusKey;
import com.cisco.spark.android.locus.model.LocusParticipant;
import com.cisco.spark.android.locus.model.LocusParticipantAudioControl;
import com.cisco.spark.android.locus.model.LocusParticipantControls;
import com.cisco.spark.android.locus.model.LocusParticipantDevice;
import com.cisco.spark.android.locus.model.LocusReplaces;
import com.cisco.spark.android.locus.model.LocusSelfRepresentation;
import com.cisco.spark.android.locus.model.LocusSequenceInfo;
import com.cisco.spark.android.locus.model.MediaDirection;
import com.cisco.spark.android.mercury.events.DeclineReason;
import com.cisco.spark.android.mercury.events.LocusChangedEvent;
import com.cisco.spark.android.mercury.events.LocusDeltaEvent;
import com.cisco.spark.android.mercury.events.LocusEvent;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import javax.inject.Provider;

import de.greenrobot.event.EventBus;

import static com.cisco.spark.android.sync.ConversationContentProviderOperation.updateConversationLocusUrl;

public class LocusProcessor {
    private final EventBus bus;
    private final LocusDataCache locusDataCache;
    private final ApiClientProvider apiClientProvider;
    private final NaturalLog ln;
    private final DeviceRegistration deviceRegistration;
    private final Provider<Batch> batchProvider;
    private Calendar utcCalendar;

    public LocusProcessor(final ApiClientProvider apiClientProvider, final EventBus bus, final LocusDataCache locusDataCache, Ln.Context lnContext, DeviceRegistration deviceRegistration, Provider<Batch> batchProvider) {
        this.apiClientProvider = apiClientProvider;
        this.bus = bus;
        this.locusDataCache = locusDataCache;
        this.batchProvider = batchProvider;
        this.ln = Ln.get(lnContext, "LocusProcessor");
        this.deviceRegistration = deviceRegistration;

        utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Process locus DTO update (this can come as response to locus REST request or as event over mercury).  Check if DTO should, based on sequence info,
     * overwrite existing DTO and, if so, determine and post the appropriate event based on difference between old and new locus DTO
     *
     * @param remoteLocus
     */
    public synchronized void processLocusUpdate(Locus remoteLocus) {
        processLocusUpdate(remoteLocus, new LocusChangedEvent());
    }

    /**
     * Process locus DTO update (this can come as response to locus REST request or as event over mercury).  Check if DTO should, based on sequence info,
     * overwrite existing DTO and, if so, determine and post the appropriate event based on difference between old and new locus DTO
     *
     * @param remoteLocus
     * @Param LocusEvent. a)LocusChangedEvent: Locus non-delta event, that means client receives the Locus whole DTO,
     *                    b)LocusDeltaEvent: Locus delta event, that means client receives the Locus Delta DTO.
     */
    public synchronized void processLocusUpdate(Locus remoteLocus, LocusEvent locusEvent) {

        boolean processLocus = false;
        LocusData call = locusDataCache.getLocusData(remoteLocus.getKey());
        Locus localLocus = null;
        if (call != null) {
            localLocus = call.getLocus();

            LocusSequenceInfo.OverwriteWithResult overwriteWithResult = localLocus.getSequence().overwriteWith(remoteLocus.getSequence());
            if (overwriteWithResult.equals(LocusSequenceInfo.OverwriteWithResult.FALSE) &&
                    localLocus.getFullState().isStartingSoon() != remoteLocus.getFullState().isStartingSoon()) {
                // Due to an issue in Locus at the moment where the sequence information is not incremented when
                // startingSoon is updated, we need to manually detect that change and force the overwrite value to
                // true so we process the update when we otherwise would not.
                Ln.d("Overwrite was FALSE, but startingSoon flags do not match so forcing overwrite to TRUE");
                overwriteWithResult = LocusSequenceInfo.OverwriteWithResult.TRUE;
            }

            if (overwriteWithResult.equals(LocusSequenceInfo.OverwriteWithResult.TRUE)) {
                if (deviceRegistration.getFeatures().isDeltaEventEnabled() && (locusEvent instanceof LocusDeltaEvent)) {
                    ln.i("processLocusUpdate() receive a delta event, local locus sequence %s, remote locus sequence %s, remote locus base sequence %s.", localLocus.getSequence(), remoteLocus.getSequence(), remoteLocus.getBaseSequence());

                    // if the local locus copying was more recent or the same as the base locus, but less recent than the target locus, apply the Locus Delta DTO
                    // otherwise, call SyncURL to get new Locus Delta DTO
                    LocusSequenceInfo.OverwriteWithResult overwriteWithResult4BaseSeq = localLocus.getSequence().overwriteWithBaseSequence(remoteLocus.getBaseSequence());
                    ln.i("processLocusUpdate() local locus sequence compare with remote locus base sequence, result is" + overwriteWithResult4BaseSeq);

                    if (overwriteWithResult4BaseSeq.equals(LocusSequenceInfo.OverwriteWithResult.TRUE)) {
                        // Merge the locus delta DTO(participants info) instead of replacing the whole locus
                        Locus locusDelta = mergeLocusDelta(localLocus, remoteLocus);
                        call.setLocus(locusDelta);
                        processLocus = true;

                        ln.i("Updating locus DTO and notifying listeners of data change for: %s", call.getKey().toString());
                        if (isMyPmrChanged(call)) {
                            bus.post(new LocusPmrChangedEvent());
                        }
                        bus.post(new LocusDataCacheChangedEvent(remoteLocus.getKey(), LocusDataCacheChangedEvent.Type.MODIFIED));
                    } else if (overwriteWithResult4BaseSeq.equals(LocusSequenceInfo.OverwriteWithResult.DESYNC)) {
                        ln.i("local locus sequence is less than remote locus base sequence, call SyncUrl");
                        //we've detected de-sync state so re-fetch Locus DTO
                        Locus newDeltaLocus = apiClientProvider.getHypermediaLocusClient().getLocusSync(remoteLocus.getSyncUrl().toString());
                        if (newDeltaLocus != null) {
                            processLocusUpdate(newDeltaLocus, new LocusDeltaEvent());
                        }
                    }
                } else {
                    call.setLocus(remoteLocus);
                    processLocus = true;

                    ln.i("Updating locus DTO and notifying listeners of data change for: %s", call.getKey().toString());
                    if (isMyPmrChanged(call)) {
                        bus.post(new LocusPmrChangedEvent());
                    }
                    bus.post(new LocusDataCacheChangedEvent(remoteLocus.getKey(), LocusDataCacheChangedEvent.Type.MODIFIED));
                }
            } else if (overwriteWithResult.equals(LocusSequenceInfo.OverwriteWithResult.FALSE)) {
                ln.i("Didn't overwrite locus DTO as new one was older version than one currently in memory.");
            } else if (overwriteWithResult.equals(LocusSequenceInfo.OverwriteWithResult.DESYNC)) {
                ln.i("Didn't overwrite locus DTO as new one was out of sync with one currently in memory.");

                // we've detected de-sync state so re-fetch locus dto
                Locus newLocus = apiClientProvider.getHypermediaLocusClient().getLocusSync(call.getKey().getUrl().toString());
                processLocusUpdate(newLocus);
            }
        } else {
            // locus doesn't exist already so store
            call = new LocusData(remoteLocus);
            locusDataCache.putLocusData(call);
            processLocus = true;

            ln.i("LocusProcessor: Notifying listeners of new entry for: %s", call.getKey().toString());
            if (isMyPmrChanged(call)) {
                bus.post(new LocusPmrChangedEvent());
            }
            bus.post(new LocusDataCacheChangedEvent(call.getKey(), LocusDataCacheChangedEvent.Type.ADDED));
        }

        if (processLocus) {
            if (remoteLocus.getConversationUrl() != null && (localLocus == null || !Strings.equals(remoteLocus.getConversationUrl(), localLocus.getConversationUrl()))) {
                Batch batch = batchProvider.get();
                batch.add(updateConversationLocusUrl(Uri.parse(remoteLocus.getConversationUrl()), remoteLocus.getKey()));
                batch.apply();
            }

            // check if we should toast
            checkIncomingCallNotification(call, localLocus);

            // updating observing resource if we're now in OBSERVING state.  this is needed specifically
            // to address case where we move call from endpoint to local device (at which point we're no
            // longer observing) but need to use previously set observing resource value to allow us to
            // hangup endpoint.
            call.setObservingResource();


            // check for participant joins/leaves, lock/unlock, self start/stop recording
            if (localLocus != null) {
                processSelfChanges(localLocus, remoteLocus);
                processParticipants(call, localLocus, remoteLocus);
                processMediaMutedStatus(localLocus, remoteLocus);
            }

            processImplicitBinding(localLocus, remoteLocus);
            processMeeting(localLocus, remoteLocus);
            processLocusControlChanges(localLocus, remoteLocus);
            processFloorStateChange(localLocus, remoteLocus);
            processMoveMediaWhileFloorGranted(localLocus, remoteLocus);
            processReplaces(remoteLocus);
        }
    }


    private void processImplicitBinding(Locus local, Locus remote) {
        if (!deviceRegistration.getFeatures().isImplicitBindingForCallEnabled() || remote == null)
            return;
        boolean isLocalObserving = false;
        boolean isRemoteObserving = false;
        Uri deviceUrl = deviceRegistration.getUrl();
        LocusParticipant.Intent remoteIntent = remote.getIntent(deviceUrl);
        if (local != null) {
            LocusParticipant.Intent localIntent = local.getIntent(deviceUrl);
            isLocalObserving = localIntent != null && localIntent.getType() != null &&
                    (localIntent.getType().equals(LocusParticipant.IntentType.OBSERVE) || localIntent.getType().equals(LocusParticipant.IntentType.MOVE_MEDIA));
        }
        isRemoteObserving = remoteIntent != null && remoteIntent.getType() != null &&
                (remoteIntent.getType().equals(LocusParticipant.IntentType.OBSERVE) || remoteIntent.getType().equals(LocusParticipant.IntentType.MOVE_MEDIA));
        if (!isLocalObserving && isRemoteObserving) {
            bus.post(new CallControlBindingEvent(CallControlBindingEvent.BindingEventType.BIND, remote, null));
        }
    }

    private void processReplaces(Locus locus) {
        List<LocusReplaces> replacesList = null;
        if (locus.getReplaces() != null)
            replacesList = locus.getReplaces();

        // Note that locusReplaces info can be present in 2 cases:
        // (1) Call was migrated (transfer, forward to VM, or other party has merged this call)
        // (2) Local user has merged 2 calls in to one (replacesList contains 2 entries in this case for the 2 existing calls)

        if (replacesList != null && replacesList.size() > 0) {
            ln.d("processReplaces()");


            // need to firstly determine if this result from merge request...this will tell us which existing locus data instance
            // to copy call info over from.if this was as a result of
            // If this was a result of transfer/forward/being merged, then copy over from one locus data instance that we have in local cache
            boolean mergeInitiated = false;
            for (LocusReplaces replace : replacesList) {
                LocusData oldLocusData = locusDataCache.getLocusData(replace.getLocusKey());
                if (oldLocusData != null && oldLocusData.isInitiatedMerge()) {
                    mergeInitiated = true;
                }
            }


            for (LocusReplaces replace : replacesList) {
                LocusData oldLocusData = locusDataCache.getLocusData(replace.getLocusKey());
                if (oldLocusData != null) {
                    if (!mergeInitiated || oldLocusData.isInitiatedMerge()) {

                        ln.i("Replace LocusData in new DTO (%s), with LocusData from old/replaces DTO (%s)", locus.getKey(), replace.getLocusKey());
                        LocusData newLocusData = locusDataCache.getLocusData(locus.getKey());

                        if (newLocusData != null) {
                            newLocusData.setEpochStartTime(oldLocusData.getEpochStartTime());
                            newLocusData.setCallConnected(oldLocusData.isCallConnected());
                            newLocusData.setCallStarted(oldLocusData.isCallStarted());
                            newLocusData.setWasMediaFlowing(oldLocusData.wasMediaFlowing());
                            newLocusData.setCallContext(oldLocusData.getCallContext());
                            newLocusData.setIsToasting(oldLocusData.isToasting());
                            newLocusData.setCallInitiationOrigin(oldLocusData.getCallInitiationOrigin());
                            newLocusData.setActiveSpeakerId(oldLocusData.getActiveSpeaker() != null ? oldLocusData.getActiveSpeaker().getId() : null);
                            newLocusData.setRemoteParticipantDetails();
                            newLocusData.setMediaSession(oldLocusData.getMediaSession());
                        }
                    }

                    ln.i("Remove %s from the LocusDataCache to complete the replacement", replace.getLocusKey());
                    locusDataCache.removeLocusData(replace.getLocusKey());
                    bus.post(new LocusDataCacheReplacesEvent(locus.getKey(), oldLocusData.getKey()));
                }
            }
        }
    }

    public void checkIncomingCallNotification(LocusData call, Locus local) {

        // only toast if we're in IDLE state, alertType=FULL, we're not already toasting and it's earlier than alertType expiration
        if (call.getLocus().getSelf() != null && call.getLocus().getSelf().getAlertType() != null) {
            String alertTypeAction = call.getLocus().getSelf().getAlertType().getAction();
            Date expiration = call.getLocus().getSelf().getAlertType().getExpiration();
            LocusParticipant.State selfState = call.getLocus().getSelf().getState();
            ln.i("Locus alertType action: " + alertTypeAction + ", expiration = " + expiration
                    + ", self state = " + selfState.name() + ", isToasting = " + call.isToasting()
                    + ", local locus = %s", local == null ? "null" : "not null");

            boolean shouldPostEvent = false;
            if (selfState.equals(LocusParticipant.State.IDLE) && LocusSelfRepresentation.AlertType.ALERT_FULL.equals(alertTypeAction)
                    && !call.isObserving(deviceRegistration.getUrl())
                    && !call.isToasting() && expiration.after(utcCalendar.getTime())
                    && !call.getLocus().isInLobbyFromThisDevice(deviceRegistration.getUrl())) {
                shouldPostEvent = true;
            } else if (local == null && selfState.equals(LocusParticipant.State.NOTIFIED)) {
                shouldPostEvent = true;
            }

            if (shouldPostEvent) {
                call.setIsToasting(true);
                bus.post(new IncomingCallEvent(call.getKey()));
            } else if (LocusSelfRepresentation.AlertType.ALERT_NONE.equals(alertTypeAction)) {
                bus.post(new DismissCallNotificationEvent(call.getKey()));
            }
        }
    }

    private void processParticipantDevicesForRoomSystem(LocusKey locusKey, LocusParticipant localParticipant, LocusParticipant remoteParticipant) {
        Map<Uri, LocusParticipantDevice> localDevicesMap = new HashMap<Uri, LocusParticipantDevice>();
        for (LocusParticipantDevice device : localParticipant.getDevices()) {
            localDevicesMap.put(device.getUrl(), device);
        }

        for (LocusParticipantDevice remoteDevice: remoteParticipant.getDevices()) {
            LocusParticipantDevice localDevice = localDevicesMap.get(remoteDevice.getUrl());
            if (localDevice != null) {
                if (localDevice.getIntent() == null) {
                    // previously it was an unpaired device but now it is paired with the room system
                    if (remoteDevice.getIntent() != null && LocusParticipant.IntentType.OBSERVE.equals(remoteDevice.getIntent().getType())
                            && remoteDevice.getIntent().getAssociatedWith() != null) {
                        bus.post(new ParticipantPairedWithRoomSystemEvent(locusKey, remoteParticipant, remoteDevice, remoteDevice.getIntent().getAssociatedWith()));
                    }
                } else {
                    // previously it was an paired device but now it is unpaired with the room system
                    if (LocusParticipant.IntentType.OBSERVE.equals(localDevice.getIntent().getType())
                            && localDevice.getIntent().getAssociatedWith() != null && remoteDevice.getIntent() == null) {
                        bus.post(new ParticipantUnPairedWithRoomSystemEvent(locusKey, remoteParticipant, remoteDevice));
                    }
                }
            } else {
                // is new device and also is paired with a room system.
                if (LocusParticipant.State.JOINED.equals(remoteDevice.getState()) && remoteDevice.getIntent() != null
                        && LocusParticipant.IntentType.OBSERVE.equals(remoteDevice.getIntent().getType())
                        && remoteDevice.getIntent().getAssociatedWith() != null) {
                    bus.post(new ParticipantPairedWithRoomSystemEvent(locusKey, remoteParticipant, remoteDevice, remoteDevice.getIntent().getAssociatedWith()));
                }
            }
        }
    }

    public void processParticipants(LocusData call, Locus local, Locus remote) {
        Map<UUID, LocusParticipant> localParticipantsMap = participantsToMap(local.getParticipants());
        List<LocusParticipant> remoteParticipants = remote.getParticipants();

        List<LocusParticipant> joined = new ArrayList<LocusParticipant>();
        List<LocusParticipant> left = new ArrayList<LocusParticipant>();
        List<LocusParticipant> idle = new ArrayList<LocusParticipant>();

        for (LocusParticipant participant : remoteParticipants) {
            if (localParticipantsMap.containsKey(participant.getId())) {
                LocusParticipant localParticipant = localParticipantsMap.get(participant.getId());

                // check change in participant display name
                if (!localParticipant.getPerson().getDisplayName().equals(participant.getPerson().getDisplayName())) {
                    Ln.d("Change to participant display name, new name = " + participant.getPerson().getDisplayName());
                    if (call.isOneOnOne()) {
                        call.setRemoteParticipantDetails();
                    }
                    bus.post(new ParticipantChangedEvent(remote.getKey(), participant.getId()));
                }

                if (localParticipant.getState().equals(participant.getState())) {
                    // for each joined participant, compare each device between local and remote for pairing/unpairing the  room system
                    if (localParticipant.getState() == LocusParticipant.State.JOINED) {
                        processParticipantDevicesForRoomSystem(remote.getKey(), localParticipant, participant);
                    }
                    continue;
                }
            }
            if (participant.getState() == LocusParticipant.State.JOINED) {
                joined.add(participant);
            } else if (participant.getState() == LocusParticipant.State.LEFT) {
                left.add(participant);
            } else if (participant.getState() == LocusParticipant.State.IDLE) {
                idle.add(participant);
            } else if (participant.getState() == LocusParticipant.State.DECLINED) {
                if (call.isOneOnOne() && !remote.getSelf().getId().equals(participant.getId())) {
                    bus.post(new ParticipantDeclinedEvent(remote.getKey(), DeclineReason.UNKNOWN));
                }
            } else if (participant.getState() == LocusParticipant.State.NOTIFIED) {
                bus.post(new ParticipantNotifiedEvent(remote.getKey(), participant.getId()));
            } else {
                bus.post(new ParticipantChangedEvent(remote.getKey(), participant.getId()));
            }
        }

        if (!joined.isEmpty())
            bus.post(new ParticipantJoinedEvent(remote.getKey(), remote, joined));
        if (!left.isEmpty()) {
            bus.post(new ParticipantLeftEvent(remote.getKey(), remote, left));
            if (deviceRegistration.getFeatures().isImplicitBindingForCallEnabled()) {
                bus.post(new CallControlBindingEvent(CallControlBindingEvent.BindingEventType.UNBIND, remote, left));
            }
        }
        if (!idle.isEmpty())
            bus.post(new ParticipantJoinedLobbyEvent());
    }

    /**
     * Check for any changes in our own state (e.g. if we got booted from call)
     *
     * @param local
     * @param remote
     */
    public void processSelfChanges(Locus local, Locus remote) {

        // check hold/resume
        if (!local.isHeld() && remote.isHeld()) {
            bus.post(new CallControlHeldEvent(remote.getKey()));
        } else if (local.isHeld() && !remote.isHeld()) {
            bus.post(new CallControlResumedEvent(remote.getKey()));
        }


        if (local.getSelf() != null && remote.getSelf() != null) {
            LocusSelfRepresentation localSelf = local.getSelf();
            LocusSelfRepresentation remoteSelf = remote.getSelf();

            // check if we're in left state (e.g. if we were booted from call)
            if (!localSelf.getState().equals(remoteSelf.getState())) {
                if (remoteSelf.getState() == LocusParticipant.State.LEFT) {
                    ln.i("processSelfChanges, detected that self is now in LEFT state in locus");
                    bus.post(new CallControlSelfParticipantLeftEvent(remote.getKey()));
                }
            }
        }
    }

    // check if locus controls state was changed -> lock/unlock and start/stop recording
    public void processLocusControlChanges(Locus local, Locus remote) {
        LocusControl remoteLocusControl = remote.getControls();

        if (remoteLocusControl != null) {
            boolean localLocusControlLocked = local != null ? local.getControls().getLock().isLocked() : false;
            boolean localLocusControlRecording = local != null ? local.getControls().getRecord().isRecording() : false;
            boolean remoteLocusControlLocked = remoteLocusControl.getLock().isLocked();

            if (localLocusControlLocked != remoteLocusControlLocked) {
                ln.d("processLocusControlChanges, meeting is locked: " + remoteLocusControlLocked);
                bus.post(new CallControlMeetingControlsLockEvent(remote.getKey(), true, remoteLocusControlLocked));
            }
            if (localLocusControlRecording != remote.getControls().getRecord().isRecording()) {
                ln.d("processLocusControlChanges, meeting is being recorded: " + remote.getControls().getRecord().isRecording());
                bus.post(new CallControlMeetingRecordEvent());
            }
        }
    }

    /**
     * Compare old/new versions of locus dto for changes to media muted status
     *
     * @param local
     * @param remote
     */
    public void processMediaMutedStatus(Locus local, Locus remote) {
        Map<UUID, LocusParticipant> localParticipantsMap = participantsToMap(local.getParticipants());
        List<LocusParticipant> remoteParticipants = remote.getParticipants();


        // Retrieve id of self either from remote or local locus
        LocusParticipant remoteSelf = remote.getSelf();

        String selfId = (remoteSelf != null && remoteSelf.getPerson() != null) ? remoteSelf.getPerson().getId() : null;

        if (selfId == null) {
            LocusParticipant localSelf = local.getSelf();
            selfId = (localSelf != null && localSelf.getPerson() != null) ? localSelf.getPerson().getId() : null;
        }

        for (LocusParticipant remoteParticipant : remoteParticipants) {
            if (localParticipantsMap.containsKey(remoteParticipant.getId())) {
                LocusParticipant localParticipant = localParticipantsMap.get(remoteParticipant.getId());

                MediaDirection localVideoDirection = localParticipant.getStatus().getVideoStatus();
                MediaDirection localAudioDirection = localParticipant.getStatus().getAudioStatus();
                MediaDirection remoteVideoDirection = remoteParticipant.getStatus().getVideoStatus();
                MediaDirection remoteAudioDirection = remoteParticipant.getStatus().getAudioStatus();

                // check change to video muted status
                if (MediaDirection.SENDRECV.equals(localVideoDirection) && MediaDirection.RECVONLY.equals(remoteVideoDirection)) {
                    bus.post(new CallControlParticipantVideoMutedEvent(remote.getKey(), remoteParticipant, true));
                } else if (MediaDirection.RECVONLY.equals(localVideoDirection) && MediaDirection.SENDRECV.equals(remoteVideoDirection)) {
                    bus.post(new CallControlParticipantVideoMutedEvent(remote.getKey(), remoteParticipant, false));
                }

                if (MediaDirection.SENDRECV.equals(localAudioDirection) && MediaDirection.RECVONLY.equals(remoteAudioDirection)) {
                    bus.post(new CallControlParticipantAudioMuteEvent(remote.getKey(), CallControlMeetingControlsEvent.Actor.SELF, true, remoteParticipant, true));
                } else if (MediaDirection.RECVONLY.equals(localAudioDirection) && MediaDirection.SENDRECV.equals(remoteAudioDirection)) {
                    bus.post(new CallControlParticipantAudioMuteEvent(remote.getKey(), CallControlMeetingControlsEvent.Actor.SELF, true, remoteParticipant, false));
                }

                String remoteParticipantId = remoteParticipant.getPerson() != null ? remoteParticipant.getPerson().getId() : null;
                boolean isSelf = (selfId != null && selfId.equalsIgnoreCase(remoteParticipantId));
                processAudioControlChanges(localParticipant, remoteParticipant, remote.getKey(), isSelf);
            }
        }
    }


    /**
     * Compare old/new versions of locus dto for changes to floor granted/released
     *
     * @param local
     * @param remote
     */
    public void processFloorStateChange(Locus local, Locus remote) {

        boolean localFloorGranted = local != null ? local.isFloorGranted() : false;
        boolean remoteFloorGranted = remote.isFloorGranted();
        boolean beneficiariesChanged = false;
        if (localFloorGranted && remoteFloorGranted) {
            beneficiariesChanged = !local.getFloorBeneficiary().getId().equals(remote.getFloorBeneficiary().getId());
        }
        if (!localFloorGranted && remoteFloorGranted || beneficiariesChanged) {
            bus.post(new FloorGrantedEvent(remote.getKey()));
        }

        boolean localFloorReleased = local != null ? local.isFloorReleased() : false;
        boolean remoteFloorReleased = remote.isFloorReleased();
        if (!localFloorReleased && remoteFloorReleased) {
            bus.post(new FloorReleasedEvent(remote.getKey()));
        }
    }

    /**
     * Compare old/new versions of locus dto for changes in local media, while in a floor granted state
     *
     * @param local
     * @param remote
     */
    public void processMoveMediaWhileFloorGranted(Locus local, Locus remote) {
        if ((isFloorGrantedAndIntentType(local, LocusParticipant.IntentType.MOVE_MEDIA) && isFloorGrantedAndIntentType(remote, LocusParticipant.IntentType.OBSERVE))
                || (isFloorGrantedAndIntentType(local, LocusParticipant.IntentType.OBSERVE) && isFloorGrantedAndIntentType(remote, LocusParticipant.IntentType.NONE))) {
            bus.post(new FloorGrantedEvent(remote.getKey()));
        }
    }

    public void processMeeting(final Locus local, final Locus remote) {
        if (remote != null && remote.isMeeting()) {
            boolean localInLobby = local != null ? (local.isMeeting() && local.isInLobbyFromThisDevice(deviceRegistration.getUrl())) : false;
            boolean localIsJoined = local != null ? (local.isMeeting() && local.isJoinedFromThisDevice(deviceRegistration.getUrl())) : false;
            boolean remoteInLobby = remote.isInLobbyFromThisDevice(deviceRegistration.getUrl());
            boolean remoteIsJoined = remote.isJoinedFromThisDevice(deviceRegistration.getUrl());

            String logMessage;
            if (local == null && remoteInLobby) {
                logMessage = "entered lobby (local=null)";
                bus.post(new JoinedLobbyEvent(remote.getKey()));
            } else if (local == null && remoteIsJoined) {
                logMessage = "enter meeting (local=null)";
                bus.post(new JoinedMeetingEvent(remote.getKey()));
            } else if (localInLobby && remoteIsJoined) {
                logMessage = "enter meeting";
                bus.post(new JoinedMeetingEvent(remote.getKey()));
            } else if (!localInLobby && remoteInLobby) {
                logMessage = "entered lobby";
                bus.post(new JoinedLobbyEvent(remote.getKey()));
            } else {
                logMessage = "No meeting transition.";
            }

            StringBuilder builder = new StringBuilder()
                    .append("localInLobby=")
                    .append(localInLobby)
                    .append(", localIsJoined=")
                    .append(localIsJoined)
                    .append(", remoteInLobby=")
                    .append(remoteInLobby)
                    .append(", remoteIsJoined=")
                    .append(remoteIsJoined);
            ln.d("processMeeting, %s, LocusKey=%s, %s", logMessage, remote.getKey(), builder.toString());
        }
    }

    private boolean isFloorGrantedAndIntentType(Locus locus, LocusParticipant.IntentType intentType) {
        if (locus == null || !locus.isFloorGranted())
            return false;
        else {
            LocusParticipant.Intent intent = locus.getIntent(deviceRegistration.getUrl());
            if (intent == null)
                return (intentType == null || intentType.equals(LocusParticipant.IntentType.NONE)) ? true : false;
            else
                return intent.getType().equals(intentType);
        }
    }

    private Map<UUID, LocusParticipant> participantsToMap(List<LocusParticipant> participants) {
        Map<UUID, LocusParticipant> map = new HashMap<UUID, LocusParticipant>();
        for (LocusParticipant participant : participants) {
            map.put(participant.getId(), participant);
        }
        return map;
    }

    // Usage of this method will help to send events for subscribers who specifically care
    // about whether the owner/user's pmr has changed without responding to the multiple events
    // generally fired for Locus changes
    private boolean isMyPmrChanged(LocusData call) {
        Locus myPmrLocus = locusDataCache.getMyPmrLocus();
        return myPmrLocus != null && myPmrLocus.getInfo().getOwner().equals(call.getLocus().getInfo().getOwner());
    }

    private Locus mergeLocusDelta(Locus localLocus, Locus remoteLocusDelta) {
        List<LocusParticipant> mergedLocusPariticpants = mergeLocusPariticpants(localLocus.getParticipants(), remoteLocusDelta.getParticipants());
        // Copy locus info to local locus from remote Locus Delta DTO
        Locus mergedLocus = (new Locus.Builder())
                .setKey(remoteLocusDelta.getKey())
                .setDate(remoteLocusDelta.getCreated() == null ? localLocus.getCreated() : remoteLocusDelta.getCreated())
                .setLocusParticipantHostInfo(remoteLocusDelta.getHost() == null ? localLocus.getHost() : remoteLocusDelta.getHost())
                .setLocusFullState(remoteLocusDelta.getFullState() == null ? localLocus.getFullState() : remoteLocusDelta.getFullState())
                .setLocusControl(remoteLocusDelta.getControls() == null ? localLocus.getControls() : remoteLocusDelta.getControls())
                .setParticipants(mergedLocusPariticpants)
                .setSelfRepresentation(remoteLocusDelta.getSelf() == null ? localLocus.getSelf() : remoteLocusDelta.getSelf())
                .setLocusSequenceInfo(remoteLocusDelta.getSequence())
                .setLocusBaseSequenceInfo(remoteLocusDelta.getBaseSequence())
                .setSyncUrl(remoteLocusDelta.getSyncUrl())
                .setMediaShares(remoteLocusDelta.getMediaShares() == null ? localLocus.getMediaShares() : remoteLocusDelta.getMediaShares())
                .setLocusReplaces(remoteLocusDelta.getReplaces() == null ? localLocus.getReplaces() : remoteLocusDelta.getReplaces())
                .setConversationUrl(remoteLocusDelta.getConversationUrl() == null ? localLocus.getConversationUrl() : remoteLocusDelta.getConversationUrl())
                .setLocusDescriptionInfo(remoteLocusDelta.getInfo() == null ? localLocus.getInfo() : remoteLocusDelta.getInfo())
                .setLocusScheduledMeeting(remoteLocusDelta.getMeeting() == null ? localLocus.getMeeting() : remoteLocusDelta.getMeeting())
                .build();
        return mergedLocus;
    }

    private List<LocusParticipant> mergeLocusPariticpants(List<LocusParticipant> localParticipants, List<LocusParticipant> remoteDeltaParticipants) {
        if (remoteDeltaParticipants == null || remoteDeltaParticipants.size() == 0)
            return localParticipants;

        List<LocusParticipant> newParticipants = new ArrayList<>();
        Map<UUID, LocusParticipant> remoteParticipantsMap = participantsToMap(remoteDeltaParticipants);
        // add participant from local locus if it does not exist in remote locus delta
        for (LocusParticipant participant : localParticipants) {
            if (!remoteParticipantsMap.containsKey(participant.getId())) {
                newParticipants.add(participant);
            }
        }
        newParticipants.addAll(remoteDeltaParticipants);
        return newParticipants;
    }

    private void processAudioControlChanges(LocusParticipant local, LocusParticipant remote, LocusKey key, boolean isSelf) {
        LocusParticipantControls localControls = local.getControls();
        LocusParticipantControls remoteControls = remote.getControls();
        LocusParticipantAudioControl localAudioControl  = localControls  != null ? localControls.getAudio()  : null;
        LocusParticipantAudioControl remoteAudioControl = remoteControls != null ? remoteControls.getAudio() : null;

        if (remoteAudioControl == null || localAudioControl == null) {
            return;
        }

        // Detect changes in audio control field and fire appropriate change events.
        if (localAudioControl.isMuted() != remoteAudioControl.isMuted()) {
            boolean muted = remoteAudioControl.isMuted();

            // Moderator Mute event should be posted only if self is muted by a another user. (remote muted)
            if (isSelf) {
                bus.post(new CallControlModeratorMutedParticipantEvent(key, remote, muted));
            }

            bus.post(new CallControlParticipantAudioMuteEvent(key, CallControlMeetingControlsEvent.Actor.MODERATOR, true, remote, muted));
        }
    }

}
