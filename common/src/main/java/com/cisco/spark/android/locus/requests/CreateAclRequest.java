package com.cisco.spark.android.locus.requests;

import com.cisco.spark.android.features.CoreFeatures;
import com.cisco.spark.android.locus.model.LocusParticipantInfo;

import java.util.List;

public class CreateAclRequest extends DeltaRequest {

    private String kmsMessage;
    private List<LocusParticipantInfo> people;

    public CreateAclRequest(CoreFeatures coreFeatures, String kmsMessage, List<LocusParticipantInfo> people) {
        super(coreFeatures);
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
