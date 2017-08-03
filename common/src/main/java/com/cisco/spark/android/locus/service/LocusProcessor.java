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
import com.cisco.spark.android.features.CoreFeatures;
import com.cisco.spark.android.locus.events.FloorGrantedEvent;
import com.cisco.spark.android.locus.events.FloorLostEvent;
import com.cisco.spark.android.locus.events.FloorReleasedEvent;
import com.cisco.spark.android.locus.events.IncomingCallEvent;
import com.cisco.spark.android.locus.events.JoinedMeetingFromLobbyEvent;
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
import com.cisco.spark.android.locus.events.ParticipantSelfChangedEvent;
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
import com.cisco.spark.android.meetings.LocusMeetingInfoProvider;
import com.cisco.spark.android.mercury.MercuryEventType;
import com.cisco.spark.android.mercury.events.DeclineReason;
import com.cisco.spark.android.mercury.events.LocusChangedEvent;
import com.cisco.spark.android.mercury.events.LocusDeltaEvent;
import com.cisco.spark.android.mercury.events.LocusEvent;
import com.cisco.spark.android.sync.Batch;
import com.cisco.spark.android.util.Strings;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Provider;

import de.greenrobot.event.EventBus;
import retrofit2.Response;

import static com.cisco.spark.android.sync.ConversationContentProviderOperation.updateConversationLocusUrl;

public class LocusProcessor implements Runnable {
    public enum EventType {
        SELF_CHANGED,
        CHANGED,
        DELTA
    }

    private final EventBus bus;
    private final LocusDataCache locusDataCache;
    private final ApiClientProvider apiClientProvider;
    private final NaturalLog ln;
    private final DeviceRegistration deviceRegistration;
    private final Provider<Batch> batchProvider;
    private final CoreFeatures coreFeatures;
    private final LocusProcessorReporter locusProcessorReporter;
    private final LocusMeetingInfoProvider locusMeetingInfoProvider;
    private Calendar utcCalendar;
    private LinkedBlockingQueue<EventQueueItem> locusEventQueue;
    private Thread consumer;

    public LocusProcessor(final ApiClientProvider apiClientProvider, final EventBus bus, final LocusDataCache locusDataCache, Ln.Context lnContext,
                          DeviceRegistration deviceRegistration, Provider<Batch> batchProvider, CoreFeatures coreFeatures, LocusProcessorReporter locusProcessorReporter, LocusMeetingInfoProvider locusMeetingInfoProvider) {
        this.apiClientProvider = apiClientProvider;
        this.bus = bus;
        this.locusDataCache = locusDataCache;
        this.batchProvider = batchProvider;
        this.ln = Ln.get(lnContext, "LocusProcessor");
        this.deviceRegistration = deviceRegistration;
        this.coreFeatures = coreFeatures;
        this.locusProcessorReporter = locusProcessorReporter;
        this.locusMeetingInfoProvider = locusMeetingInfoProvider;

        utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        locusEventQueue = new LinkedBlockingQueue<>(20);
        consumer = new Thread(this);
        consumer.start();
    }

    public synchronized void postLocusChangedEvent(LocusChangedEvent event) {
        if (event.getEventType().equals(MercuryEventType.LOCUS_SELF_CHANGED)) {
            postEvent(EventType.SELF_CHANGED, event.getLocus(), event.getId().toString());
        } else {
            postEvent(EventType.CHANGED, event.getLocus(), event.getId().toString());
        }
    }

    public synchronized void postLocusDeltaEvent(Locus locus, String eventId) {
        postEvent(EventType.DELTA, locus, eventId);
    }

    private synchronized void postEvent(LocusProcessor.EventType type, Locus locus, String label) {
        try {
            String msg = String.format("type=%s, label=%s (queue size=%d)", type == EventType.CHANGED ? "CHANGED" : type == EventType.DELTA ? "DELTA" : "unknown", label, locusEventQueue.size());
            ln.d("postEvent(): %s", msg);
            locusProcessorReporter.reportNewEvent(msg);
            locusEventQueue.put(new EventQueueItem(type, locus, label));
        } catch (InterruptedException ex) {
            ln.e(ex, "Error posting event");
        }
    }

