package com.cisco.spark.android.whiteboard.persistence.model;

import com.google.gson.annotations.SerializedName;

public enum ChannelType {
    @SerializedName("whiteboard") WHITEBOARD,
    @SerializedName("annotated") ANNOTATION,
}
