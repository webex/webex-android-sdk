package com.cisco.spark.android.locus.model;

import android.support.annotation.Nullable;

import com.cisco.spark.android.authenticator.ApiTokenProvider;
import com.cisco.spark.android.events.CallNotificationRemoveEvent;
import com.cisco.spark.android.locus.events.LocusDataCacheChangedEvent;
import com.cisco.spark.android.locus.responses.GetLocusListResponse;
import com.cisco.spark.android.wdm.DeviceRegistration;
import com.cisco.spark.android.wdm.UCDeviceType;
import com.github.benoitdion.ln.Ln;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import de.greenrobot.event.EventBus;

public class LocusDataCache {
    private volatile Map<LocusKey, LocusData> locusDataCache = new ConcurrentHashMap<LocusKey, LocusData>();
    private final EventBus bus;
    private final DeviceRegistration deviceRegistration;
    private final UCDeviceType ucDeviceType;
    private final ApiTokenProvider apiTokenProvider;

    public LocusDataCache(EventBus bus, DeviceRegistration deviceRegistration, UCDeviceType ucDeviceType, ApiTokenProvider apiTokenProvider) {
        this.bus = bus;
        this.deviceRegistration = deviceRegistration;
        this.ucDeviceType = ucDeviceType;
        this.apiTokenProvider = apiTokenProvider;
    }

    public LocusData getLocusData(LocusKey locusKey) {
        if (locusKey != null) {
            return locusDataCache.get(locusKey);
        } else {
            return null;
        }
    }

    public Set<LocusKey> getKeys() {
        return locusDataCache.keySet();
    }

    public synchronized void removeStaleLoci(GetLocusListResponse response) {
        // remove stale entries from cache
        Set<LocusKey> freshUris = new HashSet<>();
        for (Locus locus : response.getLoci()) {
            freshUris.add(locus.getKey());
        }
        for (LocusKey key : locusDataCache.keySet()) {
            if (!freshUris.contains(key)) {
                Ln.i("LocusDataCache: Removing stale locus from cache: %s", key.toString());
                locusDataCache.remove(key);
                bus.post(new LocusDataCacheChangedEvent(key, LocusDataCacheChangedEvent.Type.REMOVED));
                bus.post(new CallNotificationRemoveEvent(key));
            }
        }
    }

    public boolean exists(LocusKey locusKey) {
        return locusDataCache.containsKey(locusKey);
    }

    public void putLocusData(LocusData locusData) {
        locusDataCache.put(locusData.getKey(), locusData);
    }

    public void removeLocusData(LocusKey locusKey) {
        locusDataCache.remove(locusKey);
    }

    public LocusData updateLocus(LocusKey locusKey, Locus locus) {
        if (locusKey == null) {
            throw new NullPointerException("locusKey is null");
        }
        if (locus == null) {
            throw new NullPointerException("locus is null");
        }

        LocusData locusData = getLocusData(locusKey);
        if (!exists(locusKey)) {
            locusData = new LocusData(locus);
            locusDataCache.put(locusKey, locusData);
            Ln.i("LocusDataCache: Notifying listeners of new entry for: %s", locusKey.toString());
            bus.post(new LocusDataCacheChangedEvent(locusKey, LocusDataCacheChangedEvent.Type.ADDED));
        } else {
            Locus oldLocus = locusData.getLocus();
            LocusSequenceInfo.OverwriteWithResult overwriteWithResult = oldLocus.getSequence().overwriteWith(locus.getSequence());
            if (overwriteWithResult.equals(LocusSequenceInfo.OverwriteWithResult.TRUE)) {
                locusData.setLocus(locus);
                Ln.i("Updating locus DTO and notifying listeners of data change for: %s", locusKey.toString());
                bus.post(new LocusDataCacheChangedEvent(locusKey, LocusDataCacheChangedEvent.Type.MODIFIED));
            } else if (overwriteWithResult.equals(LocusSequenceInfo.OverwriteWithResult.FALSE)) {
                Ln.i("Didn't overwrite locus DTO as new one was older version than one currently in memory.");
            } else if (overwriteWithResult.equals(LocusSequenceInfo.OverwriteWithResult.DESYNC)) {
                Ln.i("Didn't overwrite locus DTO as new one was out of sync with one currently in memory.");
            }
        }

        return locusData;
    }

    public boolean isInCall() {
        LocusKey locusKey = getActiveLocus();
        return locusKey != null;
    }

    public boolean isInCall(LocusKey key) {
        if (key == null)
            return false;
        LocusData locusData = getLocusData(key);
        if (locusData != null && locusData.getLocus() != null) {
            Locus locus = locusData.getLocus();
            if (locus.isJoinedFromThisDevice(deviceRegistration.getUrl())
                    || locusData.isObserving(deviceRegistration.getUrl())
                    || locus.isInLobbyFromThisDevice(deviceRegistration.getUrl())) {
                return true;
            }
        }
        return false;
    }

