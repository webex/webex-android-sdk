package com.cisco.spark.android.mercury;

import com.google.gson.annotations.SerializedName;

public enum AlertType {
    @SerializedName("full")
    FULL,
    @SerializedName("visual")
    VISUAL,
    @SerializedName("none")
    NONE
}
