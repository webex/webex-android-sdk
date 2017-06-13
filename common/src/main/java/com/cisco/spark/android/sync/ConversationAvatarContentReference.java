package com.cisco.spark.android.sync;

import android.net.Uri;
import android.support.annotation.Nullable;

import com.cisco.crypto.scr.ContentReference;
import com.cisco.crypto.scr.SecureContentReference;
import com.cisco.spark.android.util.UriUtils;
import com.google.gson.Gson;

import javax.inject.Inject;

public class ConversationAvatarContentReference implements ContentReference {
    SecureContentReference secureContentReference;
    String scr;
    Uri url;

    @Inject transient Gson gson;

    @Override
    public boolean isSecureContentReference() {
        return secureContentReference != null;
    }

    @Override
    public SecureContentReference getSecureContentReference() {
        return secureContentReference;
    }

    @Override
    public void setSecureContentReference(SecureContentReference secureContentReference) {
        this.secureContentReference = secureContentReference;
        if (secureContentReference != null) {
            setUrl(UriUtils.parseIfNotNull(secureContentReference.getLoc()));
        }
    }

    @Override
    public String getScr() {
        return scr;
    }

    @Override
    public void setScr(String scr) {
        this.scr = scr;
    }

    @Override
    public @Nullable Uri getUrlOrSecureLocation() {
        if (isSecureContentReference()) {
            return UriUtils.parseIfNotNull(secureContentReference.getLoc());
        } else {
            return getUrl();
        }
    }

    public @Nullable Uri getUrl() {
        return url;
    }

    public void setUrl(Uri url) {
        this.url = url;
    }
}
