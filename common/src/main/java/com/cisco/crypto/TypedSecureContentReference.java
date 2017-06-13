package com.cisco.crypto.scr;

import com.cisco.spark.android.client.CountedTypedOutput;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import okio.BufferedSink;

public class TypedSecureContentReference extends CountedTypedOutput {
    private final SecureContentReference secureContentReference;

    public TypedSecureContentReference(String mimeType, File file) {
        super(mimeType, file);
        secureContentReference = SecureContentReference.createInstance();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        OutputStream out = secureContentReference.encrypt(sink.outputStream());
        super.writeTo(out);
        // SecureContentReference.out() doesn't close the wrapped OutputStream, so this is ok. Retrofit
        // closes it later.
        out.close();
    }

    public SecureContentReference getSecureContentReference() {
        return secureContentReference;
    }
}
