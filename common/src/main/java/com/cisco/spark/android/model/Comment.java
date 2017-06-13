package com.cisco.spark.android.model;

import com.cisco.spark.android.util.CryptoUtils;
import com.cisco.spark.android.util.Strings;

import java.io.IOException;
import java.text.ParseException;

public class Comment extends ActivityObject implements Mentionable {
    private ItemCollection<Person> mentions;

    public Comment() {
        super(ObjectType.comment);
    }

    public Comment(String message) {
        this();
        setDisplayName(message);
    }

    @Override
    public void encrypt(KeyObject key) throws IOException {
        super.encrypt(key);

        if (Strings.notEmpty(getDisplayName())) {
            setDisplayName(CryptoUtils.encryptToJwe(key, getDisplayName()));
        }
    }

    @Override
    public void decrypt(KeyObject key) throws IOException, ParseException, NullPointerException {
        super.decrypt(key);

        if (Strings.notEmpty(getDisplayName())) {
            setDisplayName(CryptoUtils.decryptFromJwe(key, getDisplayName()));
        }
    }

    @Override
    public ItemCollection<Person> getMentions() {
        return mentions;
    }

    public void setMentions(ItemCollection<Person> mentions) {
        this.mentions = mentions;
    }
}
