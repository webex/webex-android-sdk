/*
 * Copyright 2016-2021 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscowebex.androidsdk.internal.model;

import com.ciscowebex.androidsdk.internal.crypto.KeyObject;
import com.ciscowebex.androidsdk.internal.crypto.SecureContentReference;
import com.ciscowebex.androidsdk.utils.GiphyUtils;
import com.ciscowebex.androidsdk.utils.UriUtils;
import com.github.benoitdion.ln.Ln;
import com.nimbusds.jose.JOSEException;
import me.helloworld.utils.Checker;

public class ImageModel {

    private String url;
    private int width;
    private int height;
    private String scr;
    private SecureContentReference secureContentReference;
    private boolean thumbnail;

    public ImageModel() {
    }

    public ImageModel(String url, int width, int height, SecureContentReference secureContentReference, boolean thumbnail) {
        this.url = url;
        this.width = width;
        this.height = height;
        this.secureContentReference = secureContentReference;
        this.thumbnail = thumbnail;
    }

    public String getUrl() {
        return url;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public String toString() {
        return "Image{" +
                "url=" + url +
                ", width=" + width +
                ", height=" + height +
                '}';
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isThumbnail() {
        return thumbnail;
    }

    public String getScr() {
        return scr;
    }

    public void setScr(String scr) {
        this.scr = scr;
    }

    public String getUrlOrSecureLocation() {
        if (isSecureContentReference()) {
            return secureContentReference.getLoc();
        } else {
            return getUrl();
        }
    }

    public void setSecureContentReference(SecureContentReference secureContentReference) {
        this.secureContentReference = secureContentReference;
        if (secureContentReference != null) {
            setUrl(secureContentReference.getLoc());
        }
    }

    public boolean isSecureContentReference() {
        return secureContentReference != null;
    }

    public SecureContentReference getSecureContentReference() {
        return secureContentReference;
    }

    public void encrypt(KeyObject key) {
        if (secureContentReference != null) {
            secureContentReference.setIsGiphy(GiphyUtils.isUriGiphy(getUrl()));
            try {
                setScr(secureContentReference.toJWE(key.getKeyBytes()));
                setSecureContentReference(null);
            } catch (JOSEException e) {
                Ln.d(e);
            }
        }
    }

    public void decrypt(KeyObject key) {
        String scr = getScr();
        if (Checker.isEmpty(scr) && getUrl() != null && getUrl().startsWith("secureContentReference")) {
            scr = UriUtils.parseIfNotNull(getUrl()).getSchemeSpecificPart();
        }

        if (!Checker.isEmpty(scr)) {
            try {
                if (GiphyUtils.isUriGiphy(getUrl())) {
                    setSecureContentReference(SecureContentReference.fromJWEForGiphy(key.getKeyBytes(), scr));
                } else {
                    setSecureContentReference(SecureContentReference.fromJWE(key.getKeyBytes(), scr));
                }
            } catch (Throwable t) {
                Ln.e(t);
            }
        }
    }

    public void copyFromFile(FileModel file) {
        this.scr = file.getScr();
        this.url = file.getUrl();
        this.secureContentReference = file.getSecureContentReference();
    }

}
