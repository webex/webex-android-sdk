package com.cisco.spark.android.acl;

import java.net.URI;
import java.util.Date;

public class AclLinkResponse {

    URI aclUrl;
    URI linkedAclUrl;
    Date expiryTimestamp;
    AclLinkType linkType;
    String kmsMessage;
}