    public boolean isLeavingCall(LocusKey key) {
        if (key == null)
            return false;
        LocusData locusData = getLocusData(key);
        if (locusData != null && locusData.getLocus() != null) {
            Locus locus = locusData.getLocus();
            if (locus.getSelf() != null) {
                if (LocusParticipant.State.LEAVING.equals(locus.getSelf().getState())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasLeftCall(LocusKey key) {
        if (key == null)
            return false;
        LocusData locusData = getLocusData(key);
        if (locusData != null && locusData.getLocus() != null) {
            Locus locus = locusData.getLocus();
            if (locus.getSelf() != null) {
                if (LocusParticipant.State.LEFT.equals(locus.getSelf().getState())) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * A Zero Touch Meeting is defined as a call between two persons from their hybrided Uc Phones
     * Alice calls Bob from her Uc Phone and he answers on his Uc Phone, the one-on-one between
     * Alice and Bob in Spark shows as being in a call.
     *
     * For ZTM calls the media is going between the UC phones, android only shows an indication in
     * the UI that the call is ongoing, and cannot end or join the call.
     *
     * If there is a share in the call, android will join the call and show the share.
     */
    public boolean isZtmCall(LocusKey key) {
        if (key == null)
            return false;
        LocusData locusData = getLocusData(key);
        if (locusData != null) {
            Locus locus = locusData.getLocus();
            if (locus != null) {
                if (locusData.isOneOnOne()) {
                    boolean selfJoinedFromUcPhone = locus.isSelfJoinedFromDeviceType(ucDeviceType.get());
                    boolean remoteJoinedFromUcPhone = locus.isRemoteParticipantJoinedFromDeviceType(ucDeviceType.get());
                    return selfJoinedFromUcPhone && remoteJoinedFromUcPhone;
                }
            }
        }
        return false;
    }

    public LocusKey getActiveLocus() {
        for (LocusData locusData : locusDataCache.values()) {
            Locus locus = locusData.getLocus();
            if (locus != null  && isInCall(locus.getKey())) {
                return locus.getKey();
            }
        }
        return null;
    }

    public List<LocusKey> getActiveCalls() {
        ArrayList<LocusKey> activeCalls = new ArrayList();

        boolean sparkPmrEnabled = deviceRegistration.getFeatures().isSparkPMREnabled();
        for (LocusData locusData : locusDataCache.values()) {
                if (locusData.getLocus().getFullState().isActive() || (sparkPmrEnabled && isMyMeeting(locusData.getLocus()))) {
                    activeCalls.add(locusData.getKey());
                }
            }

        return activeCalls;
    }


    public List<LocusKey> getCallsWithMeetings() {
        ArrayList<LocusKey> upcomingMeetingCalls = new ArrayList<>();
        for (LocusData locusData : locusDataCache.values()) {
            if (locusData.getLocus().getMeeting() != null) {
                upcomingMeetingCalls.add(locusData.getKey());
            }
        }
        return upcomingMeetingCalls;
    }

    public boolean isCallActive(LocusKey key) {
        if (key == null)
            return false;
        LocusData locusData = getLocusData(key);
        if (locusData != null) {
            Locus locus = locusData.getLocus();
            if (locus != null) {
                return locus.getFullState().isActive();
            }
        }
        return false;
    }

    private boolean isMyMeeting(Locus locus) {
        if (locus == null) {
            return false;
        } else {
            return locus.getFullState().getType().equals(LocusState.Type.MEETING) &&
                    locus.getInfo().getOwner() != null &&
                    locus.getInfo().getOwner().toString().equals(apiTokenProvider.getAuthenticatedUser().getUserId());
        }
    }

    public boolean isInLobby() {
        LocusKey locusKey = getActiveLocus();
        if (locusKey == null) {
            return false;
        }

        LocusData locusData = getLocusData(locusKey);
        return locusData != null && locusData.getLocus().getSelf().isInLobby();
    }

    public LocusKey getIncomingCall() {
        LocusKey incomingCall = null;
        for (LocusData locusData : locusDataCache.values()) {
            if (locusData.getLocus().getFullState().isActive()) {
                LocusParticipant.State selfState = locusData.getLocus().getSelf().getState();
                if (LocusParticipant.State.IDLE.equals(selfState) || LocusParticipant.State.NOTIFIED.equals(selfState)) {
                    incomingCall = locusData.getKey();
                    break;
                }
            }
        }
        return incomingCall;
    }

    public long getStartTime(LocusKey key) {
        LocusData call = getLocusData(key);

        if (call != null) {
            return call.getLocus().getFullState().getLastActive().getTime();
        } else {
            return 0;
        }
    }

    public int getActiveLociCount() {
        int count = 0;

        for (LocusData locusData : locusDataCache.values()) {
            Locus locus = locusData.getLocus();
            if (locus != null && locus.getFullState().isActive()) {
                count++;
            }
        }
        return count;
    }

    public LocusKey getLocusKeyFromDataCache(String conversationId) {
        for (LocusKey key : locusDataCache.keySet()) {
            String conversationUrl = locusDataCache.get(key).getLocus().getConversationUrl();
            if (conversationUrl != null && conversationId != null && conversationUrl.contains(conversationId)) {
                return key;
            }
        }

        return null;
    }

    @Nullable
    public Locus getMyPmrLocus() {
        for (LocusKey locusKey : getActiveCalls()) {
            LocusData locusData = getLocusData(locusKey);
            if (locusData != null) {
                LocusDescription locusDescription = locusData.getLocus().getInfo();
                UUID owner = locusDescription.getOwner();
                if (owner != null && owner.toString()
                        .equals(apiTokenProvider.getAuthenticatedUser().getUserId())) {
                    return locusData.getLocus();
                }
            }
        }
        return null;
    }

    public void clear() {
        locusDataCache.clear();
    }
}
