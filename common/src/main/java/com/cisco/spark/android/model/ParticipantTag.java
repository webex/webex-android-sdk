package com.cisco.spark.android.model;

/*
 * Please note:
 * Do not remove values from this list if they are no longer
 * going to be used. Please annotate it as @Deprecated instead.
 * We use Gson to serialize this for Top Participants in the
 * DB and it will cause a NPE if an old value is in the
 * serialized version but not in the Enum.
 */
public enum ParticipantTag {
    ENTITLEMENT_NO_SQUARED,

    @Deprecated
    SIDE_BOARDED,

    CI_NOTFOUND,
    NOT_SIGNED_UP
}
