package com.cisco.spark.android.sync;

import android.net.Uri;
import android.support.annotation.NonNull;

import com.github.benoitdion.ln.Ln;

import java.net.URI;
import java.net.URISyntaxException;

public class KmsResourceObject {
    Uri uri;

    public KmsResourceObject(@NonNull Uri uri) {
        this.uri = uri;
    }

    public KmsResourceObject(@NonNull URI uri) {
        this(Uri.parse(uri.toString()));
    }

    public URI getURI() {
        try {
            return new URI(uri.toString());
        } catch (URISyntaxException e) {
            Ln.e(e);
        }
        return null;
    }

    public Uri getUri() {
        return uri;
    }

    public Uri getAuthorizationsUri() {
        return Uri.withAppendedPath(uri, "authorizations");
    }

    public Uri getAuthorizationsUriForAuthId(String authId) {
        return getAuthorizationsUri().buildUpon().appendQueryParameter("authId", authId).build();
    }

    @Override
    public String toString() {
        return uri.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof KmsResourceObject && uri != null) {
            return uri.equals(((KmsResourceObject) o).uri);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }
}
