package com.cisco.spark.android.whiteboard;

import android.net.Uri;
import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.cisco.spark.android.model.KeyManager;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.whiteboard.persistence.model.ChannelImage;
import com.cisco.spark.android.whiteboard.persistence.model.Content;
import com.github.benoitdion.ln.Ln;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import rx.Subscriber;

public class WhiteboardEncryptor {

    private static final long SECONDS_TO_WAIT_FOR_BOUND_KEY = 10;
    private KeyManager keyManager;

    public WhiteboardEncryptor(KeyManager keyManager) {
        this.keyManager = keyManager;
    }

    @Nullable
    @CheckResult
    public List<Content> encryptContent(List<Content> contents, Uri keyUrl) {

        if (keyUrl == null) {
            Ln.e("No key provided");
            return null;
        }

        KeyObject keyObject = getBoundKey(keyUrl);
        if (keyObject == null) {
            Ln.e("No key provided");
            return null;
        } else if (keyObject.getKey() == null) {
            Ln.e("Key provided was invalid");
            return null;
        }

        List<Content> encryptedContent = new ArrayList<>();
        for (Content content : contents) {

            String encryptedData;
            try {
                encryptedData = CryptoUtils.encryptToJwe(keyObject, content.getPayload());
            }  catch (IOException e) {
                Ln.e(e, "Could not encrypt whiteboard payload: %s", e.getMessage());
                continue;
            }

            content.setPayload(encryptedData);
            content.setEncryptionKeyUrl(keyUrl.toString());
            encryptedContent.add(content);
        }

        return encryptedContent;
    }

    @Nullable
    public String encrypt(String data, Uri keyUrl) {

        if (keyUrl == null) {
            Ln.e("Missing keyUrl");
            return null;
        }

        KeyObject keyObject = getBoundKey(keyUrl);
        if (keyObject == null) {
            Ln.e("No key provided");
            return null;
        }

        try {
            return CryptoUtils.encryptToJwe(keyObject, data);
        } catch (IOException e) {
            Ln.e(e, "Could not encrypt whiteboard payload: %s", e.getMessage());
            return null;
        }
    }

    private KeyObject getBoundKey(Uri keyUrl) {
        if (keyUrl == null) {
            return null;
        }
        final AtomicReference<KeyObject> keyObjectReference = new AtomicReference<>(null);
        final CountDownLatch latch  = new CountDownLatch(1);
        keyManager.getBoundKeySync(keyUrl).subscribe(new Subscriber<KeyObject>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                latch.countDown();
            }

            @Override
            public void onNext(KeyObject keyObject) {
                keyObjectReference.set(keyObject);
                latch.countDown();
            }
        });

        try {
            if (!latch.await(SECONDS_TO_WAIT_FOR_BOUND_KEY, TimeUnit.SECONDS)) {
                Ln.e("Timed out waiting %d seconds for key for URL %s", SECONDS_TO_WAIT_FOR_BOUND_KEY, keyUrl.toString());
                return null;
            }
        } catch (InterruptedException e) {
            Ln.e(e, "Interrupted while waiting for key for URL %s", keyUrl.toString());
            return null;
        }

        return keyObjectReference.get();
    }

    public String decryptContent(Content content) {
        String data = content.getPayload();
        String encryptionKeyUrl = (content.getEncryptionKeyUrl() != null ? content.getEncryptionKeyUrl() : "");
        return decryptContent(data, encryptionKeyUrl);
    }

    public String decryptContent(String encryptedData, String encryptionKeyUrl) {
        String decryptedData = "";
        if (TextUtils.isEmpty(encryptionKeyUrl)) {
            Ln.e(new IllegalStateException("Invalid key url"));
            return encryptedData;
        }
        try {
            KeyObject key = getBoundKey(Uri.parse(encryptionKeyUrl));
            if (key == null) {
                Ln.e("Could not get key to decrypt content");
                return decryptedData;
            }
            decryptedData = CryptoUtils.decryptFromJwe(key, encryptedData);
        } catch (JsonSyntaxException e) {
            // this was apparently not the encrypted object we were looking for
            Ln.e("JSON error decrypting payload", e);
        } catch (Exception e) {
            // do something smart here
            Ln.e("Unknown error decrypting payload", e);
        }
        return decryptedData;
    }


    public ChannelImage decryptBackgroundImageContent(Content content) {
        ChannelImage backgroundImage = content.getBackgroundImage();
        String encryptionKeyUrl = (content.getEncryptionKeyUrl() != null ? content.getEncryptionKeyUrl() : "");
        return decryptBackgroundImageContent(backgroundImage, encryptionKeyUrl);
    }

    private ChannelImage decryptBackgroundImageContent(ChannelImage backgroundImage, String encryptionKeyUrl) {
        if (TextUtils.isEmpty(encryptionKeyUrl)) {
            return null;
        }

        try {
            KeyObject keyObject = getBoundKey(Uri.parse(encryptionKeyUrl));
            if (keyObject == null) {
                Ln.e("Could not get key to decrypt background image");
                return null;
            }
            backgroundImage.decrypt(keyObject);
            return backgroundImage;
        } catch (Exception e) {
            Ln.e("Unable to decrypt background image", e);
            return null;
        }
    }
}
