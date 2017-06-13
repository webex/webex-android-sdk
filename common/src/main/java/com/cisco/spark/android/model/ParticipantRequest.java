package com.cisco.spark.android.model;

import com.cisco.spark.android.util.JsonUtils;

import java.util.Collection;

public class ParticipantRequest {
    private final String authorizations;

    public ParticipantRequest(Collection<String> userIdList) {
        this.authorizations =  JsonUtils.stringify(userIdList.toString());
    }

    public String getAuthorizations() {
        return authorizations;
    }
}
