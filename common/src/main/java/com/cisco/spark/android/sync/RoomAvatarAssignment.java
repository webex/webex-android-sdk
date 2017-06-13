package com.cisco.spark.android.sync;

import android.text.TextUtils;

import com.cisco.spark.android.model.Content;
import com.cisco.spark.android.model.File;
import com.cisco.spark.android.model.KeyObject;
import com.cisco.spark.android.model.Provider;
import com.cisco.spark.android.util.CryptoUtils;

import java.io.IOException;
import java.text.ParseException;

public class RoomAvatarAssignment extends Message {
    private Content object;

    public RoomAvatarAssignment(ActorRecord.ActorKey actorKey, Content object, Provider provider) {
        setActorKey(actorKey);
        setProvider(provider);
        this.object = object;
    }

    @Override
    public void encrypt(KeyObject key) throws IOException {
        if (!object.getFiles().getItems().isEmpty()) {
            File file = object.getFiles().getItems().get(0);
            if (!TextUtils.isEmpty(file.getScr())) {
                file.setScr(CryptoUtils.encryptToJwe(key, file.getScr()));
            }
        }
        super.encrypt(key);
    }

    @Override
    public void decrypt(KeyObject key) throws IOException, ParseException, NullPointerException {
        if (!object.getFiles().getItems().isEmpty()) {
            File file = object.getFiles().getItems().get(0);
            if (!TextUtils.isEmpty(file.getScr())) {
                file.setScr(CryptoUtils.decryptFromJwe(key, file.getScr()));
            }
        }
        super.decrypt(key);
    }
}
