package com.ciscowebex.androidsdk.internal.crypto;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jose.util.Base64URL;
import net.minidev.json.JSONObject;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESLightEngine;
import org.bouncycastle.crypto.io.InvalidCipherTextIOException;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class SecureContentReference {
    private String enc;
    private byte[] key;

    private byte[] iv;
    private String aad;
    private byte[] tag;
    private String loc;

    private boolean isGiphy;

    private SecureContentReference() {
    }

    public static SecureContentReference createInstance() {
        SecureContentReference scr = new SecureContentReference();
        SecureRandom rng = new SecureRandom();
        scr.setEnc("A256GCM");
        byte[] data;
        data = new byte[32];
        rng.nextBytes(data);
        scr.setKey(data);
        data = new byte[12];
        rng.nextBytes(data);
        scr.setIV(data);
        Date ts = new Date();
        DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
        fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        String aad = fmt.format(ts);
        scr.setAAD(aad);
        scr.isGiphy = false;
        return scr;
    }

    public static SecureContentReference fromJWEForGiphy(byte[] key, String str) throws ParseException, JOSEException {
        JWEObject jwe = JWEObject.parse(str);
        jwe.decrypt(new DirectDecrypter(key));
        SecureContentReference scr = new SecureContentReference();
        JSONObject json = jwe.getPayload().toJSONObject();
        Object value;
        value = json.get("loc");
        scr.setLoc(value.toString());
        return scr;
    }

    public static SecureContentReference fromJWE(byte[] key, String str) throws ParseException, JOSEException {
        JWEObject jwe = JWEObject.parse(str);
        jwe.decrypt(new DirectDecrypter(key));
        SecureContentReference scr = new SecureContentReference();
        JSONObject json = jwe.getPayload().toJSONObject();
        Object value;
        byte[] data;
        value = json.get("enc");
        scr.setEnc(value.toString());
        value = json.get("aad");
        scr.setAAD(value.toString());
        value = json.get("loc");
        scr.setLoc(value.toString());
        value = json.get("key");
        data = new Base64URL(value.toString()).decode();
        scr.setKey(data);
        value = json.get("iv");
        data = new Base64URL(value.toString()).decode();
        scr.setIV(data);
        value = json.get("tag");
        data = new Base64URL(value.toString()).decode();
        scr.setTag(data);
        return scr;
    }

    public final String getEnc() {
        return enc;
    }

    private void setEnc(String enc) {
        this.enc = enc;
    }

    public final byte[] getKey() {
        return key;
    }

    private void setKey(byte[] key) {
        this.key = key;
    }

    public final byte[] getIV() {
        return iv;
    }

    public final void setIsGiphy(boolean value) {
        this.isGiphy = value;
    }

    private void setIV(byte[] iv) {
        this.iv = iv;
    }

    public final String getAAD() {
        return aad;
    }

    private void setAAD(String aad) {
        this.aad = aad;
    }

    public final String getLoc() {
        return loc;
    }

    public final void setLoc(String loc) {
        this.loc = loc;
    }

    public final byte[] getTag() {
        return tag;
    }

    private void setTag(byte[] tag) {
        this.tag = tag;
    }

    public String toJWE(byte[] key) throws JOSEException {
        JSONObject json = new JSONObject();
        String value;
        if (!isGiphy) {
            value = getEnc();
            json.put("enc", value);

            value = Base64URL.encode(getKey()).toString();
            json.put("key", value);

            value = Base64URL.encode(getIV()).toString();
            json.put("iv", value);

            value = getAAD();
            json.put("aad", value);

            value = Base64URL.encode(getTag()).toString();
            json.put("tag", value);
        }

        value = getLoc();
        json.put("loc", value);

        JWEHeader header = new JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A256GCM);
        Payload payload = new Payload(json);
        JWEObject jwe = new JWEObject(header, payload);
        jwe.encrypt(new DirectEncrypter(key));
        return jwe.serialize();
    }

    public OutputStream encrypt(OutputStream os) {
        AEADBlockCipher c = createCipher(true);
        return new SecureContentReference.CipherOutputStream(os, c);
    }

    public InputStream decrypt(InputStream is) {
        AEADBlockCipher c = createCipher(false);
        return new CipherInputStream(is, c);
    }

    private AEADBlockCipher createCipher(boolean encrypting) {
        KeyParameter k = new KeyParameter(getKey());
        byte[] aad = getAAD().getBytes(StandardCharsets.UTF_8);
        AEADParameters params = new AEADParameters(k, 128, getIV(), aad);
        GCMBlockCipher c = new GCMBlockCipher(new AESLightEngine());
        c.init(encrypting, params);
        return c;
    }

    private class CipherOutputStream extends FilterOutputStream {
        private final byte[] oneByte = new byte[1];
        AEADBlockCipher cipher;
        private byte[] buf;

        public CipherOutputStream(OutputStream out, AEADBlockCipher c) {
            super(out);
            cipher = c;
        }

        @Override
        public void write(int b) throws IOException {
            oneByte[0] = (byte) b;
            write(oneByte, 0, 1);
        }

        @Override
        public void write(@NotNull byte[] b) throws IOException {
            write(b, 0, b.length);
        }

        @Override
        public void write(@NotNull byte[] b, int off, int len) throws IOException {
            ensureCapacity(len, false);
            int outLen = cipher.processBytes(b, off, len, buf, 0);
            if (outLen != 0) {
                out.write(buf, 0, outLen);
            }
        }

        private void ensureCapacity(int updateSize, boolean finalOutput) {
            int bufLen;
            if (finalOutput) {
                bufLen = cipher.getOutputSize(updateSize);
            } else {
                bufLen = cipher.getUpdateOutputSize(updateSize);
            }
            if ((buf == null) || (buf.length < bufLen)) {
                buf = new byte[bufLen];
            }
        }

        @Override
        public void close() throws IOException {
            ensureCapacity(0, true);
            IOException error = null;
            try {
                int outLen = cipher.doFinal(buf, 0);
                if (outLen > 16) {
                    out.write(buf, 0, outLen - 16);
                }
                setTag(Arrays.copyOfRange(buf, outLen - 16, outLen));
            } catch (final InvalidCipherTextException e) {
                error = new InvalidCipherTextIOException("Error finalising cipher data", e);
            } catch (Exception e) {
                error = new IOException("Error closing stream: ", e);
            }
            try {
                flush();
            } catch (IOException e) {
                if (error == null) {
                    error = e;
                }
            }
            if (error != null) {
                throw error;
            }
        }
    }

    public class CipherInputStream extends FilterInputStream {
        private static final int INPUT_BUF_SIZE = 2048;

        private byte[] inBuf;

        private AEADBlockCipher cipher;

        private byte[] buf;
        private byte[] markBuf;

        private int bufOff;
        private int maxBuf;
        private boolean finalized;
        private int markBufOff;

        public CipherInputStream(InputStream is, AEADBlockCipher cipher) {
            this(is, cipher, INPUT_BUF_SIZE);
        }

        public CipherInputStream(InputStream is, AEADBlockCipher cipher, int bufSize) {
            super(is);
            this.cipher = cipher;
            this.inBuf = new byte[bufSize];
        }

        private int nextChunk() throws IOException {
            if (finalized) {
                return -1;
            }
            bufOff = 0;
            maxBuf = 0;

            while (maxBuf == 0) {
                try {
                    int read = in.read(inBuf);
                    if (read == -1) {
                        finaliseCipher();
                        if (maxBuf == 0) {
                            return -1;
                        }
                        return maxBuf;
                    }
                    ensureCapacity(read, false);
                    maxBuf = cipher.processBytes(inBuf, 0, read, buf, 0);
                } catch (Exception e) {
                    throw new IOException("Error processing stream ", e);
                }
            }
            return maxBuf;
        }

        private void finaliseCipher() throws IOException {
            try {
                finalized = true;
                ensureCapacity(0, true);
                maxBuf = cipher.processBytes(tag, 0, tag.length, buf, 0);
                maxBuf = cipher.doFinal(buf, maxBuf) + maxBuf;
            } catch (final InvalidCipherTextException e) {
                throw new InvalidCipherTextIOException("Error finalising cipher", e);
            } catch (Exception e) {
                throw new IOException("Error finalising cipher " + e);
            }
        }

        public int read() throws IOException {
            if (bufOff >= maxBuf) {
                if (nextChunk() < 0) {
                    return -1;
                }
            }
            return buf[bufOff++] & 0xff;
        }

        public int read(@NotNull byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        public int read(@NotNull byte[] b, int off, int len) throws IOException {
            if (bufOff >= maxBuf) {
                if (nextChunk() < 0) {
                    return -1;
                }
            }
            int toSupply = Math.min(len, available());
            System.arraycopy(buf, bufOff, b, off, toSupply);
            bufOff += toSupply;
            return toSupply;
        }

        public long skip(long n) {
            if (n <= 0) {
                return 0;
            }
            int skip = (int) Math.min(n, available());
            bufOff += skip;
            return skip;
        }

        public int available() {
            return maxBuf - bufOff;
        }

        private void ensureCapacity(int updateSize, boolean finalOutput) {
            int bufLen;
            if (finalOutput) {
                bufLen = cipher.getOutputSize(updateSize);
            } else {
                bufLen = cipher.getUpdateOutputSize(updateSize);
            }
            if ((buf == null) || (buf.length < bufLen)) {
                buf = new byte[bufLen];
            }
        }

        public void close() throws IOException {
            try {
                in.close();
            } finally {
                if (!finalized) {
                    // Reset the cipher, discarding any data buffered in it
                    // Errors in cipher finalisation trump I/O error closing input
                    finaliseCipher();
                }
            }
            maxBuf = bufOff = 0;
            markBufOff = 0;
            long markPosition = 0;
            if (markBuf != null) {
                Arrays.fill(markBuf, (byte) 0);
                markBuf = null;
            }
            if (buf != null) {
                Arrays.fill(buf, (byte) 0);
                buf = null;
            }
            Arrays.fill(inBuf, (byte) 0);
        }

        public void mark(int readlimit) {
            in.mark(readlimit);
            if (buf != null) {
                markBuf = new byte[buf.length];
                System.arraycopy(buf, 0, markBuf, 0, buf.length);
            }
            markBufOff = bufOff;
        }

        public void reset() throws IOException {
            throw new IOException("Reset not supported");
        }

        public boolean markSupported() {
            return false;
        }
    }
}

