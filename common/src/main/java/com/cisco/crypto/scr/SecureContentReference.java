package com.cisco.crypto.scr;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jose.util.Base64URL;

import net.minidev.json.JSONObject;

import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.engines.AESFastEngine;
import org.spongycastle.crypto.io.InvalidCipherTextIOException;
import org.spongycastle.crypto.modes.AEADBlockCipher;
import org.spongycastle.crypto.modes.GCMBlockCipher;
import org.spongycastle.crypto.params.AEADParameters;
import org.spongycastle.crypto.params.KeyParameter;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    // fields
    private byte[] iv;
    private String aad;
    private byte[] tag;
    private String loc;

    // constructors
    private SecureContentReference() {
    }

    // methods
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

    private final void setEnc(String enc) {
        this.enc = enc;
    }

    public final byte[] getKey() {
        return key;
    }

    private final void setKey(byte[] key) {
        this.key = key;
    }

    public final byte[] getIV() {
        return iv;
    }

    private final void setIV(byte[] iv) {
        this.iv = iv;
    }

    public final String getAAD() {
        return aad;
    }

    private final void setAAD(String aad) {
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

    private final void setTag(byte[] tag) {
        this.tag = tag;
    }

    public String toJWE(byte[] key) throws JOSEException {
        JSONObject json = new JSONObject();

        // assemble JSON
        String value;
        value = getEnc();
        json.put("enc", value);

        value = Base64URL.encode(getKey()).toString();
        json.put("key", value);

        value = Base64URL.encode(getIV()).toString();
        json.put("iv", value);

        value = getAAD();
        json.put("aad", value);

        value = getLoc();
        json.put("loc", value);

        value = Base64URL.encode(getTag()).toString();
        json.put("tag", value);

        JWEHeader header = new JWEHeader(JWEAlgorithm.DIR,
                EncryptionMethod.A256GCM);
        Payload payload = new Payload(json);
        JWEObject jwe = new JWEObject(header, payload);
        jwe.encrypt(new DirectEncrypter(key));

        return jwe.serialize();
    }

    public OutputStream encrypt(OutputStream os) throws IOException {
        AEADBlockCipher c = createCipher(true);

        return new CipherOutputStream(os, c);
    }

    public InputStream decrypt(InputStream is) throws IOException {
        AEADBlockCipher c = createCipher(false);

        return new CipherInputStream(is, c);
    }

    private AEADBlockCipher createCipher(boolean encrypting) throws IOException {
        // NOTE: assumes AES-GCM!

        // Construct the Key
        KeyParameter k = new KeyParameter(getKey());

        // Apply AAD now
        byte[] aad = getAAD().getBytes("UTF-8");

        // Construct the cipher
        AEADParameters params = new AEADParameters(k, 128, getIV(), aad);
        GCMBlockCipher c = new GCMBlockCipher(new AESFastEngine());
        c.init(encrypting, params);

        return c;
    }

    // inner classes
    private class CipherOutputStream extends FilterOutputStream {
        private final byte[] oneByte = new byte[1];
        // fields
        AEADBlockCipher cipher;
        private byte[] buf;

        // constructors
        public CipherOutputStream(OutputStream out, AEADBlockCipher c) {
            super(out);
            cipher = c;
        }

        // methods
        @Override
        public void write(int b) throws IOException {
            oneByte[0] = (byte) b;
            write(oneByte, 0, 1);
        }

        @Override
        public void write(byte[] b) throws IOException {
            write(b, 0, b.length);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {

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
                // Invalid ciphertext takes precedence over close error
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
        private long markPosition;
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

            // Keep reading until EOF or cipher processing produces data
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

        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        public int read(byte[] b, int off, int len) throws IOException {
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

        public long skip(long n) throws IOException {
            if (n <= 0) {
                return 0;
            }

            int skip = (int) Math.min(n, available());
            bufOff += skip;

            return skip;
        }

        public int available() throws IOException {
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
            markPosition = 0;
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
