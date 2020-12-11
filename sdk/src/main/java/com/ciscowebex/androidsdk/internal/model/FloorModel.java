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

package com.ciscowebex.androidsdk.internal.model;

/**
 *  A Floor for a MediaShare is a "temporary permission to access or manipulate" a MediaShare. From a ReST perspective,
 *  a Floor is a sub-resource of a MediaShare.  The Benificiary of a Floor is the Locus Participant that currently has
 *  permission to control the MediaShare.  A Requester of a Floor is a Locus Participant that wishes to obtain the floor
 *  for itself or for another Participant (the beneficiary).
 */
public class FloorModel {

    public enum Disposition {
        ACCEPTED, GRANTED, RELEASED
    }

    public FloorModel(LocusParticipantModel beneficiary, LocusParticipantModel requester, Disposition disposition) {
        this.disposition = disposition;
        this.beneficiary = beneficiary;
        this.requester = requester;
    }

    private Disposition disposition;

    private String requested;

    private String granted;

    private String released;

    private LocusParticipantModel requester;

    private LocusParticipantModel beneficiary;

    public Disposition getDisposition() {
        return disposition;
    }

    public void setDisposition(Disposition disposition) {
        this.disposition = disposition;
    }

    public String getRequested() {
        return requested;
    }

    public void setRequested(String requested) {
        this.requested = requested;
    }

    public String getGranted() {
        return granted;
    }

    public void setGranted(String granted) {
        this.granted = granted;
    }

    public String getReleased() {
        return released;
    }

    public void setReleased(String released) {
        this.released = released;
    }

    public LocusParticipantModel getRequester() {
        return requester;
    }

    public void setRequester(LocusParticipantModel requester) {
        this.requester = requester;
    }

    public LocusParticipantModel getBeneficiary() {
        return beneficiary;
    }

    public void setBeneficiary(LocusParticipantModel beneficiary) {
        this.beneficiary = beneficiary;
    }

    public boolean isValid() {
        return granted != null && disposition != null && beneficiary != null;
    }

}
