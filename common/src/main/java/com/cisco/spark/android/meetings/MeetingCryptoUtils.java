package com.cisco.spark.android.meetings;


import android.net.Uri;
import android.text.TextUtils;

import com.cisco.spark.android.model.CalendarMeeting;
import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.util.CryptoUtils;
import com.github.benoitdion.ln.Ln;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import rx.Subscriber;

public class MeetingCryptoUtils {
    private static final long SECONDS_WAITING_FOR_BOUND_KEY = 10;
    private KeyManager keyManager;

    @Inject
    public MeetingCryptoUtils(KeyManager keyManager) {
        this.keyManager = keyManager;
    }

    // get key for instant use
    private KeyObject getBoundKey(Uri keyUrl) {
        if (keyUrl == null) {
            return null;
        }
        final AtomicReference<KeyObject> keyObjectReference = new AtomicReference<>(null);
        final CountDownLatch latch  = new CountDownLatch(1);
        keyManager.getBoundKeySync(keyUrl).subscribe(new Subscriber<KeyObject>() {
            @Override
            public void onCompleted() {
                Ln.i("Getting key completed");
            }

            @Override
            public void onError(Throwable e) {
                Ln.e(e, "An error occurred getting keys");
                latch.countDown();
            }

            @Override
            public void onNext(KeyObject keyObject) {
                keyObjectReference.set(keyObject);
                latch.countDown();
            }
        });

        try {
            if (!latch.await(SECONDS_WAITING_FOR_BOUND_KEY, TimeUnit.SECONDS)) {
                Ln.e("Timed out waiting %d seconds for key for URL %s", SECONDS_WAITING_FOR_BOUND_KEY, keyUrl.toString());
                return null;
            }
        } catch (InterruptedException e) {
            Ln.e(e, "Interrupted while waiting for key for URL %s", keyUrl.toString());
            return null;
        }

        return keyObjectReference.get();
    }


    public CalendarMeeting decryptMeeting(CalendarMeeting calendarMeeting) {
        if (calendarMeeting == null)
            return null;

        if (calendarMeeting != null && !calendarMeeting.isEncrypted())
            return calendarMeeting;

        if (TextUtils.isEmpty(calendarMeeting.getEncryptionKeyUrl().toString())) {
            Ln.e(new IllegalStateException("Invalid key url"));
            return null;
        }
        try {
            KeyObject key = getBoundKey(calendarMeeting.getEncryptionKeyUrl());
            if (key == null) {
                Ln.e("Could not get key to decrypt content");
                return null;
            }

            String plainSubject = CryptoUtils.decryptFromJwe(key, calendarMeeting.getSubject());
            String plainLocation = CryptoUtils.decryptFromJwe(key, calendarMeeting.getLocation());
            calendarMeeting.setSubject(plainSubject);
            calendarMeeting.setLocation(plainLocation);
            calendarMeeting.setEncrypted(false);
        } catch (Exception e) {
            Ln.e(e, "An error occurred decrypting meeting payload");
        }

        return calendarMeeting;
    }
}
