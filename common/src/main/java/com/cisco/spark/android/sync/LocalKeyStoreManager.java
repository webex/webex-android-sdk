package com.cisco.spark.android.sync;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.text.TextUtils;

import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.util.Calendar;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.security.auth.x500.X500Principal;
@Singleton
public class LocalKeyStoreManager {
    private static boolean initialized = false;
    private static final String alias = "masterKey";
    private static String password = "";
    private static Context context;

    @Inject
    public LocalKeyStoreManager(Context context) {
        this.context = context;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    protected synchronized static void createOrGetPasswordFromKeystore(Context context) {
        LocalKeyStoreManager.context = context;
        KeyStore keystore = null;
            try {
                keystore = KeyStore.getInstance("AndroidKeyStore");
                keystore.load(null);
                if (!keystore.containsAlias(alias)) {
                    Calendar start = Calendar.getInstance();
                    Calendar end = Calendar.getInstance();
                    end.add(Calendar.YEAR, 1);
                    KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
                            .setAlias(alias)
                            .setSubject(new X500Principal("CN=Sample Name, O=Android Authority"))
                            .setSerialNumber(BigInteger.ONE)
                            .setStartDate(start.getTime())
                            .setEndDate(end.getTime())
                            .build();
                    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
                    generator.initialize(spec);
                    KeyPair keyPair = generator.generateKeyPair();
                    RSAPublicKey key = (RSAPublicKey) keyPair.getPublic();
                    password = encodePassword(key.getEncoded());
                } else if (TextUtils.isEmpty(password)) {
                    KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keystore.getEntry(alias, null);
                    RSAPublicKey publicKey = (RSAPublicKey) privateKeyEntry.getCertificate().getPublicKey();
                    password = encodePassword(publicKey.getEncoded());
                }
            } catch (KeyStoreException e) {
                Ln.e(e.getMessage(), "KeyStore exception");
            } catch (CertificateException e) {
                Ln.e(e.getMessage(), "Certificate exception");
            } catch (NoSuchAlgorithmException e) {
                Ln.e(e.getMessage(), "NosuchAlgorithm exception");
            } catch (IOException e) {
                Ln.e(e.getMessage(), "IO exception");
            } catch (NoSuchProviderException e) {
                Ln.e(e.getMessage(), "No such provider exception");
            } catch (InvalidAlgorithmParameterException e) {
                Ln.e(e.getMessage(), "Invalid Algorithm parameter exception");
            } catch (UnrecoverableKeyException e) {
                Ln.e(e.getMessage(), "Unrecoverable key exception");
            } catch (UnrecoverableEntryException e) {
                Ln.e(e.getMessage(), "Unrecoverable entry exception");
            }
    }

    protected synchronized static void init(Context context) {
        createOrGetPasswordFromKeystore(context);
        if (!TextUtils.isEmpty(password)) {
            initialized = true;
        }
    }

    public synchronized static String getMasterPassword() {
        if (!initialized) {
            init(LocalKeyStoreManager.context);
        }
        return password;
    }
    public synchronized static void setMasterPassword(String newPassword) {
        password = newPassword;
    }

    public static String encodePassword(byte[] encodedKeyByteArray) {
        return new BigInteger(1, encodedKeyByteArray).toString(16);
    }
}
