package com.cisco.crypto.scr;

import android.net.Uri;

public interface ContentReference {
    public boolean isSecureContentReference();
    public SecureContentReference getSecureContentReference();
    public void setSecureContentReference(SecureContentReference secureContentReference);
    public String getScr();
    public void setScr(String scr);
    public Uri getUrlOrSecureLocation();
}
