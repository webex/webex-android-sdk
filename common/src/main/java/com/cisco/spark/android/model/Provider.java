package com.cisco.spark.android.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Class that represents an integration's service provider which allows us to render posts like:
 *
 * $SERVICE_PROVIDER via $ACTOR
 */
public class Provider implements Parcelable {
    String id;
    String displayName;
    String machineAccountId;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.displayName);
        dest.writeString(this.machineAccountId);
    }

    public Provider() {
    }

    protected Provider(Parcel in) {
        this.id = in.readString();
        this.displayName = in.readString();
        this.machineAccountId = in.readString();
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMachineAccountId() {
        return machineAccountId;
    }

    public static final Parcelable.Creator<Provider> CREATOR = new Parcelable.Creator<Provider>() {
        @Override
        public Provider createFromParcel(Parcel source) {
            return new Provider(source);
        }

        @Override
        public Provider[] newArray(int size) {
            return new Provider[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Provider provider = (Provider) o;

        return id != null ? id.equals(provider.id) : provider.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public static boolean equals(Provider provider1, Provider provider2) {
        if (provider1 == null && provider2 == null) {
            return true;
        }

        return provider1 != null && provider1.equals(provider2);
    }
}
