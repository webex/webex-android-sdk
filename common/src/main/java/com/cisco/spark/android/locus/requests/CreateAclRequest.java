package com.cisco.spark.android.locus.requests;

import com.cisco.spark.android.locus.model.LocusParticipantInfo;

import java.util.List;

public class CreateAclRequest {

    private String kmsMessage;
    private List<LocusParticipantInfo> people;

    public CreateAclRequest(String kmsMessage, List<LocusParticipantInfo> people) {
        this.kmsMessage = kmsMessage;
        this.people = people;
    }

    public String getKmsMessage() {
        return kmsMessage;
    }

    public List<LocusParticipantInfo> getPeople() {
        return people;
    }
}
