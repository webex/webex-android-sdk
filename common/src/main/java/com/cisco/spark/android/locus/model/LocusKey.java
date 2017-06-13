package com.cisco.spark.android.locus.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class LocusKey implements Parcelable {
    private Uri url;

    private LocusKey(Uri locusUrl) {
        this.url = locusUrl;
    }

    protected LocusKey(Parcel in) {
        url = in.readParcelable(Uri.class.getClassLoader());
    }

    public static final Creator<LocusKey> CREATOR = new Creator<LocusKey>() {
        @Override
        public LocusKey createFromParcel(Parcel in) {
            return new LocusKey(in);
        }

        @Override
        public LocusKey[] newArray(int size) {
            return new LocusKey[size];
        }
    };

    public Uri getUrl() {
        return url;
    }

    public String getLocusId() {
        if (url == null) {
            return "<NULL>";
        }
        String[] parts = url.toString().split("/");
        if (parts.length == 0) {
            return "<NULL>";
        } else {
            return parts[parts.length - 1];
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocusKey locusKey = (LocusKey) o;

        return url.equals(locusKey.url);
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }

    @Override
    public String toString() {
        return url.toString();
    }

    public static LocusKey fromString(String url) {
        try {
            return new LocusKey(Uri.parse(url));
        } catch (Exception e) {
            return null;
        }
    }

    public static LocusKey fromUri(android.net.Uri uri) {
        if (uri == null)
            return null;
        return new LocusKey(uri);
    }

    public static LocusKey fromJavaURI(java.net.URI uri) {
        if (uri == null) {
            return null;
        }

        return LocusKey.fromString(uri.toString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(url, flags);
    }
}
