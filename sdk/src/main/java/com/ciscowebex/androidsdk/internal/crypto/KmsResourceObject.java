/*
 * Copyright 2016-2020 Cisco Systems Inc
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

package com.ciscowebex.androidsdk.internal.crypto;

import android.net.Uri;
import android.support.annotation.NonNull;
import com.github.benoitdion.ln.Ln;

import java.net.URI;
import java.net.URISyntaxException;

public class KmsResourceObject {

    String uri;

    public KmsResourceObject(@NonNull String uri) {
        this.uri = uri;
    }

    public URI getURI() {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            Ln.e(e);
        }
        return null;
    }

    public String getUri() {
        return uri;
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