    @Override
    public void run() {
        ln.d("event queue worker thread started");

        try {
            EventQueueItem event;
            while ((event = locusEventQueue.take()) != null) {
                ln.d("processing event label: %s (queue size=%d)", event.label, locusEventQueue.size());

                if (event.type == EventType.DELTA) {
                    processLocusUpdate(event.locus, new LocusDeltaEvent());
                } else {
                    processLocusUpdate(event.locus);

                    // There are a number of integration tests that have sensitive timing on this event occurring AFTER processLocusUpdate()
                    // The event itself is not comparing or reflecting any particular change in the self state; rather it is used to
                    // trigger reactions in mocked CCS components and gating waitForEvents in tests.
                    if (event.type == EventType.SELF_CHANGED) {
                        bus.post(new ParticipantSelfChangedEvent(event.locus.getKey(), event.locus));
                    }
                }
            }
        } catch (InterruptedException ex) {
            ln.e(ex, "Error getting or processing event");
        }

        ln.d("event queue worker thread stopped");
    }

    /**
     * Process locus DTO update (this can come as response to locus REST request or as event over mercury).  Check if DTO should, based on sequence info,
     * overwrite existing DTO and, if so, determine and post the appropriate event based on difference between old and new locus DTO
     *
     * @param remoteLocus
     */
    // TODO: CAN THIS BE private, EVERYTHING GO THROUGH post?  OR DO WE NEED A BLOCKING post, EQUIVALENT TO THIS?
    public void processLocusUpdate(Locus remoteLocus) {
        processLocusUpdate(remoteLocus, null);
    }

    /**
     * Process locus DTO update (this can come as response to locus REST request or as event over mercury).  Check if DTO should, based on sequence info,
     * overwrite existing DTO and, if so, determine and post the appropriate event based on difference between old and new locus DTO
     *
     * @param remoteLocus
     * @Param LocusEvent. a)LocusChangedEvent: Locus non-delta event, that means client receives the Locus whole DTO,
     *                    b)LocusDeltaEvent: Locus delta event, that means client receives the Locus Delta DTO.
     */
    private void processLocusUpdate(Locus remoteLocus, LocusEvent locusEvent) {

        boolean processLocus = false;
        LocusData call = locusDataCache.getLocusData(remoteLocus.getKey());
        Locus localLocus = null;
        if (call != null) {
            localLocus = call.getLocus();

            LocusSequenceInfo.OverwriteWithResult overwriteWithResult;
            boolean isDelta = false;
            if (locusEvent instanceof LocusDeltaEvent) {
                ln.d("processLocusUpdate() processing with delta sequence information");
                overwriteWithResult = localLocus.getSequence().overwriteWith(remoteLocus.getBaseSequence(), remoteLocus.getSequence());
                isDelta = true;
            } else {
                ln.d("processLocusUpdate() processing with full DTO");
                overwriteWithResult = localLocus.getSequence().overwriteWith(remoteLocus.getSequence());
            }

            if (overwriteWithResult.equals(LocusSequenceInfo.OverwriteWithResult.TRUE)) {
                if (isDelta) {
                    Locus locusDelta = mergeLocusDelta(localLocus, remoteLocus);
                    call.setLocus(locusDelta);
                } else {
                    call.setLocus(remoteLocus);
                }
                processLocus = true;

                ln.i("Updating locus DTO and notifying listeners of data change for: %s", call.getKey().toString());
                if (isMyPmrChanged(call)) {
                    bus.post(new LocusPmrChangedEvent());
                }
                bus.post(new LocusDataCacheChangedEvent(remoteLocus.getKey(), LocusDataCacheChangedEvent.Type.MODIFIED));
                locusProcessorReporter.reportProcessedEvent("OVERWRITE");
            } else if (overwriteWithResult.equals(LocusSequenceInfo.OverwriteWithResult.FALSE)) {
                ln.i("Didn't overwrite locus DTO as new one was older version than one currently in memory.");
                locusProcessorReporter.reportProcessedEvent("IGNORE");
            } else if (overwriteWithResult.equals(LocusSequenceInfo.OverwriteWithResult.DESYNC)) {
                ln.i("Didn't overwrite locus DTO as new one was out of sync with one currently in memory.");
                locusProcessorReporter.reportProcessedEvent("DESYNC");

                // we've detected de-sync state so re-fetch locus dto
                String url = isDelta ? localLocus.getSyncUrl() : call.getKey().getUrl().toString();
                Locus newLocus = getLocusSync(url, localLocus.getSequence());
                processLocusUpdate(newLocus);
            }
        } else {
            // locus doesn't exist already so store
            call = new LocusData(remoteLocus);
            call.setUseNewBridgeTest(deviceRegistration.getFeatures().useBridgeTestV2());
            locusDataCache.putLocusData(call);
            locusProcessorReporter.reportProcessedEvent("NEW");

            // Attempt to cache associated LocusMeetingInfo
            locusMeetingInfoProvider.cache(call.getLocus().getKey().getLocusId());

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
            processLocusControlChanges(localLocus, remoteLocus);
            processFloorStateChange(localLocus, remoteLocus);
            processReplaces(remoteLocus);
        }
    }

