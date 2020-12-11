/*
 * Copyright 2016-2021 Cisco Systems Inc
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

package com.ciscowebex.androidsdk.phone.internal;

import android.util.Size;
import com.cisco.wx2.diagnostic_events.MediaLine;
import com.ciscowebex.androidsdk.internal.metric.CallAnalyzerReporter;
import com.ciscowebex.androidsdk.internal.queue.Queue;
import com.ciscowebex.androidsdk.internal.media.MediaError;
import com.ciscowebex.androidsdk.internal.media.WMEngine;
import com.ciscowebex.androidsdk.internal.media.WmeObserver;
import com.ciscowebex.androidsdk.internal.media.WmeTrack;
import com.ciscowebex.androidsdk.phone.AuxStream;
import com.ciscowebex.androidsdk.phone.CallMembership;
import com.ciscowebex.androidsdk.phone.CallObserver;
import com.ciscowebex.androidsdk.phone.MultiStreamObserver;
import com.ciscowebex.androidsdk.utils.Json;
import com.github.benoitdion.ln.Ln;
import me.helloworld.utils.Checker;

import java.util.List;

class MediaObserver implements WmeObserver {

    private final CallImpl call;

    public MediaObserver(CallImpl call) {
        this.call = call;
    }

    @Override
    public void onRemoteVideoAvailable(boolean available) {
        Queue.main.run(() -> {
            if (call != null && call.getObserver() != null) {
                call.getObserver().onMediaChanged(new CallObserver.RemoteSendingVideoEvent(call, available));
            }
        });
    }

    @Override
    public void onAuxVideoAvailable(int vid, boolean available) {
        Queue.main.run(() -> {
            if (call != null && call.getMultiStreamObserver() != null) {
                AuxStream stream = call.getAuxStream(vid);
                if (stream != null) {
                    call.getMultiStreamObserver().onAuxStreamChanged(new MultiStreamObserver.AuxStreamSendingVideoEvent(call, stream));
                }
            }
        });
    }

    @Override
    public void onRemoteSharingAvailable(boolean available) {
//        Queue.main.run(() -> {
//            if (call != null && call.getObserver() != null) {
//                call.getObserver().onMediaChanged(new CallObserver.RemoteSendingSharingEvent(call, available));
//            }
//        });
    }

    @Override
    public void onActiveSpeakerChanged(long[] oldCSIs, long[] newCSIs) {
        Queue.main.run(() -> {
            if (call != null && newCSIs != null) {
                CallMembershipImpl membership = null;
                boolean done = false;
                if (newCSIs.length < 1) {
                    done = true;
                }
                else {
                    loop: for (long csi : newCSIs) {
                        for (CallMembership member : call.getMemberships()) {
                            if (((CallMembershipImpl)member).containsCSI(csi)) {
                                membership = (CallMembershipImpl) member;
                                done = true;
                                break loop;
                            }
                        }
                    }
                }
                if (done) {
                    CallMembershipImpl old = (CallMembershipImpl) call.getActiveSpeaker();
                    if (membership != null && old != null && Checker.isEqual(membership.getId(), old.getId())) {
                        return;
                    }
                    call.setActiveSpeaker(membership);
                    if (call.getObserver() != null) {
                        call.getObserver().onMediaChanged(new CallObserver.ActiveSpeakerChangedEvent(call, old, membership));
                    }
                }
            }
        });
    }

    @Override
    public void onCSIChanged(long vid, long[] oldCSIs, long[] newCSIs) {
        Queue.main.run(() -> {
            if (call != null && newCSIs != null) {
                AuxStreamImpl stream = (AuxStreamImpl) call.getAuxStream(vid);
                if (stream != null) {
                    CallMembershipImpl membership = null;
                    boolean done = false;
                    if (newCSIs.length < 1) {
                        done = true;
                    }
                    else {
                        loop: for (long csi : newCSIs) {
                            for (CallMembership member : call.getMemberships()) {
                                if (((CallMembershipImpl)member).containsCSI(csi)) {
                                    membership = (CallMembershipImpl) member;
                                    done = true;
                                    break loop;
                                }
                            }
                        }
                    }
                    if (done) {
                        CallMembershipImpl old = (CallMembershipImpl) stream.getPerson();
                        if (membership != null && old != null && Checker.isEqual(membership.getId(), old.getId())) {
                            return;
                        }
                        stream.setPerson(membership);
                        if (call.getMultiStreamObserver() != null) {
                            call.getMultiStreamObserver().onAuxStreamChanged(new MultiStreamObserver.AuxStreamPersonChangedEvent(call, stream, old, membership));
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onAvailableMediaChanged(int count) {
        Queue.main.run(() -> {
            if (call != null) {
                call.updateAuxStreamCount();
            }
        });
    }

    @Override
    public void onMediaDecodeSizeChanged(WMEngine.Media media, long vid, Size size) {
        if (media == WMEngine.Media.Video) {
            Queue.main.run(() -> {
                if (call != null) {
                    if (vid == WMEngine.MAIN_VID && call.getObserver() != null) {
                        call.getObserver().onMediaChanged(new CallObserver.RemoteVideoViewSizeChanged(call));
                    }
                    else if (vid >= 0 && call.getMultiStreamObserver() != null){
                        AuxStream stream = call.getAuxStream(vid);
                        if (stream != null) {
                            call.getMultiStreamObserver().onAuxStreamChanged(new MultiStreamObserver.AuxStreamSizeChangedEvent(call, stream));
                        }
                    }
                }
            });
        }
        else if (media == WMEngine.Media.Sharing) {
            Queue.main.run(() -> {
                if (call != null && call.getObserver() != null) {
                    call.getObserver().onMediaChanged(new CallObserver.RemoteSharingViewSizeChanged(call));
                }
            });
        }
    }

    @Override
    public void onMediaRenderSizeChanged(WMEngine.Media media, long vid, Size size) {
        if (media == WMEngine.Media.Video) {
            Queue.main.run(() -> {
                if (call != null && call.getObserver() != null) {
                    call.getObserver().onMediaChanged(new CallObserver.LocalVideoViewSizeChanged(call));
                }
            });
        }
        if (media == WMEngine.Media.Sharing) {
            Queue.main.run(() -> {
                if (call != null && call.getObserver() != null) {
                    call.getObserver().onMediaChanged(new CallObserver.LocalSharingViewSizeChanged(call));
                }
            });
        }
    }

    @Override
    public void onTrackMuted(WmeTrack.Type track, long vid, boolean mute) {
        Ln.d("onTrackMuted: %s, %s, %s", track, vid, mute);
        Queue.main.run(() -> {
            if (call != null) {
                CallObserver.MediaChangedEvent event = null;
                if (track == WmeTrack.Type.LocalVideo) {
                    call.updateMedia(call.isSendingAudio(), !mute);
                    event = new CallObserver.SendingVideo(call, !mute);
                }
                else if (track == WmeTrack.Type.RemoteVideo) {
                    event = new CallObserver.ReceivingVideo(call, !mute);
                }
                else if (track == WmeTrack.Type.LocalAudio) {
                    call.updateMedia(!mute, call.isSendingVideo());
                    event = new CallObserver.SendingAudio(call, !mute);
                }
                else if (track == WmeTrack.Type.RemoteAudio) {
                    event = new CallObserver.ReceivingAudio(call, !mute);
                }
                else if (track == WmeTrack.Type.LocalSharing) {
                    event = new CallObserver.SendingSharingEvent(call, !mute);
                }
                else if (track == WmeTrack.Type.RemoteSharing) {
                    event = new CallObserver.ReceivingSharing(call, !mute);
                }
                if (call.getObserver() != null && event != null) {
                    call.getObserver().onMediaChanged(event);
                }
            }
        });
    }

    @Override
    public void onCameraSwitched() {
        Queue.main.run(() -> {
            if (call != null && call.getObserver() != null) {
                call.getObserver().onMediaChanged(new CallObserver.CameraSwitched(call));
            }
        });
    }

    @Override
    public void onError(MediaError error) {
        Ln.e("Media Error: " + Json.get().toJson(error));
    }

    @Override
    public void onICEComplete() {

    }

    @Override
    public void onICEFailed() {

    }

    @Override
    public void onICEReportReady(boolean connectSuccess, List<MediaLine> mediaLines) {
        CallAnalyzerReporter.shared.reportIceEnd(call, connectSuccess, mediaLines);
    }

    @Override
    public void onMediaQualityMetricsReady(String metrics) {
        CallAnalyzerReporter.shared.reportMediaQualityMetrics(call, metrics);
    }

    @Override
    public void onMediaError(int mid, int vid, int errorCode) {
        CallAnalyzerReporter.shared.reportMediaCapabilities(call, errorCode);
    }

    @Override
    public void onMediaRxStart(WMEngine.Media media, Long csi) {
        CallAnalyzerReporter.shared.reportMediaRxStart(call, media, csi);
        if (media == WMEngine.Media.Audio) {
            CallAnalyzerReporter.shared.reportMediaRenderStart(call, media, null);
        }
    }

    @Override
    public void onMediaRxStop(WMEngine.Media media, Integer mediaStatus) {
        CallAnalyzerReporter.shared.reportMediaRxStop(call, media, mediaStatus);
    }

    @Override
    public void onMediaTxStart(WMEngine.Media media) {
        CallAnalyzerReporter.shared.reportMediaTxStart(call, media);
    }

    @Override
    public void onMediaTxStop(WMEngine.Media media) {
        CallAnalyzerReporter.shared.reportMediaTxStop(call, media);
    }

    @Override
    public void onMediaBlocked(WMEngine.Media media, boolean blocked) {
        if (blocked) {
            CallAnalyzerReporter.shared.reportMediaRenderStop(call, media);
            if (media == WMEngine.Media.Sharing) {
                onMediaRxStop(media, 0);
            }
        } else {
            CallAnalyzerReporter.shared.reportMediaRenderStart(call, media, null);
        }
    }
}
