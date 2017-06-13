package com.cisco.spark.android.core;

import android.content.Context;
import android.net.Uri;

import com.cisco.spark.android.R;
import com.cisco.spark.android.authenticator.OAuth2;
import com.cisco.spark.android.client.UrlProvider;
import com.cisco.spark.android.events.DeviceRegistrationChangedEvent;
import com.github.benoitdion.ln.Ln;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import de.greenrobot.event.EventBus;
import okhttp3.CertificatePinner;
import okio.ByteString;

public class SquaredCertificatePinner {
    private static final String SHA1_BASE = "sha1/";
    private static final String SHA1_INVALID = SHA1_BASE + "INVALID";
    private static final String SHA1_UNKNOWN = SHA1_BASE + "UNKNOWN";
    private static final String CERT_FILE_PREFIX = "cert_go_daddy";

    private CertificatePinner certificatePinner;
    private Context context;
    private Set<String> whitelist;
    private final UrlProvider urlProvider;
    private String intermediateSha1Pin;


    public SquaredCertificatePinner(Context context, Set<String> whitelist, EventBus bus, UrlProvider urlProvider) {
        this.context = context;
        this.whitelist = whitelist;
        this.urlProvider = urlProvider;
        refreshCertificatePinner();
        bus.register(this);
    }

    public CertificatePinner getCertificatePinner() {
        return certificatePinner;
    }

    private void refreshCertificatePinner() {
        CertificatePinner.Builder pinnedBuilder = new CertificatePinner.Builder();

        List<PinnedCert> pinnedCerts = getPinnedCerts();
        for (PinnedCert pinnedCert : pinnedCerts) {
            pinnedBuilder.add(pinnedCert.getDomain(), pinnedCert.getSha1pin());
        }

        certificatePinner = pinnedBuilder.build();
    }

    private List<PinnedCert> getPinnedCerts() {
        List<PinnedCert> pinnedCerts = new ArrayList<>();

        // Add the static domains to the list
        pinnedCerts.add(new PinnedCert(Uri.parse(urlProvider.getRegionUrl()).getHost(), getIntermediateSha1Pin()));
        pinnedCerts.addAll(getCIPinnedCerts());

        // Get the rest from the service catalog
        for (String domain : whitelist) {
            pinnedCerts.add(new PinnedCert(domain, getIntermediateSha1Pin()));
        }

        return pinnedCerts;
    }

    /**
     * The CI certs are different from the Spark certs and are managed by the WebEx team.
     * We were originally pinned on the intermediate cert only. Going forward we always want to pin on both the leaf and intermediate for more flexibility on cert upgrade.
     */
    private static List<PinnedCert> getCIPinnedCerts() {
        ArrayList<PinnedCert> pinnedCerts = new ArrayList<>();
        String host = Uri.parse(OAuth2.BASE_URL).getHost();
        // The WebEx certs need to be updated. Pin to the new and old during the transition.
        pinnedCerts.add(new PinnedCert(host, SHA1_BASE + "UCM4nF92oH7yVEZeTyGW+BRFtB4=")); // new intermediate (CN=Symantec Class 3 Secure Server CA - G4,OU=Symantec Trust Network,O=Symantec Corporation,C=US)
        return Collections.unmodifiableList(pinnedCerts);
    }

    private String getIntermediateSha1Pin() {
        if (intermediateSha1Pin == null) {
            try {
                InputStream certFile;
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                ArrayList<Integer> certs = getCertFileResIds();
                certFile = context.getResources().openRawResource(certs.get(0).intValue());
                Certificate certificate = certificateFactory.generateCertificate(certFile);
                certFile.close();
                intermediateSha1Pin = SHA1_BASE + getSha1Pin(certificate);
            } catch (CertificateException e) {
                Ln.e(e, "Error generating certificate from file.");
                return SHA1_INVALID;
            } catch (IOException e) {
                Ln.e(e, "Error opening/closing certificate file.");
                return SHA1_UNKNOWN;
            }
        }
        return intermediateSha1Pin;
    }

    // Iterate over the res/raw directory looking for the intermediate GoDaddy certificate
    // NOTE: THIS SHOULD ONLY RETURN 1 ID, BUT LEAVING COLLECTION LOGIC IF NEEDED IN FUTURE
    private ArrayList<Integer> getCertFileResIds() {
        Field[] fields = R.raw.class.getFields();
        ArrayList<Integer> certIds = new ArrayList<>();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getName().startsWith(CERT_FILE_PREFIX)) {
                try {
                    certIds.add(fields[i].getInt(fields[i]));
                } catch (IllegalAccessException e) {
                    Ln.e(e, "Error getting certificate resource ID during initialization.");
                }
            }
        }
        return certIds;
    }

    private static String getSha1Pin(Certificate certificate) {
        try {
            ByteString s = ByteString.of(certificate.getPublicKey().getEncoded());
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            byte[] sha1Bytes = messageDigest.digest(s.toByteArray());
            return ByteString.of(sha1Bytes).base64();
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("UnusedDeclaration") // Called by the event bus.
    public void onEventMainThread(DeviceRegistrationChangedEvent event) {
        refreshCertificatePinner();
    }

    private static class PinnedCert {
        private String domain;
        private String sha1pin;

        public PinnedCert(String domain, String sha1pin) {
            this.domain = domain;
            this.sha1pin = sha1pin;
        }

        public String getDomain() {
            return domain;
        }

        public String getSha1pin() {
            return sha1pin;
        }
    }
}