    private Locus getLocusSync(String url, LocusSequenceInfo locusSequenceInfo) {
        try {
            String syncDebug = null;
            if (coreFeatures.isDeltaEventEnabled()) {
                ln.d("sync_debug parameter is enabled: %s", syncDebug);
                syncDebug = locusSequenceInfo.getSyncDebug();
            }
            Response<Locus> response = apiClientProvider.getHypermediaLocusClient().getLocus(url, syncDebug).execute();
            if (response.isSuccessful()) {
                return response.body();
            }
        } catch (IOException ex) {
            ln.e(ex, "Error getting Locus sync");
        }
        return null;
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
                            newLocusData.setIsToasting(oldLocusData.isToasting());
                            newLocusData.setActiveSpeakerId(oldLocusData.getActiveSpeaker() != null ? oldLocusData.getActiveSpeaker().getId() : null);
                            newLocusData.setRemoteParticipantDetails();
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
            } else if ((local == null || !local.getFullState().isActive()) && selfState.equals(LocusParticipant.State.NOTIFIED)) {
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

        for (LocusParticipantDevice remoteDevice : remoteParticipant.getDevices()) {
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
        Map<UUID, LocusParticipant> localParticipantsMap = participantsToMap(local.getRawParticipants());
        List<LocusParticipant> remoteParticipants = remote.getRawParticipants();

        List<LocusParticipant> joined = new ArrayList<LocusParticipant>();
        List<LocusParticipant> left = new ArrayList<LocusParticipant>();
        List<LocusParticipant> idle = new ArrayList<LocusParticipant>();

        for (LocusParticipant participant : remoteParticipants) {
            if (localParticipantsMap.containsKey(participant.getId())) {
                LocusParticipant localParticipant = localParticipantsMap.get(participant.getId());

                // check change in participant display name
                if ((localParticipant.getPerson() != null && participant.getPerson() != null) &&
                        (localParticipant.getPerson().getDisplayName() != null && participant.getPerson().getDisplayName() != null) &&
                        !localParticipant.getPerson().getDisplayName().equals(participant.getPerson().getDisplayName())) {
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

        boolean localInLobby = local.isInLobbyFromThisDevice(deviceRegistration.getUrl());
        boolean remoteIsJoined = remote.isJoinedFromThisDevice(deviceRegistration.getUrl());
        if (localInLobby && remoteIsJoined) {
            ln.d("Entering meeting from lobby, LocusKey=%s", remote.getKey());
            bus.post(new JoinedMeetingFromLobbyEvent(remote.getKey()));
        }
    }

    // check if locus controls state was changed -> lock/unlock and start/stop recording and pause/resume recording
    public void processLocusControlChanges(Locus local, Locus remote) {
        LocusControl remoteLocusControl = remote.getControls();

        if (remoteLocusControl != null) {
            boolean localLocusControlLocked = local != null && local.getControls().getLock().isLocked();
            boolean localLocusControlRecording = local != null && local.getRecordControl().isRecording();
            boolean localLocusControlRecordingPaused = local != null && local.getRecordControl().isPaused();
            boolean remoteLocusControlLocked = remoteLocusControl.getLock().isLocked();

            if (localLocusControlLocked != remoteLocusControlLocked) {
                ln.d("processLocusControlChanges, meeting is locked: " + remoteLocusControlLocked);
                bus.post(new CallControlMeetingControlsLockEvent(remote.getKey(), true, remoteLocusControlLocked));
            }
            if (localLocusControlRecording != remote.getRecordControl().isRecording()
                    || localLocusControlRecordingPaused != remote.getRecordControl().isPaused()) {
                ln.d("processLocusControlChanges, meeting is being recorded: " + remote.getRecordControl().isRecording() + "; paused: " + remote.getRecordControl().isPaused());
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

    private void processFloorStateChange(Locus local, Locus remote) {
        if (remote == null) {
            Ln.e("LocusProcessor(processFloorStateChanged): remote is null");
            return;
        }

        if (remote.isFloorGranted()) {
            // Join call
            LocusParticipantDevice localCurrentDevice = local == null ? null : local.getMyDevice(deviceRegistration.getUrl());
            LocusParticipantDevice remoteCurrentDevice = remote.getMyDevice(deviceRegistration.getUrl());
            boolean localNotJoined = localCurrentDevice == null || !localCurrentDevice.getState().equals(LocusParticipant.State.JOINED);
            boolean remoteJoined = remoteCurrentDevice != null && remoteCurrentDevice.getState().equals(LocusParticipant.State.JOINED);
            if (localNotJoined && remoteJoined) {
                Ln.i("LocusProcessor(processFloorStateChanged): join call when is FloorGranted. post FloorGrantedEvent");
                bus.post(new FloorGrantedEvent(remote.getKey()));
                return;
            }

            // Move media
            boolean moveToLocalWhileGranted = isFloorGrantedAndIntentType(local, LocusParticipant.IntentType.OBSERVE)
                    && isFloorGrantedAndIntentType(remote, LocusParticipant.IntentType.NONE);
            boolean moveToRemoteWhileGranted = isFloorGrantedAndIntentType(local, LocusParticipant.IntentType.MOVE_MEDIA)
                    && isFloorGrantedAndIntentType(remote, LocusParticipant.IntentType.OBSERVE);
            if (moveToRemoteWhileGranted || moveToLocalWhileGranted) {
                Ln.i("LocusProcessor(processFloorStateChanged): move media when is FloorGranted, moveToRemoteWhileGranted: %s, moveToLocalWhileGranted: %s. post FloorGrantedEvent",
                        moveToRemoteWhileGranted, moveToLocalWhileGranted);
                bus.post(new FloorGrantedEvent(remote.getKey()));
                return;
            }

            // New Floor Granted
            boolean isLocalFloorGranted = local != null && local.isFloorGranted();
            if (isLocalFloorGranted) {
                String localMediaShareType = local.getGrantedFloor().getName();
                Uri localMediaShareDeviceUrl = local.getGrantedFloor().getFloor().getBeneficiary().getDeviceUrl();
                String remoteMediaShareType = remote.getGrantedFloor().getName();
                Uri remoteMediaShareDeviceUrl = remote.getGrantedFloor().getFloor().getBeneficiary().getDeviceUrl();
                Ln.i("LocusProcessor(processFloorStateChanged): remote: %s %s, local: %s %s",
                        remoteMediaShareType, remoteMediaShareDeviceUrl,
                        localMediaShareType, localMediaShareDeviceUrl);
                if (!localMediaShareType.equals(remoteMediaShareType)
                        || !localMediaShareDeviceUrl.equals(remoteMediaShareDeviceUrl)) {
                    Ln.i("LocusProcessor(processFloorStateChanged): post FloorLostEvent");
                    bus.post(new FloorLostEvent(remote.getKey(), local.getGrantedFloor(), remote.getGrantedFloor()));
                    return;
                }
            } else {
                Ln.i("LocusProcessor(processFloorStateChanged): post FloorGrantedEvent");
                bus.post(new FloorGrantedEvent(remote.getKey()));
            }
        } else if (remote.isFloorReleased()) {
            boolean isLocalFloorGranted = local != null && local.isFloorGranted();
            if (isLocalFloorGranted) {
                Ln.i("LocusProcessor(processFloorStateChanged): post FloorReleasedEvent");
                bus.post(new FloorReleasedEvent(remote.getKey(), local.getGrantedFloor().getName()));
            }
        }
    }

    private boolean isFloorGrantedAndIntentType(Locus locus, LocusParticipant.IntentType intentType) {
        if (locus == null || !locus.isFloorGranted())
            return false;
        else {
            LocusParticipant.Intent intent = locus.getIntent(deviceRegistration.getUrl());
            if (intent == null)
                return intentType == null || intentType.equals(LocusParticipant.IntentType.NONE);
            else
                return intent.getType().equals(intentType);
        }
    }

    // Usage of this method will help to send events for subscribers who specifically care
    // about whether the owner/user's pmr has changed without responding to the multiple events
    // generally fired for Locus changes
    private boolean isMyPmrChanged(LocusData call) {
        Locus myPmrLocus = locusDataCache.getMyPmrLocus();
        return myPmrLocus != null && myPmrLocus.getInfo().getOwner().equals(call.getLocus().getInfo().getOwner());
    }

    private Locus mergeLocusDelta(Locus localLocus, Locus remoteLocusDelta) {
        List<LocusParticipant> mergedLocusPariticpants = mergeLocusPariticpants(localLocus.getRawParticipants(), remoteLocusDelta.getRawParticipants());
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

    // TODO: Remove other references to this method (using HashSet logic below)
    private Map<UUID, LocusParticipant> participantsToMap(List<LocusParticipant> participants) {
        Map<UUID, LocusParticipant> map = new HashMap<UUID, LocusParticipant>();
        for (LocusParticipant participant : participants) {
            map.put(participant.getId(), participant);
        }
        return map;
    }

    private List<LocusParticipant> mergeLocusPariticpants(List<LocusParticipant> localParticipants, List<LocusParticipant> remoteDeltaParticipants) {
        if (remoteDeltaParticipants == null || remoteDeltaParticipants.size() == 0)
            return localParticipants;

        HashSet<LocusParticipant> mergedParticipants = new HashSet<>(remoteDeltaParticipants);
        mergedParticipants.addAll(localParticipants);
        return new ArrayList<>(mergedParticipants);
    }

    private void processAudioControlChanges(LocusParticipant local, LocusParticipant remote, LocusKey key, boolean isSelf) {
        LocusParticipantControls localControls = local.getControls();
        LocusParticipantControls remoteControls = remote.getControls();
        LocusParticipantAudioControl localAudioControl = localControls != null ? localControls.getAudio() : null;
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

    private class EventQueueItem {
        public EventType type;
        public Locus locus;
        public String label;

        public EventQueueItem(EventType type, Locus locus, String label) {
            this.type = type;
            this.locus = locus;
            this.label = label;
        }
    }
}
