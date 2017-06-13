package com.cisco.spark.android.acl;

import android.net.Uri;

public class AclLinkRequest {

    public enum AclLinkOperation {
        ADD,
        DELETE
    }

    private Uri linkedAcl;
    private AclLinkType aclLinkType = AclLinkType.INCOMING;
    private AclLinkOperation aclLinkOperation;
    private String kmsMessage;

    public AclLinkRequest(AclLinkOperation operation, Uri linkedAcl, String kmsMessage) {
        this.linkedAcl = linkedAcl;
        this.kmsMessage = kmsMessage;
        this.aclLinkOperation = operation;
    }
}
