package com.cisco.spark.android.sync;

/*
* Mapping based on KMS Draft section 4.7
*/
public enum KmsMessageRequestType {
    AUTHORIZATIONS,
    CREATE_EPHEMERAL_KEY,
    GET_KEYS,
    PING,
    CREATE_RESOURCE;
}
