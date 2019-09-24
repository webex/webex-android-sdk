package com.ciscowebex.androidsdk.membership;

import java.util.Date;

public class MembershipReadStatus {
    protected Membership _membership;

    protected String _lastSeenId;

    protected Date _lastSeenDate;

    /**
     * The membership of the space
     * @return the membership of the space
     * @since 2.2.0
     */
    public Membership getMembership() {
        return _membership;
    }

    /**
     * The id of the last message which the member have seen
     * @return the id of the last message which the member have seen
     * @since 2.2.0
     */
    public String getLastSeenId() {
        return _lastSeenId;
    }

    /**
     * The published date of the last message that the member have seen
     * @return the published date of the last message that the member have seen
     * @since 2.2.0
     */
    public Date getLastSeenDate() {
        return _lastSeenDate;
    }
}
